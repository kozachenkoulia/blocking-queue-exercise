# Blocking queues

The purpose of this task is to familiarize you with the concepts of blocking
queues and their use for thread interaction.

Duration _1 hour_.

## Description

In this task you need to implement the "_Batch Job Processing Subsystem_",
which in this task will wash the plates. `BatchJobProcessingSubsystem`
takes in the constructor a `source` Collection with objects to process
(_jobs_) and a sequence of functions to be performed for jobs.
Each job from the `source` Collection must be processed
by all functions in the list. The `source` Collection is considered as
a _batch_ and is not changed.

`BatchJobProcessingSubsystem` creates as many `Operation` threads
as functions in the list. Each `Operation` applies only one function
from the list to each object.

Each `Operation` has two queues. It takes a job from `source` queue
and puts it, after applying the given function, in the `destination`
queue, which is the source of the next operation, and so on.

> source --> OP1 --> queue1 --> OP2 --> queue2 --> ... --> OPn --> destination

Proceed the `BatchJobProcessingSubsystem` class that takes in the
constructor a `Collection` of objects to process and `List<UnaryOperator<T>>`.
For each function in the list it (constructor) creates the separate
`Operation` object (see below) and all necessary queues.

The methods of the class:

* `public void start()` \
  starts all `Operation` threads.
* `public void stop()` \
  Stops all threads. The threads must interrupt immediately
  after finishing current processing object. It must wait until 
  all threads are stopped.
* `public Optional<Collection<T>> getOutput()` \
  returns `destinationQueue` of the last operation as immutable Collection
  wrapped by `Optional` if there is no more job to process or the
  operation threads forcibly stopped, otherwise `Optional.empty()`.
  The `destinationQueue` of `BatchJobProcessingSubsystem` is the 
  `destination` queue of the last `Operation`.
* `List<BlockingQueue<T>> getState()` \
  returns a list, that contains the source and destination queues of 
  `BatchJobProcessingSubsystem`. Because it package-private it's 
  guaranteed that the queues will not be changed by other classes.

`Operation` class is a thread. It takes in the constructor the `source` queue,
`destination` queue and one `function`. In general, the processing algorithm is
as follows:

1. take the next job from the `sourceQueue`;
2. apply the given function;
3. put the job in the `destinationQueue`.

The algorithm is processed in a loop until the thread is interrupted.

`Plate` class objects are the jobs to be processed. It has an `id` 
and `state`. You must not change this class. \
In this task each function just calls `Plate#nextState()` for each 
processed job.

### Details

`BatchJobProcessingSubsystem` constructor must throw `NullPointerException`
if any of parameters is null and throw `IllegalArgumentException` if the
source queue or functions list are empty.

`Operation` constructor must throw `NullPointerException` if any of the
parameters is null.

>All queues created by `BatchJobProcessingSubsystem` must 
always be limited in capacity. \
The 'source' and 'destination' queues 
must be limited to the size of the 'source' collection. \
Intermediate queues must be limited to 1/2 the size of the 'source' 
collection.

### Restrictions

You must not use any synchronization objects like locks, synchronized 
keyword etc.
