package de.oliver_heger.mediastore.client.pages.overview;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

import de.oliver_heger.mediastore.client.ImageResources;
import de.oliver_heger.mediastore.client.pageman.PageManager;
import de.oliver_heger.mediastore.client.pages.Pages;
import de.oliver_heger.mediastore.shared.search.MediaSearchParameters;

/**
 * <p>
 * A specialized component representing the main overview page.
 * </p>
 * <p>
 * This page mainly consists of a tab panel. The different tabs contain overview
 * tables for the different media types.
 * </p>
 *
 * @author Oliver Heger
 * @version $Id: $
 */
public class OverviewPage extends Composite implements SearchListener
{
    /** The binder used for building this component. */
    private static MyUiBinder binder = GWT.create(MyUiBinder.class);

    /** The tab panel. */
    @UiField
    TabLayoutPanel tabPanel;

    /** The table for the artists. */
    @UiField
    OverviewTable tabArtists;

    /** The table for the songs. */
    @UiField
    OverviewTable tabSongs;

    /** The table for the albums. */
    @UiField
    OverviewTable tabAlbums;

    /** A map with handlers for processing search queries. */
    private Map<OverviewTable, OverviewData> overviewTables;

    /** Holds a reference to the page manager. */
    private PageManager pageManager;

    /** The image resources. */
    private ImageResources imageResources;

    /**
     * Creates a new instance of {@code OverviewPage}.
     */
    public OverviewPage()
    {
        initWidget(binder.createAndBindUi(this));
    }

    /**
     * Returns a reference to the {@link RMSPageManager}. Using this component
     * it is possible to navigate to other pages.
     *
     * @return the page manager
     */
    public PageManager getPageManager()
    {
        return pageManager;
    }

    /**
     * Returns the object for accessing image resources.
     *
     * @return the object with image resources
     */
    public ImageResources getImageResources()
    {
        return imageResources;
    }

    /**
     * Handles a search request. This method is called when the user enters a
     * search text in one of the overview tables and hits the search button. It
     * delegates to the query handler associated with the overview table.
     *
     * @param source the overview table which is the source of the request
     * @param params the search parameters
     */
    @Override
    public void searchRequest(OverviewTable source, MediaSearchParameters params)
    {
        AbstractOverviewQueryHandler<?> handler = fetchQueryHandler(source);
        handler.handleQuery(params, null);
    }

    /**
     * Initializes this component.
     *
     * @param pm a reference to the page manager
     */
    public void initialize(PageManager pm)
    {
        pageManager = pm;
        imageResources = GWT.create(ImageResources.class);

        initQueryHandlers();
        initSingleElementHandlers();
        ensureOverviewTableInitialized(tabPanel.getSelectedIndex());
    }

    /**
     * Creates the handler for artist queries. This method is called when the
     * map with the query handlers is initialized.
     *
     * @return the query handler for artists
     */
    protected AbstractOverviewQueryHandler<?> createArtistQueryHandler()
    {
        return new ArtistQueryHandler(tabArtists);
    }

    /**
     * Creates the handler for song queries. This method is called when a query
     * for songs is initiated.
     *
     * @return the query handler for songs
     */
    protected AbstractOverviewQueryHandler<?> createSongQueryHandler()
    {
        return new SongQueryHandler(tabSongs);
    }

    /**
     * Creates the handler for album queries. This method is called when a query
     * for albums is initiated.
     *
     * @return the query handler for albums
     */
    protected AbstractOverviewQueryHandler<?> createAlbumQueryHandler()
    {
        return new AlbumQueryHandler(tabAlbums);
    }

    /**
     * Initializes the map with query handlers. This method also registers this
     * object as search listener at all overview tables.
     */
    void initQueryHandlers()
    {
        overviewTables = new HashMap<OverviewTable, OverviewData>();
        overviewTables.put(tabArtists, new OverviewData(
                createArtistQueryHandler()));
        tabArtists.setSearchListener(this);
        overviewTables
                .put(tabSongs, new OverviewData(createSongQueryHandler()));
        tabSongs.setSearchListener(this);
        overviewTables.put(tabAlbums, new OverviewData(createAlbumQueryHandler()));
        tabAlbums.setSearchListener(this);
        // TODO add further handlers
    }

    /**
     * Initializes the single element handlers for the overview tables.
     */
    void initSingleElementHandlers()
    {
        tabArtists.addSingleElementHandler(getImageResources().viewDetails(),
                new OpenPageSingleElementHandler(getPageManager(),
                        Pages.ARTISTDETAILS));
        tabSongs.addSingleElementHandler(getImageResources().viewDetails(),
                new OpenPageSingleElementHandler(getPageManager(),
                        Pages.SONGDETAILS));
    }

    /**
     * Returns the query handler for the specified overview table. The query
     * handlers are initialized when an instance is created. They are then used
     * to process search queries.
     *
     * @param table the overview table in question
     * @return the query handler for this overview table
     */
    AbstractOverviewQueryHandler<?> fetchQueryHandler(OverviewTable table)
    {
        OverviewData overviewData = overviewTables.get(table);
        return overviewData.getOverviewHandler();
    }

    /**
     * The selection of the tab panel has changed. We check whether the overview
     * panel for this tab has already been initialized. If not, it is
     * initialized now. This causes an initial query to be sent to the server
     * when the tab is opened for the first time.
     *
     * @param event the selection event
     */
    @UiHandler("tabPanel")
    void tabSelectionChanged(SelectionEvent<Integer> event)
    {
        ensureOverviewTableInitialized(event.getSelectedItem());
    }

    /**
     * Ensures that the overview table with the given index is initialized. This
     * method is called when the application starts and when the selection of
     * the tab panel with the overview tables changes.
     *
     * @param index the current index of the tab panel
     */
    private void ensureOverviewTableInitialized(int index)
    {
        OverviewData overviewData =
                overviewTables.get(tabPanel.getWidget(index));
        if (overviewData != null)
        {
            overviewData.ensureInit();
        }
        else
        {
            Window.alert("Could not initialize overview table at " + index);
        }
    }

    /**
     * The specific UI binder interface for this page component.
     */
    interface MyUiBinder extends UiBinder<Widget, OverviewPage>
    {
    }

    /**
     * A simple data class for storing information about an overview table.
     * Objects of this class are created for all overview tables managed by
     * {@code RemoteMediaStore}. In addition to storing the handler object for
     * the overview table, this class keeps track if the table has already been
     * initialized.
     */
    private static class OverviewData
    {
        /** Stores the handler for the overview table. */
        private final AbstractOverviewQueryHandler<?> overviewHandler;

        /** A flag whether the table has already been initialized. */
        private boolean initialized;

        /**
         * Creates a new instance of {@code OverviewData} and sets the handler.
         *
         * @param handler the handler for the overview table
         */
        public OverviewData(AbstractOverviewQueryHandler<?> handler)
        {
            overviewHandler = handler;
        }

        /**
         * Returns the handler for the overview table managed by this data
         * object.
         *
         * @return the overview table handler
         */
        public AbstractOverviewQueryHandler<?> getOverviewHandler()
        {
            return overviewHandler;
        }

        /**
         * Ensures that the represented overview table has been initialized. If
         * this has not been the case, the handler is invoked with an empty
         * search parameters object. This causes a query to be sent to the
         * server. With the results of this query the table is initialized.
         */
        public void ensureInit()
        {
            if (!initialized)
            {
                initialized = true;
                // TODO initialize parameters object properly
                overviewHandler.handleQuery(new MediaSearchParameters(), null);
            }
        }
    }
}
