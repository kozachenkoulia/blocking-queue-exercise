package com.epam.rd.autotask.threads.batchprocessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.epam.rd.autotask.threads.batchprocessor.Util.*;
import static org.junit.jupiter.api.Assertions.*;

class BatchJobProcessingSubsystemTest {
    static final Logger log = LoggerFactory.getLogger(BatchJobProcessingSubsystemTest.class);

    private static final String FAIL_MESSAGE = "Something went wrong";
    private static final long DEFAULT_TIMEOUT = 10L;
    private static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.SECONDS;
    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(DEFAULT_TIMEOUT);
    static UnaryOperator<Plate> washFunc = newPlateFunction(1, 5);
    static UnaryOperator<Plate> dryFunc = newPlateFunction(1, 5);
    static UnaryOperator<Plate> putFunc = newPlateFunction(1, 5);
    static List<UnaryOperator<Plate>> functions = List.of(washFunc, dryFunc, putFunc);
    static ExecutorService pool = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("poll");
            thread.setUncaughtExceptionHandler(eh);
            return thread;
        }
    });
    static Thread.UncaughtExceptionHandler eh = (t, e) -> {
        throw new RuntimeException("You must choose appropriate method to add and get " +
                    "jobs in 'Operation'. " + t.toString(), e);
    };
