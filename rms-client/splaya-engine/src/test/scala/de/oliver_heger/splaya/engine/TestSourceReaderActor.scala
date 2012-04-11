package de.oliver_heger.splaya.engine

import org.scalatest.junit.JUnitSuite
import org.scalatest.mock.EasyMockSugar
import org.junit.After
import org.junit.Test
import org.easymock.EasyMock
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.BeforeClass
import org.junit.Before
import java.io.IOException
import java.io.OutputStream
import de.oliver_heger.splaya.PlaybackError
import de.oliver_heger.splaya.AudioSource

/**
 * Test class for ''SourceReaderActor''.
 */
class TestSourceReaderActor extends JUnitSuite with EasyMockSugar {
  /** Constant for the prefix of a URI. */
  private val URIPrefix = "file:testFile"

  /** The stream generator. */
  private val streamGenerator = StreamDataGenerator()

  /** Constant for the chunk size used by the tests. */
  private val ChunkSize = 1000 * streamGenerator.blockLen

  /** A mock for the source resolver. */
  private var resolver: SourceResolver = _

  /** A mock for the temporary file factory. */
  private var factory: TempFileFactory = _

  /** The current position in the test source stream. */
  private var streamPosition: Int = 0

  /** The current index of a source stream. */
  private var playlistIndex = 0

  /** The actor to be tested. */
  private var actor: SourceReaderActor = _

  @Before def setUp() {
    Gateway.start()
  }

  @After def tearDown {
    if (actor != null) {
      actor ! Exit
    }
  }

  /**
   * Causes the test actor to exit and waits until its shutdown is complete.
   */
  private def shutdownActor() {
    val exitCmd = new WaitForExit
    if (!exitCmd.shutdownActor(actor)) {
      fail("Actor did not exit!")
    }
    actor = null
  }

  /**
   * Generates a URI for the test stream with the given index.
   * @param index the index
   * @return the URI for this stream
   */
  private def streamURI(index: Int): String = URIPrefix + index

  /**
   * Prepares the resolver mock to resolve a source stream. The stream and its
   * length are specified. The corresponding data object is returned.
   * @param stream the stream
   * @param length the length of the stream
   * @return the data object for the next source stream
   */
  private def prepareStream(stream: InputStream, length: Int): AddSourceStream = {
    val ssrc = mock[StreamSource]
    EasyMock.expect(ssrc.size).andReturn(length).anyTimes()
    EasyMock.expect(ssrc.openStream).andReturn(stream).anyTimes()
    EasyMock.replay(ssrc)
    val plIdx = playlistIndex
    playlistIndex += 1
    val uri = streamURI(plIdx)
    EasyMock.expect(resolver.resolve(uri)).andReturn(ssrc)
    new AddSourceStream(uri, plIdx)
  }

  /**
   * Prepares the resolver mock to resolve a new test source stream. The length
   * of the stream can be specified. A new test stream with this length is
   * generated.
   * @param length the length of the stream
   * @return the data object for the next source stream
   */
  private def prepareStream(length: Int): AddSourceStream =
    prepareStream(streamGenerator.nextStream(length), length)

  /**
   * Prepares a mock for a temporary file. The mock is assigned an output stream
   * which is always returned.
   * @return a tuple with the mock temporary file and the associated stream
   */
  private def prepareTempFile() = {
    val tempFile = mock[TempFile]
    val os = new ByteArrayOutputStream
    EasyMock.expect(tempFile.outputStream()).andReturn(os).anyTimes()
    EasyMock.expect(factory.createFile()).andReturn(tempFile)
    (tempFile, os)
  }

  /**
   * Creates the test actor with default settings.
   */
  private def setUpActor() {
    resolver = mock[SourceResolver]
    factory = mock[TempFileFactory]
    actor = new SourceReaderActor(resolver, factory, ChunkSize)
  }

  /**
   * Tests whether the actor can exit gracefully.
   */
  @Test def testStartAndExit() {
    setUpActor();
    actor.start()
    shutdownActor
  }

