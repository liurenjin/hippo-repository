/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.repository.events;

import org.onehippo.cms7.event.HippoEvent;

/**
 * Developers can implement this PersistedWorkflowEventListener interface to asynchronously receive
 * {@link HippoWorkflowEvent HippoWorkflowEvents} after they occurred in the repository, across a repository cluster.
 * <p>
 *   Such a listener must be registered as listener on the {@link PersistedWorkflowEventsService} through the
 *   {@link }HippoServicesRegistry}, like for example:
 *   <pre>
 *   <code>
 *       HippoServicesRegistry.registerService(myPersistedWorkflowEventListener, PersistedWorkflowEventsService.class);
 *   </code>
 *   </pre>
 * </p>
 * <p>
 *   A PersistedWorkflowEventListener must provide a (cluster node instance unique) channel name which will be used to
 *   track which persisted events already have been delivered to the listener.
 * </p>
 * <p>
 *   A listener will get a consistent and ordered delivery of all workflow events through that channel, but such events
 *   will only be delivered <em>once</em> (within one cluster) through that channel.<br/>
 *   <em>Therefore there should only be one listener (registered) per channel per cluster node.</em><br/>
 *   If more than one listener is registered on one channel, only one will get the events delivered!
 * </p>
 * <p>
 *   A listener also has to define through {@link #onlyNewEvents()} if it should receive persisted events even from before
 *   the first time registration on its {@link #getChannelName() channel} (for as much is looked back in time,
 *   depending on the service configuration), or only events after the first time channel registration will be delivered.
 * </p>
 */
public interface PersistedWorkflowEventListener {

    /**
     * @return The cluster node unique channel name to listen on for events. Note: must conform to JCR node name restrictions.
     */
    String getChannelName();

    /**
     * @return if true, only events persisted after the first time registration on this {@link #getChannelName()} will
     * be delivered.
     */
    boolean onlyNewEvents();

    /**
     * Persisted HippoWorkflowEvents will be delivered through this method.
     * @param event the event
     */
    void onWorkflowEvent(HippoWorkflowEvent event);
}
