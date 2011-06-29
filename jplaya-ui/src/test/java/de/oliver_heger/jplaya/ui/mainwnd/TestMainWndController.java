package de.oliver_heger.jplaya.ui.mainwnd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.util.Locale;

import net.sf.jguiraffe.di.BeanContext;
import net.sf.jguiraffe.gui.app.Application;
import net.sf.jguiraffe.gui.builder.action.ActionStore;
import net.sf.jguiraffe.gui.builder.action.FormAction;
import net.sf.jguiraffe.gui.builder.window.WindowEvent;

import org.apache.commons.configuration.Configuration;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import de.oliver_heger.jplaya.ui.ConfigurationConstants;
import de.oliver_heger.mediastore.localstore.MediaStore;
import de.olix.playa.engine.AudioPlayer;
import de.olix.playa.engine.AudioReader;
import de.olix.playa.engine.AudioStreamSource;
import de.olix.playa.engine.DataBuffer;
import de.olix.playa.playlist.CurrentPositionInfo;
import de.olix.playa.playlist.FSScanner;
import de.olix.playa.playlist.PlaylistController;
import de.olix.playa.playlist.PlaylistManager;
import de.olix.playa.playlist.PlaylistOrder;

/**
 * Test class for {@code MainWndController}.
 *
 * @author Oliver Heger
 * @version $Id: $
 */
public class TestMainWndController extends EasyMockSupport
{
    /** A mock for the bean context. */
    private BeanContext mockContext;

    /** A mock for the media store. */
    private MediaStore store;

    @Before
    public void setUp() throws Exception
    {
        mockContext = createMock(BeanContext.class);
        store = createMock(MediaStore.class);
    }

    /**
     * Tries to create an instance without a bean context.
     */
    @Test(expected = NullPointerException.class)
    public void testInitNoContext()
    {
        new MainWndController(null, store);
    }

    /**
     * Tries to create an instance without a media store.
     */
    @Test(expected = NullPointerException.class)
    public void testInitNoStore()
    {
        new MainWndController(mockContext, null);
    }

    /**
     * Tests the window activated event. We can only check that the event is not
     * touched.
     */
    @Test
    public void testWindowActivated()
    {
        WindowEvent event = createMock(WindowEvent.class);
        replayAll();
        MainWndController ctrl = new MainWndController(mockContext, store);
        ctrl.windowActivated(event);
        verifyAll();
    }

    /**
     * Tests the window closing event. We can only check that the event is not
     * touched.
     */
    @Test
    public void testWindowClosing()
    {
        WindowEvent event = createMock(WindowEvent.class);
        replayAll();
        MainWndController ctrl = new MainWndController(mockContext, store);
        ctrl.windowClosing(event);
        verifyAll();
    }

    /**
     * Tests the window closed event. We can only check that the event is not
     * touched.
     */
    @Test
    public void testWindowClosed()
    {
        WindowEvent event = createMock(WindowEvent.class);
        replayAll();
        MainWndController ctrl = new MainWndController(mockContext, store);
        ctrl.windowClosed(event);
        verifyAll();
    }

    /**
     * Tests the window deactivated event. We can only check that the event is
     * not touched.
     */
    @Test
    public void testWindowDeactivated()
    {
        WindowEvent event = createMock(WindowEvent.class);
        replayAll();
        MainWndController ctrl = new MainWndController(mockContext, store);
        ctrl.windowDeactivated(event);
        verifyAll();
    }

    /**
     * Tests the window deiconified event. We can only check that the event is
     * not touched.
     */
    @Test
    public void testWindowDeiconified()
    {
        WindowEvent event = createMock(WindowEvent.class);
        replayAll();
        MainWndController ctrl = new MainWndController(mockContext, store);
        ctrl.windowDeiconified(event);
        verifyAll();
    }

    /**
     * Tests the window iconified event. We can only check that the event is not
     * touched.
     */
    @Test
    public void testWindowIconified()
    {
        WindowEvent event = createMock(WindowEvent.class);
        replayAll();
        MainWndController ctrl = new MainWndController(mockContext, store);
        ctrl.windowIconified(event);
        verifyAll();
    }

    /**
     * Tests reaction on the window opened event if there is no configuration
     * data for setting up a playlist.
     */
    @Test
    public void testWindowOpenedNoMediaDir()
    {
        Application app = createMock(Application.class);
        Configuration config = createMock(Configuration.class);
        ActionStore actStore = createMock(ActionStore.class);
        WindowEvent event = createMock(WindowEvent.class);
        EasyMock.expect(mockContext.getBean(Application.BEAN_APPLICATION))
                .andReturn(app);
        EasyMock.expect(app.getUserConfiguration()).andReturn(config);
        EasyMock.expect(config.getString(ConfigurationConstants.PROP_MEDIA_DIR))
                .andReturn(null);
        actStore.enableGroup(MainWndController.ACTGRP_PLAYER, false);
        replayAll();
        MainWndController ctrl = new MainWndController(mockContext, store)
        {
            @Override
            protected void initAudioEngine()
            {
                throw new UnsupportedOperationException(
                        "Unexpected method call!");
            }
        };
        ctrl.setActionStore(actStore);
        ctrl.windowOpened(event);
        verifyAll();
    }

