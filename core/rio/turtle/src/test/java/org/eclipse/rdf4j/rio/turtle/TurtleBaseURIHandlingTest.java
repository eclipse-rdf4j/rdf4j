/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import java.io.InputStream;

import org.eclipse.rdf4j.rio.BaseURIHandlingTest;
import org.eclipse.rdf4j.rio.RDFParserFactory;

/**
 * @author jeen
 *
 */
public class TurtleBaseURIHandlingTest extends BaseURIHandlingTest {

	@Override
	protected RDFParserFactory getParserFactory() {
		return new TurtleParserFactory();
	}

	@Override
	protected InputStream getDataWithAbsoluteIris() {
		return getClass().getResourceAsStream("/org/eclipse/rdf4j/rio/turtle/turtle-absolute-iris.ttl");
	}

	@Override
	protected InputStream getDataWithRelativeIris() {
		return getClass().getResourceAsStream("/org/eclipse/rdf4j/rio/turtle/turtle-relative-iris-missing-base.ttl");
	}

	@Override
	protected InputStream getDataWithRelativeIris_InternalBase() {
		return getClass().getResourceAsStream("/org/eclipse/rdf4j/rio/turtle/turtle-relative-iris-internal-base.ttl");
	}

}
