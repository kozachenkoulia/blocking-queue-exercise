package com.epam.rd.autotask.threads.batchprocessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.epam.rd.autotask.threads.batchprocessor.Util.pause;
import static com.epam.rd.autotask.threads.batchprocessor.Util.randomWait;
import static org.junit.jupiter.api.Assertions.*;

class MessagePreprocessorTest {
    static final Logger log = LoggerFactory.getLogger(MessagePreprocessorTest.class);

    private static final String FAIL_MESSAGE = "Something went wrong";
    private static final long DEFAULT_TIMEOUT = 20L;
    private static final long DEFAULT_OP_TIMEOUT = 1L;
    private static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.SECONDS;
    private static final Duration DEFAULT_DURATION = Duration.ofSeconds(DEFAULT_TIMEOUT);
    public static final String DOUBLE_EOL = "((\r\n)|(\n)){2}";

    static UnaryOperator<String> removeLinks = m -> { pause(DEFAULT_OP_TIMEOUT); return "<" + m + ">"; };
    static UnaryOperator<String> toLower = m -> "#" + m + "#";
    static UnaryOperator<String> removePunctuation = m -> "!" + m + "!";
    static UnaryOperator<String> trimSpaces = m -> "_" + m + "_";
    static UnaryOperator<String> nop = m -> m;
    static List<UnaryOperator<String>> functions =
            List.of(removeLinks, toLower, removePunctuation, trimSpaces);
    static List<String> data = List.of("A", "B", "C", "D", "");

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
        fail("You must choose appropriate method to add and get " +
                "jobs in 'Operation'", e);
        throw new RuntimeException("You must choose appropriate method to add and get " +
                "jobs in 'Operation'. " + t.toString(), e);
    };

    @BeforeEach
    void startUp() {
        try {
            pool.submit(() -> {/* empty job to start a thread in the pool */}).get();
            log.debug("Active count: {}, Running threads: {}", Thread.activeCount(), Thread.getAllStackTraces().keySet());
        } catch (InterruptedException | ExecutionException e) {
            fail(FAIL_MESSAGE);
        }
    }

    public static Stream<Arguments> casesConstructorShouldThrow() {
        return Stream.of(
                Arguments.of(NullPointerException.class, null, List.of(removeLinks)),
                Arguments.of(NullPointerException.class, data, null),
                Arguments.of(IllegalArgumentException.class, data, List.of()),
                Arguments.of(IllegalArgumentException.class, List.of(), List.of(toLower))
        );
    }

    @ParameterizedTest
    @MethodSource("casesConstructorShouldThrow")
    void testConstructorShouldThrow(Class<RuntimeException> ex,
                                    Collection<String> queue,
                                    List<UnaryOperator<String>> func) {
        assertThrows(ex, () -> new MessagePreprocessor(queue, func),
                "Constructor should throw an exception for incorrect parameters");
    }

    @Test
    void testConstructorShouldNotProcess() {
        int activeCount = Thread.activeCount();
        assertTimeoutPreemptively(DEFAULT_DURATION,
                () -> new MessagePreprocessor(data, functions),
                "It must not perform all work in the constructor");
        long actual = Thread.activeCount();
        assertEquals(activeCount + 1, actual,
                "Threads must not be started until 'start()' " +
                        "method is called");
    }

    @Test
    void testStart() {
        try {
            int activeCount = Thread.activeCount();
            MessagePreprocessor processor = pool.submit(
                            () -> new MessagePreprocessor(List.of("A", "B", "C"), functions))
                    .get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            long actual = Thread.activeCount();
            assertEquals(activeCount, actual,
                    "Threads must not be started until 'start()' " +
                            "method is called");

            assert processor != null;
            int beforeStartCount = Thread.activeCount();
            log.debug("Active count: {}", beforeStartCount);
            pool.submit(processor::start).get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            int afterStartCount = Thread.activeCount();
            log.debug("Actual count: {}", afterStartCount);
            assertEquals(beforeStartCount + functions.size(), afterStartCount,
                    "Threads must not be started until 'start()' " +
                            "method is called or probably you finish " +
                            "operation threads without waiting 'empty' message.");
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
        List<UnaryOperator<String>> funcs = List.of(
                newDelayedFunction(removeLinks, 20, 25),
                newDelayedFunction(toLower, 20, 25),
                newDelayedFunction(removePunctuation, 20, 25),
                newDelayedFunction(trimSpaces, 20, 25),
                newDelayedFunction(nop, 20, 25)
        );
        try {
            int activeCount = Thread.activeCount();
            MessagePreprocessor processor =
                    pool.submit(() -> new MessagePreprocessor(data, funcs))
                            .get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            long actual = Thread.activeCount();
            assertEquals(activeCount, actual,
                    "Threads must not be started until 'start()' " +
                            "method is called");

            assert processor != null;
            log.debug("Active count: {}", (activeCount + 3));
            pool.submit(processor::start).get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            activeCount = Thread.activeCount();
            assertTrue(processor.getResult().isEmpty(),
                    "getResult() must return non-empty Optional " +
                            "only if it is stopped or all jobs are processed. " +
                            "The stop() was not called explicitly and not all " +
                            "jobs are processed.");
            pause(20);
            actual = Thread.activeCount();
            assertEquals(activeCount, actual,
                    "getResult() must stop processor " +
                            "only if all jobs are processed.\n" +
                            "If you see 'java.lang.IllegalStateException: Queue full', " +
                            "you probably chose the wrong method for adding or " +
                            "getting jobs to queues.");
            pool.submit(processor::stop).get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            assertTrue(processor.getResult().isPresent(),
                    "getResult() must return non-empty Optional " +
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

    private UnaryOperator<String> newDelayedFunction(UnaryOperator<String> f, int origin, int bound) {
        return s -> {
            randomWait(origin, bound);
            return f.apply(s);
        };
    }

    @Test
    void testExplicitStopWithOneFunction() {
        int size = 5;
        List<UnaryOperator<String>> funcs = List.of(
                newDelayedFunction(removeLinks, 20, 25)
        );
        try {
            MessagePreprocessor processor =
                    pool.submit(() -> new MessagePreprocessor(data, funcs))
                            .get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);

            assert processor != null;
            pool.submit(processor::start).get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            pause(15);
            pool.submit(processor::stop).get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            Optional<Collection<? extends String>> result = processor.getResult();
            assertTrue(result.isPresent(),
                    "getResult() must return non-empty Optional " +
                            "only if it is stopped or all jobs are processed. " +
                            "The stop() was called explicitly.");

            List<BlockingQueue<String>> state = processor.getState();
            int sourceSize = state.get(0).size();
            int destSize = state.get(state.size() - 1).size();
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

    @ExtendWith(com.epam.rd.autotask.threads.batchprocessor.TimeoutExceptionHandler.class)
    @Timeout(5)
    @Test
    void testStopIfAllJobsProcessed() {
        List<UnaryOperator<String>> funcs = List.of(
                newDelayedFunction(nop, 1, 2),
                newDelayedFunction(nop, 1, 2),
                newDelayedFunction(nop, 1, 2)
        );
        try {
            int activeCount = Thread.activeCount();
            MessagePreprocessor processor =
                    pool.submit(() -> new MessagePreprocessor(data, funcs))
                            .get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            long actual = Thread.activeCount();
            assertEquals(activeCount, actual,
                    "Threads must not be started until 'start()' " +
                            "method is called");

            assert processor != null;
            log.debug("Active count: {}", (activeCount + 3));
            pool.submit(processor::start).get(DEFAULT_TIMEOUT, DEFAULT_TIMEUNIT);
            waitResult(processor);
            // transforming threads stopped
            actual = Thread.activeCount();
            assertEquals(activeCount, actual,
                    "Tread must stop itself if empty message occurs, " +
                            "expected running threads: " + activeCount +
                            " actual: " + actual);
            log.debug("{}", data);
            log.debug("{}", processor.getResult().get());
        } catch (InterruptedException e) {
            fail(FAIL_MESSAGE, e);
        } catch (ExecutionException e) {
            fail("Constructor must not throw any exceptions for correct parameters.");
        } catch (TimeoutException e) {
            fail("It must not perform all work in start() method");
        }
    }

    @ExtendWith({ExceptionHandler.class, com.epam.rd.autotask.threads.batchprocessor.TimeoutExceptionHandler.class})
    @Timeout(value = 5)
    @Test
    void testAppropriateMethodGetAddJobs() {
        List<UnaryOperator<String>> funcs = List.of(
                newDelayedFunction(removeLinks, 1, 2),
                newDelayedFunction(toLower, 20, 25),
                newDelayedFunction(nop, 1, 2)
        );
        Thread t = new Thread(() -> {
            MessagePreprocessor processor = new MessagePreprocessor(data, funcs);
            processor.start();
            waitResult(processor);
            processor.stop();
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            fail("Unexpected interruption. Probably you don't correctly handle threads interruption.", e);
        }
    }

    @Test
    void testGetState() {
        List<String> list = List.of("A", "A", "A");
        int size = list.size();
        MessagePreprocessor processor =
                new MessagePreprocessor(list, functions);
        List<BlockingQueue<String>> state = processor.getState();
        assertNotNull(state, "MessagePreprocessor must always return own state.");
        BlockingQueue<String> source = state.get(0);
        BlockingQueue<String> dest = state.get(state.size() - 1);
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

        String plate = assertDoesNotThrow(source::take);
        assertEquals("A", plate,
                "the work must be performed only in operations " +
                        "when processor is started.");
        assertNull(dest.poll(),
                "the work must be performed only when 'processor' is started.");
        assertEquals(1, source.remainingCapacity());
        assertEquals(size, dest.remainingCapacity());
    }

    @ExtendWith(com.epam.rd.autotask.threads.batchprocessor.TimeoutExceptionHandler.class)
    @Timeout(5)
    @Test
    void testQueuesCapacity() {
        List<String> list = List.of("A", "A", "");
        int size = list.size();
        MessagePreprocessor processor =
                new MessagePreprocessor(list, List.of(nop, nop, nop));
        List<BlockingQueue<String>> state = processor.getState();
        Iterator<BlockingQueue<String>> it = state.iterator();
        BlockingQueue<String> queue = it.next();
        // src queue
        assertFalse(queue.isEmpty(),
                "Threads must not start until the start() method is called. " +
                        "In this state source queue must be full.");
        assertEquals(0, queue.remainingCapacity(),
                "The source queue must be limited in capacity.");
        // intermediate queues
        queue = it.next();
        assertTrue(queue.isEmpty(),
                "Threads must not start until the start() method is called. " +
                        "In this state intermediate queues must be empty.");
        assertEquals(size / 2, queue.remainingCapacity(),
                "Intermediate queues must be limited in capacity.");
        queue = it.next();
        assertTrue(queue.isEmpty(),
                "Threads must not start until the start() method is called. " +
                        "In this state intermediate queues must be empty.");
        assertEquals(size / 2, queue.remainingCapacity(),
                "Intermediate queues must be limited in capacity.");
        // dst queue
        queue = it.next();
        assertTrue(queue.isEmpty(),
                "Threads must not start until the start() method is called. " +
                        "In this state destination queue must be empty.");
        assertEquals(size, queue.remainingCapacity(),
                "Destination queue must be limited in capacity.");
    }

    @ExtendWith(com.epam.rd.autotask.threads.batchprocessor.TimeoutExceptionHandler.class)
    @Timeout(value = 5)
    @Test
    void testCorrectOrderingOfProcessing2() {
        MessagePreprocessor processor =
                new MessagePreprocessor(
                        List.of("A", "B", "C"),
                        List.of(
                                s -> "@" + s + "@",
                                s -> "#" + s + "#",
                                s -> "_" + s + "_"
                        ));
        processor.start();
        waitResult(processor);
        processor.stop();
        Collection<? extends String> actual = processor.getResult().get();
        List<String> expected = List.of(
                "_#@A@#_", "_#@B@#_", "_#@C@#_"
        );
        assertIterableEquals(expected, actual);
    }

    @ExtendWith(com.epam.rd.autotask.threads.batchprocessor.TimeoutExceptionHandler.class)
    @Timeout(5)
    @Test
    void testUnmodifiableOutput() {
        MessagePreprocessor processor =
                new MessagePreprocessor(
                        List.of("A", "B", "C", ""),
                        List.of(s -> "_" + s + "_"));
        processor.start();
        waitResult(processor);
        processor.stop();
        Collection<? extends String> actual = processor.getResult().get();
        assertThrows(UnsupportedOperationException.class, () -> actual.remove("_A_"));
    }

    // test correctness of message transforming by operations
    @Timeout(5)
    @ExtendWith(TimeoutExceptionHandler.class)
    @Test
    void testTransforming() {
        try {
            String text = Files.readString(Path.of("src/test/resources/data.txt"));
            List<String> messages = new ArrayList<>(List.of(text.split(DOUBLE_EOL)));
            messages.add("");

            MessagePreprocessor preprocessor = new MessagePreprocessor(messages,
                        List.of(Demo.removeLinks, Demo.toLower, Demo.removePunctuation, Demo.trimSpaces));
            preprocessor.start();
            waitResult(preprocessor);
            Collection<? extends String> actual = preprocessor.getResult().get();
            String exp = Files.readString(Path.of("src/test/resources/exp.txt"));
            List<String> expected = new ArrayList<>(List.of(exp.split(DOUBLE_EOL)));
            expected.add("");
            assertIterableEquals(expected, actual);

        } catch (IOException e) {
            fail("Something went wrong. You must not change tests and their resources.", e);
        }catch (NullPointerException e) {
            fail("Probably you didn't implement operations", e);
        }
    }

    private static void waitResult(MessagePreprocessor processor){
        log.info("Start wait the result");
        while (processor.getResult().isEmpty()) {
            pause(1);
        }
        log.info("Finish wait the result");
    }
}
