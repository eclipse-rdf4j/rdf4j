/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console.util;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.Util;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;

/**
 * Write query results to console
 *
 * @author Bart Hanssens
 */
public class ConsoleRDFWriter extends AbstractRDFWriter {
	private final ConsoleIO consoleIO;
	private final int consoleWidth;
	private final Map<String, String> namespaces = new HashMap<>();
	private int columnWidth;
	private final String separatorLine = "";
	private final String header = "";
	private final RDFFormat rdfFormat = new RDFFormat("Console RDF", "application/x-dummy", StandardCharsets.UTF_8,
			"dummy", true, false, true);

	/**
	 * Constructor
	 *
	 * @param consoleIO
	 * @param consoleWidth console width
	 */
	public ConsoleRDFWriter(ConsoleIO consoleIO, int consoleWidth) {
		this.consoleIO = consoleIO;
		this.consoleWidth = consoleWidth;
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws QueryResultHandlerException {
		checkWritingStarted();
		// use uri as the key, so the prefix can be retrieved and shown on the console
		namespaces.put(uri, prefix);
	}

	@Override
	public RDFFormat getRDFFormat() {
		return rdfFormat;
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		checkWritingStarted();
		// do nothing
	}

	@Override
	public void consumeStatement(Statement st) throws RDFHandlerException {
		consoleIO.write(Util.getPrefixedValue(st.getSubject(), namespaces));
		consoleIO.write("   ");
		consoleIO.write(Util.getPrefixedValue(st.getPredicate(), namespaces));
		consoleIO.write("   ");
		consoleIO.write(Util.getPrefixedValue(st.getObject(), namespaces));
		consoleIO.writeln();
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		checkWritingStarted();
		// do nothing
	}
}
