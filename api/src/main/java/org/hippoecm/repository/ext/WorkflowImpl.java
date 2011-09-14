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
package org.hippoecm.repository.ext;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;

/**
 * 
 */
public abstract class WorkflowImpl implements Remote, Workflow
{
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    /**
     * 
     */
    protected WorkflowContext context;
    /**
     * 
     * @throws java.rmi.RemoteException
     */
    public WorkflowImpl() throws RemoteException {
    }

    /**
     * This method should never be invoked by extensions or applications
     * @param context 
     */
    final public void setWorkflowContext(WorkflowContext context) {
        this.context = context;
    }

    /**
     * 
     * @return
     */
    final protected WorkflowContext getWorkflowContext() {
        return context;
    }

    final protected WorkflowContext getWorkflowContext(Object specification) throws MappingException, RepositoryException {
        return context.getWorkflowContext(specification);
    }

    /**
     * 
     * @return
     */
    public Map<String,Serializable> hints() {
        return hints(this);
    }

    static Map<String,Serializable> hints(Workflow workflow) {
        Map<String,Serializable> map = new TreeMap<String,Serializable>();
        for(Class cls : workflow.getClass().getInterfaces()) {
            if(Workflow.class.isAssignableFrom(cls)) {
                for(Method method : cls.getDeclaredMethods()) {
                    String methodName = method.getName();
                    if(methodName.equals("hints")) {
                        map.put(methodName, new Boolean(true));
                    }
                }
            }
        }
        return map;
    }
}
