package de.oliver_heger.mediastore.client.pages.detail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.AsyncCallback;

import de.oliver_heger.mediastore.client.pages.MockPageConfiguration;
import de.oliver_heger.mediastore.client.pages.MockPageManager;
import de.oliver_heger.mediastore.client.pages.Pages;
import de.oliver_heger.mediastore.shared.BasicMediaServiceAsync;
import de.oliver_heger.mediastore.shared.model.ArtistDetailInfo;

/**
 * Test class for {@code ArtistDetailsPage}. This class also tests functionality
 * inherited from the super class.
 *
 * @author Oliver Heger
 * @version $Id: $
 */
public class TestArtistDetailsPage extends GWTTestCase
{
    /** Constant for the ID of the test artist. */
    private static final Long ARTIST_ID = 20101216075421L;

    /** Constant for the test artist name. */
    private static final String NAME = "Elvis";

    /** An array with synonyms. */
    private static final String[] SYNONYMS = {
            "The King", "Elvis Presley"
    };

    @Override
    public String getModuleName()
    {
        return "de.oliver_heger.mediastore.RemoteMediaStore";
    }

    /**
     * Tests whether the passed in string is empty. This means that the string
     * is either null or has length 0.
     *
     * @param msg the error message
     * @param s the string to check
     */
    private static void assertEmpty(String msg, String s)
    {
        assertTrue(msg, s == null || s.length() < 1);
    }

    /**
     * Creates a details info object for an artist with test data.
     *
     * @return the details object
     */
    private static ArtistDetailInfo createArtistInfo()
    {
        ArtistDetailInfo info = new ArtistDetailInfo();
        info.setArtistID(ARTIST_ID);
        info.setName(NAME);
        info.setSynonyms(new HashSet<String>(Arrays.asList(SYNONYMS)));
        info.setCreationDate(new Date());
        return info;
    }

    /**
     * Tests whether a page can be created correctly.
     */
    public void testInit()
    {
        ArtistDetailsPage page = new ArtistDetailsPage();
        assertNotNull("No name span", page.spanArtistName);
        assertNotNull("No creation date span", page.spanCreationDate);
        assertNotNull("No synonyms span", page.spanSynonyms);
        assertNotNull("No progress indicator", page.progressIndicator);
        assertNotNull("No error panel", page.pnlError);
        assertNotNull("No overview link", page.lnkOverview);
    }

    /**
     * Tests the setPageConfiguration() method. This method should initiate a
     * server request.
     */
    public void testSetPageConfiguration()
    {
        final Map<String, Object> params = new HashMap<String, Object>();
        ArtistDetailsPage page = new ArtistDetailsPage()
        {
            @Override
            protected DetailsQueryHandler<ArtistDetailInfo> getDetailsQueryHandler()
            {
                assertTrue("No progress indicator",
                        progressIndicator.isVisible());
                assertFalse("Error panel visible", pnlError.isInErrorState());
                return new DetailsQueryHandler<ArtistDetailInfo>()
                {
                    @Override
                    public void fetchDetails(
                            BasicMediaServiceAsync mediaService, String elemID,
                            AsyncCallback<ArtistDetailInfo> callback)
                    {
                        assertEquals("Wrong callback", getCallback(), callback);
                        assertEquals("Wrong ID", ARTIST_ID.toString(), elemID);
                        assertNotNull("No media service", mediaService);
                        params.put(NAME, Boolean.TRUE);
                    }
                };
            }
        };
        page.pnlError.displayError(new Exception());
        page.progressIndicator.setVisible(false);
        params.put(null, ARTIST_ID.toString());
        page.setPageConfiguration(new MockPageConfiguration(Pages.ARTISTDETAILS
                .name(), params));
        assertTrue("Handler not called", params.containsKey(NAME));
        assertFalse("In error state", page.pnlError.isInErrorState());
        assertTrue("No progress indicator", page.progressIndicator.isVisible());
    }

