package com.eyecode.runtime;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.LongValue;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.StepRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Captures a didactic post-execution trace of a lesson program using JDI.
 *
 * <p>The service compiles the lesson sources with debug information ({@code -g})
 * in its own temporary directory, launches the real main class in a separate
 * virtual machine, steps through it line by line and records the local
 * variables observed at each step. The normal lesson run in {@link RunService}
 * is never touched: the trace is a second, instrumented execution of the same
 * real program.</p>
 *
 * <p>Safety limits: wall-clock timeout, maximum steps, maximum variables per
 * step, maximum array elements, maximum object fields, maximum captured text
 * and output volume. A trace that hits a limit reports it honestly through
 * {@link LessonTrace#status()} and {@link LessonTrace#limitations()}.</p>
 */
public final class LessonTraceService implements AutoCloseable {

    public interface Listener {
        void onTrace(LessonTrace trace);
    }

    static final long DEFAULT_TIMEOUT_MS = 10_000;
    static final int DEFAULT_MAX_STEPS = 500;
    private static final int MAX_VARIABLES_PER_STEP = 24;
    private static final int MAX_ARRAY_ELEMENTS = 32;
    private static final int MAX_OBJECT_FIELDS = 24;
    private static final int MAX_TEXT_LENGTH = 200;
    private static final int MAX_OUTPUT_CHARS = 64_000;

    private final long timeoutMs;
    private final int maxSteps;
    private final ExecutorService executor;
    private volatile boolean closed;
    private volatile ActiveRun active;

    public LessonTraceService() {
        this(DEFAULT_TIMEOUT_MS, DEFAULT_MAX_STEPS);
    }

    public LessonTraceService(long timeoutMs, int maxSteps) {
        this.timeoutMs = Math.max(500, timeoutMs);
        this.maxSteps = Math.max(1, maxSteps);
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "eyecode-lesson-trace");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized boolean trace(LessonRunRequest request, Listener listener) {
        if (closed || request == null || listener == null || active != null) {
            return false;
        }
        ActiveRun run = new ActiveRun(request, listener);
        active = run;
        try {
            executor.execute(run);
        } catch (RejectedExecutionException exception) {
            active = null;
            return false;
        }
        return true;
    }

    public synchronized void stop() {
        ActiveRun run = active;
        if (run == null) {
            return;
        }
        run.stopRequested = true;
        VirtualMachine machine = run.vm;
        if (machine != null) {
            try {
                machine.exit(1);
            } catch (RuntimeException ignored) {
            }
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        stop();
        executor.shutdownNow();
        active = null;
    }

    private final class ActiveRun implements Runnable {
        private final LessonRunRequest request;
        private final Listener listener;
        volatile boolean stopRequested;
        volatile VirtualMachine vm;

        private ActiveRun(LessonRunRequest request, Listener listener) {
            this.request = request;
            this.listener = listener;
        }

        @Override
        public void run() {
            LessonTrace trace;
            try {
                trace = execute();
            } catch (Throwable failure) {
                trace = new LessonTrace(request.mainClass(), LessonTraceStatus.FAILED, -1,
                        failure.getMessage() == null ? failure.toString() : failure.getMessage(),
                        -1, List.of(), List.of(), List.of("Falha ao capturar o traço: " + failure));
            } finally {
                synchronized (LessonTraceService.this) {
                    if (active == this) {
                        active = null;
                    }
                }
            }
            try {
                listener.onTrace(trace);
            } catch (RuntimeException ignored) {
            }
        }

        private LessonTrace execute() throws Exception {
            Path root = Files.createTempDirectory("eyecode-lesson-trace-");
            try {
                Path classes;
                try {
                    classes = compile(root);
                } catch (CompileFailure failure) {
                    List<LessonTrace.Chunk> output = new ArrayList<>();
                    for (String line : failure.compilerOutput.split("\n", -1)) {
                        if (!line.isBlank()) {
                            output.add(new LessonTrace.Chunk(line + "\n", true));
                        }
                    }
                    return new LessonTrace(request.mainClass(), LessonTraceStatus.COMPILE_ERROR, -1, null,
                            -1, List.of(), output, List.of("O programa não compilou, então não há execução para capturar."));
                }
                return traceExecution(classes);
            } finally {
                deleteRecursively(root);
            }
        }

        private Path compile(Path root) throws IOException, InterruptedException {
            Path sources = root.resolve("src");
            Path classes = root.resolve("classes");
            List<String> javaSources = new ArrayList<>();
            for (LessonRunRequest.SourceFile file : request.files()) {
                Path target = safeSourcePath(sources, file.name());
                Files.createDirectories(target.getParent());
                Files.writeString(target, file.source(), StandardCharsets.UTF_8);
                if (target.toString().endsWith(".java")) {
                    javaSources.add(target.toString());
                }
            }
            if (javaSources.isEmpty()) {
                throw new IllegalStateException("Lesson workspace has no Java source files");
            }
            Files.createDirectories(classes);
            List<String> command = new ArrayList<>(List.of("javac", "-encoding", "UTF-8", "-g",
                    "-d", classes.toString()));
            command.addAll(javaSources);
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String compilerOutput = readFully(process.getInputStream());
            int code = process.waitFor();
            if (code != 0) {
                throw new CompileFailure(compilerOutput.isBlank()
                        ? "A compilação da lição falhou." : compilerOutput);
            }
            return classes;
        }

        private LessonTrace traceExecution(Path classes) {
            List<LessonTrace.Chunk> outputChunks = new CopyOnWriteArrayList<>();
            AtomicInteger outputUsed = new AtomicInteger(0);
            List<Thread> drainers = new ArrayList<>();
            List<LessonTrace.Step> steps = new ArrayList<>();
            List<String> limitations = new ArrayList<>();
            CaptureState capture = new CaptureState(limitations);
            String runtimeError = null;
            int runtimeErrorLine = -1;
            LessonTraceStatus status = LessonTraceStatus.COMPLETED;
            boolean sawVmDeath = false;
            VirtualMachine machine = null;
            Process process = null;
            try {
                LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
                Map<String, Connector.Argument> arguments = connector.defaultArguments();
                Connector.Argument main = arguments.get("main");
                Connector.Argument options = arguments.get("options");
                if (main == null || options == null) {
                    throw new IllegalStateException("O conector de depuração não aceita os argumentos necessários.");
                }
                main.setValue(request.mainClass());
                options.setValue("-classpath \"" + classes + "\"");
                Connector.Argument suspend = arguments.get("suspend");
                if (suspend instanceof Connector.BooleanArgument suspendArgument) {
                    suspendArgument.setValue(true);
                }
                try {
                    machine = connector.launch(arguments);
                } catch (IOException | IllegalConnectorArgumentsException | VMStartException launchFailure) {
                    limitations.add("Não foi possível iniciar a execução instrumentada: "
                            + launchFailure.getMessage() + ".");
                    return new LessonTrace(request.mainClass(), LessonTraceStatus.FAILED, -1,
                            "Não foi possível iniciar a execução instrumentada", -1, List.of(), outputChunks, limitations);
                }
                vm = machine;
                process = machine.process();
                drainers.add(startDrainer(process.getInputStream(), false, outputChunks, outputUsed));
                drainers.add(startDrainer(process.getErrorStream(), true, outputChunks, outputUsed));

                ClassPrepareRequest prepare = machine.eventRequestManager().createClassPrepareRequest();
                prepare.addClassFilter(request.mainClass());
                prepare.enable();
                machine.resume();

                long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
                boolean terminated = false;
                while (!terminated) {
                    if (stopRequested) {
                        status = LessonTraceStatus.STOPPED;
                        terminated = true;
                        break;
                    }
                    long remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
                    if (remaining <= 0) {
                        status = LessonTraceStatus.TIMEOUT;
                        terminated = true;
                        break;
                    }
                    EventSet set;
                    try {
                        set = machine.eventQueue().remove((int) Math.min(Integer.MAX_VALUE, remaining));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (RuntimeException disconnected) {
                        break;
                    }
                    if (set == null) {
                        continue;
                    }
                    for (Event event : set) {
                        if (event instanceof StepEvent stepEvent) {
                            if (request.mainClass().equals(stepEvent.location().declaringType().name())) {
                                LessonTrace.Step captured = captureStep(stepEvent.thread(), stepEvent.location(), capture);
                                if (captured != null) {
                                    steps.add(captured);
                                }
                            }
                            if (steps.size() >= maxSteps) {
                                status = LessonTraceStatus.TRUNCATED;
                                limitations.add("Traço truncado: o limite de " + maxSteps
                                        + " passos foi atingido antes do fim do programa.");
                                terminated = true;
                                break;
                            }
                        } else if (event instanceof ClassPrepareEvent prepared) {
                            ThreadReference mainThread = prepared.thread();
                            Method mainMethod = prepared.referenceType().methodsByName("main")
                                    .stream().findFirst().orElse(null);
                            Location entryLocation = null;
                            if (mainMethod != null) {
                                try {
                                    List<Location> locations = mainMethod.allLineLocations();
                                    if (!locations.isEmpty()) {
                                        entryLocation = locations.get(0);
                                    }
                                } catch (AbsentInformationException missing) {
                                    entryLocation = null;
                                }
                            }
                            if (entryLocation == null) {
                                capture.limitation("Não foi possível localizar o método main da lição para iniciar a captura.");
                            } else {
                                BreakpointRequest entry = machine.eventRequestManager()
                                        .createBreakpointRequest(entryLocation);
                                entry.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                                entry.enable();
                            }
                            ExceptionRequest exceptions = machine.eventRequestManager()
                                    .createExceptionRequest(null, false, true);
                            exceptions.addThreadFilter(mainThread);
                            exceptions.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                            exceptions.enable();
                        } else if (event instanceof BreakpointEvent entry) {
                            ThreadReference thread = entry.thread();
                            LessonTrace.Step captured = captureStep(thread, entry.location(), capture);
                            if (captured != null) {
                                steps.add(captured);
                            }
                            StepRequest stepRequest = machine.eventRequestManager()
                                    .createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_OVER);
                            stepRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                            stepRequest.enable();
                            machine.eventRequestManager()
                                    .deleteEventRequests(machine.eventRequestManager().breakpointRequests());
                        } else if (event instanceof ExceptionEvent exception) {
                            if (runtimeError == null) {
                                runtimeError = describeException(exception);
                                Location location = exception.location();
                                runtimeErrorLine = location == null ? -1 : location.lineNumber();
                            }
                        } else if (event instanceof VMDeathEvent) {
                            sawVmDeath = true;
                            terminated = true;
                            break;
                        } else if (event instanceof VMDisconnectEvent) {
                            terminated = true;
                            break;
                        } else if (event instanceof VMStartEvent) {
                            // launched above; nothing to do
                        }
                    }
                    if (!terminated) {
                        set.resume();
                    }
                }
            } catch (RuntimeException failure) {
                if (stopRequested) {
                    status = LessonTraceStatus.STOPPED;
                } else {
                    status = LessonTraceStatus.FAILED;
                    limitations.add("Falha ao capturar o traço: " + failure.getMessage());
                }
            } finally {
                if (machine != null) {
                    if (!sawVmDeath) {
                        try {
                            machine.exit(1);
                        } catch (RuntimeException ignored) {
                        }
                    }
                    try {
                        machine.dispose();
                    } catch (RuntimeException ignored) {
                    }
                }
            }

            int exitCode = -1;
            if (process != null) {
                try {
                    if (process.waitFor(5, TimeUnit.SECONDS)) {
                        exitCode = process.exitValue();
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            for (Thread drainer : drainers) {
                try {
                    drainer.join(1_000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (stopRequested && !sawVmDeath && status == LessonTraceStatus.COMPLETED) {
                status = LessonTraceStatus.STOPPED;
            }
            if (runtimeError != null) {
                limitations.add(0, "Erro de execução: " + runtimeError
                        + (runtimeErrorLine > 0 ? " (linha " + runtimeErrorLine + ")" : "") + ".");
            }
            if (runtimeError != null && status == LessonTraceStatus.COMPLETED) {
                status = LessonTraceStatus.RUNTIME_ERROR;
            } else if (runtimeError == null && status == LessonTraceStatus.COMPLETED && exitCode != 0) {
                status = LessonTraceStatus.FAILED;
            }
            return new LessonTrace(request.mainClass(), status, exitCode, runtimeError,
                    runtimeErrorLine, steps, outputChunks, limitations);
        }

        private String describeException(ExceptionEvent event) {
            try {
                ObjectReference reference = event.exception();
                if (reference == null) {
                    return "Exceção não identificada";
                }
                String type = reference.referenceType().name();
                for (Method method : reference.referenceType().methodsByName("getMessage")) {
                    if (method.argumentTypeNames().isEmpty()) {
                        Value message = reference.invokeMethod(event.thread(), method,
                                List.of(), ObjectReference.INVOKE_SINGLE_THREADED);
                        if (message instanceof StringReference text && !text.value().isBlank()) {
                            return type + ": " + text.value();
                        }
                        break;
                    }
                }
                return type;
            } catch (RuntimeException | InvalidTypeException | ClassNotLoadedException
                     | IncompatibleThreadStateException | com.sun.jdi.InvocationException failure) {
                return event.exception() == null ? "Exceção não identificada"
                        : event.exception().referenceType().name();
            }
        }

        private LessonTrace.Step captureStep(ThreadReference thread, Location location, CaptureState capture) {
            int line = location.lineNumber();
            if (line < 0) {
                return null;
            }
            capture.beginStep();
            try {
                StackFrame frame = thread.frame(0);
                List<LocalVariable> visible;
                try {
                    visible = new ArrayList<>(frame.visibleVariables());
                } catch (AbsentInformationException missing) {
                    capture.limitation("Sem tabela de variáveis locais: não é possível capturar valores neste passo.");
                    return new LessonTrace.Step(capture.nextIndex(), location.declaringType().name(), line, List.of());
                }
                visible.sort(Comparator.comparing(LocalVariable::name));
                int count = Math.min(visible.size(), MAX_VARIABLES_PER_STEP);
                List<LessonTrace.Variable> variables = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    LocalVariable variable = visible.get(index);
                    try {
                        variables.add(describe(variable.name(), variable.typeName(),
                                frame.getValue(variable), 0, capture));
                    } catch (RuntimeException unavailable) {
                        variables.add(new LessonTrace.Variable(variable.name(), variable.typeName(),
                                "valor indisponível", "unsupported", List.of(), false));
                    }
                }
                if (visible.size() > MAX_VARIABLES_PER_STEP) {
                    capture.limitation("Passo truncado: apenas as primeiras " + MAX_VARIABLES_PER_STEP
                            + " variáveis foram capturadas.");
                }
                return new LessonTrace.Step(capture.nextIndex(), location.declaringType().name(), line, variables);
            } catch (RuntimeException | IncompatibleThreadStateException frameUnavailable) {
                capture.limitation("Um passo não pôde ser capturado: frame indisponível.");
                return null;
            }
        }

        private LessonTrace.Variable describe(String name, String declaredType, Value value, int depth,
                                              CaptureState capture) {
            if (value == null) {
                return new LessonTrace.Variable(name, declaredType, "null", "null", List.of(), false);
            }
            if (value instanceof StringReference string) {
                return text(name, declaredType, string.value(), "string", '"');
            }
            if (value instanceof BooleanValue bool) {
                return new LessonTrace.Variable(name, declaredType, String.valueOf(bool.value()),
                        "boolean", List.of(), false);
            }
            if (value instanceof CharValue character) {
                return text(name, declaredType, String.valueOf(character.value()), "char", '\'');
            }
            if (value instanceof IntegerValue number) {
                return new LessonTrace.Variable(name, declaredType, String.valueOf(number.value()),
                        "primitive", List.of(), false);
            }
            if (value instanceof LongValue number) {
                return new LessonTrace.Variable(name, declaredType, String.valueOf(number.value()),
                        "primitive", List.of(), false);
            }
            if (value instanceof ShortValue number) {
                return new LessonTrace.Variable(name, declaredType, String.valueOf(number.value()),
                        "primitive", List.of(), false);
            }
            if (value instanceof ByteValue number) {
                return new LessonTrace.Variable(name, declaredType, String.valueOf(number.value()),
                        "primitive", List.of(), false);
            }
            if (value instanceof DoubleValue number) {
                return new LessonTrace.Variable(name, declaredType, String.valueOf(number.value()),
                        "primitive", List.of(), false);
            }
            if (value instanceof FloatValue number) {
                return new LessonTrace.Variable(name, declaredType, String.valueOf(number.value()),
                        "primitive", List.of(), false);
            }
            if (value instanceof ArrayReference array) {
                int length = array.length();
                boolean truncated = length > MAX_ARRAY_ELEMENTS;
                List<LessonTrace.Variable> children = new ArrayList<>();
                if (depth < 2) {
                    int count = Math.min(length, MAX_ARRAY_ELEMENTS);
                    for (int index = 0; index < count; index++) {
                        Value element = null;
                        try {
                            element = array.getValue(index);
                        } catch (RuntimeException ignored) {
                        }
                        children.add(describe("[" + index + "]", componentType(declaredType), element,
                                depth + 1, capture));
                    }
                }
                return new LessonTrace.Variable(name, declaredType,
                        declaredType + " · comprimento " + length, "array", children, truncated);
            }
            if (value instanceof ObjectReference reference) {
                String typeName = reference.referenceType().name();
                if (depth >= 1) {
                    return new LessonTrace.Variable(name, declaredType, typeName, "reference", List.of(), false);
                }
                if (!capture.visit(reference)) {
                    return new LessonTrace.Variable(name, declaredType, typeName + " (referência repetida)",
                            "reference", List.of(), false);
                }
                List<Field> fields = reference.referenceType().allFields().stream()
                        .filter(field -> !field.isStatic() && !field.isSynthetic())
                        .sorted(Comparator.comparing(Field::name))
                        .toList();
                boolean truncated = fields.size() > MAX_OBJECT_FIELDS;
                List<LessonTrace.Variable> children = new ArrayList<>();
                int count = Math.min(fields.size(), MAX_OBJECT_FIELDS);
                for (int index = 0; index < count; index++) {
                    Field field = fields.get(index);
                    Value fieldValue = null;
                    try {
                        fieldValue = reference.getValue(field);
                    } catch (RuntimeException ignored) {
                    }
                    children.add(describe(field.name(), field.typeName(), fieldValue, depth + 1, capture));
                }
                return new LessonTrace.Variable(name, typeName, typeName, "object", children, truncated);
            }
            return new LessonTrace.Variable(name, declaredType, value.type() == null ? "?" : value.type().name(),
                    "unsupported", List.of(), false);
        }

        private LessonTrace.Variable text(String name, String declaredType, String raw, String kind, char quote) {
            boolean truncated = raw.length() > MAX_TEXT_LENGTH;
            String value = quote + (truncated ? raw.substring(0, MAX_TEXT_LENGTH) : raw) + quote;
            return new LessonTrace.Variable(name, declaredType, value, kind, List.of(), truncated);
        }

        private Thread startDrainer(InputStream stream, boolean error, List<LessonTrace.Chunk> chunks,
                                    AtomicInteger used) {
            Thread thread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (used.addAndGet(line.length() + 1) > MAX_OUTPUT_CHARS) {
                            chunks.add(new LessonTrace.Chunk("…saída truncada…", error));
                            break;
                        }
                        chunks.add(new LessonTrace.Chunk(line + "\n", error));
                    }
                } catch (IOException ignored) {
                }
            }, "eyecode-lesson-trace-output");
            thread.setDaemon(true);
            thread.start();
            return thread;
        }
    }

    private static final class CompileFailure extends RuntimeException {
        private final String compilerOutput;

        private CompileFailure(String compilerOutput) {
            super(compilerOutput);
            this.compilerOutput = compilerOutput;
        }
    }

    private static final class CaptureState {
        private final List<String> limitations;
        private final Set<ObjectReference> visited = new HashSet<>();
        private int index;

        private CaptureState(List<String> limitations) {
            this.limitations = limitations;
        }

        void beginStep() {
            visited.clear();
        }

        boolean visit(ObjectReference reference) {
            return visited.add(reference);
        }

        int nextIndex() {
            return index++;
        }

        void limitation(String message) {
            if (!limitations.contains(message)) {
                limitations.add(message);
            }
        }
    }

    private static Path safeSourcePath(Path root, String name) {
        Path relative = Path.of(name).normalize();
        if (relative.isAbsolute() || relative.startsWith("..") || relative.getNameCount() == 0) {
            throw new IllegalArgumentException("Invalid lesson source path");
        }
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid lesson source path");
        }
        return target;
    }

    private static String componentType(String arrayType) {
        if (arrayType == null) {
            return "?";
        }
        return arrayType.endsWith("[]") ? arrayType.substring(0, arrayType.length() - 2) : arrayType;
    }

    private static String readFully(InputStream stream) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        } catch (IOException ignored) {
        }
        return builder.toString();
    }

    private static void deleteRecursively(Path root) {
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
