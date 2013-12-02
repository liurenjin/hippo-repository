/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.scxml;

import java.util.List;

import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.model.SCXML;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.testutils.slf4j.LogRecord;
import org.onehippo.repository.testutils.slf4j.LoggerRecordingWrapper;

/**
 * RepositorySCXMLExecutorFactoryTest
 */
public class RepositorySCXMLExecutorFactoryTest {

    private static final String SCXML_HELLO_WITH_ERROR_JEXL_SCRIPTS =
            "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" initial=\"hello\">\n" +
            "  <state id=\"hello\">\n" +
            "    <onentry>\n" +
            "      <script>\n" +
            "        unknownObject.invoke();\n" +
            "      </script>\n" +
            "    </onentry>\n" +
            "  </state>\n" +
            "</scxml>";

    private static LoggerRecordingWrapper recordingLogger;

    private MockRepositorySCXMLRegistry registry;
    private RepositorySCXMLExecutorFactory execFactory;

    @BeforeClass
    public static void beforeClass() throws Exception {
        recordingLogger = new LoggerRecordingWrapper(RepositorySCXMLExecutorFactory.log);
        RepositorySCXMLExecutorFactory.log = recordingLogger;
    }

    @Before
    public void before() throws Exception {
        recordingLogger.clearLogRecords();
        registry = new MockRepositorySCXMLRegistry();
        execFactory = new RepositorySCXMLExecutorFactory();
    }

    @Test
    public void testLoadWithErrorJexlScripts() throws Exception {
        MockNode scxmlConfigNode = registry.createConfigNode();
        registry.addScxmlNode(scxmlConfigNode, "hello-with-error-jexl-scripts", SCXML_HELLO_WITH_ERROR_JEXL_SCRIPTS);
        registry.setUp(scxmlConfigNode);

        SCXML helloScxml = registry.getSCXML("hello-with-error-jexl-scripts");
        SCXMLExecutor helloExec = execFactory.createSCXMLExecutor(helloScxml);

        helloExec.go();

        // TODO
        List<LogRecord> logRecords = recordingLogger.getLogRecords();
    }

}
