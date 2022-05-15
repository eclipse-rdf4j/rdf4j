/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.rdfxml;

import java.io.InputStream;

import org.eclipse.rdf4j.rio.BaseURIHandlingTest;
import org.eclipse.rdf4j.rio.RDFParserFactory;

/**
 * @author Jeen Broekstra
 */
public class RDFXMLBaseURIHandlingTest extends BaseURIHandlingTest {

	@Override
	protected InputStream getDataWithAbsoluteIris() {
		return RDFXMLBaseURIHandlingTest.class
				.getResourceAsStream("/org/eclipse/rdf4j/rio/rdfxml/rdfxml-absolute-iris.rdf");
	}

	@Override
	protected InputStream getDataWithRelativeIris() {
		return RDFXMLBaseURIHandlingTest.class
				.getResourceAsStream("/org/eclipse/rdf4j/rio/rdfxml/rdfxml-relative-iris-missing-base.rdf");
	}

	@Override
	protected InputStream getDataWithRelativeIris_InternalBase() {
		return RDFXMLBaseURIHandlingTest.class
				.getResourceAsStream("/org/eclipse/rdf4j/rio/rdfxml/rdfxml-relative-iris-internal-base.rdf");
	}

	@Override
	protected RDFParserFactory getParserFactory() {
		return new RDFXMLParserFactory();
	}

}
