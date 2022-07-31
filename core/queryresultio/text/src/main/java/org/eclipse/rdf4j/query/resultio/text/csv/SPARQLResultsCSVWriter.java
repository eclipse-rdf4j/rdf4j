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
package org.eclipse.rdf4j.query.resultio.text.csv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.rdf4j.common.io.CharSink;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;

/**
 * TupleQueryResultWriter for the SPARQL CSV (Comma-Separated Values) format.
 *
 * @see <a href="http://www.w3.org/TR/sparql11-results-csv-tsv/#csv">SPARQL 1.1 Query Results CSV Format</a>
 * @author Jeen Broekstra
 */
public class SPARQLResultsCSVWriter extends AbstractQueryResultWriter implements TupleQueryResultWriter, CharSink {

	private final Writer writer;

	private List<String> bindingNames;

	/**
	 * @param out
	 */
	public SPARQLResultsCSVWriter(OutputStream out) {
		this(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), 1024));
	}

	public SPARQLResultsCSVWriter(Writer writer) {
		this.writer = writer;
	}

	public Writer getWriter() {
		return writer;
	}

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
		super.startQueryResult(bindingNames);

		this.bindingNames = bindingNames;

		try {
			for (int i = 0; i < bindingNames.size(); i++) {
				writer.write(bindingNames.get(i));
				if (i < bindingNames.size() - 1) {
					writer.write(",");
				}
			}
			writer.write("\r\n");
		} catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void endQueryResult() throws TupleQueryResultHandlerException {
		if (bindingNames == null) {
			throw new IllegalStateException("Could not end query result as startQueryResult was not called first.");
		}
		try {
			writer.flush();
		} catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}

	}

	@Override
	protected void handleSolutionImpl(BindingSet bindingSet) throws TupleQueryResultHandlerException {
		if (bindingNames == null) {
			throw new IllegalStateException("Must call startQueryResult before handleSolution");
		}
		try {
			for (int i = 0; i < bindingNames.size(); i++) {
				String name = bindingNames.get(i);
				Value value = bindingSet.getValue(name);
				if (value != null) {
					writeValue(value);
				}

				if (i < bindingNames.size() - 1) {
					writer.write(",");
				}
			}
			writer.write("\r\n");
		} catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public final TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.CSV;
	}

	@Override
	public final TupleQueryResultFormat getQueryResultFormat() {
		return getTupleQueryResultFormat();
	}

	protected void writeValue(Value val) throws IOException {
		if (val instanceof Resource) {
			writeResource((Resource) val);
		} else {
			writeLiteral((Literal) val);
		}
	}

	protected void writeResource(Resource res) throws IOException {
		if (res instanceof IRI) {
			writeURI((IRI) res);
		} else {
			writeBNode((BNode) res);
		}
	}

	protected void writeURI(IRI uri) throws IOException {
		String uriString = uri.toString();
		boolean quoted = uriString.contains(",");

		if (quoted) {
			// write opening quote for entire value
			writer.write("\"");
		}

		writer.write(uriString);

		if (quoted) {
			// write closing quote for entire value
			writer.write("\"");
		}
	}

	protected void writeBNode(BNode bNode) throws IOException {
		writer.write("_:");
		writer.write(bNode.getID());
	}

	private void writeLiteral(Literal literal) throws IOException {
		String label = literal.getLabel();
		IRI datatype = literal.getDatatype();

		boolean quoted = false;

		if (XMLDatatypeUtil.isIntegerDatatype(datatype) || XMLDatatypeUtil.isDecimalDatatype(datatype)
				|| XSD.DOUBLE.equals(datatype)) {
			try {
				String normalized = XMLDatatypeUtil.normalize(label, datatype);
				writer.write(normalized);
				return; // done
			} catch (IllegalArgumentException e) {
				// not a valid numeric datatyped literal. ignore error and write as
				// (optionally quoted) string instead.
			}
		}

		if (label.contains(",") || label.contains("\r") || label.contains("\n") || label.contains("\"")) {
			quoted = true;

			// escape quotes inside the string
			label = label.replace("\"", "\"\"");
		}

		if (quoted) {
			// write opening quote for entire value
			writer.write("\"");
		}

		writer.write(label);

		if (quoted) {
			// write closing quote for entire value
			writer.write("\"");
		}

	}

	@Override
	public void startDocument() throws QueryResultHandlerException {
		// Ignored by SPARQLResultsCSVWriter
	}

	@Override
	public void handleStylesheet(String stylesheetUrl) throws QueryResultHandlerException {
		// Ignored by SPARQLResultsCSVWriter
	}

	@Override
	public void startHeader() throws QueryResultHandlerException {
		// Ignored by SPARQLResultsCSVWriter
	}

	@Override
	public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
		// Ignored by SPARQLResultsCSVWriter
	}

	@Override
	public void endHeader() throws QueryResultHandlerException {
		// Ignored by SPARQLResultsCSVWriter
	}

	@Override
	public void handleBoolean(boolean value) throws QueryResultHandlerException {
		throw new UnsupportedOperationException("Cannot handle boolean results");
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws QueryResultHandlerException {
		// Ignored by SPARQLResultsCSVWriter
	}

}
