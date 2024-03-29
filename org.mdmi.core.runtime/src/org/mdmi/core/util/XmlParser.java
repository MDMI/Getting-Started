/*******************************************************************************
* Copyright (c) 2012, 2017, 2018 MDIX Inc
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     MDIX Inc - initial API and implementation
*
* Author:
*     Gabriel Oancea
*
*******************************************************************************/
package org.mdmi.core.util;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.mdmi.core.MdmiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * Wrapper for an XML DOM parser.
 *
 * @author goancea
 */
public final class XmlParser {

	private static Logger logger = LoggerFactory.getLogger(XmlParser.class);

	private DocumentBuilder m_parser; // the parser

	private boolean m_strict; // if true, both errors and fatal errors set the m_errors

	private boolean m_errors; // flag is set to true if there are any fatal errors
								// (or errors if the m_strict is true)

	private ArrayList<String> m_messages; // will be initialized for each parse

	/**
	 * Default constructor, uses default values for all attributes.
	 */
	public XmlParser() {
		this(true, false, true, false, false, true, false);
	}

	/**
	 * Create a new parser with the specified attributes. The strict attribute indicates if the errors should be treated
	 * as fatal errors (i.e. no document is returned from the parse method).
	 * <p>
	 * See the see the java.xml.DocumentBuilderFactory for descriptions of the rest of the parameters. Note that
	 * <code>ignoreElementWhitespace</code> requires validation on (by default both are false).
	 *
	 * @param strict
	 *            Treat parse errors as fatal errors (default value is TRUE)
	 * @param coalescing
	 *            Coalesce adjacent text and CDATA nodes (default value is FALSE)
	 * @param expandEntityReferences
	 *            Expand entity references (default value is TRUE)
	 * @param ignoreComments
	 *            Ignore comments (default value is FALSE)
	 * @param ignoreElementWhitespace
	 *            Igrore whitespace in element content (default value is FALSE)
	 * @param namespaceAware
	 *            Use namespaces (default value is TRUE)
	 * @param validating
	 *            Use validation (default value is FALSE)
	 */
	public XmlParser(boolean strict, // default is true
			boolean coalescing, // default is false
			boolean expandEntityReferences, // default is true
			boolean ignoreComments, // default is false
			boolean ignoreElementWhitespace, // default is false
			boolean namespaceAware, // default is false
			boolean validating // default is false
	) {
		m_strict = strict;
		// force Xerces. Alternative: new org.apache.xerces.jaxp.DocumentBuilderFactoryImpl();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		// factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		factory.setCoalescing(coalescing);
		factory.setExpandEntityReferences(expandEntityReferences);
		factory.setIgnoringComments(ignoreComments);
		factory.setNamespaceAware(namespaceAware);
		factory.setValidating(validating);
		if (validating && ignoreElementWhitespace) {
			factory.setIgnoringElementContentWhitespace(ignoreElementWhitespace);
		}
		try {
			m_parser = factory.newDocumentBuilder();
			m_parser.setErrorHandler(new EH());
		} catch (ParserConfigurationException ex) {
			logger.error("Parser configuration fails", ex);
			throw new MdmiException(ex, "Parser configuration fails");
		}
	}

	/**
	 * Get the error messages as generated by the last operation (if any).
	 *
	 * @return List of error messages.
	 */
	public ArrayList<String> getMessageList() {
		return m_messages;
	}

	/**
	 * Get the error messages as generated by the last operation (if any).
	 *
	 * @return Concatenated error messages, or an empty string ("") if no errors were encountered.
	 */
	public String getMessages() {
		if (m_messages == null) {
			return "";
		}
		StringBuffer msgs = new StringBuffer(m_messages.size() * 128);
		for (int i = 0; i < m_messages.size(); i++) {
			if (i > 0) {
				msgs.append('\n');
			}
			msgs.append(m_messages.get(i));
		}
		return msgs.toString();
	}

	/**
	 * Create a new DOM Document.
	 *
	 * @return The created document instance.
	 */
	public Document newDocument() {
		if (m_parser == null) {
			throw new IllegalStateException("Invalid object state - constructor failed, m_parser is null!");
		}
		return m_parser.newDocument();
	}