    /**
     * Tests reaction on the window opened event if the directory for media
     * files has been configured. In this case the audio player should be
     * started immediately.
     */
    @Test
    public void testWindowOpenedMediaDir()
    {
        Application app = createMock(Application.class);
        Configuration config = createMock(Configuration.class);
        FSScanner scanner = createMock(FSScanner.class);
        ActionStore actStore = createMock(ActionStore.class);
        FormAction action = createMock(FormAction.class);
        WindowEvent event = createMock(WindowEvent.class);
        final String mediaDir = "R:\\";
        actStore.enableGroup(MainWndController.ACTGRP_PLAYER, false);
        EasyMock.expect(mockContext.getBean(Application.BEAN_APPLICATION))
                .andReturn(app);
        EasyMock.expect(app.getUserConfiguration()).andReturn(config);
        EasyMock.expect(config.getString(ConfigurationConstants.PROP_MEDIA_DIR))
                .andReturn(mediaDir);
        scanner.setRootURI(mediaDir);
        EasyMock.expect(
                actStore.getAction(MainWndController.ACTION_INIT_PLAYLIST))
                .andReturn(action);
        action.execute(event);
        replayAll();
        MainWndController ctrl = new MainWndController(mockContext, store);
        ctrl.setScanner(scanner);
        ctrl.setActionStore(actStore);
        ctrl.windowOpened(event);
        verifyAll();
    }

    /**
     * Helper method for checking whether the default order can be obtained from
     * a configuration object.
     *
     * @param propValue the value of the configuration property
     * @param expOrder the expected default order
     */
    private void checkGetDefaultPlaylistOrder(String propValue,
            PlaylistOrder expOrder)
    {
        Configuration config = createMock(Configuration.class);
        EasyMock.expect(
                config.getString(ConfigurationConstants.PROP_DEF_PLAYLIST_ORDER))
                .andReturn(propValue);
        replayAll();
        MainWndController ctrl = new MainWndController(mockContext, store);
        assertEquals("Wrong default order", expOrder,
                ctrl.getDefaultPlaylistOrder(config));
        verifyAll();
    }

    /**
     * Tests getDefaultPlaylistOrder() if no order is defined in the
     * configuration.
     */
    @Test
    public void testGetDefaultPlaylistOrderUndefined()
    {
        checkGetDefaultPlaylistOrder(null, PlaylistOrder.DIRECTORIES);
    }

    /**
     * Tests getDefaultPlaylistOrder() if the configuration has an invalid
     * value.
     */
    @Test
    public void testGetDefaultPlaylistOrderInvalidConfigValue()
    {
        checkGetDefaultPlaylistOrder("not a valid order!",
                PlaylistOrder.DIRECTORIES);
    }

    /**
     * Tests getDefaultPlaylistOrder() if the configuration property is set to a
     * valid value.
     */
    @Test
    public void testGetDefaultPlaylistOrderDefined()
    {
        checkGetDefaultPlaylistOrder(
                PlaylistOrder.RANDOM.name().toLowerCase(Locale.ENGLISH),
                PlaylistOrder.RANDOM);
    }

    /**
     * Tests whether an audio reader is correctly created.
     */
    @Test
    public void testCreateAudioReader()
    {
        PlaylistController plc = createMock(PlaylistController.class);
        DataBuffer buffer = createMock(DataBuffer.class);
        replayAll();
        MainWndController ctrl = new MainWndController(mockContext, store);
        ctrl.setPlaylistController(plc);
        AudioReader reader = ctrl.createAudioReader(buffer);
        assertSame("Wrong buffer", buffer, reader.getAudioBuffer());
        assertSame("Wrong source", plc, reader.getStreamSource());
        verifyAll();
    }

    /**
     * Tests a successful initialization of the audio engine.
     */
    @Test
    public void testInitAudioEngineSuccess() throws IOException
    {
        PlaylistController plc = createMock(PlaylistController.class);
        AudioPlayer player = createMock(AudioPlayer.class);
        Application app = createMock(Application.class);
        Configuration config = createMock(Configuration.class);
        PlaylistManager pm = createMock(PlaylistManager.class);
        final DataBufferStreamSource bufferSource =
                createMock(DataBufferStreamSource.class);
        final AudioReader reader = createMock(AudioReader.class);
        EasyMock.expect(mockContext.getBean(Application.BEAN_APPLICATION))
                .andReturn(app);
        EasyMock.expect(app.getUserConfiguration()).andReturn(config);
        EasyMock.expect(
                config.getString(ConfigurationConstants.PROP_DEF_PLAYLIST_ORDER))
                .andReturn(PlaylistOrder.RANDOM.name());
        plc.initializePlaylist(PlaylistOrder.RANDOM);
        EasyMock.expect(plc.getPlaylistManager()).andReturn(pm);
        EasyMock.expect(mockContext.getBean(AudioPlayer.class)).andReturn(
                player);
        EasyMock.expect(player.getAudioSource()).andReturn(bufferSource);
        final CurrentPositionInfo posInfo =
                new CurrentPositionInfo(20110627215305L, 201106272125621L);
        EasyMock.expect(pm.getInitialPositionInfo()).andReturn(posInfo);
        player.setSkipPosition(posInfo.getPosition());
        player.setSkipTime(posInfo.getTime());
        MainWndController ctrl = new MainWndController(mockContext, store)
        {
            @Override
            protected AudioReader createAudioReader(DataBuffer buffer)
            {
                assertSame("Wrong buffer", bufferSource, buffer);
                return reader;
            }
        };
        EasyMock.expect(reader.start()).andReturn(null);
        player.addAudioPlayerListener(plc);
        player.addAudioPlayerListener(ctrl);
        player.start();
        replayAll();
        ctrl.setPlaylistController(plc);
        ctrl.initAudioEngine();
        assertSame("Wrong audio player", player, ctrl.getAudioPlayer());
        assertNotNull("No synchronizer", ctrl.getSongDataSynchronizer());
        verifyAll();
    }

    /**
     * An interface which combines the data buffer with the audio stream source
     * interface. This is needed because the source of the player is also a data
     * buffer.
     */
    private static interface DataBufferStreamSource extends DataBuffer,
            AudioStreamSource
    {
    }
}
