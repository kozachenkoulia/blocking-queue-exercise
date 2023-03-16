package com.epam.rd.autotask.threads.batchprocessor;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

public class Util {
    private Util() {
    }

    static final int SEED = 25;
    static final Random r = new Random(SEED);

    public static String nextString(Random r) {
        return "" + (char) r.nextInt('a', 'z') + (char) r.nextInt('a', 'z');
    }

    static void randomWait(int origin, int bound) {
        try {
            Thread.sleep(r.nextInt(origin, bound));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static BlockingQueue<Plate> init(int limit) {
        BlockingQueue<Plate> q = new LinkedBlockingQueue<>(limit);
        Stream.generate(Plate::new).limit(limit).forEach(q::add);
        return q;
    }


    static void join(String msg, Thread... threads) {
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            fail(msg);
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
