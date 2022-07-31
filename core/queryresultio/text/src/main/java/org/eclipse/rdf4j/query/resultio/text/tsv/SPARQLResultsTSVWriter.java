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
package org.eclipse.rdf4j.query.resultio.text.tsv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.rdf4j.common.io.CharSink;
import org.eclipse.rdf4j.common.text.StringUtil;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;

/**
 * TupleQueryResultWriter for the SPARQL TSV (Tab-Separated Values) format.
 *
 * @see <a href="http://www.w3.org/TR/sparql11-results-csv-tsv/#tsv">SPARQL 1.1 Query Results TSV Format</a>
 * @author Jeen Broekstra
 */
public class SPARQLResultsTSVWriter extends AbstractQueryResultWriter implements TupleQueryResultWriter, CharSink {

	protected Writer writer;

	private List<String> bindingNames;

	protected boolean tupleVariablesFound = false;

	/**
	 * @param out
	 */
	public SPARQLResultsTSVWriter(OutputStream out) {
		this(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), 1024));
	}

	public SPARQLResultsTSVWriter(Writer writer) {
		this.writer = writer;
	}

	@Override
	public Writer getWriter() {
		return writer;
	}

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
		super.startQueryResult(bindingNames);

		tupleVariablesFound = true;

		this.bindingNames = bindingNames;

		try {
			for (int i = 0; i < bindingNames.size(); i++) {
				writer.write("?"); // mandatory prefix in TSV
				writer.write(bindingNames.get(i));
				if (i < bindingNames.size() - 1) {
					writer.write("\t");
				}
			}
			writer.write("\n");
		} catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void endQueryResult() throws TupleQueryResultHandlerException {
		if (!tupleVariablesFound) {
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
		if (!tupleVariablesFound) {
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
					writer.write("\t");
				}
			}
			writer.write("\n");
		} catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.TSV;
	}

	@Override
	public final TupleQueryResultFormat getQueryResultFormat() {
		return getTupleQueryResultFormat();
	}

	protected void writeValue(Value val) throws IOException {
		if (val instanceof Triple) {
			writer.write("<<");
			writeValue(((Triple) val).getSubject());
			writer.write(' ');
			writeValue(((Triple) val).getPredicate());
			writer.write(' ');
			writeValue(((Triple) val).getObject());
			writer.write(">>");
		} else if (val instanceof Resource) {
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
		writer.write("<" + uriString + ">");
	}

	protected void writeBNode(BNode bNode) throws IOException {
		writer.write("_:");
		writer.write(bNode.getID());
	}

	private void writeLiteral(Literal lit) throws IOException {
		String label = lit.getLabel();

		IRI datatype = lit.getDatatype();

		if (XSD.INTEGER.equals(datatype) || XSD.DECIMAL.equals(datatype)
				|| XSD.DOUBLE.equals(datatype)) {
			try {
				writer.write(XMLDatatypeUtil.normalize(label, datatype));
				return; // done
			} catch (IllegalArgumentException e) {
				// not a valid numeric typed literal. ignore error and write as
				// quoted string instead.
			}
		}

		String encoded = encodeString(label);

		if (Literals.isLanguageLiteral(lit)) {
			writer.write("\"");
			writer.write(encoded);
			writer.write("\"");
			// Append the literal's language
			writer.write("@");
			writer.write(lit.getLanguage().get());
		} else if (!XSD.STRING.equals(datatype) || !xsdStringToPlainLiteral()) {
			writer.write("\"");
			writer.write(encoded);
			writer.write("\"");
			// Append the literal's datatype
			writer.write("^^");
			writeURI(datatype);
		} else if (label.length() > 0 && encoded.equals(label) && label.charAt(0) != '<' && label.charAt(0) != '_'
				&& !label.matches("^[\\+\\-]?[\\d\\.].*")) {
			// no need to include double quotes
			writer.write(encoded);
		} else {
			writer.write("\"");
			writer.write(encoded);
			writer.write("\"");
		}
	}

	private static String encodeString(String s) {
		s = StringUtil.gsub("\\", "\\\\", s);
		s = StringUtil.gsub("\t", "\\t", s);
		s = StringUtil.gsub("\n", "\\n", s);
		s = StringUtil.gsub("\r", "\\r", s);
		s = StringUtil.gsub("\"", "\\\"", s);
		return s;
	}

	@Override
	public void startDocument() throws TupleQueryResultHandlerException {
		// Ignored in SPARQLResultsTSVWriter
	}

	@Override
	public void handleStylesheet(String stylesheetUrl) throws TupleQueryResultHandlerException {
		// Ignored in SPARQLResultsTSVWriter
	}

	@Override
	public void startHeader() throws TupleQueryResultHandlerException {
		// Ignored in SPARQLResultsTSVWriter
	}

	@Override
	public void handleLinks(List<String> linkUrls) throws TupleQueryResultHandlerException {
		// Ignored in SPARQLResultsTSVWriter
	}

	@Override
	public void endHeader() throws TupleQueryResultHandlerException {
		// Ignored in SPARQLResultsTSVWriter
	}

	@Override
	public void handleBoolean(boolean value) throws QueryResultHandlerException {
		throw new UnsupportedOperationException("Cannot handle boolean results");
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws QueryResultHandlerException {
		// Ignored in SPARQLResultsTSVWriter
	}
}
