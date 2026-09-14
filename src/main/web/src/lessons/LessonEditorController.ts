import type { DocumentSnapshot } from '../document/protocol';
import { MonacoWorkspaceService } from '../monaco/MonacoWorkspaceService';
import type { LessonEditorCommand, LessonEditorRange, LessonFile, LessonSession } from './protocol';

type LessonDocument = { file: LessonFile; uri: string };

export class LessonEditorController {
  private activeUri: string | null = null;
  private previousUri: string | null = null;
  private readonly documentsByUri = new Map<string, LessonDocument>();
  private commandGeneration = 0;
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

  setPresentationReadyHandler(handler: ((ready: boolean) => void) | null): void { this.presentationReadyHandler = handler; }

  cancelAnimation(): void {
    this.commandGeneration++;
    this.service.cancelLessonTyping();
    this.presentationReadyHandler?.(false);
  }

  openWorkspace(session: LessonSession): DocumentSnapshot[] {
    this.exit();

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
    else this.apply(session.commands);
  }

  apply(commands: LessonEditorCommand[]): void {
    if (!this.activeUri) return;
    this.cancelAnimation();
    const uri = this.activeUri;
    this.documentsByUri.forEach(document => {
      this.service.setLessonPracticeIntelligence(document.uri, false);
      this.service.setEphemeralReadOnly(document.uri, true);
    });
    const generation = this.commandGeneration;
    void this.executeCommands(uri, commands, generation);
  }

  enterPractice(): void {
    if (!this.activeUri) return;

    this.cancelAnimation();

    const active = this.documentsByUri.get(this.activeUri);
    if (!active) return;

    this.service.clearEphemeralDecorations(this.activeUri);

    this.service.updateLessonFile(
      this.activeUri,
      active.file.starterCode
    );

    this.documentsByUri.forEach(document => {
      this.service.setEphemeralReadOnly(
        document.uri,
        document.file.readOnly
      );

      this.service.setLessonPracticeIntelligence(
        document.uri,
        true
      );
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
    this.documentsByUri.clear();
    this.service.disposeLessonWorkspace(uris);
    if (previousUri) this.service.activate(previousUri);
  }

  private lessonUriForSession(session: LessonSession, file: LessonFile): string {
    return `lesson://${encodeURIComponent(session.lessonId)}/${encodeURIComponent(session.sessionId)}/${encodeURIComponent(file.id)}`;
  }

  private async executeCommands(uri: string, commands: LessonEditorCommand[], generation: number): Promise<void> {
    for (const command of commands) {
      if (generation !== this.commandGeneration || this.activeUri !== uri) return;
      if (command.type === 'SET_CODE') this.service.setEphemeralModelValue(uri, command.code ?? '');
      if (command.type === 'ANIMATE_EDIT') {
        const finished = await this.service.animateEphemeralEdit(uri, command.range!, command.replacementText!, command.finalCode!, command.cadenceMillis!);
        if (!finished) return;
      }
      if (command.type === 'HIGHLIGHT_RANGE' && command.range) this.service.setEphemeralDecorations(uri, [command.range]);
      if (command.type === 'REVEAL_RANGE') this.service.revealEphemeralRange(uri, command.range as LessonEditorRange);
      if (command.type === 'CLEAR_HIGHLIGHTS') this.service.clearEphemeralDecorations(uri);
    }
    if (generation === this.commandGeneration && this.activeUri === uri) this.presentationReadyHandler?.(true);
  }
}
