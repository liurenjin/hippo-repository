/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onehippo.repository.documentworkflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DocumentWorkflowTransition {

    private Map<String, Object> eventPayload = new HashMap<>();
    private final Map<String, Object> initializationPayload;
    private final String action;
    private Map<String, Boolean> actionsMap = Collections.emptyMap();

    public Map<String, Boolean> getActionsMap() {
        return actionsMap;
    }

    public Map<String, Object> getInitializationPayload() {
        return initializationPayload;
    }

    public Map<String, Object> getEventPayload() {
        return eventPayload;
    }

    public String getAction() {
        return action;
    }

    public static class Builder {
        private final Map<String, Object> eventPayload = new HashMap<>();
        private Map<String, Object> initializationPayload = Collections.emptyMap();
        private String action;
        private Map<String, Boolean> actionsMap = new HashMap<>();

        public Builder actionsMap(Map<String, Boolean> actionsMap) {
            this.actionsMap = actionsMap;
            return this;
        }

        public Builder eventPayload(Map<String, Object> eventPayload) {
            this.eventPayload.putAll(eventPayload);
            return this;
        }

        public Builder eventPayload(String key, Object value){
            this.eventPayload.put(key,value);
            return this;
        }

        public Builder eventPayload(String key1, Object value1, String key2, Object value2){
            this.eventPayload.put(key1,value1);
            this.eventPayload.put(key2,value2);
            return this;
        }


        public Builder initializationPayload(Map<String, Object> initializationPayload) {
            this.initializationPayload = initializationPayload;
            return this;
        }


        public Builder action(String action){
            this.action = action;
            return this;
        }

        public Builder action(DocumentWorkflowAction action){
            this.action = action.getAction();
            return this;
        }

        public DocumentWorkflowTransition build() {
            return new DocumentWorkflowTransition(this);
        }

    }

    private DocumentWorkflowTransition(Builder b) {
        this.action = b.action;
        this.actionsMap = b.actionsMap;
        this.initializationPayload = b.initializationPayload;
        this.eventPayload = b.eventPayload;
    }
}
