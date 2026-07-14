package com.eyecode.learning.ui;

import java.util.Objects;
import java.util.function.LongSupplier;

public final class HoverStateMachine {

    private static final long SHOW_DELAY_MS = 500L;
    private static final long HIDE_DELAY_MS = 300L;

    private final LongSupplier clock;

    private HoverState state = HoverState.IDLE;
    private String activeKey;
    private long waitingSince = -1L;
    private long hideSince = -1L;

    public HoverStateMachine() {
        this(System::currentTimeMillis);
    }

    public HoverStateMachine(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public HoverState getState() {
        return state;
    }

    public String getActiveKey() {
        return activeKey;
    }

    public void enter(String key){

    if(key == null){
        reset();
        return;
    }

    if(!Objects.equals(activeKey,key)){

        activeKey = key;
        state = HoverState.WAITING;
        waitingSince = clock.getAsLong();
        hideSince = -1;

        return;
    }

}

    public void leave() {
        if (state == HoverState.WAITING) {
            reset();
            return;
        }

        if (state == HoverState.VISIBLE || state == HoverState.INTERACTING) {
            state = HoverState.VISIBLE;
            if (hideSince < 0L) {
                hideSince = clock.getAsLong();
            }
        }
    }

    public void setPopupHover(boolean hovering) {
        if (state == HoverState.VISIBLE || state == HoverState.INTERACTING) {
            state = hovering ? HoverState.INTERACTING : HoverState.VISIBLE;
            if (hovering) {
                hideSince = -1L;
            }
        }
    }

    public boolean canShow() {
        System.out.println("[HoverStateMachine] canShow:state=" + state + " activeKey=" + activeKey + " waitingSince=" + waitingSince);
        if (state != HoverState.WAITING || activeKey == null || waitingSince < 0L) {
            System.out.println("[HoverStateMachine] canShow:false reason=state-or-key-or-waitingSince");
            return false;
        }

        long elapsed = clock.getAsLong() - waitingSince;
        if (elapsed < SHOW_DELAY_MS) {
            System.out.println("[HoverStateMachine] canShow:false reason=delay-not-complete elapsed=" + elapsed + " required=" + SHOW_DELAY_MS);
            return false;
        }

        System.out.println("[HoverStateMachine] canShow:true elapsed=" + elapsed);
        state = HoverState.VISIBLE;
        waitingSince = -1L;
        hideSince = -1L;
        return true;
    }

    public boolean canHide() {
        if (state != HoverState.VISIBLE || hideSince < 0L) {
            return false;
        }

        if (clock.getAsLong() - hideSince < HIDE_DELAY_MS) {
            return false;
        }

        reset();
        return true;
    }

    public void reset() {
        resetState();
    }

    private void resetState() {
        state = HoverState.IDLE;
        activeKey = null;
        waitingSince = -1L;
        hideSince = -1L;
    }
}
