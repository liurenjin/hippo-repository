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
package org.onehippo.repository.documentworkflow.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.apache.commons.scxml2.SCInstance;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.easymock.EasyMock;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.reviewedactions.HippoStdPubWfNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.MockWorkflowContext;
import org.onehippo.repository.documentworkflow.task.HintWorkflowTask;
import org.onehippo.repository.mock.MockNode;

/**
 * HintDelegatingActionTest
 */
public class HintDelegatingActionTest {

    private HintWorkflowTask task;
    private DocumentHandle dm;
    private HintDelegatingAction delegatingAction;
    private SCInstance scInstance;

    @Before
    public void before() throws Exception {
        MockNode handle = MockNode.root().addMockNode("test", HippoNodeType.NT_HANDLE);
        MockNode liveVariant = handle.addMockNode(handle.getName(), HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
        liveVariant.setProperty(HippoStdNodeType.HIPPOSTD_STATE, "published");
        dm = new DocumentHandle(new MockWorkflowContext("testuser"), liveVariant);

        delegatingAction = new HintDelegatingAction() {
            private static final long serialVersionUID = 1L;
            @Override
            protected HintWorkflowTask createWorkflowTask() {
                return task;
            }
            @Override
            public <T> T getContextAttribute(String name) throws ModelException {
                if ("dm".equals(name)) {
                    return (T) dm;
                }
                return super.getContextAttribute(name);
            }
            @Override
            public <T> T eval(String expr) throws ModelException, SCXMLExpressionException {
                return (T) expr;
            }
        };

        task = new HintWorkflowTask();
        task.setAbstractAction(delegatingAction);

        scInstance = EasyMock.createNiceMock(SCInstance.class);
    }

    @Test
    public void testBasic() throws Exception {
        delegatingAction.setHint("hint1");
        delegatingAction.setValue("value1");
        delegatingAction.execute(null, null, scInstance, null, null);

        assertEquals("value1", dm.getHints().get("hint1"));

        delegatingAction.setHint("hint1");
        delegatingAction.setValue(null);
        delegatingAction.execute(null, null, scInstance, null, null);

        assertFalse(dm.getHints().containsKey("hint1"));
    }

    
}
