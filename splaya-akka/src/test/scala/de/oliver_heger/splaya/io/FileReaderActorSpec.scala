package de.oliver_heger.splaya.io

import java.io.{File, FileOutputStream, IOException}
import java.lang
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.charset.StandardCharsets
import java.nio.file.{StandardOpenOption, OpenOption, Files, Path}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

/**
 * Companion object for ''FileReaderActorSpec''.
 */
object FileReaderActorSpec {
  private val TestData = """Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy
                  eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam
                  voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita
                  kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem
                  ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod
                  tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At
                  vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd
                  gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum
                  dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor
                  invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero
                  eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no
                  sea takimata sanctus est Lorem ipsum dolor sit amet."""

  /**
   * Helper method for converting a string to a byte array.
   * @param s the string
   * @return the byte array
   */
  private def toBytes(s: String): Array[Byte] = s.getBytes(StandardCharsets.UTF_8)

  /**
   * Returns a byte array with the complete test data.
   * @return the test bytes
   */
  private def testBytes() = toBytes(TestData)

  /**
   * A specialized ''FileChannelFactory'' implementation that allows access to
   * te channel created later on. This can be used to check whether the
   * channel has been closed correctly.
   */
  private class WrappingFileChannelFactory extends FileChannelFactory with Matchers {
    var createdChannel: AsynchronousFileChannel = _

    override def createChannel(path: Path, options: OpenOption*): AsynchronousFileChannel = {
      options should have length 1
      options.head should be(StandardOpenOption.READ)
      createdChannel = super.createChannel(path)
      createdChannel
    }
  }

  /**
   * A convenience implementation of a file channel factory which helps
   * checking that the created channel has been closed.
   */
  private class VerifyClosedChannelFactory extends WrappingFileChannelFactory with Matchers {
    /**
     * Checks whether the channel has been closed.
     */
    def verifyChannelClosed(): Unit = {
      createdChannel.isOpen should be (right = false)
    }
  }

  /**
   * A specialized test implementation of ''FileChannelFactory'' which always
   * returns the channel passed to the constructor.
   *
   * This is useful for instance to inject mock channel implementations.
   * @param channel the channel to be returned
   */
  private class ConfigurableChannelFactory(channel: AsynchronousFileChannel) extends FileChannelFactory {

    override def createChannel(path: Path, options: OpenOption*): AsynchronousFileChannel = channel
  }

  /**
   * A specialized implementation of ''FileChannelFactory'' which expects
   * multiple requests for creating a channel factory; each request is answered
   * with a configurable factory.
   *
   * An instance is initialized with a list of ''FileChannelFactory''
   * instances. On each request, the next element is removed from the list and
   * asked to create the channel.
   *
   * @param factories the factories to be returned
   */
  private class MultiFileChannelFactory(factories: FileChannelFactory*) extends FileChannelFactory {
    private var factoryList = factories.toList

    /**
     * @inheritdoc Creates the channel by delegating to the first element in
     *             the list of managed factories. Then this element is
     *             removed.
     */
    override def createChannel(path: Path, options: OpenOption*): AsynchronousFileChannel = {
      val currentFactory = factoryList.head
      factoryList = factoryList.tail
      currentFactory.createChannel(path, options: _*)
    }
  }
}

/**
 * Test class for ''FileReaderActor''.
 */
