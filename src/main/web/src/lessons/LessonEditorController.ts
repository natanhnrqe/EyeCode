import type { DocumentSnapshot } from '../document/protocol';
import { MonacoWorkspaceService } from '../monaco/MonacoWorkspaceService';
import type { LessonEditorCommand, LessonFile, LessonSession, PresentationProgram } from './protocol';

type LessonDocument = { file: LessonFile; uri: string };

export class LessonEditorController {
  private activeUri: string | null = null;
  private previousUri: string | null = null;
  private readonly documentsByUri = new Map<string, LessonDocument>();
  private commandGeneration = 0;
  private practiceStarted = false;
  private presentation: { step: number; index: number; canonicalCode: string } | null = null;
  private presentationReadyHandler: ((ready: boolean) => void) | null = null;

  constructor(private readonly service: MonacoWorkspaceService) {}

  lessonUri(): string | null { return this.activeUri; }

  lessonUriFor(fileId: string): string | null {
    return [...this.documentsByUri.values()].find(document => document.file.id === fileId)?.uri ?? null;
  }

  lessonFiles(): LessonFile[] { return [...this.documentsByUri.values()].map(document => document.file); }

  activeFileId(): string | null { return this.activeUri ? this.documentsByUri.get(this.activeUri)?.file.id ?? null : null; }

  documents(): DocumentSnapshot[] {
    return [...this.documentsByUri.values()].map(({ file, uri }) => ({
      uri,
      displayName: file.name,
      language: file.language,
      content: this.service.ephemeralModelValue(uri) ?? file.starterCode,
      version: 0,
      dirty: false,
      readOnly: file.readOnly,
      kind: 'lesson'
    }));
  }

  practiceSource(): string | null { return this.activeUri ? this.service.ephemeralModelValue(this.activeUri) : null; }

  executionFiles(): { name: string; source: string }[] {
    return [...this.documentsByUri.values()].map(({ file, uri }) => ({
      name: file.name,
      source: this.service.ephemeralModelValue(uri) ?? file.starterCode
    }));
  }

  setPresentationReadyHandler(handler: ((ready: boolean) => void) | null): void { this.presentationReadyHandler = handler; }

  cancelAnimation(): void {
    this.commandGeneration++;
    this.service.cancelLessonTyping();
    this.presentationReadyHandler?.(false);
  }

  openWorkspace(session: LessonSession): DocumentSnapshot[] {
    this.exit();
    this.practiceStarted = false;

    const workspace = session.workspace;
    if (!workspace) return [];

    this.previousUri = this.service.activeModelUri();

    for (const file of workspace.files) {
      const uri = this.lessonUriForSession(session, file);

      this.documentsByUri.set(uri, { file, uri });

      this.service.registerLessonFile(
        uri,
        file.starterCode,
        file.language,
        true
      );
    }

    this.activeUri = this.lessonUriFor(workspace.entryFileId);
    if (this.activeUri) {
      this.service.activateLessonFile(this.activeUri);
    }

    return this.documents();
  }

  applySession(session: LessonSession): void {
    if (session.phase === 'PRACTICE') this.enterPractice();
    else this.applyPresentation(session);
  }

  private applyPresentation(session: LessonSession): void {
    const canonicalCode = session.canonicalCode;
    const program = session.presentationProgram;
    const previous = this.presentation;
    const currentCode = this.service.ephemeralModelValue(this.activeUri ?? '');
    const sequential = canonicalCode !== undefined && program !== undefined && currentCode === program.sourceCode
      && previous !== null && session.navigationDirection !== 'NONE';
    this.presentation = canonicalCode === undefined ? null : {
      step: session.currentStep, index: session.currentPresentation, canonicalCode
    };
    if (!sequential || !canonicalCode) {
      if (canonicalCode) this.materializeCanonical(canonicalCode, session.commands);
      else this.apply(session.commands);
      return;
    }
    this.play(program, session.commands);
  }

