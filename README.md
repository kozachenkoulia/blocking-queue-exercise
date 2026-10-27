Java Multithreaded Text Preprocessor

Overview

This project demonstrates multithreaded text preprocessing in Java using BlockingQueue and the producer-consumer pattern.

The application processes text messages through a pipeline of independent threads. Each thread performs a specific transformation and passes the processed message to the next stage.

The main objective is to demonstrate concurrent programming, thread coordination, and safe data exchange between threads without using explicit synchronization mechanisms.

Technologies Used

Java

Java Collections Framework

Java Concurrency API

BlockingQueue

Java Threads

Functional Interfaces

Regular Expressions

Features

The application supports the following text preprocessing operations:

Whitespace Normalization: Replaces multiple whitespace characters with a single space.

URL Removal: Removes URLs beginning with http:// or https://.

Lowercase Conversion: Converts text to lowercase.

Punctuation Removal: Removes punctuation characters from messages.

Multithreaded Processing: Executes preprocessing operations in separate threads.

Thread-Safe Communication: Uses blocking queues to exchange messages between processing stages.

Graceful Thread Termination: Supports thread interruption and termination signals.

Processing Monitoring: Provides access to intermediate queues to monitor processing progress.

Architecture

The application follows a pipeline architecture.

Each preprocessing operation runs in a separate thread and communicates with other threads through blocking queues.

Input Messages -> Source Queue -> Trim Spaces -> Queue 1 -> Remove Links -> Queue 2 -> To Lowercase -> Queue 3 ->
Remove Punctuation -> Result Queue -> Processed Messages

This architecture allows different messages to be processed simultaneously at different stages of the pipeline.

Project Structure

The application consists of three main classes.

Transformer

Represents an individual preprocessing stage.

Responsibilities:

Receives messages from an input BlockingQueue.

Applies a specified text transformation.

Transfers processed messages to an output queue.

Handles thread interruption.

Propagates the termination signal to the next processing stage.

MessagePreprocessor

Manages the complete preprocessing pipeline.

Responsibilities:

Initializes blocking queues.

Creates transformation threads.

Starts and stops the processing pipeline.

Monitors processing progress.

Collects processed messages.

Provides immutable processing results.

Demo

Demonstrates how to configure and execute the preprocessing pipeline.

Defines the following transformation operations:

trimSpaces

removeLinks

toLower

removePunctuation

Example

Input

Hello, WORLD!!! Visit https://github.com for MORE information.

Output

hello world visit for more information

The preprocessing pipeline removes URLs and punctuation, normalizes whitespace, and converts the message to lowercase.

Concurrency Implementation

The project uses Java's BlockingQueue interface to provide thread-safe communication between processing stages.

Each transformation runs independently in its own thread.

The application demonstrates:

Producer-consumer communication.

Thread lifecycle management.

Blocking queue operations.

Thread interruption handling.

Concurrent pipeline processing.

Immutable result collections.

No explicit locks or synchronized blocks are required.

Learning Objectives

This project was developed to practice:

Working with Java threads.

Understanding concurrent programming.

Implementing producer-consumer patterns.

Using blocking queues for thread communication.

Managing thread interruption and termination.

Applying functional interfaces and lambda expressions.

Processing text using regular expressions.

Designing multistage processing pipelines.

Conclusion

This project demonstrates how Java concurrency utilities can be used to build a multithreaded text preprocessing pipeline.

By combining independent transformation threads with blocking queues, the application provides a practical example of thread-safe communication and concurrent data processing.