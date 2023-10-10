# Text Preparation

The purpose of this exercise is to practice using blocking queues as a common resource for threads while avoiding synchronizing threads explicitly.

Duration: *1 hour 30 minutes*.

## Description

Processing texts in natural language for analysis often requires preliminary preparation of the text. As a rule, this involves several steps. In this task, you will prepare a set of messages received from a social network for analysis – for example, their degree of their aggressiveness. Preparation might involve the following steps:

-   Remove punctuation marks
-   Remove URL
-   Convert characters to lowercase
-   Splitting into tokens
-   Etc.

Preparing a large set of messages can be time consuming if they are processed sequentially – each step involves performing an operation on all messages. For a set of steps, the order in which they are performed must be specified. However, to reduce preparation time, all steps can be performed in parallel. In this case, each step is processed in a separate thread. Each thread takes a message from one queue, performs an operation, and puts the result into another queue. Message queues for threads are organized in such a way that the output queue of the previous step is the input queue for the next step. The output queue of the last step stores fully prepared messages.

For example:


```
source --> OP1 --> queue1 --> OP2 --> queue2 --> ... --> OPn --> destination
```

Here, _source_ is the original collection of text messages, _OP_ refers to operations, and _destination_ is the resulting queue that contains the processed messages.

You are given a description of the following classes:

- The `Transformer` class is a thread and a preprocessing step.
- The `MessagePreprocessor` class describes the process of preparing a set of messages for further analysis. Its task is to create and manage threads that perform preprocessing steps.
- The `Demo` class demonstrates the use of the `MessagePreprocessor` class and defines the operations to be performed as message processing steps.

First, proceed to the `Transformer` class and implement its content:

- `public Transformer(UnaryOperator<String> op, BlockingQueue<String> sourceQueue, BlockingQueue<String> destinationQueue)`  
  Initializes the `Transformer` object by the parameters: op is the operation to be performed on each message; _sourceQueue_ is the incoming message queue; _destinationQueue_ is the queue of processed messages
- `public void run()`  
  Performs the specified operation on each message selected from the source queue and puts the result into the output queue.

Then, proceed to the MessagePreprocessor class and implement its content:
- `public MessagePreprocessor(Collection<String> initial, List<UnaryOperator<String>> functions)`  
  Initializes the `MessagePreprocessor` object using parameters as follows: creates queues that are one more than the number of received operations; creates threads for each received operation; sets the completion flag to `false`.
- `public void start()`  
  Starts all Transformer threads
- `public void stop()`  
  Sends interrupt requests to the `Transformer` threads and, after they stop, sets the completion flag to `true`
- `public Optional<Collection<? extends String>> getResult()`  
  Returns `Optional.empty()` if not all messages have been processed and the `MessagePreprocessor` has not been forcibly stopped by a call to the `stop()` method. Otherwise, it returns the set of processed messages as an immutable collection wrapped in an `Optional`.
- `	List<BlockingQueue<String>> getState()`  
  Returns a list, containing all the `MessagePreprocessor` queues. The method is used for the monitoring purposes of the preprocessing process.

Finally, proceed to the `Demo` class and implement four operations that describe the steps for preprocessing a set of messages:

- `public static UnaryOperator<String> trimSpaces`  
  Replaces one or more whitespace characters with one space.
- `public static UnaryOperator<String> removeLinks`  
  Removes continuous sequences of non-whitespace characters beginning with 'http://' or 'https://'
- `public static UnaryOperator<String> toLower`  
  Converts all the characters of the message to lower case
- `public static UnaryOperator<String> removePunctuation`  
  Removes all characters of the POSIX punctuation character class

> Note: The `main()` method of the `Demo` class has an implementation and demonstrates how to use the `MessagePreprocessor` class.


### Details
- The `Transformer` constructor must throw `NullPointerException` if any of the parameters is `null`.
- The `Transformer` thread runs until it receives an interrupt request or the message 'empty'. This message indicates that all messages have been processed by the thread. The thread must then put the 'empty' message on the output queue and stop.
- The `MessagePreprocessor` constructor must throw a `NullPointerException` if any of the parameters is `null` and throw an `IllegalArgumentException` if any of the parameters are empty.
- The `MessagePreprocessor` constructor considers the following when creating queues:
  - The first queue receives source messages.
  - The last queue is for fully processed messages.
  - The rest queues are an intermediate queues.
  - Each queue must be limited in capacity. The first and last queue must have a capacity equal to the size of the source collection of messages, and the intermediate queues must have a capacity equal to half the size of the source collection.
- In order for threads of the `Transformer` type to interact correctly, the message queues must be one of the types of blocking queues.
- The `stop()` method of the `MessagePreprocessor` class must ensure that all threads are stopped. Threads must interrupt immediately after processing the current message.
- The `getState()` method has package-private access, and the queues are guaranteed not to be modified by other classes.


## Restrictions

You must not use synchronization based on locks or the keyword **synchronized**.

## Example

Suppose you have like the following message:

```
Logback-classic will automatically ask the web-server to install a LogbackServletContainerInitializer (https://logback.qos.ch/apidocs/ch/qos/logback/classic/servlet/LogbackServletContainerInitializer.html) implementing the ServletContainerInitializer interface (available in servlet-api 3.x and later) .
This initializer will in turn install and instance of LogbackServletContextListener (https://logback.qos.ch/apidocs/ch/qos/logback/classic/servlet/LogbackServletContextListener.html) . This listener will stop the current logback-classic context when the web-app is stopped or reloaded.
```

The result of processing it will be as follows:

```
logback classic will automatically ask the web server to install a logbackservletcontainerinitializer implementing the servletcontainerinitializer interface available in servlet api 3 x and later this initializer will in turn install and instance of logbackservletcontextlistener this listener will stop the current logback classic context when the web app is stopped or reloaded
```