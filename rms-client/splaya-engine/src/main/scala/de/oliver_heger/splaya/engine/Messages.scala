package de.oliver_heger.splaya.engine;
import javax.sound.sampled.SourceDataLine
import org.slf4j.LoggerFactory

/**
 * A message that indicates that processing should be aborted.
 */
case class Exit() {
  /** The logger. */
  val log = LoggerFactory.getLogger(classOf[Exit])

  /**
   * Records that the specified object has processed the Exit message. This
   * implementation just prints a log message.
   * @param x the affected object
   */
  def confirmed(x: Any) {
    log.info("{} exited.", x)
  }
}

/**
 * A message for adding a stream to be played to the source reader actor.
 * Messages of this type are typically sent by the component which controls the
 * current playlist.
 * @param uri the URI of the audio stream to be played
 * @param index the index of this audio source in the current playlist
 * @param skip the initial skip position
 * @param skipTime the time to be skipped in the beginning of this audio stream
 */
case class AddSourceStream(uri: String, index: Int, skip: Long, skipTime: Long) {
  /**
   * A specialized constructor for creating an instance that does not contain
   * any skip information. This means that the referenced audio stream is to
   * be played directly from start.
   * @param uri the URI of the audio stream to be played
   * @param index the index of this audio source in the current playlist
   */
  def this(uri: String, index: Int) = this(uri, index, 0, 0)

  /**
   * A specialized constructor for creating an instance that does not contain
   * any real data. Such an instance can be used to indicate the end of a
   * playlist.
   */
  def this() = this(null, -1)

  /**
   * A flag whether this instance contains actual data.
   */
  val isDefined = uri != null && index >= 0
}

/**
 * A message which instructs the reader actor to read another chunk copy it to
 * the target location.
 */
case object ReadChunk

/**
 * A message indicating the end of the playlist. After this message was sent to
 * an actor, no more audio streams are accepted.
 */
case object PlaylistEnd

/**
 * A message for writing a chunk of audio data into the specified line.
 * @param line the line
 * @param chunk the array with the data to be played
 * @param len the length of the array (the number of valid bytes)
 * @param currentPos the current position in the source stream
 * @param skipPos the skip position for the current stream
 */
case class PlayChunk(line: SourceDataLine, chunk: Array[Byte], len: Int,
  currentPos: Long, skipPos: Long)

/**
 * A message which indicates that a chunk of audio data was played. The chunk
 * may be written partly; the exact number of bytes written to the data line
 * is contained in the message.
 * @param bytesWritten the number of bytes which has been written
 */
case class ChunkPlayed(bytesWritten: Int)

/**
 * A message sent out by the source reader actor if reading from a source
 * causes an error. This message tells the playback actor that the current
 * source is skipped after this position. Playback can continue with the next
 * source.
 */
case class SourceReadError(bytesRead: Long)

/**
 * A message which tells the playback actor that playback should start now.
 */
case object StartPlayback

/**
 * A message which tells the playback actor that playback should pause.
 */
case object StopPlayback

/**
 * A message which tells the playback actor to skip the currently played audio
 * stream.
 */
case object SkipCurrentSource

/**
 * A message indicating that the audio player is to be flushed. Flushing means
 * that all actors wipe out their current state so that playback can start
 * anew, at a different position in the playlist.
 */
case object FlushPlayer

/**
 * A message to be evaluated by [[de.oliver_heger.splaya.engine.TimingActor]]
 * for performing a specific action and passing in the current time. The timing
 * actor will call the function which is part of the message and passes the
 * current time as argument. That way the current playback time can be accessed
 * in a thread-safe fashion.
 * @param f the function to be invoked with the current playback time
 */
case class TimeAction(f: Long => Unit)