  private def installPlaybackActor(): QueuingActor = {
    val qa = new QueuingActor
    qa.start()
    Gateway += Gateway.ActorPlayback -> qa
    qa
  }

  /**
   * Checks whether the specified stream contains the expected test data.
   * @param bos the stream to check
   * @param startIdx the start index in the test stream
   * @param length the length
   */
  private def checkStream(bos: ByteArrayOutputStream, startIdx: Int, length: Int) {
    val content = bos.toByteArray()
    assert(length === content.length)
    val strContent = new String(content)
    assert(streamGenerator.generateStreamContent(startIdx, length) === strContent)
  }

  /**
   * Checks whether the specified stream has the expected content.
   * @param bos the stream to check
   * @param expContent the expected content
   */
  private def checkStream(bos: ByteArrayOutputStream, expContent: String) {
    val content = bos.toByteArray()
    assert(expContent.length === content.length)
    assert(expContent === new String(content))
  }

  /**
   * Tests whether a single source stream is directly copied when it is added to
   * the actor.
   */
  @Test def testAddSingleSource() {
    setUpActor()
    val qa = installPlaybackActor()
    val len = 111
    val src = prepareStream(len)
    val tempData = prepareTempFile()
    EasyMock.expect(tempData._1.delete()).andReturn(true)
    actor.start()
    whenExecuting(factory, resolver, tempData._1) {
      actor ! src
      qa.expectMessage(AudioSource(streamURI(0), 0, len, 0, 0))
      qa.shutdown()
      shutdownActor
      checkStream(tempData._2, 0, len)
    }
  }

  /**
   * Tests whether skip information is taken into account when sending messages
   * to the playback actor.
   */
  @Test def testAddSingleSourceWithSkip() {
    setUpActor()
    val qa = installPlaybackActor()
    val len = 111
    val srcOrg = prepareStream(len)
    val src = AddSourceStream(srcOrg.uri, srcOrg.index, 1000, 2222)
    val tempData = prepareTempFile()
    EasyMock.expect(tempData._1.delete()).andReturn(true)
    actor.start()
    whenExecuting(factory, resolver, tempData._1) {
      actor ! src
      qa.expectMessage(AudioSource(streamURI(0), 0, len, src.skip, src.skipTime))
      qa.shutdown()
      shutdownActor
      checkStream(tempData._2, 0, len)
    }
  }

  /**
   * Tests whether a full chunk of data can be copied.
   */
  @Test def testCopyFullChunk() {
    setUpActor()
    val qa = installPlaybackActor()
    val len1 = ChunkSize - 1
    val len2 = ChunkSize + 100
    val src1 = prepareStream(len1)
    val src2 = prepareStream(len2)
    val tempData1 = prepareTempFile()
    val tempData2 = prepareTempFile()
    actor.start()
    whenExecuting(factory, resolver, tempData1._1, tempData2._1) {
      actor ! src1
      actor ! src2
      qa.expectMessage(AudioSource(streamURI(0), 0, len1, 0, 0))
      qa.expectMessage(AudioSource(streamURI(1), 1, len2, 0, 0))
      qa.expectMessage(tempData1._1)
      qa.expectMessage(tempData2._1)
      qa.shutdown()
      shutdownActor
      checkStream(tempData1._2, 0, ChunkSize)
      checkStream(tempData2._2, ChunkSize, ChunkSize)
    }
  }

