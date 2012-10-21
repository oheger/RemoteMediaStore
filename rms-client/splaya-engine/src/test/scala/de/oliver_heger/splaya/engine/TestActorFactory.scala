package de.oliver_heger.splaya.engine

import scala.actors.Actor

import org.apache.commons.lang3.time.StopWatch
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.scalatest.junit.JUnitSuite
import org.scalatest.mock.EasyMockSugar

import de.oliver_heger.splaya.engine.io.TempFileFactory
import de.oliver_heger.splaya.engine.msg.EventTranslatorActor
import de.oliver_heger.splaya.engine.msg.Gateway
import de.oliver_heger.splaya.fs.FSService
import de.oliver_heger.splaya.osgiutil.ServiceWrapper
import de.oliver_heger.splaya.playlist.impl.AudioSourceDataExtractorActor
import de.oliver_heger.splaya.playlist.impl.PlaylistCtrlActor
import de.oliver_heger.splaya.playlist.impl.PlaylistDataExtractorActor
import de.oliver_heger.splaya.playlist.impl.AudioSourceDataExtractor
import de.oliver_heger.splaya.playlist.PlaylistFileStore
import io.SourceStreamWrapperFactory

/**
 * Test class for ''ActorFactory''.
 */
class TestActorFactory extends JUnitSuite with EasyMockSugar {
  /** A test gateway object. */
  private var gateway: Gateway = _

  /** The factory to be tested. */
  private var factory: ActorFactory = _

  @Before def setUp() {
    factory = new ActorFactory {}
  }

  /**
   * Tests whether a source reader actor can be created.
   */
  @Test def testCreateSourceReaderActor() {
    val fsService = new ServiceWrapper[FSService]
    val tempFactory = mock[TempFileFactory]
    assertEquals("Wrong actor", classOf[SourceReaderActor],
      factory.createSourceReaderActor(gateway, fsService, tempFactory, 100).getClass)
  }

  /**
   * Tests whether a playback actor can be created.
   */
  @Test def testCreatePlaybackActor() {
    val ctxFactory = mock[PlaybackContextFactory]
    val streamFactory = mock[SourceStreamWrapperFactory]
    assertEquals("Wrong actor", classOf[PlaybackActor],
      factory.createPlaybackActor(gateway, ctxFactory, streamFactory).getClass)
  }

  /**
   * Tests whether a line actor can be created.
   */
  @Test def testCreateLineActor() {
    assertEquals("Wrong actor", classOf[LineWriteActor],
      factory.createLineActor(gateway).getClass)
  }

  /**
   * Tests whether a timing actor can be created.
   */
  @Test def testCreateTimingActor() {
    val watch = mock[StopWatch]
    assertEquals("Wrong actor", classOf[TimingActor],
      factory.createTimingActor(gateway, watch).getClass)
  }

  /**
   * Tests whether an event translator actor can be created.
   */
  @Test def testCreateEventTranslatorActor() {
    assertEquals("Wrong actor", classOf[EventTranslatorActor],
      factory.createEventTranslatorActor(gateway, 3).getClass)
  }

  /**
   * Tests whether an audio source data extractor actor can be created.
   */
  @Test def testCreateAudioSourceDataExtractorActor() {
    val extr = mock[AudioSourceDataExtractor]
    assertEquals("Wrong class", classOf[AudioSourceDataExtractorActor],
      factory.createAudioSourceDataExtractorActor(extr).getClass)
  }

  /**
   * Tests whether an actor for extracting meta data for a whole playlist can
   * be created.
   */
  @Test def testCreatePlaylistDataExtractorActor() {
    val extrActor = mock[Actor]
    assertEquals("Wrong actor", classOf[PlaylistDataExtractorActor],
      factory.createPlaylistDataExtractorActor(gateway, extrActor).getClass)
  }

  /**
   * Tests whether a playlist controller actor can be created.
   */
  @Test def testCreatePlaylistCtrlActor() {
    val fsService = new ServiceWrapper[FSService]
    val store = mock[PlaylistFileStore]
    val readActor = mock[Actor]
    val plCreateActor = mock[Actor]
    assertEquals("Wrong actor", classOf[PlaylistCtrlActor],
      factory.createPlaylistCtrlActor(gateway, readActor, fsService, store,
        plCreateActor, Set("mp3")).getClass)
  }
}
