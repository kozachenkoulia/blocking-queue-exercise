package com.epam.rd.autotask.threads.batchprocessor;

import java.util.concurrent.BlockingQueue;
import java.util.function.UnaryOperator;

public class Transformer extends Thread {
    // place your code here

    public Transformer(UnaryOperator<String> op,
                       BlockingQueue<String> sourceQueue,
                       BlockingQueue<String> destinationQueue) {
        // place your code here
    }

    @Override
    public void run() {
        // place your code here
    }
}
