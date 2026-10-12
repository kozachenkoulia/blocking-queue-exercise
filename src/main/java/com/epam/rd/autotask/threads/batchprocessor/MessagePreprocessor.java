package com.epam.rd.autotask.threads.batchprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.UnaryOperator;

public class MessagePreprocessor {
    private Collection<String> initial;
    private List<UnaryOperator<String>> functions;
    private List<BlockingQueue<String>> queues;
    private List<Transformer> transformers;
    private boolean isCompleted;

    public MessagePreprocessor(Collection<String> initial, List<UnaryOperator<String>> functions) {
        if (initial == null || functions == null) {
            throw new NullPointerException();
        }
        if (initial.isEmpty() || functions.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.initial = initial;
        this.functions = functions;
        this.queues = new ArrayList<>();
        this.transformers = new ArrayList<>();
        this.isCompleted = false;

        for (int i = 0; i < functions.size() + 1; i++) {
            int capacity;
            if (i == 0 || i == functions.size()) {
                capacity = initial.size();
            } else {
                capacity = initial.size() / 2;
            }
            queues.add(new ArrayBlockingQueue<>(capacity));
        }

        queues.get(0).addAll(initial);

        for (int i = 0; i < functions.size(); i++) {
            Transformer transformer = new Transformer(
                    functions.get(i),
                    queues.get(i),
                    queues.get(i + 1)
            );
            transformers.add(transformer);
        }
    }

    List<BlockingQueue<String>> getState() {
        return List.copyOf(queues);
    }

    public void start() {
        for (Transformer transformer : transformers) {
            transformer.start();
        }
    }

    /**
     * @return Optional.empty() if not all jobs have been processed
     * and processor isn't forcibly stopped, otherwise it stops all
     * running operations and returns a collection of processed jobs
     * @see MessagePreprocessor#stop()
     */
    public Optional<Collection<? extends String>> getResult() {
        BlockingQueue<String> resultQueue = queues.get(queues.size() - 1);
        if (!isCompleted && resultQueue.size() < initial.size()) {
            return Optional.empty();
        }
        return Optional.of(List.copyOf(resultQueue));
    }

    public void stop() {
        for (Transformer transformer : transformers) {
            transformer.interrupt();
        }
        boolean interrupted = false;
        for (Transformer transformer : transformers) {
            while (true) {
                try {
                    transformer.join();
                    break;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        isCompleted = true;
    }
}
