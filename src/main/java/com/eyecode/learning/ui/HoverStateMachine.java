package com.eyecode.learning.ui;

import java.util.Objects;
import java.util.function.LongSupplier;

public final class HoverStateMachine {

    private static final long DEFAULT_SHOW_DELAY_MS = 500L;
    private static final long HIDE_DELAY_MS = 300L;

    private final LongSupplier clock;
    private final long showDelayMs;

    private HoverState state = HoverState.IDLE;
    private String activeKey;
    private long waitingSince = -1L;
    private long hidingSince = -1L;

    public HoverStateMachine() {
        this(DEFAULT_SHOW_DELAY_MS, System::currentTimeMillis);
    }

    public HoverStateMachine(LongSupplier clock) {
        this(DEFAULT_SHOW_DELAY_MS, clock);
    }

    public HoverStateMachine(long showDelayMs) {
        this(showDelayMs, System::currentTimeMillis);
    }

    public HoverStateMachine(long showDelayMs, LongSupplier clock) {
        if (showDelayMs < 0) {
            throw new IllegalArgumentException("showDelayMs must be non-negative");
        }
        this.showDelayMs = showDelayMs;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public HoverState getState() {
        return state;
    }

    public String getActiveKey() {
        return activeKey;
    }

    public void enter(String key) {
        if (key == null || key.isBlank()) {
            if (state == HoverState.WAITING) {
                reset();
            } else if (state == HoverState.HIDING) {
                enterVisible();
            }
            return;
        }

        switch (state) {
            case IDLE -> {
                activeKey = key;
                beginWaiting();
            }
            case WAITING -> restartWaitingIfNeeded(key);
            case HIDING -> {
                activeKey = key;
                enterVisible();
            }
            case VISIBLE, INTERACTING -> activeKey = key;
        }
    }

    public void leave() {
        switch (state) {
            case WAITING -> reset();
            case VISIBLE, INTERACTING -> beginHiding();
            case HIDING -> {
                // preserve the existing hide countdown
            }
            case IDLE -> {
                // nothing to do
            }
        }
    }

    public void setPopupHover(boolean hovering) {
        if (hovering) {
            if (state == HoverState.VISIBLE || state == HoverState.INTERACTING || state == HoverState.HIDING) {
                state = HoverState.INTERACTING;
                hidingSince = -1L;
            }
            return;
        }

        if (state == HoverState.INTERACTING) {
            state = HoverState.VISIBLE;
        }
    }

    public boolean canShow() {
        if (state != HoverState.WAITING || activeKey == null || waitingSince < 0L) {
            return false;
        }

        long elapsed = clock.getAsLong() - waitingSince;
        if (elapsed < showDelayMs) {
            return false;
        }

        state = HoverState.VISIBLE;
        waitingSince = -1L;
        return true;
    }

    public boolean canHide() {
        if (state != HoverState.HIDING || hidingSince < 0L) {
            return false;
        }

        long elapsed = clock.getAsLong() - hidingSince;
        if (elapsed < HIDE_DELAY_MS) {
            return false;
        }

        reset();
        return true;
    }

    public void reset() {
        state = HoverState.IDLE;
        activeKey = null;
        waitingSince = -1L;
        hidingSince = -1L;
    }

    private void beginWaiting() {
        state = HoverState.WAITING;
        waitingSince = clock.getAsLong();
        hidingSince = -1L;
    }

    private void restartWaitingIfNeeded(String key) {
        if (!Objects.equals(activeKey, key)) {
            waitingSince = clock.getAsLong();
        }
        activeKey = key;
        hidingSince = -1L;
    }

    private void enterVisible() {
        state = HoverState.VISIBLE;
        hidingSince = -1L;
    }

    private void beginHiding() {
        state = HoverState.HIDING;
        hidingSince = clock.getAsLong();
    }
}
