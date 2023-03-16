package com.epam.rd.autotask.threads.batchprocessor;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class Plate {
    static AtomicInteger counter = new AtomicInteger();
    enum State {
        DIRTY,
        CLEAN,
        DRY,
        ON_SHELF
    }

    private State state = State.DIRTY;
    private final int id = counter.incrementAndGet();

    public int getId() {
        return id;
    }

    public State getState() {
        return state;
    }

    public void nextState() {
        int ordinal = state.ordinal();
        if (ordinal != State.values().length - 1) {
            state = State.values()[ordinal + 1];
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Plate plate = (Plate) o;
        return id == plate.id && state == plate.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, id);
    }

    @Override
    public String toString() {
        return "{" +
                "id=" + getId() + ", " +
                state + '}';
    }
}