	/**
	 * Create a new DOM Document.
	 *
	 * @return The created document instance.
	 */
	public static Document newDomDocument() {
		return new XmlParser().newDocument();
	}

	/**
	 * Parse the XML document stored in the specified file.
	 *
	 * @param uri
	 *            The XML document file.
	 * @return The DOM Document.
	 */
	public Document parse(File uri) {
		Document doc = null;
		try {
			m_messages = new ArrayList<>();
			m_errors = false;
			doc = m_parser.parse(uri);
			if (m_errors) {
				doc = null; // the errors are in the m_messages
			}
		} catch (Exception ex) {
			logger.error("Unexpected exception while parsing " + uri.getAbsolutePath(), ex);
			throw new MdmiException(ex, "Unexpected exception while parsing " + uri.getAbsolutePath());
		}
		return doc;
	}

	/**
	 * Parse the XML document coming from the specified input stream.
	 *
	 * @param ios
	 *            The XML document input stream.
	 * @return The DOM Document.
	 */
	public Document parse(InputStream ios) {
		return parse(ios, null);
	}

	/**
	 * Parse the XML document coming from the specified input stream, using the specified system ID as a reference URI.
	 *
	 * @param ios
	 *            The XML document input stream.
	 * @param systemID
	 *            The system ID is the base for resolving relative URIs.
	 * @return The DOM Document.
	 */
	public Document parse(InputStream ios, String systemID) {
		Document doc = null;
		try {
			m_messages = new ArrayList<>();
			m_errors = false;
			if (systemID == null) {
				doc = m_parser.parse(ios);
			} else {
				doc = m_parser.parse(ios, systemID);
			}
			if (m_errors) {
				doc = null; // the errors are in the m_messages
			}
		} catch (Exception ex) {
			logger.error("Unexpected exception while parsing ", ex);
			throw new MdmiException(ex, "Unexpected exception while parsing " + ios.toString());
		}
		return doc;
	}

	/**
	 * Parse the XML document coming from the specified reader
	 *
	 * @param reader
	 *            The XML document character reader.
	 * @return The DOM Document.
	 */
	public Document parse(Reader reader) {
		Document doc = null;
		try {
			m_messages = new ArrayList<>();
			m_errors = false;
			doc = m_parser.parse(new InputSource(reader));
			if (m_errors) {
				doc = null; // the errors are in the m_messages
			}
		} catch (Exception ex) {
			logger.error("Unexpected exception while parsing ", ex);
			throw new MdmiException(ex, "Unexpected exception while parsing " + reader.toString());
		}
		return doc;
	}

	/**
	 * Parse the XML document stored at the specified URI.
	 *
	 * @param uri
	 *            The XML document location URI.
	 * @return The DOM Document.
	 */
	public Document parse(String uri) {
		Document doc = null;
		try {
			m_messages = new ArrayList<>();
			m_errors = false;
			doc = m_parser.parse(uri);
			if (m_errors) {
				doc = null; // the errors are in the m_messages
			}
		} catch (Exception ex) {
			logger.error("Unexpected exception while parsing ", ex);
			throw new MdmiException(ex, "Unexpected exception while parsing " + uri.toString());
		}
		return doc;
	}

	private void printError(String type, SAXParseException ex) {
		StringBuffer sb = new StringBuffer(128);
		sb.append("[");
		sb.append(type);
		sb.append("] ");
		String systemId = ex.getSystemId();
		if (systemId != null) {
			int index = systemId.lastIndexOf('/');
			if (index != -1) {
				systemId = systemId.substring(index + 1);
			}
			sb.append(systemId);
		}
		sb.append(':');
		sb.append(ex.getLineNumber());
		sb.append(':');
		sb.append(ex.getColumnNumber());
		sb.append(": ");
		sb.append(ex.getMessage());

		String s = sb.toString();
		if (m_messages == null) {
			m_messages = new ArrayList<>();
		}
		m_messages.add(s);
	}

	private class EH implements ErrorHandler {
		public void warning(SAXParseException ex) {
			printError("Warning", ex);
		}

		public void error(SAXParseException ex) {
			printError("Error", ex);
			if (m_strict) {
				m_errors = true;
			}
		}

		public void fatalError(SAXParseException ex) {
			printError("Fatal error", ex);
			m_errors = true;
		}
	} // EH
} // XmlParser