  /**
   * Tests whether further data is written after the receiver has processed a
   * chunk.
   */
  @Test def testCopyChunkAfterRead() {
    setUpActor()
    val qa = installPlaybackActor()
    val len1 = ChunkSize + 100
    val len2 = ChunkSize + 200
    val src1 = prepareStream(len1)
    val src2 = prepareStream(len2)
    val tempData1 = prepareTempFile()
    val tempData2 = prepareTempFile()
    val tempData3 = prepareTempFile()
    EasyMock.expect(tempData3._1.delete()).andReturn(true)
    actor.start()
    whenExecuting(factory, resolver, tempData1._1, tempData2._1,
      tempData3._1) {
        actor ! src1
        actor ! src2
        actor ! ReadChunk
        qa.expectMessage(AudioSource(streamURI(0), 0, len1, 0, 0))
        qa.expectMessage(tempData1._1)
        qa.expectMessage(AudioSource(streamURI(1), 1, len2, 0, 0))
        qa.expectMessage(tempData2._1)
        qa.shutdown()
        shutdownActor
        checkStream(tempData1._2, 0, ChunkSize)
        checkStream(tempData2._2, ChunkSize, ChunkSize)
        checkStream(tempData3._2, 2 * ChunkSize, len1 + len2 - 2 * ChunkSize)
      }
  }

  /**
   * Tests whether an error when opening an audio source is detected.
   */
  @Test def testErrorWhenResolving() {
    setUpActor()
    val qa = new QueuingActor
    qa.start()
    Gateway.register(qa)
    val uri = streamURI(1)
    val addsrc = new AddSourceStream(uri, 1)
    val ioex = new RuntimeException("Testexception")
    EasyMock.expect(resolver.resolve(uri)).andThrow(ioex)
    val tempData = prepareTempFile()
    EasyMock.expect(tempData._1.delete()).andReturn(true)
    actor.start()
    whenExecuting(factory, resolver, tempData._1) {
      actor ! addsrc
      qa.nextMessage() match {
        case err: PlaybackError =>
          assert(ioex === err.exception)
          assert(err.fatal === false)
        case _ => fail("Unexpected message!")
      }
      qa.shutdown()
      shutdownActor()
    }
  }

  /**
   * Tests whether a read error is handled correctly.
   */
  @Test def testErrorWhenReading() {
    setUpActor()
    val playback = installPlaybackActor()
    val listener = new QueuingActor
    listener.start()
    Gateway.register(listener)
    val len = actor.BufSize + 222
    val stream = new ExceptionInputStream(
      streamGenerator.generateStreamContent(0, len))
    val src = prepareStream(stream, len + 10)
    val len2 = 333
    val src2 = prepareStream(len2)
    val tempData = prepareTempFile()
    EasyMock.expect(tempData._1.delete()).andReturn(true)
    actor.start()
    whenExecuting(factory, resolver, tempData._1) {
      actor ! src
      actor ! src2
      playback.expectMessage(AudioSource(streamURI(0), 0, len + 10, 0, 0))
      playback.expectMessage(SourceReadError(actor.BufSize))
      listener.nextMessage() match {
        case err: PlaybackError =>
          assert(err.fatal === false)
        case _ => fail("Unexpected message!")
      }
      playback.expectMessage(AudioSource(streamURI(1), 1, len2, 0, 0))
      val expContent = streamGenerator.generateStreamContent(0, actor.BufSize) +
        streamGenerator.generateStreamContent(0, len2)
      listener.shutdown()
      playback.shutdown()
      shutdownActor()
      checkStream(tempData._2, expContent)
    }
    Gateway.unregister(listener)
  }

  /**
   * Tests whether a write error (which is a fatal error) is handled correctly.
   */
  @Test def testWriteError() {
    setUpActor()
    val playback = installPlaybackActor()
    val listener = new QueuingActor
    listener.start()
    Gateway.register(listener)
    val len = ChunkSize
    val src = prepareStream(len)
    val tempFile = mock[TempFile]
    EasyMock.expect(tempFile.outputStream()).andReturn(new OutputStream {
      override def write(x: Int) {
        throw new IOException("Test exception from temp file.")
      }
    })
    EasyMock.expect(factory.createFile()).andReturn(tempFile)
    EasyMock.expect(tempFile.delete()).andReturn(true)
    actor.start()
    whenExecuting(factory, resolver, tempFile) {
      actor ! src
      playback.expectMessage(AudioSource(streamURI(0), 0, len, 0, 0))
      listener.nextMessage() match {
        case err: PlaybackError =>
          assert(err.fatal === true)
        case _ => fail("Unexpected message!")
      }
      listener.shutdown()
      playback.shutdown()
      shutdownActor()
    }
    Gateway.unregister(listener)
  }

