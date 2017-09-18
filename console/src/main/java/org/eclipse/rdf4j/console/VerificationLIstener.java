/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.ParseErrorListener;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

/**
 * @author Dale Visser
 */
class VerificationListener extends AbstractRDFHandler implements ParseErrorListener {

	private final ConsoleIO consoleIO;

	VerificationListener(ConsoleIO consoleIO) {
		super();
		this.consoleIO = consoleIO;
	}

	private int warnings;

	private int errors;

	private int statements;

	public int getWarnings() {
		return warnings;
	}

	public int getErrors() {
		return errors;
	}

	public int getStatements() {
		return statements;
	}

	public void handleStatement(final Statement statement)
		throws RDFHandlerException
	{
		statements++;
	}

	public void warning(final String msg, final long lineNo, final long colNo) {
		warnings++;
		consoleIO.writeParseError("WARNING", lineNo, colNo, msg);
	}

	public void error(final String msg, final long lineNo, final long colNo) {
		errors++;
		consoleIO.writeParseError("ERROR", lineNo, colNo, msg);
	}

	public void fatalError(final String msg, final long lineNo, final long colNo) {
		errors++;
		consoleIO.writeParseError("FATAL ERROR", lineNo, colNo, msg);
	}
}
