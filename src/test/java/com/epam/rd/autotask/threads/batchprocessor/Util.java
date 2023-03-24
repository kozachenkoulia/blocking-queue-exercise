package com.epam.rd.autotask.threads.batchprocessor;

import java.util.Random;

public class Util {
    private Util() {
    }

    static final int SEED = 25;
    static final Random r = new Random(SEED);

    static void randomWait(int origin, int bound) {
        try {
            Thread.sleep(r.nextInt(origin, bound));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