  /**
   * Tests whether a playlist end message is correctly processed.
   */
  @Test def testPlaylistEnd() {
    setUpActor()
    val playback = installPlaybackActor()
    val len = 100
    val src = prepareStream(len)
    val temp = prepareTempFile()
    actor.start()
    whenExecuting(factory, resolver, temp._1) {
      actor ! src
      actor ! PlaylistEnd
      playback.expectMessage(AudioSource(streamURI(0), 0, len, 0, 0))
      playback.expectMessage(temp._1)
      playback.expectMessage(PlaylistEnd)
      playback.shutdown()
    }
  }

  /**
   * Tests whether a playlist end message is handled correctly if no data has
   * been written to the current chunk.
   */
  @Test def testPlaylistEndNoChunkData() {
    setUpActor()
    val playback = installPlaybackActor()
    val tempData = prepareTempFile()
    EasyMock.expect(tempData._1.delete()).andReturn(true)
    actor.start()
    whenExecuting(factory, resolver, tempData._1) {
      actor ! PlaylistEnd
      playback.expectMessage(PlaylistEnd)
      playback.shutdown()
      shutdownActor()
    }
  }

  /**
   * Tests that it is not possible to add sources after the end of the playlist.
   */
  @Test def testAddSourceAfterPlaylistEnd() {
    setUpActor()
    val playback = installPlaybackActor()
    val tempData = prepareTempFile()
    EasyMock.expect(tempData._1.delete()).andReturn(true)
    actor.start()
    whenExecuting(factory, resolver, tempData._1) {
      actor ! PlaylistEnd
      actor ! AddSourceStream(streamURI(0), 0, 0, 0)
      playback.expectMessage(PlaylistEnd)
      playback.ensureNoMessages()
      playback.shutdown()
      shutdownActor()
    }
  }

  /**
   * Tests whether sources added after a playlist end source are ignored.
   */
  @Test def testAddSourceAfterSourceIndicatingPlaylistEnd() {
    setUpActor()
    val playback = installPlaybackActor()
    val tempData = prepareTempFile()
    EasyMock.expect(tempData._1.delete()).andReturn(true)
    actor.start()
    whenExecuting(factory, resolver, tempData._1) {
      actor ! new AddSourceStream
      actor ! AddSourceStream(streamURI(0), 0, 0, 0)
      playback.expectMessage(PlaylistEnd)
      playback.ensureNoMessages()
      playback.shutdown()
      shutdownActor()
    }
  }

  /**
   * Tests whether the actor can handle a flush operation.
   */
  @Test def testFlush() {
    setUpActor()
    val qa = installPlaybackActor()
    val len1 = 3 * ChunkSize
    val src1 = prepareStream(len1)
    val len2 = ChunkSize - 5
    val src2 = prepareStream(len2)
    val tempData1 = prepareTempFile()
    val tempData2 = prepareTempFile()
    val tempData3 = prepareTempFile()
    EasyMock.expect(tempData3._1.delete()).andReturn(true)
    actor.start()
    whenExecuting(factory, resolver, tempData1._1, tempData2._1, tempData3._1) {
      actor ! src1
      actor ! new AddSourceStream("someUri", 42)
      actor ! new AddSourceStream("anotherUri", 815)
      actor ! PlaylistEnd
      actor ! FlushPlayer
      actor ! src2
      qa.expectMessage(AudioSource(streamURI(0), 0, len1, 0, 0))
      qa.expectMessage(tempData1._1)
      qa.expectMessage(tempData2._1)
      qa.expectMessage(FlushPlayer)
      qa.expectMessage(AudioSource(streamURI(1), 1, len2, 0, 0))
      qa.shutdown()
      shutdownActor
      checkStream(tempData1._2, 0, ChunkSize)
      checkStream(tempData2._2, ChunkSize, ChunkSize)
      checkStream(tempData3._2, len1, len2)
    }
  }
}