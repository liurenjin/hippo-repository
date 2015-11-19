/**
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.documentworkflow.integration;

import java.util.concurrent.CountDownLatch;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.PUBLISHED;
import static org.junit.Assert.assertFalse;

public class DocumentWorkflowConcurrencyTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void concurrentPublishFailsOnDuplicatePublishedVariants() throws Exception {
        for (int i = 0; i < 5; i++) {
            doConcurrentPublishFailsOnDuplicatePublishedVariants();
            tearDown();
            setUp();
        }
    }

    private void doConcurrentPublishFailsOnDuplicatePublishedVariants() throws Exception {
        final Session session1 = session.impersonate(new SimpleCredentials("admin", new char[] {}));
        final Session session2 = session.impersonate(new SimpleCredentials("admin", new char[]{}));
        try {
            final Node handle1 = session1.getNode(handle.getPath());
            final Node handle2 = session2.getNode(handle.getPath());
            final CountDownLatch latch = new CountDownLatch(2);
            final PublicationThread thread1 = new PublicationThread(session1, handle1, latch);
            final PublicationThread thread2 = new PublicationThread(session2, handle2, latch);
            thread1.start();
            thread2.start();
            latch.await();
            assertFalse(hasMulipleVariants(PUBLISHED));
        } finally {
            session1.logout();
            session2.logout();
        }
    }

    private boolean hasMulipleVariants(String state) throws RepositoryException {
        int count = 0;
        for (Node variant : new NodeIterable(handle.getNodes(handle.getName()))) {
            if (variant.getProperty(HIPPOSTD_STATE).getString().equals(state)) {
                count++;
            }
        }
        return count > 1;
    }

    private static class PublicationThread extends Thread {

        private final Session session;
        private final Node handle;
        private final CountDownLatch latch;

        private PublicationThread(final Session session, final Node handle, final CountDownLatch latch) {
            this.session = session;
            this.handle = handle;
            this.latch = latch;
        }

        @Override
        public void run() {
            publish(session, handle);
        }

        private void publish(final Session session, final Node handle) {
            try {
                final WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                final DocumentWorkflow workflow = (DocumentWorkflow) workflowManager.getWorkflow("default", handle);
                workflow.publish();
            } catch (Exception ignore) {
            }
            latch.countDown();
        }

    }

}
