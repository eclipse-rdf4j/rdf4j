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
package org.eclipse.rdf4j.rio.ntriples;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.io.CharSink;
import org.eclipse.rdf4j.common.text.ASCIIUtil;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.eclipse.rdf4j.rio.helpers.NTriplesWriterSettings;

/**
 * An implementation of the RDFWriter interface that writes RDF documents in N-Triples format. The N-Triples format is
 * defined in <a href="http://www.w3.org/TR/rdf-testcases/#ntriples">this section</a> of the RDF Test Cases document.
 */
public class NTriplesWriter extends AbstractRDFWriter implements CharSink {

	protected final Writer writer;

	private boolean xsdStringToPlainLiteral = true;
	private boolean escapeUnicode;

	/**
	 * Creates a new NTriplesWriter that will write to the supplied OutputStream.
	 *
	 * @param out The OutputStream to write the N-Triples document to.
	 */
	public NTriplesWriter(OutputStream out) {
		this.writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
	}

	/**
	 * Creates a new NTriplesWriter that will write to the supplied Writer.
	 *
	 * @param writer The Writer to write the N-Triples document to.
	 */
	public NTriplesWriter(Writer writer) {
		this.writer = writer;
	}

	@Override
	public Writer getWriter() {
		return writer;
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.NTRIPLES;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		super.startRDF();
		xsdStringToPlainLiteral = getWriterConfig().get(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL);
		escapeUnicode = getWriterConfig().get(NTriplesWriterSettings.ESCAPE_UNICODE);
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		checkWritingStarted();
		try {
			writer.flush();
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void handleNamespace(String prefix, String name) {
		checkWritingStarted();
		// N-Triples does not support namespace prefixes.
	}

	@Override
	protected void consumeStatement(Statement st) {
		try {
			writeValue(st.getSubject());
			writer.write(" ");
			writeIRI(st.getPredicate());
			writer.write(" ");
			writeValue(st.getObject());

			writer.write(" .\n");
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		checkWritingStarted();
		try {
			writer.write("# ");
			writer.write(comment);
			writer.write("\n");
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public final Collection<RioSetting<?>> getSupportedSettings() {
		Set<RioSetting<?>> result = new HashSet<>(super.getSupportedSettings());

		result.add(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL);
		result.add(NTriplesWriterSettings.ESCAPE_UNICODE);

		return result;
	}

	/**
	 * Writes the N-Triples representation of the given {@link Value}.
	 *
	 * @param value The value to write.
	 * @throws IOException
	 */
	protected void writeValue(Value value) throws IOException {
		if (value instanceof IRI) {
			writeIRI((IRI) value);
		} else if (value instanceof BNode) {
			writeBNode((BNode) value);
		} else if (value instanceof Literal) {
			writeLiteral((Literal) value);
		} else {
			throw new IllegalArgumentException("Unknown value type: " + value.getClass());
		}
	}

	private void writeIRI(IRI iri) throws IOException {
		NTriplesUtil.append(iri, writer, escapeUnicode);
	}

	private void writeBNode(BNode bNode) throws IOException {
		String nextId = bNode.getID();
		writer.append("_:");

		if (nextId.isEmpty()) {
			writer.append("genid");
			writer.append(Integer.toHexString(bNode.hashCode()));
		} else {
			if (!ASCIIUtil.isLetter(nextId.charAt(0))) {
				writer.append("genid");
				writer.append(Integer.toHexString(nextId.charAt(0)));
			}

			for (int i = 0; i < nextId.length(); i++) {
				if (ASCIIUtil.isLetterOrNumber(nextId.charAt(i))) {
					writer.append(nextId.charAt(i));
				} else {
					// Append the character as its hex representation
					writer.append(Integer.toHexString(nextId.charAt(i)));
				}
			}
		}
	}

	/**
	 * Write the N-Triples representation of the given {@link Literal}, optionally ignoring the xsd:string datatype as
	 * it is implied for RDF-1.1.
	 *
	 * @param lit The literal to write.
	 * @throws IOException
	 */
	private void writeLiteral(Literal lit) throws IOException {
		NTriplesUtil.append(lit, writer, getWriterConfig().get(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL),
				escapeUnicode);
	}
}