  private materializeCanonical(canonicalCode: string, commands: LessonEditorCommand[]): void {
    if (!this.activeUri) return;
    this.cancelAnimation();
    this.documentsByUri.forEach(document => {
      this.service.setLessonPracticeIntelligence(document.uri, false);
      this.service.setEphemeralReadOnly(document.uri, true);
    });
    this.service.setEphemeralModelValue(this.activeUri, canonicalCode);
    this.applyFocusCommands(this.activeUri, commands);
    this.presentationReadyHandler?.(true);
  }

  private play(program: PresentationProgram, commands: LessonEditorCommand[]): void {
    if (!this.activeUri) return;
    this.cancelAnimation();
    const uri = this.activeUri;
    this.documentsByUri.forEach(document => {
      this.service.setLessonPracticeIntelligence(document.uri, false);
      this.service.setEphemeralReadOnly(document.uri, true);
    });
    const generation = this.commandGeneration;
    void this.service.playPresentationProgram(uri, program).then(finished => {
      if (!finished || generation !== this.commandGeneration || this.activeUri !== uri) return;
      this.applyFocusCommands(uri, commands);
      this.presentationReadyHandler?.(true);
    });
  }

  private applyFocusCommands(uri: string, commands: LessonEditorCommand[]): void {
    for (const command of commands) {
      if (command.type === 'HIGHLIGHT_RANGE' && command.range) this.service.setEphemeralDecorations(uri, [command.range]);
      if (command.type === 'REVEAL_RANGE' && command.range) this.service.revealEphemeralRange(uri, command.range);
      if (command.type === 'CLEAR_HIGHLIGHTS') this.service.clearEphemeralDecorations(uri);
    }
  }

  apply(commands: LessonEditorCommand[], canonicalCode?: string): void {
    if (!this.activeUri) return;
    this.cancelAnimation();
    const uri = this.activeUri;
    this.documentsByUri.forEach(document => {
      this.service.setLessonPracticeIntelligence(document.uri, false);
      this.service.setEphemeralReadOnly(document.uri, true);
    });
    this.applyFocusCommands(uri, commands);
    if (canonicalCode && this.service.ephemeralModelValue(uri) !== canonicalCode) {
      this.service.setEphemeralModelValue(uri, canonicalCode);
    }
    this.presentationReadyHandler?.(true);
  }

  enterPractice(): void {
    if (!this.activeUri) return;

    this.cancelAnimation();

    const active = this.documentsByUri.get(this.activeUri);
    if (!active) return;

    this.documentsByUri.forEach(document => this.service.clearEphemeralDecorations(document.uri));

    if (!this.practiceStarted || this.service.ephemeralModelValue(this.activeUri) !== active.file.starterCode) {
      this.service.updateLessonFile(
        this.activeUri,
        active.file.starterCode
      );
      this.practiceStarted = true;
    }

    this.documentsByUri.forEach(document => {
      this.service.setEphemeralReadOnly(
        document.uri,
        document.file.readOnly
      );

      this.service.setLessonPracticeIntelligence(
        document.uri,
        true
      );

      if (!document.file.readOnly && document.file.editableRange) {
        this.service.setEphemeralDecorations(document.uri, [document.file.editableRange]);
      }
    });

    this.service.activateLessonFile(this.activeUri);

    this.service.focus();
  }

  activateLessonFile(uri: string): boolean {
    if (!this.documentsByUri.has(uri)) return false;
    this.activeUri = uri;
    return this.service.activateLessonFile(uri);
  }

  activateFile(fileId: string): string | null {
    const uri = this.lessonUriFor(fileId);
    return uri && this.activateLessonFile(uri) ? uri : null;
  }

  exit(): void {
    this.cancelAnimation();
    if (!this.documentsByUri.size) return;
    const previousUri = this.previousUri;
    const uris = [...this.documentsByUri.keys()];
    this.activeUri = null;
    this.previousUri = null;
    this.practiceStarted = false;
    this.documentsByUri.clear();
    this.service.disposeLessonWorkspace(uris);
    if (previousUri) this.service.activate(previousUri);
  }

  private lessonUriForSession(session: LessonSession, file: LessonFile): string {
    return `lesson://${encodeURIComponent(session.lessonId)}/${encodeURIComponent(session.sessionId)}/${encodeURIComponent(file.id)}`;
  }

}
