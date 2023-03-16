package com.epam.rd.autotask.threads.batchprocessor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.function.UnaryOperator;

/**
 *
 */
public class BatchJobProcessingSubsystem<T> {
    // place your code here

    public BatchJobProcessingSubsystem(Collection<T> initial, List<UnaryOperator<T>> functions) {
        // place your code here
    }

    public List<BlockingQueue<T>> getState() {
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
     * @see BatchJobProcessingSubsystem#stop()
     */
    public Optional<Collection<? extends T>> getOutput() {
        // place your code here
        return null;
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
