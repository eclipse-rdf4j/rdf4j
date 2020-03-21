/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.n3;

import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;

/**
 * An implementation of the RDFWriter interface that writes RDF documents in N3 format. Note: the current implementation
 * simply wraps a {@link TurtleWriter} and writes documents in Turtle format, which is a subset of N3.
 */
public class N3Writer extends AbstractRDFWriter implements RDFWriter {

	/*-----------*
	 * Variables *
	 *-----------*/

	private TurtleWriter ttlWriter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new N3Writer that will write to the supplied OutputStream.
	 * 
	 * @param out The OutputStream to write the N3 document to.
	 */
	public N3Writer(OutputStream out) {
		this(out, null);
	}

	/**
	 * Creates a new N3Writer that will write to the supplied OutputStream.
	 *
	 * @param out     The OutputStream to write the N3 document to.
	 * @param baseIRI used to relativize IRIs to relative IRIs.
	 */
	public N3Writer(OutputStream out, ParsedIRI baseIRI) {
		super(out);
		ttlWriter = new TurtleWriter(out, baseIRI);
	}

	/**
	 * Creates a new N3Writer that will write to the supplied Writer.
	 * 
	 * @param writer The Writer to write the N3 document to.
	 */
	public N3Writer(Writer writer) {
		this(writer, null);
	}

	/**
	 * Creates a new N3Writer that will write to the supplied Writer.
	 *
	 * @param writer  The Writer to write the N3 document to.
	 * @param baseIRI used to relativize IRIs to relative IRIs.
	 */
	public N3Writer(Writer writer, ParsedIRI baseIRI) {
		ttlWriter = new TurtleWriter(writer, baseIRI);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.N3;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		ttlWriter.startRDF();
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		ttlWriter.endRDF();
	}

	@Override
	public void handleNamespace(String prefix, String name) throws RDFHandlerException {
		ttlWriter.handleNamespace(prefix, name);
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		ttlWriter.handleStatement(st);
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		ttlWriter.handleComment(comment);
	}
}