//            fail("You must choose appropriate method to add and get " +
//                    "jobs in 'Operation'", e);

    @BeforeEach
    void startUp() {
        try {
            pool.submit(() -> {/* empty job to start a thread in the pool */}).get();
        } catch (InterruptedException | ExecutionException e) {
            fail(FAIL_MESSAGE);
        }
    }

    private static UnaryOperator<Plate> newPlateFunction(int origin, int bound) {
        return plate -> {
            String from = Thread.currentThread().getName() + ": processing from - " + plate;
            plate.nextState();
            Util.randomWait(origin, bound);
            log.debug("{}, to - {}", from, plate);
            return plate;
        };
    }

    public static Stream<Arguments> casesConstructorShouldThrow() {
        return Stream.of(
                Arguments.of(NullPointerException.class, null, List.of(washFunc)),
                Arguments.of(NullPointerException.class, init(1), null),
                Arguments.of(IllegalArgumentException.class, init(1), List.of()),
                Arguments.of(IllegalArgumentException.class, new LinkedBlockingQueue<>(), List.of(washFunc))
        );
    }

    @ParameterizedTest
    @MethodSource("casesConstructorShouldThrow")
    void testConstructorShouldThrow(Class<RuntimeException> ex,
                                    BlockingQueue<Plate> queue,
                                    List<UnaryOperator<Plate>> func) {
        assertThrows(ex, () -> new BatchJobProcessingSubsystem<>(queue, func));
    }

    @Test
    void testConstructorShouldNotProcess() {
        BlockingQueue<Plate> sQueue = init(5);
        int activeCount = Thread.activeCount();
        assertTimeoutPreemptively(DEFAULT_DURATION,
                () -> new BatchJobProcessingSubsystem<>(sQueue, functions),
                "It must not perform all work in the constructor");
        long actual = Thread.activeCount();
        assertEquals(activeCount + 1, actual,
                "Threads must not be started until 'start()' " +
                        "method is called");
    }

    @Test
    void testStart() {
        BlockingQueue<Plate> sQueue = init(5);
        try {
            int activeCount = Thread.activeCount();
            BatchJobProcessingSubsystem<Plate> processor = pool.submit(
                            () -> new BatchJobProcessingSubsystem<>(sQueue, functions))
                    .get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            long actual = Thread.activeCount();
            assertEquals(activeCount, actual,
                    "Threads must not be started until 'start()' " +
                            "method is called");

            assert processor != null;
            activeCount = Thread.activeCount();
            log.debug("Active count: {}", (activeCount + 3));
            pool.submit(processor::start).get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            actual = Thread.activeCount();
            log.debug("Actual count: {}", actual);
            assertEquals(activeCount + 3, actual,
                    "Threads must not be started until 'start()' " +
                            "method is called");
        } catch (InterruptedException e) {
            fail(FAIL_MESSAGE);
        } catch (ExecutionException e) {
            fail("Constructor must not throw any exceptions for correct parameters.");
        } catch (TimeoutException e) {
            fail("It must not perform all work in start() method");
        }
    }


    @Test
    void testExplicitStopCall() {
        BlockingQueue<Plate> sQueue = init(5);
        List<UnaryOperator<Plate>> funcs = List.of(
                newPlateFunction(20, 25),
                newPlateFunction(20, 25),
                newPlateFunction(20, 25),
                newPlateFunction(20, 25),
                newPlateFunction(20, 25)
        );
        try {
            int activeCount = Thread.activeCount();
            BatchJobProcessingSubsystem<Plate> processor =
                    pool.submit(() -> new BatchJobProcessingSubsystem<>(sQueue, funcs))
                            .get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            long actual = Thread.activeCount();
            assertEquals(activeCount, actual,
                    "Threads must not be started until 'start()' " +
                            "method is called");

            assert processor != null;
            log.debug("Active count: {}", (activeCount + 3));
            pool.submit(processor::start).get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            activeCount = Thread.activeCount();
            assertTrue(processor.getOutput().isEmpty(),
                    "getOutput() must return non-empty Optional " +
                            "only if it is stopped or all jobs are processed. " +
                            "The stop() was not called explicitly and not all " +
                            "jobs are processed.");
            pause(20);
            actual = Thread.activeCount();
            assertEquals(activeCount, actual,
                    "getOutput() must stop processor " +
                            "only if all jobs are processed.\n" +
                            "If you see 'java.lang.IllegalStateException: Queue full', " +
                            "you probably chose the wrong method for adding or " +
                            "getting jobs to queues.");
            pool.submit(processor::stop).get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            assertTrue(processor.getOutput().isPresent(),
                    "getOutput() must return non-empty Optional " +
                            "only if it is stopped or all jobs are processed. " +
                            "The stop() was called explicitly.");
        } catch (InterruptedException e) {
            fail(FAIL_MESSAGE, e);
        } catch (ExecutionException e) {
            fail("Constructor must not throw any exceptions for correct parameters.");
        } catch (TimeoutException e) {
            fail("It must not perform all work in start() method");
        }
    }

    @Test
    void testExplicitStopWithOneFunction() {
        int size = 5;
        BlockingQueue<Plate> sQueue = init(size);
        List<UnaryOperator<Plate>> funcs = List.of(
                newPlateFunction(20, 25)
        );
        try {
            BatchJobProcessingSubsystem<Plate> processor =
                    pool.submit(() -> new BatchJobProcessingSubsystem<>(sQueue, funcs))
                            .get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);

            assert processor != null;
            pool.submit(processor::start).get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            pause(20);
            pool.submit(processor::stop).get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            Optional<Collection<? extends Plate>> result = processor.getOutput();
            assertTrue(result.isPresent(),
                    "getOutput() must return non-empty Optional " +
                            "only if it is stopped or all jobs are processed. " +
                            "The stop() was called explicitly.");

            List<BlockingQueue<Plate>> state = processor.getState();
            int sourceSize = state.get(0).size();
            int destSize = state.get(1).size();
            pause(50);
            assertTrue(size - 1 <= (sourceSize + destSize),
                    "Sum of jobs count in source (" + sourceSize +
                            ") and dest (" + destSize + ") must equals " +
                            size);
        } catch (InterruptedException e) {
            fail(FAIL_MESSAGE);
        } catch (ExecutionException e) {
            fail("Constructor must not throw any exceptions for correct parameters.");
        } catch (TimeoutException e) {
            fail("It must not perform all work in start() method");
        }
    }

    @Test
    void testStopIfAllJobsProcessed() {
        BlockingQueue<Plate> sQueue = init(5);
        List<UnaryOperator<Plate>> funcs = List.of(
                newPlateFunction(1, 2),
                newPlateFunction(1, 2),
                newPlateFunction(1, 2),
                newPlateFunction(1, 2),
                newPlateFunction(1, 2)
        );
        try {
            int activeCount = Thread.activeCount();
            BatchJobProcessingSubsystem<Plate> processor =
                    pool.submit(() -> new BatchJobProcessingSubsystem<>(sQueue, funcs))
                            .get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            long actual = Thread.activeCount();
            assertEquals(activeCount, actual,
                    "Threads must not be started until 'start()' " +
                            "method is called");

            assert processor != null;
            log.debug("Active count: {}", (activeCount + 3));
            pool.submit(processor::start).get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
//            activeCount = Thread.activeCount();
            boolean sourceIsEmpty;
            boolean destIsFull;
            boolean outputIsEmpty;
            /*true*/
            do {
                pause(1);
                List<BlockingQueue<Plate>> state = processor.getState();
                sourceIsEmpty = state.get(0).size() == 0;
                destIsFull = state.get(1).remainingCapacity() == 0;
                outputIsEmpty = processor.getOutput().isEmpty();
            } while (outputIsEmpty && !(sourceIsEmpty && destIsFull));
            assertFalse(outputIsEmpty);
            assertTrue(sourceIsEmpty && destIsFull);
            actual = Thread.activeCount();
            assertEquals(activeCount, actual,
                    "getOutput() must stop processor " +
                            "when all jobs are processed.");
            assertIterableEquals(sQueue, processor.getOutput().get());
            log.debug("{}", sQueue);
            log.debug("{}", processor.getOutput().get());
        } catch (InterruptedException e) {
            fail(FAIL_MESSAGE, e);
        } catch (ExecutionException e) {
            fail("Constructor must not throw any exceptions for correct parameters.");
        } catch (TimeoutException e) {
            fail("It must not perform all work in start() method");
        }
    }

    @Disabled
    @Test
    void testAppropriateMethodGetAddJobs() {
        BlockingQueue<Plate> sQueue = init(5);
        List<UnaryOperator<Plate>> funcs = List.of(
                newPlateFunction(1, 5),
                newPlateFunction(20, 25),
                newPlateFunction(1, 5),
                newPlateFunction(20, 25),
                newPlateFunction(1, 5)
        );
        Thread t = new Thread(() -> {
            BatchJobProcessingSubsystem<Plate> processor = new BatchJobProcessingSubsystem<>(sQueue, funcs);
            processor.start();
            while (processor.getOutput().isEmpty()) {
                pause(10);
            }
            processor.stop();
        });
        t.setUncaughtExceptionHandler(eh);
        t.start();
    }

    @Test
    void testGetState() {
        int size = 3;
        BlockingQueue<Plate> sQueue = init(size);
        BatchJobProcessingSubsystem<Plate> processor =
                new BatchJobProcessingSubsystem<>(Collections.unmodifiableCollection(sQueue), functions);
        List<BlockingQueue<Plate>> state = processor.getState();
        assertNotNull(state);
        BlockingQueue<Plate> source = state.get(0);
        BlockingQueue<Plate> dest = state.get(1);
        assertEquals(size, source.size(),
                "the work must be performed only in operations.");
        assertEquals(0, source.remainingCapacity(),
                "the work must be performed only in operations " +
                        "when processor is started or the capacity of the " +
                        "source queue must bounded.");
        assertTrue(dest.isEmpty(),
                "the work must be performed only in operations.");
        assertEquals(size, dest.remainingCapacity(),
                "the work must be performed only in operations " +
                        "when processor is started or the capacity of the " +
                        "destination queue must bounded.");

        Plate plate = assertDoesNotThrow(source::take);
        assertEquals(Plate.State.DIRTY, plate.getState(),
                "the work must be performed only in operations " +
                        "when processor is started.");
        assertNull(dest.poll(),
                "the work must be performed only when 'processor' is started.");
        assertEquals(1, source.remainingCapacity());
        assertEquals(size, dest.remainingCapacity());
    }

    @Test
    void testCorrectOrderingOfProcessing() {
        List<String> list = new ArrayList<>();
        UnaryOperator<Plate> operator = t -> {
            list.add(t.toString());
            t.nextState();
            return t;
        };
        BatchJobProcessingSubsystem<Plate> processor =
                new BatchJobProcessingSubsystem<>(
                        List.of(new Plate()),
                        List.of(operator, operator, operator, operator));
        processor.start();
        while (processor.getOutput().isEmpty()) {
            pause(10);
        }
        List<String> expected = List.of(
                Plate.State.DIRTY.toString(),
                Plate.State.CLEAN.toString(),
                Plate.State.DRY.toString(),
                Plate.State.ON_SHELF.toString()
        );
        List<String> actual = list.stream().map(s -> s.split(", ")[1])
                .map(s -> s.replace("}", ""))
                .toList();
        assertIterableEquals(expected, actual);
    }

    @Test
    void testCorrectOrderingOfProcessing2() {
        Thread t = new Thread(() -> {
            BatchJobProcessingSubsystem<String> processor =
                    new BatchJobProcessingSubsystem<>(
                            List.of("A", "B", "C"),
                            List.of(
                                    s -> "@" + s + "@",
                                    s -> "#" + s + "#",
                                    s -> "_" + s + "_"
                            ));
            processor.start();
            while (processor.getOutput().isEmpty()) {
                pause(10);
            }
            processor.stop();
            Collection<? extends String> actual = processor.getOutput().get();
            List<String> expected = List.of(
                    "_#@A@#_", "_#@B@#_", "_#@C@#_"
            );
            assertIterableEquals(expected, actual);
        });
        t.setUncaughtExceptionHandler(eh);
        t.start();
    }

    @Test
    void testUnmodifiableOutput() {
        BatchJobProcessingSubsystem<String> processor =
                new BatchJobProcessingSubsystem<>(
                        List.of("A", "B", "C"),
                        List.of(s -> "_" + s + "_"));
        processor.start();
        while (processor.getOutput().isEmpty()) {
            pause(1);
        }
        processor.stop();
        Collection<? extends String> actual = processor.getOutput().get();
        assertThrows(UnsupportedOperationException.class, () -> actual.remove("_A_"));
    }

    @Test
    void testUnmodifiableOutput2() {
        BatchJobProcessingSubsystem<String> processor =
                new BatchJobProcessingSubsystem<>(
                        List.of("A", "B", "C"),
                        List.of(s -> "_" + s + "_"));
        processor.start();
        processor.stop();
        processor.getOutput().ifPresent(s ->
                assertThrows(UnsupportedOperationException.class,
                        () -> s.remove("_A_")));
    }
}
