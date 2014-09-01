/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang.StringUtils;

/**
 * View on a subset of the entries in a zipfile below a certain path.
 */
class SubZipFile extends ZipFile {

    private final ZipFile delegate;
    private final String subPath;

    SubZipFile(final ZipFile delegate, final String subPath) throws IOException {
        super(delegate.getName());
        this.delegate = delegate;
        this.subPath = StringUtils.removeEnd(subPath, "/");
    }

    @Override
    public String getComment() {
        return delegate.getComment();
    }

    @Override
    public ZipEntry getEntry(final String name) {
        if (isIncluded(name, subPath)) {
            return delegate.getEntry(name);
        }
        return null;
    }

    @Override
    public InputStream getInputStream(final ZipEntry entry) throws IOException {
        return delegate.getInputStream(entry);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Enumeration<? extends ZipEntry> entries() {
        return new SubEnumeration<>(delegate.entries(), subPath);
    }

    @Override
    public int size() {
        int result = 0;
        SubEnumeration enumeration = new SubEnumeration(delegate.entries(), subPath);
        while (enumeration.hasMoreElements()) {
            enumeration.nextElement();
            result++;
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    protected void finalize() throws IOException {
        super.finalize();
        delegate.close();
    }

    private static boolean isIncluded(final String entryName, final String subPath) {
        return entryName.equals(subPath) || entryName.startsWith(subPath + "/");
    }

    private static class SubEnumeration<T extends ZipEntry> implements Enumeration<T> {

        private final Enumeration<T> delegate;
        private final String subPath;
        private T next;

        private SubEnumeration(final Enumeration<T> delegate, final String subPath) {
            this.delegate = delegate;
            this.subPath = subPath;
            this.next = null;
        }

        @Override
        public boolean hasMoreElements() {
            if (!delegate.hasMoreElements()) {
                return false;
            }
            if (next == null) {
                next = delegate.nextElement();
            }
            boolean nextMatches = isIncluded(next.getName(), subPath);
            while (!nextMatches && delegate.hasMoreElements()) {
                next = delegate.nextElement();
                nextMatches = isIncluded(next.getName(), subPath);
            }
            return nextMatches;
        }

        @Override
        public T nextElement() {
            if (next != null) {
                T result = next;
                next = null;
                return result;
            } else {
                return delegate.nextElement();
            }
        }
    }

}
