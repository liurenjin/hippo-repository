/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.decorating.client;

import javax.jcr.Session;

import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;

import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.api.HierarchyResolver;

import org.hippoecm.repository.decorating.remote.RemoteDocumentManager;
import org.hippoecm.repository.decorating.remote.RemoteSecurityService;
import org.hippoecm.repository.decorating.remote.RemoteWorkflowManager;
import org.hippoecm.repository.decorating.remote.RemoteHierarchyResolver;
import org.onehippo.repository.security.SecurityService;

public interface LocalServicingAdapterFactory extends LocalAdapterFactory {

    public DocumentManager getDocumentManager(Session session, RemoteDocumentManager remote);
    public WorkflowManager getWorkflowManager(Session session, RemoteWorkflowManager remote);
    public HierarchyResolver getHierarchyResolver(Session session, RemoteHierarchyResolver remote);
    public SecurityService getSecurityService(Session session, RemoteSecurityService remote);
}
