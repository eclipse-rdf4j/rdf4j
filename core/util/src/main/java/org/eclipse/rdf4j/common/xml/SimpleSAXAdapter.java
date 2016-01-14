/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
 
package org.eclipse.rdf4j.common.xml;

import java.util.Map;

import org.xml.sax.SAXException;

/**
 * An implementation of <tt>SimpleSAXListener</tt> providing dummy
 * implementations for all its methods.
 */
public class SimpleSAXAdapter implements SimpleSAXListener {

	// implements SimpleSAXListener.startDocument()
	public void startDocument()
		throws SAXException
	{
	}

	// implements SimpleSAXListener.endDocument()
	public void endDocument()
		throws SAXException
	{
	}

	// implements SimpleSAXListener.startTag()
	public void startTag(String tagName, Map<String, String> atts, String text)
		throws SAXException
	{
	}

	// implements SimpleSAXListener.endTag()
	public void endTag(String tagName)
		throws SAXException
	{
	}
}
