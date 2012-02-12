package de.oliver_heger.test.actors

import scala.actors.Actor
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

import org.junit.Assert._

/**
 * A test implementation of an actor which stores all messages received in a
 * blocking queue. Clients can query this queue to retrieve the messages in a
 * thread-safe way. This class should be used by unit tests rather than
 * {@code ActorTestImpl} if messages are sent by different threads.
 */
class QueuingActor extends Actor {
  /** The queue for storing messages. */
  val queue: BlockingQueue[Any] = new LinkedBlockingQueue

  /**
   * Stores the message in the queue.
   */
  def act() {
    while (true) {
      receive {
        case msg => queue.put(msg)
      }
    }
  }

  /**
   * Returns the next message received by this actor. Waits for a certain amount
   * of time if necessary. If a timeout occurs, an assertion error is thrown.
   * @return the next message received by this actor
   */
  def nextMessage(): Any = {
    val msg = queue.poll(5, java.util.concurrent.TimeUnit.SECONDS)
    assertNotNull("No message received!", msg)
    msg
  }

  /**
   * Convenience method for checking whether the expected message was received.
   * @param msg the expected message
   */
  def expectMessage(msg: Any) {
    assertEquals("Wrong message", msg, nextMessage())
  }
}