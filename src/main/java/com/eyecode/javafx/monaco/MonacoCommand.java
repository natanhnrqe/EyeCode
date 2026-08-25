package com.eyecode.javafx.monaco;

public sealed interface MonacoCommand permits MonacoCommand.OpenModel,
        MonacoCommand.ActivateModel, MonacoCommand.UpdateModel, MonacoCommand.CloseModel,
        MonacoCommand.SetReadOnly, MonacoCommand.RevealPosition, MonacoCommand.Focus,
        MonacoCommand.ApplyEdit {

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
}
