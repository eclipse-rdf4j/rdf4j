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
package org.eclipse.rdf4j.console;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.ParseErrorListener;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

/**
 * Listen to parser errors and warnings
 *
 * @author Dale Visser
 */
public class VerificationListener extends AbstractRDFHandler implements ParseErrorListener {

	private final ConsoleIO consoleIO;

	/**
	 * Constructor
	 *
	 * @param consoleIO
	 */
	public VerificationListener(ConsoleIO consoleIO) {
		super();
		this.consoleIO = consoleIO;
	}

	private int warnings;
	private int errors;
	private int statements;

	/**
	 * Get number of warnings
	 *
	 * @return number of warnings
	 */
	public int getWarnings() {
		return warnings;
	}

	/**
	 * Get number of errors
	 *
	 * @return number of errors
	 */
	public int getErrors() {
		return errors;
	}

	/**
	 * Get number of statements
	 *
	 * @return number of statements
	 */
	public int getStatements() {
		return statements;
	}

	@Override
	public void handleStatement(final Statement statement) throws RDFHandlerException {
		statements++;
	}

	@Override
	public void warning(final String msg, final long lineNo, final long colNo) {
		warnings++;
		consoleIO.writeParseError("WARNING", lineNo, colNo, msg);
	}

	@Override
	public void error(final String msg, final long lineNo, final long colNo) {
		errors++;
		consoleIO.writeParseError("ERROR", lineNo, colNo, msg);
	}

	@Override
	public void fatalError(final String msg, final long lineNo, final long colNo) {
		errors++;
		consoleIO.writeParseError("FATAL ERROR", lineNo, colNo, msg);
	}
}
