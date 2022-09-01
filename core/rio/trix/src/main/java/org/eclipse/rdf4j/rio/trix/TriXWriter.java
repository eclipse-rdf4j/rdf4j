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
package org.eclipse.rdf4j.rio.trix;

import static org.eclipse.rdf4j.rio.trix.TriXConstants.BNODE_TAG;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.CONTEXT_TAG;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.DATATYPE_ATT;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.LANGUAGE_ATT;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.NAMESPACE;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.PLAIN_LITERAL_TAG;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.ROOT_TAG;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.TRIPLE_TAG;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.TYPED_LITERAL_TAG;
import static org.eclipse.rdf4j.rio.trix.TriXConstants.URI_TAG;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.common.io.CharSink;
import org.eclipse.rdf4j.common.xml.XMLWriter;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;
import org.eclipse.rdf4j.rio.helpers.XMLWriterSettings;

/**
 * An implementation of the RDFWriter interface that writes RDF documents in
 * <a href="http://www.w3.org/2004/03/trix/">TriX format</a>.
 *
 * @author Arjohn Kampman
 */
public class TriXWriter extends AbstractRDFWriter implements CharSink {

	private final XMLWriter xmlWriter;

	private boolean inActiveContext = false;

	private boolean convertRDFStar;
	private Resource currentContext = null;

	/**
	 * Creates a new TriXWriter that will write to the supplied OutputStream.
	 *
	 * @param out The OutputStream to write the RDF/XML document to.
	 */
	public TriXWriter(OutputStream out) {
		this.xmlWriter = new XMLWriter(out);
		this.xmlWriter.setPrettyPrint(true);
	}

	/**
	 * Creates a new TriXWriter that will write to the supplied Writer.
	 *
	 * @param writer The Writer to write the RDF/XML document to.
	 */
	public TriXWriter(Writer writer) {
		this(new XMLWriter(writer));
	}

	protected TriXWriter(XMLWriter xmlWriter) {
		this.xmlWriter = xmlWriter;
		this.xmlWriter.setPrettyPrint(true);
	}

	@Override
	public Writer getWriter() {
		return xmlWriter.getWriter();
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.TRIX;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		super.startRDF();

		try {

			if (getWriterConfig().get(XMLWriterSettings.INCLUDE_XML_PI)) {
				xmlWriter.startDocument();
			}

			xmlWriter.setAttribute("xmlns", NAMESPACE);
			xmlWriter.startTag(ROOT_TAG);
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		checkWritingStarted();
		try {
			if (inActiveContext) {
				xmlWriter.endTag(CONTEXT_TAG);
				inActiveContext = false;
				currentContext = null;
			}
			xmlWriter.endTag(ROOT_TAG);
			xmlWriter.endDocument();
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void handleNamespace(String prefix, String name) {
		checkWritingStarted();
		// ignore
	}

	@Override
	protected void consumeStatement(Statement st) {
		try {
			Resource context = st.getContext();

			if (inActiveContext && !contextsEquals(context, currentContext)) {
				// Close currently active context
				xmlWriter.endTag(CONTEXT_TAG);
				inActiveContext = false;
			}

			if (!inActiveContext) {
				// Open new context
				xmlWriter.startTag(CONTEXT_TAG);

				if (context != null) {
					writeValue(context);
				}

				currentContext = context;
				inActiveContext = true;
			}

			xmlWriter.startTag(TRIPLE_TAG);

			writeValue(st.getSubject());
			writeValue(st.getPredicate());
			writeValue(st.getObject());

			xmlWriter.endTag(TRIPLE_TAG);
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		checkWritingStarted();
		try {
			xmlWriter.comment(comment);
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	/**
	 * Writes out the XML-representation for the supplied value.
	 */
	private void writeValue(Value value) throws IOException, RDFHandlerException {
		if (value instanceof IRI) {
			IRI uri = (IRI) value;
			xmlWriter.textElement(URI_TAG, uri.toString());
		} else if (value instanceof BNode) {
			BNode bNode = (BNode) value;
			xmlWriter.textElement(BNODE_TAG, bNode.getID());
		} else if (value instanceof Literal) {
			Literal literal = (Literal) value;
			IRI datatype = literal.getDatatype();

			if (Literals.isLanguageLiteral(literal)) {
				xmlWriter.setAttribute(LANGUAGE_ATT, literal.getLanguage().get());
				xmlWriter.textElement(PLAIN_LITERAL_TAG, literal.getLabel());
			} else {
				xmlWriter.setAttribute(DATATYPE_ATT, datatype.toString());
				xmlWriter.textElement(TYPED_LITERAL_TAG, literal.getLabel());
			}
		} else {
			throw new RDFHandlerException("Unknown value type: " + value.getClass());
		}
	}

	private static boolean contextsEquals(Resource context1, Resource context2) {
		if (context1 == null) {
			return context2 == null;
		} else {
			return context1.equals(context2);
		}
	}
}
