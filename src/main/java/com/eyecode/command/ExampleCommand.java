package com.eyecode.command;

public class ExampleCommand implements Command {

    private final CommandContext context;

    public ExampleCommand(CommandContext context) {
        this.context = context;
    }

    @Override
    public String getName() {
        return "Example";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void execute() {
        // demonstration only
    }
}
