/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.benchmark;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkConfigs {
	{
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}
	public static final int NUMBER_OF_TRANSACTIONS = 30;
	public static final int NUMBER_OF_EMPTY_TRANSACTIONS = 10;

	public static final int STATEMENTS_PER_TRANSACTION = 100;

	public static List<List<Statement>> generateStatements(StatementCreator statementCreator) {

		List<List<Statement>> allStatements = new ArrayList<>();

		for (int j = 0; j < BenchmarkConfigs.NUMBER_OF_TRANSACTIONS; j++) {
			List<Statement> statements = new ArrayList<>();
			allStatements.add(statements);
			for (int i = 0; i < BenchmarkConfigs.STATEMENTS_PER_TRANSACTION; i++) {

				statementCreator.createStatement(statements, i, j);
			}
		}

		for (int j = 0; j < BenchmarkConfigs.NUMBER_OF_EMPTY_TRANSACTIONS; j++) {
			List<Statement> statements = new ArrayList<>();
			allStatements.add(statements);
		}

		return allStatements;
	}

	interface StatementCreator {
		void createStatement(List<Statement> statements, int i, int j);
	}

}
