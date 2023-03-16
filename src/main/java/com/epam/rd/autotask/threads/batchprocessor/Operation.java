package com.epam.rd.autotask.threads.batchprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class Operation<T> extends Thread {
    // place your code here

    public Operation(UnaryOperator<T> op, BlockingQueue<T> from, BlockingQueue<T> to) {
        // place your code here
    }

    @Override
    public void run() {
        // place your code here
    }
}
