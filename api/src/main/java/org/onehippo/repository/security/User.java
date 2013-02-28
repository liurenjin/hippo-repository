/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.security;

import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * Represents a user in the repository.
 */
public interface User {

    /**
     * Get the id of the user.
     *
     * @return  the id of the user
     * @throws RepositoryException
     */
    public String getId() throws RepositoryException;

    /**
     * Whether this user is marked as a system user.
     *
     * @return  whether this user is marked as a system user.
     * @throws RepositoryException
     */
    public boolean isSystemUser() throws RepositoryException;

    /**
     * Whether this user is marked as active.
     *
     * @return  whether this user is marked as active.
     * @throws RepositoryException
     */
    public boolean isActive() throws RepositoryException;

    /**
     * Get the ids of the {@link Group}s this user is a member of.
     *
     * @return the ids of the {@link Group}s this user is a member of.
     * @throws RepositoryException
     */
    public Set<String> getMemberships() throws RepositoryException;

}