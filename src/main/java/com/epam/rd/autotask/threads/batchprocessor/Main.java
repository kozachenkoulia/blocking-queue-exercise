package com.epam.rd.autotask.threads.batchprocessor;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class Main {

    static UnaryOperator<Plate> f = p -> {
        String from = Thread.currentThread().getName() + ": processing from - " + p;
        p.nextState();
        System.out.println(from + ", to - " + p);
        return p;
    };

    public static void main(String[] args) throws InterruptedException {
        List<Plate> plates = List.of(new Plate(), new Plate(), new Plate(), new Plate());
        BatchJobProcessingSubsystem<Plate> conveyor = new BatchJobProcessingSubsystem<>(plates, List.of(f, f, f));
        System.out.println("Initial state: " + conveyor.getState());
        conveyor.start();
        while (conveyor.getOutput().equals(Optional.empty())) {
            System.out.println("State: " + conveyor.getState());
            Thread.sleep(1);
        }
        System.out.println(conveyor.getOutput().get());
        System.out.println("Finished state: " + conveyor.getState());
    }
}
