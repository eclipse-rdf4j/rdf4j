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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.text.StringUtil;
import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.Util;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;

/**
 * Write query results to console
 *
 * @author Bart Hanssens
 */
public class ConsoleQueryResultWriter extends AbstractQueryResultWriter {
	private final ConsoleIO consoleIO;
	private final int consoleWidth;
	private final Map<String, String> namespaces = new HashMap<>();
	private List<String> bindingNames;
	private int columnWidth;
	private String separatorLine = "";
	private String header = "";
	private final TupleQueryResultFormat queryResultFormat = new TupleQueryResultFormat("Console query result format",
			"application/x-dummy", "dummy", true);

	/**
	 * Constructor
	 *
	 * @param consoleIO
	 * @param consoleWidth console width
	 */
	public ConsoleQueryResultWriter(ConsoleIO consoleIO, int consoleWidth) {
		this.consoleIO = consoleIO;
		this.consoleWidth = consoleWidth;
	}

	@Override
	public QueryResultFormat getQueryResultFormat() {
		return queryResultFormat;
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws QueryResultHandlerException {
		// use uri as the key, so the prefix can be retrieved and shown on the console
		namespaces.put(uri, prefix);
	}

	@Override
	public void startDocument() throws QueryResultHandlerException {
		//
	}

	@Override
	public void handleStylesheet(String stylesheetUrl) throws QueryResultHandlerException {
		//
	}

	@Override
	public void startHeader() throws QueryResultHandlerException {
	}

	@Override
	public void endHeader() throws QueryResultHandlerException {
		consoleIO.writeln(separatorLine);
		consoleIO.writeln(header);
		consoleIO.writeln(separatorLine);
	}

	@Override
	public void handleBoolean(boolean value) throws QueryResultHandlerException {
		consoleIO.writeln("Answer: " + value);
	}

	@Override
	public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
		//
	}

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
		super.startQueryResult(bindingNames);

		this.bindingNames = bindingNames;
		int columns = bindingNames.size();
		columnWidth = (consoleWidth - 1) / columns - 3;

		StringBuilder builder = new StringBuilder(consoleWidth);
		for (int i = columns; i > 0; i--) {
			builder.append('+');
			StringUtil.appendN('-', columnWidth + 1, builder);
		}
		builder.append('+');
		separatorLine = builder.toString();

		// Build table header
		builder = new StringBuilder(consoleWidth);
		for (String bindingName : bindingNames) {
			builder.append("| ").append(bindingName);
			StringUtil.appendN(' ', columnWidth - bindingName.length(), builder);
		}
		builder.append("|");
		header = builder.toString();
	}

	@Override
	public void endQueryResult() throws TupleQueryResultHandlerException {
		if (!separatorLine.isEmpty()) {
			consoleIO.writeln(separatorLine);
		}
	}

	@Override
	protected void handleSolutionImpl(BindingSet bindingSet) throws TupleQueryResultHandlerException {
		StringBuilder builder = new StringBuilder(512);

		for (String bindingName : bindingNames) {
			Value value = bindingSet.getValue(bindingName);
			String valueStr = (value != null) ? Util.getPrefixedValue(value, namespaces) : "";
			builder.append("| ").append(valueStr);
			StringUtil.appendN(' ', columnWidth - valueStr.length(), builder);
		}
		builder.append("|");
		consoleIO.writeln(builder.toString());
	}
}
