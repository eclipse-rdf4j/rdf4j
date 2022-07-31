/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.benchmark.rio.util;

import java.util.function.Consumer;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

/**
 * Empty RDF Handler that provides a {@link Statement} consumer that can be used with JMH BlackHole to avoid JVM (JIT
 * Compiler) optimizations
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
public class BlackHoleRDFHandler implements RDFHandler {
	Consumer<Statement> blackHoleConsumer;

	@Override
	public void startRDF() throws RDFHandlerException {
	}

	@Override
	public void endRDF() throws RDFHandlerException {
	}

	@Override
	public void handleNamespace(String s, String s1) throws RDFHandlerException {
	}

	@Override
	public void handleStatement(Statement statement) throws RDFHandlerException {
		blackHoleConsumer.accept(statement);
	}

	@Override
	public void handleComment(String s) throws RDFHandlerException {
	}

	// to avoid JVM "optimizing" the execution of unused code
	public void setBlackHoleConsumer(Consumer<Statement> blackHoleConsumer) {
		this.blackHoleConsumer = blackHoleConsumer;
	}
}
