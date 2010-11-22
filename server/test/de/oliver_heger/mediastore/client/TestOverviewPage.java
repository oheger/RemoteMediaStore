package de.oliver_heger.mediastore.client;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.AsyncCallback;

import de.oliver_heger.mediastore.shared.search.MediaSearchParameters;
import de.oliver_heger.mediastore.shared.search.MediaSearchServiceAsync;
import de.oliver_heger.mediastore.shared.search.SearchIterator;
import de.oliver_heger.mediastore.shared.search.SearchResult;

/**
 * Test class for {@code OverviewPage}.
 *
 * @author Oliver Heger
 * @version $Id: $
 */
public class TestOverviewPage extends GWTTestCase
{
    @Override
    public String getModuleName()
    {
        return "de.oliver_heger.mediastore.RemoteMediaStore";
    }

    /**
     * Tests whether all fields are properly set by the UI binder.
     */
    public void testInit()
    {
        OverviewPage page = new OverviewPage();
        assertNotNull("No tab panel", page.tabPanel);
        assertNotNull("No artist table", page.tabArtists);
        assertNotNull("No songs table", page.tabSongs);
    }

    /**
     * Tests whether a query handler for artists can be obtained.
     */
    public void testFetchArtistQueryHandler()
    {
        OverviewPage page = new OverviewPage();
        page.initQueryHandlers();
        AbstractOverviewQueryHandler<?> handler =
                page.fetchQueryHandler(page.tabArtists);
        assertTrue("Wrong query handler", handler instanceof ArtistQueryHandler);
    }

    /**
     * Tests whether a search request is correctly processed.
     */
    public void testSearchRequest()
    {
        final OverviewTable view = new OverviewTable();
        final QueryHandlerTestImpl handler = new QueryHandlerTestImpl(view);
        OverviewPage page = new OverviewPage()
        {
            @Override
            AbstractOverviewQueryHandler<?> fetchQueryHandler(
                    OverviewTable table)
            {
                assertSame("Wrong view", view, table);
                return handler;
            }
        };
        MediaSearchParameters params = new MediaSearchParameters();
        params.setSearchText("testSearchText");
        page.searchRequest(view, params);
        handler.verifyHandleQuery(params, null);
    }

    /**
     * Helper method for testing whether an overview page is initialized when
     * its tab is activated for the first time.
     *
     * @return the page used by the test
     */
    private OverviewPage checkTabSelectionChanged()
    {
        final QueryHandlerTestImpl handler =
                new QueryHandlerTestImpl(new OverviewTable());
        OverviewPage page = new OverviewPage()
        {
            @Override
            protected AbstractOverviewQueryHandler<?> createArtistQueryHandler()
            {
                return handler;
            }
        };
        page.initQueryHandlers();
        SelectionEvent.fire(page.tabPanel, 0);
        handler.verifyHandleQuery(new MediaSearchParameters(), null);
        return page;
    }

    /**
     * Tests whether an overview page is initialized when its tab is activated
     * for the first time.
     */
    public void testTabSelectionChanged()
    {
        checkTabSelectionChanged();
    }

    /**
     * Tests whether an initialization of an overview table is only performed at
     * the first access.
     */
    public void testTabSelectionChangedMultipleTimes()
    {
        OverviewPage page = checkTabSelectionChanged();
        // The following would cause an exception if another query is issued.
        SelectionEvent.fire(page.tabPanel, 0);
    }

    /**
     * A test implementation of a query handler that provides mocking
     * facilities.
     */
    private static class QueryHandlerTestImpl extends
            AbstractOverviewQueryHandler<Object>
    {
        /** Stores the passed in search parameters object. */
        private MediaSearchParameters queryParameters;

        /** Stores the passed in search iterator. */
        private SearchIterator queryIterator;

        public QueryHandlerTestImpl(SearchResultView v)
        {
            super(v);
        }

        /**
         * Checks whether handleQuery() was called with the expected parameters.
         *
         * @param expParams the expected search parameters
         * @param expIt the expected search iterator
         */
        public void verifyHandleQuery(MediaSearchParameters expParams,
                SearchIterator expIt)
        {
            assertEquals("Wrong search parameters", expParams, queryParameters);
            assertEquals("Wrong search iterator", expIt, queryIterator);
        }

        /**
         * Records this invocation.
         */
        @Override
        public void handleQuery(MediaSearchParameters searchParams,
                SearchIterator searchIterator)
        {
            assertNull("Too many invocations", queryParameters);
            queryParameters = searchParams;
            queryIterator = searchIterator;
        }

        @Override
        protected void callService(MediaSearchServiceAsync service,
                MediaSearchParameters searchParams,
                SearchIterator searchIterator,
                AsyncCallback<SearchResult<Object>> callback)
        {
            throw new UnsupportedOperationException("Unexpected method call!");
        }

        @Override
        protected ResultData createResult(SearchResult<Object> result)
        {
            throw new UnsupportedOperationException("Unexpected method call!");
        }
    }
}
