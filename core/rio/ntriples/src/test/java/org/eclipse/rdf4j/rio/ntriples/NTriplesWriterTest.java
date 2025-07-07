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
package org.eclipse.rdf4j.rio.ntriples;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;

/**
 * JUnit test for the RDF/JSON parser.
 *
 * @author Peter Ansell
 */
public class NTriplesWriterTest extends AbstractNTriplesWriterTest {

	public NTriplesWriterTest() {
		super(new NTriplesWriterFactory(), new NTriplesParserFactory());
	}
}