class FileReaderActorSpec(actorSystem: ActorSystem) extends TestKit(actorSystem)
with ImplicitSender with Matchers with FlatSpecLike with BeforeAndAfterAll with MockitoSugar {

  import ChannelHandler._
  import de.oliver_heger.splaya.io.FileReaderActor._
  import de.oliver_heger.splaya.io.FileReaderActorSpec._

  /** Stores the temporary file created by a test case. */
  private lazy val testFile = createDataFile()

  def this() = this(ActorSystem("FileReaderActorSpec"))

  override protected def afterAll(): Unit = {
    system.shutdown()
    system awaitTermination 10.seconds

    Files deleteIfExists testFile
  }

  /**
   * Creates a temporary file with test data.
   * @return the path to the test file
   */
  private def createDataFile(): Path = {
    val file = File.createTempFile("FileReaderActor", "tmp")
    val out = new FileOutputStream(file)
    try {
      out.write(testBytes())
    } finally {
      out.close()
    }
    file.toPath
  }

  /**
   * Returns a ''Props'' object for a file reader actor with the specified
   * channel factory.
   * @param factory the channel factory
   * @return the ''Props'' for the test actor
   */
  private def propsForActorWithFactory(factory: FileChannelFactory): Props =
    Props(classOf[FileReaderActor], factory)

  /**
   * Creates a new reader actor in the test actor system. Optionally, a
   * specialized channel factory can be provided.
   * @return the test reader actor
   */
  private def readerActor(optChannelFactory: Option[FileChannelFactory] = None): ActorRef = {
    if(optChannelFactory.isEmpty) system.actorOf(Props[FileReaderActor])
    else system.actorOf(propsForActorWithFactory(optChannelFactory.get))
  }

  /**
   * Helper method for calling the test actor to read the test file in a single
   * chunk.
   * @param reader the test reader actor
   * @return the result object with the chunk of data read from the file
   */
  private def readTestFile(reader: ActorRef): ReadResult = {
    val BufferSize = 2 * TestData.length
    reader ! ReadData(BufferSize)
    val result = expectMsgType[ReadResult]
    result.data.length should be(BufferSize)
    reader ! ReadData(BufferSize)
    expectMsgType[EndOfFile].path should be(testFile)
    result
  }

  /**
   * Checks the result object obtained from reading the test file with the
   * test read actor.
   * @param result the result object to be checked
   * @return the checked result object
   */
  private def checkTestFileReadResult(result: ReadResult): ReadResult = {
    result.length should be(TestData.length)
    result.data.take(result.length) should be(testBytes())
    result
  }

  "A FileReaderActor" should
    "send EOF message when queried for data in uninitialized state" in {
    val reader = readerActor()
    reader ! ReadData(128)
    expectMsgType[EndOfFile].path should be(null)
  }

  it should "be able to read a file in a single chunk" in {
    val reader = readerActor()
    reader ! InitFile(testFile)
    val result = readTestFile(reader)
    checkTestFileReadResult(result)
  }

  it should "be able to read a file in multiple small chunks" in {
    val BufferSize = TestData.length / 4
    val resultBuffer = ArrayBuffer.empty[Byte]
    val reader = readerActor()
    reader ! InitFile(testFile)
    reader ! ReadData(BufferSize)

    fishForMessage() {
      case ReadResult(data, len) =>
        resultBuffer ++= data take len
        reader ! ReadData(BufferSize)
        false

      case EndOfFile(_) =>
        true
    }
    resultBuffer.toArray should be(testBytes())
  }

  it should "allow reading multiple files in series" in {
    val reader = readerActor()
    reader ! InitFile(testFile)
    readTestFile(reader)

    reader ! InitFile(testFile)
    checkTestFileReadResult(readTestFile(reader))
  }

  it should "close the channel when the file is read" in {
    val channelFactory = new VerifyClosedChannelFactory
    val reader = readerActor(Some(channelFactory))

    reader ! InitFile(testFile)
    readTestFile(reader)
    channelFactory.verifyChannelClosed()
  }

  it should "allow starting a new read operation before the current one is done" in {
    val closedChannelFactory = new VerifyClosedChannelFactory
    val reader = readerActor(Some(new MultiFileChannelFactory(closedChannelFactory, new
        FileChannelFactory)))

    reader ! InitFile(testFile)
    reader ! ReadData(8)
    expectMsgType[ReadResult]

    reader ! InitFile(testFile)
    readTestFile(reader)
    closedChannelFactory.verifyChannelClosed()
  }

  it should "prevent read operations after the channel was closed" in {
    val reader = readerActor()
    reader ! InitFile(testFile)
    readTestFile(reader)

    reader ! ReadData(32)
    expectMsgType[EndOfFile].path should be(null)
  }

  /**
   * Obtains the ''CompletionHandler'' that was passed to a mock file channel.
   * @param mockChannel the mock channel
   * @return the ''CompletionHandler''
   */
  private def fetchCompletionHandler(mockChannel: AsynchronousFileChannel):
  CompletionHandler[Integer, ActorRef] = {
    val argCaptor = ArgumentCaptor.forClass(classOf[CompletionHandler[Integer, ActorRef]])
    verify(mockChannel).read(any(classOf[ByteBuffer]), any(classOf[lang.Long]), any
      (classOf[ActorRef]), argCaptor.capture())
    argCaptor.getValue
  }

  it should "handle results of outdated read operations correctly" in {
    val mockChannel = mock[AsynchronousFileChannel]
    val reader = readerActor(Some(new MultiFileChannelFactory(new ConfigurableChannelFactory
    (mockChannel), new FileChannelFactory)))
    reader ! InitFile(testFile)
    reader ! ReadData(16)

    reader ! InitFile(testFile)
    reader ! ReadData(8)
    expectMsgType[ReadResult]

    verify(mockChannel).close()
    val handler = fetchCompletionHandler(mockChannel)
    handler.completed(16, testActor)
    reader ! ReadData(8)
    expectMsgType[ReadResult].length should be(8)
  }

  /**
   * Helper method for checking whether exceptions passed to the completion
   * handler are processed correctly.
   * @param exCause the exception to be passed to the handler
   * @return the resulting exception
   */
  private def checkExceptionInReadOperation(exCause: Throwable): Throwable = {
    val mockChannel = mock[AsynchronousFileChannel]
    val reader = TestActorRef(propsForActorWithFactory(new ConfigurableChannelFactory(mockChannel)))
    reader receive InitFile(testFile)
    reader receive ReadData(1)

    try {
      reader receive ChannelReadComplete(testActor, 1, exception = Some(exCause))
      fail("Exception not thrown!")
    } catch {
      case ex: Exception => ex
    }
  }

  it should "throw an exception if receiving a message about a failed read operation" in {
    val exCause = new RuntimeException
    checkExceptionInReadOperation(exCause) match {
      case ioEx: IOException =>
        ioEx.getCause should be(exCause)
    }
  }

  it should "not wrap occurring IOExceptions if a read operation fails" in {
    val exCause = new IOException
    checkExceptionInReadOperation(exCause) should be(exCause)
  }

  it should "handle exceptions reported asynchronously by the file channel" in {
    val mockChannel = mock[AsynchronousFileChannel]
    val reader = TestActorRef(propsForActorWithFactory(new ConfigurableChannelFactory(mockChannel)))
    val readerActor = reader.underlyingActor.asInstanceOf[FileReaderActor]
    val handler = readerActor.createCompletionHandler(testActor, new Array[Byte](8))

    val exception = new RuntimeException
    handler.failed(exception, testActor)
    val msg = expectMsgType[ChannelReadComplete]
    msg.exception.get should be(exception)
    msg.target should be(testActor)
  }

  it should "be able to handle a close request if no file is open" in {
    val reader = readerActor()
    reader ! CloseRequest

    expectMsgType[CloseAck].actor should be(reader)
  }

  it should "close the current channel on a close request" in {
    val factory = new VerifyClosedChannelFactory
    val reader = readerActor(Some(factory))
    reader ! InitFile(testFile)

    reader ! CloseRequest
    expectMsgType[CloseAck]
    factory.verifyChannelClosed()
  }

  it should "ignore read results after the channel was closed" in {
    val mockChannel = mock[AsynchronousFileChannel]
    val reader = readerActor(Some(new ConfigurableChannelFactory(mockChannel)))
    reader ! InitFile(testFile)
    reader ! ReadData(8)

    reader ! CloseRequest
    expectMsgType[CloseAck]
    fetchCompletionHandler(mockChannel).completed(8, testActor)
    expectNoMsg(1.second)
  }
}
