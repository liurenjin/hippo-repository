/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.security;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.hippoecm.repository.security.group.DummyGroupManager;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.role.DummyRoleManager;
import org.hippoecm.repository.security.role.RoleManager;
import org.hippoecm.repository.security.user.DummyUserManager;

public interface SecurityProvider {

    @SuppressWarnings("unused")
    static final String SVN_ID = "$Id$";

    /**
     * Initialize the security provider with the given context
     * @see SecurityProviderContext
     * @param context
     * @throws RepositoryException
     */
    void init(SecurityProviderContext context) throws RepositoryException;

    /**
     * Synchronize the backend users with the users in the repository
     */
    void sync();

    /**
     * This method is called when the provider is remove and can be used
     * for things like stopping listeners
     */
    void remove();

    /**
     * Get the {@link UserManager} from the provider
     * @see DummyUserManager
     * @return the implemented manager or a dummy manager
     */
    UserManager getUserManager() throws RepositoryException;

    /**
     * Get the {@link GroupManager} from the provider
     * @see DummyGroupManager
     * @return the implemented manager or a dummy manager
     */
    GroupManager getGroupManager() throws RepositoryException;

    /**
     * Get the {@link RoleManager} from the provider
     * @see DummyRoleManager
     * @return the implemented manager or a dummy manager
     */
    RoleManager getRoleManager() throws RepositoryException;

}
