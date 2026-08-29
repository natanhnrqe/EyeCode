package com.eyecode.javafx.monaco;

public sealed interface MonacoCommand permits MonacoCommand.OpenModel,
        MonacoCommand.ActivateModel, MonacoCommand.UpdateModel, MonacoCommand.CloseModel,
        MonacoCommand.SetReadOnly, MonacoCommand.RevealPosition, MonacoCommand.Focus,
        MonacoCommand.ApplyEdit, MonacoCommand.InsertSnippet, MonacoCommand.CompletionResponse,
        MonacoCommand.ShowOverlay, MonacoCommand.UpdateOverlay, MonacoCommand.HideOverlay,
        MonacoCommand.Layout {

    record OpenModel(String id, String language, String content, boolean readOnly) implements MonacoCommand { }
    record ActivateModel(String id, boolean readOnly) implements MonacoCommand {
        public ActivateModel(String id) { this(id, false); }
    }
    record UpdateModel(String id, String content, long version, String origin) implements MonacoCommand { }
    record CloseModel(String id) implements MonacoCommand { }
    record SetReadOnly(String id, boolean readOnly) implements MonacoCommand { }
    record RevealPosition(String id, int line, int column) implements MonacoCommand { }
    record Focus() implements MonacoCommand { }
    record ApplyEdit(String id, int start, int end, String text) implements MonacoCommand { }
    record InsertSnippet(String id, int start, int end, String snippet) implements MonacoCommand { }
    record CompletionResponse(String id, long requestId, java.util.List<MonacoCompletionItem> items)
            implements MonacoCommand { }
    record ShowOverlay(String overlayId, MonacoOverlayType type, int line, int column,
                       String content, long generation) implements MonacoCommand { }
    record UpdateOverlay(String overlayId, MonacoOverlayType type, int line, int column,
                         String content, long generation) implements MonacoCommand { }
    record HideOverlay(String overlayId, long generation, boolean hard) implements MonacoCommand {
        public HideOverlay(String overlayId, long generation) {
            this(overlayId, generation, false);
        }
    }
    record Layout() implements MonacoCommand { }
}
