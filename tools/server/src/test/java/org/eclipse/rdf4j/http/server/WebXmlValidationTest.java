/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import org.eclipse.rdf4j.common.xml.DocumentUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author Herko ter Horst
 */
public class WebXmlValidationTest {

	@Test
	@Ignore("temporarily disabled to avoid problems with downloading of XML Schema on Hudson instance")
	public void testValidXml() throws MalformedURLException, IOException, SAXException {
		File webXml = new File("src/main/webapp/WEB-INF/web.xml");

		DocumentUtil.getDocument(webXml.toURL(),
				SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema());
	}
}
