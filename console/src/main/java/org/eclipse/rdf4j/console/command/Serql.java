/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.util.Collection;
import java.util.Map;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleParameters;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.setting.ConsoleSetting;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.query.parser.serql.SeRQLUtil;

/**
 * SERQL query command
 * 
 * @author Bart Hanssens
 */
public class Serql extends QueryEvaluator {
	private static final String NAMESPACE = "USING NAMESPACE";
	
	@Override
	public String getName() {
		return "serql";
	}
	
	@Override
	public String getHelpShort() {
		return "Evaluate a SeRQL query";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE 
			+ "serql <query>                 Evaluates the SeRQL query on the currently open repository\n"
			+ "serql                         Starts multi-line input for large SeRQL queries.\n";
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 * @param params 
	 */
	@Deprecated
	public Serql(ConsoleIO consoleIO, ConsoleState state, ConsoleParameters params) {
		super(consoleIO, state, params);
	}
	
	/**
	 * Constructor
	 * 
	 * @param evaluator
	 * @param settings 
	 */
	public Serql(TupleAndGraphQueryEvaluator evaluator, Map<String,ConsoleSetting> settings) {
		super(evaluator, settings);
	}
	
	@Override
	protected boolean hasQueryPrefixes(String query) {
		return query.contains(NAMESPACE + " ");
	}

	@Override
	protected void addQueryPrefixes(StringBuffer result, Collection<Namespace> namespaces) {
		StringBuilder str = new StringBuilder(512);
		
		str.append(" ").append(NAMESPACE).append(" ");
		for (Namespace namespace : namespaces) {
			str.append(namespace.getPrefix()).append(" = ");
			str.append("<").append(SeRQLUtil.encodeString(namespace.getName())).append(">, ");
		}
		
	}
}