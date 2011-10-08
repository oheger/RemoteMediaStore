package de.oliver_heger.mediastore.client.pages.overview;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;

import de.oliver_heger.mediastore.client.DisplayErrorPanel;
import de.oliver_heger.mediastore.client.pageman.PageManager;
import de.oliver_heger.mediastore.shared.search.MediaSearchService;
import de.oliver_heger.mediastore.shared.search.MediaSearchServiceAsync;

/**
 * <p>
 * An abstract base class for overview tables.
 * </p>
 * <p>
 * This base defines the general structure of an overview page for different
 * media objects. The UI mainly consists of a search and tool bar and a cell
 * table displaying the actual content of the page. The logic for constructing
 * and managing the UI is implemented to the major part.
 * </p>
 * <p>
 * Concrete subclasses have to provide some helper objects, e.g. for performing
 * search queries.
 * </p>
 *
 * @author Oliver Heger
 * @version $Id: $
 * @param <T> the type of data objects this page deals with
 */
public abstract class AbstractOverviewTable<T> extends Composite
{
    /** The binder instance. */
    private static OverviewTableUiBinder uiBinder = GWT
            .create(OverviewTableUiBinder.class);

    /** The main table widget. */
    @UiField(provided = true)
    CellTable<T> cellTable;

    /** The pager providing paging functionality. */
    @UiField(provided = true)
    SimplePager pager;

    /** The text component with the search text. */
    @UiField
    TextBox txtSearch;

    /** The search button. */
    @UiField
    Button btnSearch;

    /** The refresh button. */
    @UiField
    PushButton btnRefresh;

    /** The panel showing an error message. */
    @UiField
    DisplayErrorPanel pnlError;

    /** The search service. */
    private MediaSearchServiceAsync searchService;

    /** The page manager. */
    private PageManager pageManager;

    /** The query handler. */
    private final OverviewQueryHandler<T> queryHandler;

    /** The data provider. */
    private OverviewDataProvider<T> dataProvider;

    /**
     * Creates a new instance of {@code AbstractOverviewTable} and initializes
     * it. Some important helper objects must be provided. Before the object can
     * actually be used its {@code initialize()} method must have been called.
     *
     * @param keyProvider the key provider
     * @param handler the {@code OverviewQueryHandler}
     */
    protected AbstractOverviewTable(ProvidesKey<T> keyProvider,
            OverviewQueryHandler<T> handler)
    {
        cellTable = new CellTable<T>(keyProvider);
        SimplePager.Resources pagerResources =
                GWT.create(SimplePager.Resources.class);
        pager =
                new SimplePager(TextLocation.CENTER, pagerResources, false, 0,
                        true);
        pager.setDisplay(cellTable);
        queryHandler = handler;
        initWidget(uiBinder.createAndBindUi(this));
    }

    /**
     * Initializes this object. The reference to the {@code PageManager} is set.
     * Also the initialization of the cell table is initiated.
     *
     * @param pm the page manager
     */
    public void initialize(PageManager pm)
    {
        pageManager = pm;
        dataProvider = initDataProvider();
        getDataProvider().addDataDisplay(cellTable);
        initCellTableColumns(cellTable);
    }

    /**
     * Returns the {@code PageManager} used by this object.
     *
     * @return the {@code PageManager}
     */
    public PageManager getPageManager()
    {
        return pageManager;
    }

    /**
     * Returns the {@code OverviewQueryHandler} for querying the media search
     * service.
     *
     * @return the {@code OverviewQueryHandler}
     */
    public OverviewQueryHandler<T> getQueryHandler()
    {
        return queryHandler;
    }

    /**
     * Returns the {@code OverviewDataProvider} used by this object. This object
     * provides the data displayed by the cell widget. The provider cannot be
     * accessed before {@code initialize()} has been called.
     *
     * @return the {@code OverviewDataProvider}
     */
    public OverviewDataProvider<T> getDataProvider()
    {
        return dataProvider;
    }

    /**
     * Returns the search service.
     *
     * @return the search service
     */
    protected MediaSearchServiceAsync getSearchService()
    {
        if (searchService == null)
        {
            searchService = GWT.create(MediaSearchService.class);
        }
        return searchService;
    }

    /**
     * Performs a refresh. This method causes the cell table to wipe out its
     * data and contact the data provider again.
     */
    protected void refresh()
    {
        Range r = new Range(0, cellTable.getPageSize());
        cellTable.setVisibleRangeAndClearData(r, true);
    }

    /**
     * Initializes the columns of the {@code CellTable} managed by this object.
     * This method is called when this page is initialized. All other helper
     * objects have been created before.
     *
     * @param table the table to be initialized
     */
    protected abstract void initCellTableColumns(CellTable<T> table);

    /**
     * Reacts on a click of the search button.
     *
     * @param e the click event
     */
    @UiHandler("btnSearch")
    void handleSearchClick(ClickEvent e)
    {
        getDataProvider().setSearchText(txtSearch.getText());
        refresh();
    }

    /**
     * Reacts on a click of the refresh button. A refresh causes a search with
     * the latest search parameters to be started again.
     *
     * @param e the click event
     */
    @UiHandler("btnRefresh")
    void handleRefreshClick(ClickEvent e)
    {
        refresh();
    }

    /**
     * Creates and initializes the data provider for the cell table managed by
     * this object.
     *
     * @return the {@code OverviewDataProvider}
     */
    private OverviewDataProvider<T> initDataProvider()
    {
        OverviewCallbackFactory<T> factory = createCallbackFactory();
        return new OverviewDataProvider<T>(getSearchService(),
                getQueryHandler(), factory);
    }

    /**
     * Creates the callback factory for the data provider.
     *
     * @return the callback factory
     */
    private OverviewCallbackFactory<T> createCallbackFactory()
    {
        return new OverviewCallbackFactoryImpl<T>(pnlError);
    }

    /**
     * The UI binder.
     */
    interface OverviewTableUiBinder extends
            UiBinder<Widget, AbstractOverviewTable<?>>
    {
    }
}