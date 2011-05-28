package de.olix.playa.engine;

import java.util.EventListener;

/**
 * <p>
 * Definition of an interface to be implemented by objects that are interested
 * in change notifications of an <code>{@link AudioBuffer}</code>.
 * </p>
 * <p>
 * This is a typical event listener interface. It allows implementors to receive
 * events when the status of an audio buffer objects changes. It can be used for
 * instance to provide some status information in the GUI of an audio player
 * application.
 * </p>
 *
 * @author Oliver Heger
 * @version $Id$
 */
public interface AudioBufferListener extends EventListener
{
    /**
     * Notifies this listener about a change in the state of a monitored audio
     * buffer. There is only a generic notification method. What exactly
     * happened can be found out by inspecting the properties of the passed in
     * event object.
     *
     * @param event the event describing the changes
     */
    void bufferChanged(AudioBufferEvent event);
}
