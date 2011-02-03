/*
 *  Copyright 2011 Hippo (www.hippo.nl).
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
package org.hippoecm.repository.export;

import static org.hippoecm.repository.export.Constants.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.impl.SessionDecorator;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;


class ContentResourceInstruction extends ResourceInstruction {
    
    private final String m_root;
    final String m_context;
    private final boolean m_enabled;
    private final Extension m_extension;
    
    ContentResourceInstruction(String name, Double sequence, File file, String root, String context, boolean enabled, Extension extension) {
        super(name, sequence, file);
        m_root = root;
        m_context = context;
        if (!m_file.exists()) {
        	m_changed = true;
        }
        m_enabled = enabled;
        m_extension = extension;
    }
    
    @Override
    synchronized void export(Session session) {
    	if (!m_enabled) {
    		log.info("Export in this context is disabled. Changes will be lost.");
    		return;
    	}
    	log.debug("Exporting " + m_file.getName());
    	try {
        	if (!m_file.exists()) m_file.createNewFile();
            OutputStream out = null;
            try {
                out = new FileOutputStream(m_file);
                SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
                TransformerHandler handler = stf.newTransformerHandler();
                Transformer transformer = handler.getTransformer();
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));
                handler.setResult(new StreamResult(out));
                List<String> excluded = new ArrayList<String>();
                for (Instruction instruction : m_extension.getInstructions()) {
                	if (instruction instanceof ContentResourceInstruction) {
                		String context = ((ContentResourceInstruction) instruction).m_context;
                		if (!context.equals(m_context) && context.startsWith(m_context)) {
                			String subcontext = context.substring(m_root.length());
//                			log.debug("filtering subcontext " + subcontext);
                			excluded.add(subcontext);
                		}
                	}
                }
                ContentHandler filter = new FilterContentHandler(handler, excluded);
                session = ((HippoSession) session).impersonate(new SimpleCredentials("system", new char[]{}));
                ((SessionDecorator) session).exportDereferencedView(m_context, filter, false, false);
			} finally {
                try {
                    out.close();
                } catch (IOException ex) {}
            }
    	}
    	catch (IOException e) {
    		log.error("Exporting " + m_file.getName() + " failed.", e);
    	}
    	catch (RepositoryException e) {
    		log.error("Exporting " + m_file.getName() + " failed.", e);
        } 
    	catch (TransformerConfigurationException e) {
    		log.error("Exporting " + m_file.getName() + " failed.", e);
		} 
    	catch (SAXException e) {
    		log.error("Exporting " + m_file.getName() + " failed.", e);
        }
        m_changed = false;
    }
    
    boolean nodeRemoved(String path) {
    	m_changed = true;
    	return path.equals(m_context);
    }
    
    @Override
    public Element createInstructionElement() {
        Element element = createBaseInstructionElement();
        // create element:
        // <sv:property sv:name="hippo:contentresource" sv:type="String">
        //   <sv:value>{this.m_file.getName()}</sv:value>
        // </sv:property>
        Element contentResourceProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
        contentResourceProperty.add(DocumentFactory.getInstance().createAttribute(contentResourceProperty, NAME_QNAME, "hippo:contentresource"));
        contentResourceProperty.add(DocumentFactory.getInstance().createAttribute(contentResourceProperty, TYPE_QNAME, "String"));
        Element contentResourcePropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
        contentResourcePropertyValue.setText(m_file.getName());
        contentResourceProperty.add(contentResourcePropertyValue);
        element.add(contentResourceProperty);

        // create element:
        // <sv:property sv:name="hippo:contentroot" sv:type="String">
        //   <sv:value>{this.m_root}</sv:value>
        // </sv:property>
        Element contentRootProperty = DocumentFactory.getInstance().createElement(PROPERTY_QNAME);
        contentRootProperty.add(DocumentFactory.getInstance().createAttribute(contentRootProperty, NAME_QNAME, "hippo:contentroot"));
        contentRootProperty.add(DocumentFactory.getInstance().createAttribute(contentRootProperty, TYPE_QNAME, "String"));
        Element contentRootPropertyValue = DocumentFactory.getInstance().createElement(VALUE_QNAME);
        contentRootPropertyValue.setText(m_root);
        contentRootProperty.add(contentRootPropertyValue);
        element.add(contentRootProperty);
        
        return element;
    }
    
    boolean matchesPath(String path) {
    	return path.startsWith(m_context);
    }
    
    @Override
    public String toString() {
    	return "ResourceContentInstruction[context=" + m_context + "]";
    }
    
    /** 
     * Filters out all namespace declarations except {http://www.jcp.org/jcr/sv/1.0}; 
     * excludes all declared subcontexts from export; and strips version strings from
     * namespace prefixes
     */
    static class FilterContentHandler implements ContentHandler {

    	private final ContentHandler m_handler;
    	private final List<String> m_excluded;
    	private final Path m_path;

    	private String m_svprefix;
    	private boolean m_skip = false;
    	private boolean m_insideTypeProperty = false;
    	private boolean m_modifyvalue = false;
    	
    	FilterContentHandler(ContentHandler handler, List<String> excluded) {
    		m_handler = handler;
    		m_excluded = excluded;
    		m_path = new Path();
    	}
    	
		@Override
		public void setDocumentLocator(Locator locator) {
			m_handler.setDocumentLocator(locator);
		}

		@Override
		public void startDocument() throws SAXException {
			m_handler.startDocument();
		}

		@Override
		public void endDocument() throws SAXException {
			m_handler.endDocument();
		}

		@Override
		public void startPrefixMapping(String prefix, String uri) throws SAXException {
			// only forward prefix mappings in the jcr/sv namespace
			if (uri.equals("http://www.jcp.org/jcr/sv/1.0")) {
				m_svprefix = prefix;
				m_handler.startPrefixMapping(prefix, uri);
			}
		}

		@Override
		public void endPrefixMapping(String prefix) throws SAXException {
			if (prefix.equals(m_svprefix)) {
				m_handler.endPrefixMapping(prefix);
			}
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
			if (localName.equals("node") && uri.equals("http://www.jcp.org/jcr/sv/1.0")) {
				String name = atts.getValue("http://www.jcp.org/jcr/sv/1.0", "name");
				m_path.push(name);
				String path = m_path.toString();
//				log.debug("exclude " + path + "?");
				for (String exclude : m_excluded) {
					if (path.startsWith(exclude)) {
//						log.debug("yes");
						m_skip = true;
						// don't propagate event
						return;
					}
				}
			}
			else if (m_skip) {
				return;
			}
			else if (localName.equals("property") && uri.equals("http://www.jcp.org/jcr/sv/1.0")) {
				String propName = atts.getValue("http://www.jcp.org/jcr/sv/1.0", "name");
				if (propName.equals("type") || propName.equals("hipposysedit:supertype")
						|| propName.equals("jcr:primaryType")) {
					m_insideTypeProperty = true;
				}
			}
			else if (localName.equals("value") && uri.equals("http://www.jcp.org/jcr/sv/1.0")) {
				m_modifyvalue = m_insideTypeProperty;
			}
			m_handler.startElement(uri, localName, qName, atts);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (localName.equals("node") && uri.equals("http://www.jcp.org/jcr/sv/1.0")) {
				String path = m_path.toString();
				for (String exclude : m_excluded) {
					if (path.startsWith(exclude)) {
						m_path.pop();
						m_skip = false;
						// don't propagate event
						return;
					}
				}
				m_path.pop();
			}
			else if (m_skip) {
				return;
			}
			else if (localName.equals("property") && uri.equals("http://www.jcp.org/jcr/sv/1.0")) {
				m_insideTypeProperty = false;
			}
			else if (localName.equals("value") && uri.equals("http://www.jcp.org/jcr/sv/1.0")) {
				m_modifyvalue = false;
			}
			m_handler.endElement(uri, localName, qName);
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (m_skip) return;
			if (m_modifyvalue) {
				// strip the prefix of version data
				// e.g. example_1_1:basedocument becomes example:basedocument
				String value = new String(ch, start, length);
				int indexOfColon = value.indexOf(':');
				if (indexOfColon != -1) {
					String prefix = value.substring(0, indexOfColon);
					String rest = value.substring(indexOfColon);
					int indexOfUnderscore = prefix.indexOf('_');
					prefix = indexOfUnderscore == -1 ? prefix : prefix.substring(0, indexOfUnderscore);
					ch = (prefix + rest).toCharArray();
					start = 0;
					length = ch.length;
				}
			}
			m_handler.characters(ch, start, length);
		}

		@Override
		public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
			if (m_skip) return;
			m_handler.ignorableWhitespace(ch, start, length);
		}

		@Override
		public void processingInstruction(String target, String data) throws SAXException {
			m_handler.processingInstruction(target, data);
		}

		@Override
		public void skippedEntity(String name) throws SAXException {
			m_handler.skippedEntity(name);
		}
		
		private static class Path {
			
			private Stack<String> m_stack = new Stack<String>();
			
			void push(String element) {
				m_stack.push(element);
			}
			
			void pop() {
				m_stack.pop();
			}
			
			@Override
			public String toString() {
				StringBuilder sb = new StringBuilder();
				for (String element : m_stack) {
					sb.append("/" + element);
				}
				return sb.toString();
			}
		}
    }
    
}