package de.oliver_heger.mediastore.client.pageman.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@code PageSpecificationImpl}.
 *
 * @author Oliver Heger
 * @version $Id: $
 */
public class TestPageSpecificationImpl
{
    /** Constant for the name of a test page. */
    private static final String PAGE_NAME = "testTargetPage";

    /** Constant for a parameter name. */
    private static final String PARAM = "myParam";

    /** Constant for an encoded parameter. */
    private static final String PARAM_ENC = "p_" + PARAM;

    /** Constant for a parameter value. */
    private static final String VALUE = "paramValue";

    /** The page manager. */
    private PageManagerTestImpl pageMan;

    /** The specification to be tested. */
    private PageSpecificationImpl spec;

    @Before
    public void setUp() throws Exception
    {
        pageMan = new PageManagerTestImpl();
        spec = new PageSpecificationImpl(pageMan, PAGE_NAME);
    }

    /**
     * Tests whether the expected page manager is returned.
     */
    @Test
    public void testGetPageManager()
    {
        assertSame("Wrong page manager", pageMan, spec.getPageManager());
    }

    /**
     * Tests the generated token if no parameters have been specified.
     */
    @Test
    public void testToTokenNoParams()
    {
        assertEquals("Wrong token", PAGE_NAME, spec.toToken());
    }

    /**
     * Tests the generated token if the default parameter has been specified.
     */
    @Test
    public void testToTokenDefaultParam()
    {
        assertEquals("Wrong result", spec, spec.withParameter(VALUE));
        assertEquals("Wrong token", PAGE_NAME + ";default=" + VALUE,
                spec.toToken());
    }

    /**
     * Tests the generated token if a named parameter has been specified.
     */
    @Test
    public void testToTokenNamedParam()
    {
        assertEquals("Wrong result", spec, spec.withParameter(PARAM, VALUE));
        assertEquals("Wrong token", PAGE_NAME + ";" + PARAM_ENC + "=" + VALUE,
                spec.toToken());
    }

    /**
     * Tests whether a null parameter name is handled like the default
     * parameter.
     */
    @Test
    public void testToTokenNamedParamNull()
    {
        assertEquals("Wrong result", spec, spec.withParameter(null, VALUE));
        assertEquals("Wrong token", PAGE_NAME + ";default=" + VALUE,
                spec.toToken());
    }

    /**
     * Tests toToken() if multiple parameters have been provided.
     */
    @Test
    public void testToTokenMultipleParams()
    {
        spec.withParameter(PARAM, VALUE).withParameter(VALUE + 1)
                .withParameter(PARAM + 1, VALUE + 2);
        String expected =
                PAGE_NAME + ";" + PARAM_ENC + "=" + VALUE + ";default=" + VALUE
                        + 1 + ";" + PARAM_ENC + "1=" + VALUE + 2;
        assertEquals("Wrong token", expected, spec.toToken());
    }

    /**
     * Tries to pass a null parameter value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testWithParameterNullValue()
    {
        spec.withParameter(null);
    }

    /**
     * Tries to pass an empty parameter value.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testWithParameterEmptyValue()
    {
        spec.withParameter("");
    }

    /**
     * Tests the open() method.
     */
    @Test
    public void testOpen()
    {
        spec.withParameter("defaultValue").withParameter(PARAM, VALUE).open();
        assertEquals("Wrong token passed to page manager", spec.toToken(),
                pageMan.getToken());
    }

    /**
     * A test implementation of the page manager which allows mocking the method
     * for opening a page. In the tests we need to intercept this method to
     * verify that the expected page token is generated.
     */
    private static class PageManagerTestImpl extends PageManagerImpl
    {
        /** The token passed to this object. */
        private String token;

        public String getToken()
        {
            return token;
        }

        /**
         * Just stores the token passed to this method.
         */
        @Override
        void openPage(String token)
        {
            this.token = token;
        }
    }
}
