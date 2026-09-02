package com.eyecode.terminal;

import com.jediterm.terminal.ProcessTtyConnector;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;

import java.awt.Dimension;
import java.nio.charset.Charset;

public final class PtyProcessTerminalConnector extends ProcessTtyConnector {
    private final PtyProcess process;

    public PtyProcessTerminalConnector(PtyProcess process, Charset charset) {
        super(process, charset);
        this.process = process;
    }

    @Override
    public void resize(Dimension size) {
        if (isConnected()) {
            process.setWinSize(new WinSize(size.width, size.height));
        }
    }

    @Override
    public boolean isConnected() {
        return process.isRunning();
    }

    @Override
    public String getName() {
        return "Local";
    }
}