    /**
     * Tests whether the callback works correctly if the server call was
     * successful.
     */
    public void testCallbackSuccess()
    {
        ArtistDetailsPage page = new ArtistDetailsPage();
        page.pnlError.displayError(new Exception());
        page.progressIndicator.setVisible(true);
        AsyncCallback<ArtistDetailInfo> callback = page.getCallback();
        callback.onSuccess(createArtistInfo());
        assertFalse("Got a progress indicator",
                page.progressIndicator.isVisible());
        assertEquals("Wrong artist name", NAME,
                page.spanArtistName.getInnerText());
        assertNotNull("No creation date", page.spanCreationDate.getInnerText());
        String s = page.spanSynonyms.getInnerText();
        for (String syn : SYNONYMS)
        {
            assertTrue("Synonym not found: " + s, s.contains(syn));
        }
    }

    /**
     * Tests whether exceptions are correctly processed by the callback.
     */
    public void testCallbackError()
    {
        ArtistDetailsPage page = new ArtistDetailsPage();
        Throwable ex = new Exception("Test exception");
        page.progressIndicator.setVisible(true);
        AsyncCallback<ArtistDetailInfo> callback = page.getCallback();
        callback.onFailure(ex);
        assertTrue("Not in error state", page.pnlError.isInErrorState());
        assertEquals("Wrong exception", ex, page.pnlError.getError());
        assertFalse("Got a progress indicator",
                page.progressIndicator.isVisible());
        assertEmpty("Got artist name", page.spanArtistName.getInnerText());
        assertEmpty("Got creation date", page.spanCreationDate.getInnerText());
        assertEmpty("Got synonyms", page.spanSynonyms.getInnerText());
    }

    /**
     * Tests whether formatSynonyms() can handle a null set.
     */
    public void testFormatSynonymsNull()
    {
        ArtistDetailsPage page = new ArtistDetailsPage();
        assertEmpty("Got synonyms", page.formatSynonyms(null));
    }

    /**
     * Tests whether a single synonym is correctly formatted.
     */
    public void testFormatSynonymsSingle()
    {
        Set<String> syns = Collections.singleton(SYNONYMS[0]);
        assertEquals("Wrong formatted synonyms", SYNONYMS[0],
                new ArtistDetailsPage().formatSynonyms(syns));
    }

    /**
     * Tests whether multiple synonyms can be correctly formatted.
     */
    public void testFormatSynonymsMultiple()
    {
        Set<String> syns = new LinkedHashSet<String>();
        syns.add(SYNONYMS[0]);
        syns.add(SYNONYMS[1]);
        ArtistDetailsPage page = new ArtistDetailsPage();
        assertEquals("Wrong formatted synonyms", SYNONYMS[0] + ", "
                + SYNONYMS[1], page.formatSynonyms(syns));
    }

    /**
     * Tests whether the correct query handler is returned.
     */
    public void testGetDetailsQueryHandler()
    {
        ArtistDetailsPage page = new ArtistDetailsPage();
        assertTrue(
                "Wrong query handler",
                page.getDetailsQueryHandler() instanceof ArtistDetailsQueryHandler);
    }

    /**
     * Tests whether only a single instance of the query handler exists.
     */
    public void testGetDetailsQueryHandlerCached()
    {
        ArtistDetailsPage page = new ArtistDetailsPage();
        DetailsQueryHandler<ArtistDetailInfo> handler =
                page.getDetailsQueryHandler();
        assertSame("Multiple handlers", handler, page.getDetailsQueryHandler());
    }

    /**
     * Tests whether the link back to the overview page works.
     */
    public void testOnClickOverview()
    {
        ArtistDetailsPage page = new ArtistDetailsPage();
        MockPageManager pm = new MockPageManager();
        pm.expectCreatePageSpecification(Pages.OVERVIEW, null).open();
        page.initialize(pm);
        page.onClickOverview(null);
        pm.verify();
    }
}
