package com.eyecode.command;

public interface Command {

    String getName();

    boolean isEnabled();

    void execute();
}
