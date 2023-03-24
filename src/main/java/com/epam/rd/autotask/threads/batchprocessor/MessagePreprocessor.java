package com.epam.rd.autotask.threads.batchprocessor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.function.UnaryOperator;

/**
 *
 */
public class MessagePreprocessor {

    // place your code here

    public MessagePreprocessor(Collection<String> initial, List<UnaryOperator<String>> functions) {
        // place your code here
    }

    List<BlockingQueue<String>> getState() {
        // place your code here
        return null;
    }

    /**
     * Starts all operations in separate threads.
     */
    public void start() {
        // place your code here
    }

    /**
     * @return Optional.empty() if not all jobs have been processed
     * and processor isn't forcibly stopped, otherwise it stops all
     * running operations and returns a collection of processed jobs
     * @see MessagePreprocessor#stop()
     */
    public Optional<Collection<? extends String>> getResult() {
        // place your code here
        return Optional.empty();
    }


    /**
     * Stops all threads. The threads must interrupt immediately
     * after finishing current processing object.<br>
     * It must ensure that all threads are stopped.
     */
    public void stop() {
        // place your code here
    }
}
