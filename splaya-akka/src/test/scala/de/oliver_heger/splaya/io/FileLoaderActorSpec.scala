package de.oliver_heger.splaya.io

import java.nio.file.{Path, Paths}

import akka.actor._
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import de.oliver_heger.splaya.FileTestHelper
import de.oliver_heger.splaya.io.FileLoaderActor.{FileContent, LoadFile}
import de.oliver_heger.splaya.io.FileReaderActor.{EndOfFile, ReadResult}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

/**
 * Test class for ''FileLoaderActor''.
 */
class FileLoaderActorSpec(testSystem: ActorSystem) extends TestKit(testSystem) with
ImplicitSender with FlatSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter with
FileTestHelper {

  import de.oliver_heger.splaya.FileTestHelper._

  /** A content string for a large test file. */
  private val largeFileContent = createLargeFileContent()

  def this() = this(ActorSystem("FileLoaderActorSpec"))

  override protected def afterAll(): Unit = {
    system.shutdown()
    system awaitTermination 10.seconds
  }

  /**
   * Checks a ''FileContent'' message. (Messages of this type cannot be checked
   * using equals because they contain a byte array.)
   * @param msg the message to be checked
   * @param expPath the expected path
   * @param expContent the expected content array
   * @return the message that has been checked
   */
  private def checkContentMessage(msg: FileContent, expPath: Path, expContent: Array[Byte]):
  FileContent = {
    msg.path should be(expPath)
    msg.content should be(expContent)
    msg
  }

  "A FileLoaderActor" should "load the content of a file" in {
    val file = createDataFile()
    val loader = system.actorOf(Props[FileLoaderActor])

    loader ! LoadFile(file)
    checkContentMessage(expectMsgType[FileContent], file, testBytes())
  }

  /**
   * Creates a content string for a large file whose size is bigger than the
   * chunk size used by the read actors involved.
   * @return the content string for the large test file
   */
  private def createLargeFileContent(): String = {
    val Size = 4096
    val buffer = new StringBuilder(Size + FileTestHelper.TestData.length)
    do {
      buffer append FileTestHelper.TestData
    } while (buffer.length < Size)
    buffer.toString()
  }

  /**
   * Creates a test file with a size that is bigger than the actor's chunk
   * size.
   * @return the path to the newly created file
   */
  private def createLargeFile(): Path = {
    createDataFile(largeFileContent)
  }

  it should "handle multiple load operations in parallel" in {
    val loader = system.actorOf(Props[FileLoaderActor])
    val file1 = createLargeFile()
    val file2 = createDataFile()
    val probe = TestProbe()

    loader.tell(LoadFile(file1), probe.ref)
    loader ! LoadFile(file2)
    checkContentMessage(probe.expectMsgType[FileContent], file1, toBytes(largeFileContent))
    checkContentMessage(expectMsgType[FileContent], file2, testBytes())
  }

  it should "ignore ReadResult messages from unknown read actors" in {
    val loader = TestActorRef(Props[FileLoaderActor])
    loader receive ReadResult(data = testBytes(), length = 42)
  }

  it should "ignore EndOfFile messages from unknown read actors" in {
    val loader = TestActorRef(Props[FileLoaderActor])
    loader receive EndOfFile(createFileReference())
  }

  it should "stop a read actor when a file has been read" in {
    val probe = TestProbe()
    val deathWatchReaderFactory = new TrackReaderActorFactory(new FileReaderActorFactory)
    val file = createDataFile()
    val loader = system.actorOf(Props(classOf[FileLoaderActor], deathWatchReaderFactory))

    loader ! LoadFile(file)
    checkContentMessage(expectMsgType[FileContent], file, testBytes())
    probe watch deathWatchReaderFactory.createdActor
    val termMsg = probe.expectMsgType[Terminated]
    termMsg.actor should be(deathWatchReaderFactory.createdActor)
  }

  it should "send an error message if a load operation fails" in {
    val loader = system.actorOf(Props[FileLoaderActor])

    val path = Paths.get("a non existing path!")
    loader ! LoadFile(path)
    val errMsg = expectMsgType[ChannelHandler.IOOperationError]
    errMsg.path should be(path)
  }

  /**
   * A specialized reader actor factory implementation which keeps track about
   * the file reader actor created. This factory is used to check whether read
   * actors are stopped when they are no longer needed.
   * @param wrappedFactory the wrapped factory
   */
  private class TrackReaderActorFactory(wrappedFactory: FileReaderActorFactory) extends
  FileReaderActorFactory {
    /** The last actor created by this factory. */
    var createdActor: ActorRef = _

    /**
     * Creates a new ''FileReaderActor'' using the specified context object.
     * @param context the ''ActorContext''
     * @return the newly created ''FileReaderActor''
     */
    override def createFileReaderActor(context: ActorContext): ActorRef = {
      createdActor = wrappedFactory createFileReaderActor context
      createdActor
    }
  }

}