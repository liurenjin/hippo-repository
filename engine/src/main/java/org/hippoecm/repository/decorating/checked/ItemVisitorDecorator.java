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
package org.hippoecm.repository.decorating.checked;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 */
public class ItemVisitorDecorator extends AbstractDecorator implements ItemVisitor {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    protected final ItemVisitor visitor;

    protected ItemVisitorDecorator(DecoratorFactory factory, SessionDecorator session, ItemVisitor visitor) {
        super(factory, session);
        this.visitor = visitor;
    }

    @Override
    protected void repair(Session upstreamSession) throws RepositoryException {
        throw new RepositoryException("session is no longer valid");
    }
    /**
     * @inheritDoc
     */
    public void visit(Property property) throws RepositoryException {
        check();
        visitor.visit(factory.getPropertyDecorator(session, property));
    }

    /**
     * @inheritDoc
     */
    public void visit(Node node) throws RepositoryException {
        check();
        visitor.visit(factory.getNodeDecorator(session, node));
    }

}
