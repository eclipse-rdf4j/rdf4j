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
package org.eclipse.rdf4j.rio.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.rio.ParseErrorListener;

/**
 * A ParseErrorListener that collects Rio parse errors in the sequence they were collected in.
 *
 * @author Peter Ansell
 */
public class ParseErrorCollector implements ParseErrorListener {

	private final List<String> warnings = new ArrayList<>();

	private final List<String> errors = new ArrayList<>();

	private final List<String> fatalErrors = new ArrayList<>();

	@Override
	public void warning(String msg, long lineNo, long colNo) {
		warnings.add(msg + " (" + lineNo + ", " + colNo + ")");
	}

	@Override
	public void error(String msg, long lineNo, long colNo) {
		errors.add("[Rio error] " + msg + " (" + lineNo + ", " + colNo + ")");
	}

	@Override
	public void fatalError(String msg, long lineNo, long colNo) {
		fatalErrors.add("[Rio fatal] " + msg + " (" + lineNo + ", " + colNo + ")");
	}

	/**
	 * @return An unmodifiable list of strings representing warnings that were received using the
	 *         {@link ParseErrorListener#warning(String, long, long)} interface.
	 */
	public List<String> getWarnings() {
		return Collections.unmodifiableList(warnings);
	}

	/**
	 * @return An unmodifiable list of strings representing potential errors that were received using the
	 *         {@link ParseErrorListener#error(String, long, long)} interface.
	 */
	public List<String> getErrors() {
		return Collections.unmodifiableList(errors);
	}

	/**
	 * @return An unmodifiable list of strings representing fatal errors that were received using the
	 *         {@link ParseErrorListener#fatalError(String, long, long)} interface.
	 */
	public List<String> getFatalErrors() {
		return Collections.unmodifiableList(fatalErrors);
	}

	/**
	 * Resets the lists of warnings, errors and fatal errors.
	 */
	public void reset() {
		warnings.clear();
		errors.clear();
		fatalErrors.clear();
	}
}
