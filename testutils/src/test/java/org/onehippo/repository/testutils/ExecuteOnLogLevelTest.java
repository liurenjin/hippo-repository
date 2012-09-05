package org.onehippo.repository.testutils;


import org.junit.Test;
import org.onehippo.repository.testutils.ExecuteOnLogLevel;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ExecuteOnLogLevelTest {

    /**
     * We have configured log4j.xml like below. ExecuteOnLogLevelTest should start on debug.
     * We can change it now as we want
     * <category name="java.org.onehippo.repository.testutils.ExecuteOnLogLevelTest">
     *     <level value="debug"/>
     *  </category>
     */
    static final Logger logDebug = LoggerFactory.getLogger(ExecuteOnLogLevelTest.class);


    /**
     * We have configured log4j.xml like below. RepositoryTestCase should start on info thus!
     * <category name="java.org.onehippo.repository">
     *   <level value="info"/>
     *  </category>
     *
     * <category name="java.org.onehippo.repository.testutils.ExecuteOnLogLevelTest">
     *  <level value="debug"/>
     * </category>
     */
    static final Logger logInfo = LoggerFactory.getLogger(RepositoryTestCase.class);

    /**
     * We did not configure Object at all, and also not some parent package of it
     */
    static final Logger logLeveLNull= LoggerFactory.getLogger(Object.class);

    @Test
    public void changeLogLevelsByNames() {
        assertTrue(logDebug.isDebugEnabled());
        assertTrue(logDebug.isInfoEnabled());
        assertTrue(logDebug.isWarnEnabled());

        assertFalse(logInfo.isDebugEnabled());
        assertTrue(logInfo.isInfoEnabled());
        assertTrue(logInfo.isWarnEnabled());

        // default when not configured is WARN enabled
        assertFalse(logLeveLNull.isDebugEnabled());
        assertFalse(logLeveLNull.isInfoEnabled());
        assertTrue(logLeveLNull.isWarnEnabled());


        // now have a run() method with ExecuteOnLogLevelTest.class.getName() logger on ERROR
        ExecuteOnLogLevel.error(new Runnable() {
            @Override
            public void run() {
                // during run, we now expect the log level to be error
                assertFalse(logDebug.isDebugEnabled());
                assertFalse(logDebug.isInfoEnabled());
                assertFalse(logDebug.isWarnEnabled());
                assertTrue(logDebug.isErrorEnabled());
                // assert that logInfo and logLeveLNull are not changed

                assertFalse(logInfo.isDebugEnabled());
                assertTrue(logInfo.isInfoEnabled());
                assertTrue(logInfo.isWarnEnabled());

                assertFalse(logLeveLNull.isDebugEnabled());
                assertFalse(logLeveLNull.isInfoEnabled());
                assertTrue(logLeveLNull.isWarnEnabled());

            }
        }, ExecuteOnLogLevelTest.class.getName());


        // now we are done with the callback Runnable, the logDebug should be reset to debug level
        assertTrue(logDebug.isDebugEnabled());
        assertTrue(logDebug.isInfoEnabled());
        assertTrue(logDebug.isWarnEnabled());
        assertTrue(logDebug.isErrorEnabled());

        // now have a run() method with RepositoryTestCase.class.getName() logger on WARN
        ExecuteOnLogLevel.warn(new Runnable() {
            @Override
            public void run() {
                // during run, we now expect the log level to be warn
                assertFalse(logInfo.isDebugEnabled());
                assertFalse(logInfo.isInfoEnabled());
                assertTrue(logInfo.isWarnEnabled());
                assertTrue(logInfo.isErrorEnabled());

                // assert that logDebug and logLeveLNull are not changed
                assertTrue(logDebug.isDebugEnabled());
                assertTrue(logDebug.isInfoEnabled());
                assertTrue(logDebug.isWarnEnabled());

                assertFalse(logLeveLNull.isDebugEnabled());
                assertFalse(logLeveLNull.isInfoEnabled());
                assertTrue(logLeveLNull.isWarnEnabled());

            }
        }, RepositoryTestCase.class.getName());

        // now we are done with the callback Runnable, the logInfo should be reset to info level
        assertFalse(logInfo.isDebugEnabled());
        assertTrue(logInfo.isInfoEnabled());
        assertTrue(logInfo.isWarnEnabled());

        // now change logLeveLNull : Even though not configured, you should be able to set it
        // and it gets reset afterwards
        // now have a run() method with Object.class.getName() logger on info
        ExecuteOnLogLevel.info(new Runnable() {
            @Override
            public void run() {
                // during run, we now expect the log level to be info

                assertFalse(logLeveLNull.isDebugEnabled());
                assertTrue(logLeveLNull.isInfoEnabled());
                assertTrue(logLeveLNull.isWarnEnabled());

            }
        }, Object.class.getName());


        assertFalse(logLeveLNull.isDebugEnabled());
        assertFalse(logLeveLNull.isInfoEnabled());
        assertTrue(logLeveLNull.isWarnEnabled());

        // change three loggers in one Runnable
        // now have a run() method with ExecuteOnLogLevelTest.class.getName() logger on ERROR
        ExecuteOnLogLevel.error(new Runnable() {
            @Override
            public void run() {
                // during run, we now expect the log level to be error
                assertFalse(logDebug.isDebugEnabled());
                assertFalse(logDebug.isInfoEnabled());
                assertFalse(logDebug.isWarnEnabled());
                assertTrue(logDebug.isErrorEnabled());

                assertFalse(logInfo.isDebugEnabled());
                assertFalse(logInfo.isInfoEnabled());
                assertFalse(logInfo.isWarnEnabled());
                assertTrue(logInfo.isErrorEnabled());

                assertFalse(logLeveLNull.isDebugEnabled());
                assertFalse(logLeveLNull.isInfoEnabled());
                assertFalse(logLeveLNull.isWarnEnabled());
                assertTrue(logLeveLNull.isErrorEnabled());

            }
        }, ExecuteOnLogLevelTest.class.getName(), RepositoryTestCase.class.getName(), Object.class.getName());

        // now assert they are get set back to their original levels
        assertTrue(logDebug.isDebugEnabled());
        assertTrue(logDebug.isInfoEnabled());
        assertTrue(logDebug.isWarnEnabled());

        assertFalse(logInfo.isDebugEnabled());
        assertTrue(logInfo.isInfoEnabled());
        assertTrue(logInfo.isWarnEnabled());

        // default when not configured is WARN enabled
        assertFalse(logLeveLNull.isDebugEnabled());
        assertFalse(logLeveLNull.isInfoEnabled());
        assertTrue(logLeveLNull.isWarnEnabled());

    }

    @Test
    public void changeLogLevelsByClasses() {
        assertTrue(logDebug.isDebugEnabled());
        assertTrue(logDebug.isInfoEnabled());
        assertTrue(logDebug.isWarnEnabled());

        assertFalse(logInfo.isDebugEnabled());
        assertTrue(logInfo.isInfoEnabled());
        assertTrue(logInfo.isWarnEnabled());

        // default when not configured is WARN enabled
        assertFalse(logLeveLNull.isDebugEnabled());
        assertFalse(logLeveLNull.isInfoEnabled());
        assertTrue(logLeveLNull.isWarnEnabled());


        // now have a run() method with ExecuteOnLogLevelTest.class.getName() logger on ERROR
        ExecuteOnLogLevel.error(new Runnable() {
            @Override
            public void run() {
                // during run, we now expect the log level to be error
                assertFalse(logDebug.isDebugEnabled());
                assertFalse(logDebug.isInfoEnabled());
                assertFalse(logDebug.isWarnEnabled());
                assertTrue(logDebug.isErrorEnabled());
                // assert that logInfo and logLeveLNull are not changed

                assertFalse(logInfo.isDebugEnabled());
                assertTrue(logInfo.isInfoEnabled());
                assertTrue(logInfo.isWarnEnabled());

                assertFalse(logLeveLNull.isDebugEnabled());
                assertFalse(logLeveLNull.isInfoEnabled());
                assertTrue(logLeveLNull.isWarnEnabled());

            }
        }, ExecuteOnLogLevelTest.class);


        // now we are done with the callback Runnable, the logDebug should be reset to debug level
        assertTrue(logDebug.isDebugEnabled());
        assertTrue(logDebug.isInfoEnabled());
        assertTrue(logDebug.isWarnEnabled());
        assertTrue(logDebug.isErrorEnabled());

        // now have a run() method with RepositoryTestCase.class.getName() logger on WARN
        ExecuteOnLogLevel.warn(new Runnable() {
            @Override
            public void run() {
                // during run, we now expect the log level to be warn
                assertFalse(logInfo.isDebugEnabled());
                assertFalse(logInfo.isInfoEnabled());
                assertTrue(logInfo.isWarnEnabled());
                assertTrue(logInfo.isErrorEnabled());

                // assert that logDebug and logLeveLNull are not changed
                assertTrue(logDebug.isDebugEnabled());
                assertTrue(logDebug.isInfoEnabled());
                assertTrue(logDebug.isWarnEnabled());

                assertFalse(logLeveLNull.isDebugEnabled());
                assertFalse(logLeveLNull.isInfoEnabled());
                assertTrue(logLeveLNull.isWarnEnabled());

            }
        }, RepositoryTestCase.class);

        // now we are done with the callback Runnable, the logInfo should be reset to info level
        assertFalse(logInfo.isDebugEnabled());
        assertTrue(logInfo.isInfoEnabled());
        assertTrue(logInfo.isWarnEnabled());

        // now change logLeveLNull : Even though not configured, you should be able to set it
        // and it gets reset afterwards
        // now have a run() method with Object.class.getName() logger on info
        ExecuteOnLogLevel.info(new Runnable() {
            @Override
            public void run() {
                // during run, we now expect the log level to be info

                assertFalse(logLeveLNull.isDebugEnabled());
                assertTrue(logLeveLNull.isInfoEnabled());
                assertTrue(logLeveLNull.isWarnEnabled());

            }
        }, Object.class);


        assertFalse(logLeveLNull.isDebugEnabled());
        assertFalse(logLeveLNull.isInfoEnabled());
        assertTrue(logLeveLNull.isWarnEnabled());

        // change three loggers in one Runnable
        // now have a run() method with ExecuteOnLogLevelTest.class.getName() logger on ERROR
        ExecuteOnLogLevel.error(new Runnable() {
            @Override
            public void run() {
                // during run, we now expect the log level to be error
                assertFalse(logDebug.isDebugEnabled());
                assertFalse(logDebug.isInfoEnabled());
                assertFalse(logDebug.isWarnEnabled());
                assertTrue(logDebug.isErrorEnabled());

                assertFalse(logInfo.isDebugEnabled());
                assertFalse(logInfo.isInfoEnabled());
                assertFalse(logInfo.isWarnEnabled());
                assertTrue(logInfo.isErrorEnabled());

                assertFalse(logLeveLNull.isDebugEnabled());
                assertFalse(logLeveLNull.isInfoEnabled());
                assertFalse(logLeveLNull.isWarnEnabled());
                assertTrue(logLeveLNull.isErrorEnabled());

            }
        }, ExecuteOnLogLevelTest.class, RepositoryTestCase.class, Object.class);

        // now assert they are get set back to their original levels
        assertTrue(logDebug.isDebugEnabled());
        assertTrue(logDebug.isInfoEnabled());
        assertTrue(logDebug.isWarnEnabled());

        assertFalse(logInfo.isDebugEnabled());
        assertTrue(logInfo.isInfoEnabled());
        assertTrue(logInfo.isWarnEnabled());

        // default when not configured is WARN enabled
        assertFalse(logLeveLNull.isDebugEnabled());
        assertFalse(logLeveLNull.isInfoEnabled());
        assertTrue(logLeveLNull.isWarnEnabled());

    }


}
