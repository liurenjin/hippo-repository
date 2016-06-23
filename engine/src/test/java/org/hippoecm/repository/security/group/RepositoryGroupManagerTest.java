/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.security.group;

import junit.framework.Assert;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.security.ManagerContext;
import org.junit.After;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import javax.jcr.Node;

/**
 */
public class RepositoryGroupManagerTest extends RepositoryTestCase {

    ManagerContext managerContext;
    private static final String GROUP_NAME = "external-editors";
    private static final String USER_NAME = "external-editor-1";

    @After
    @Override
    public void tearDown() throws Exception {

        final Node groups = session.getNode("/hippo:configuration/hippo:groups");
        if (groups.hasNode(GROUP_NAME)) {
            groups.getNode(GROUP_NAME).remove();
        }
        session.save();
        super.tearDown();
    }

    /**
     * test REPO-1487
     * @throws Exception
     */
    @Test
    public void testAddMember() throws Exception {
        //first create an empty group
        final Node groups = session.getNode("/hippo:configuration/hippo:groups");
        final Node group = groups.addNode(NodeNameCodec.encode(GROUP_NAME, true), HippoNodeType.NT_GROUP);
        Assert.assertNotNull(group);
        session.save();

        managerContext = new ManagerContext(session, "hippo:configuration/hippo:security/internal", "hippo:configuration/hippo:groups",true);
        RepositoryGroupManager repositoryGroupManager = new RepositoryGroupManager();
        repositoryGroupManager.init(managerContext);

        repositoryGroupManager.addMember(group, USER_NAME);

        Assert.assertTrue(repositoryGroupManager.getMembers(group).contains(USER_NAME));
    }
}