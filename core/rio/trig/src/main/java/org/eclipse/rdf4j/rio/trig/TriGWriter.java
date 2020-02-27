/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.trig;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;

/**
 * An extension of {@link TurtleWriter} that writes RDF documents in
 * <a href="http://www.wiwiss.fu-berlin.de/suhl/bizer/TriG/Spec/">TriG</a> format by adding graph scopes to the Turtle
 * document.
 * 
 * @author Arjohn Kampman
 */
public class TriGWriter extends TurtleWriter {

	/*-----------*
	 * Variables *
	 *-----------*/

	private boolean inActiveContext;

	private Resource currentContext;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new TriGWriter that will write to the supplied OutputStream.
	 * 
	 * @param out The OutputStream to write the TriG document to.
	 */
	public TriGWriter(OutputStream out) {
		super(out);
	}

	/**
	 * Creates a new TriGWriter that will write to the supplied OutputStream.
	 *
	 * @param out The OutputStream to write the TriG document to.
	 */
	public TriGWriter(OutputStream out, ParsedIRI baseIRI) {
		super(out, baseIRI);
	}

	/**
	 * Creates a new TriGWriter that will write to the supplied Writer.
	 * 
	 * @param writer The Writer to write the TriG document to.
	 */
	public TriGWriter(Writer writer) {
		super(writer);
	}

	/**
	 * Creates a new TriGWriter that will write to the supplied Writer.
	 *
	 * @param writer The Writer to write the TriG document to.
	 */
	public TriGWriter(Writer writer, ParsedIRI baseIRI) {
		super(writer, baseIRI);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.TRIG;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		super.startRDF();

		inActiveContext = false;
		currentContext = null;
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		super.endRDF();

		try {
			closeActiveContext();
			writer.flush();
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	protected void handleStatementImpl(Statement st) {
		// If we are pretty-printing, all writing is buffered until endRDF is called
		if (prettyPrintModel != null) {
			prettyPrintModel.add(st);
		} else {
			handleStatementInternal(st, false, false, false);
		}
	}

	@Override
	protected void handleStatementInternal(Statement st, boolean endRDFCalled, boolean canShortenSubject,
			boolean canShortenObject) {
		// Avoid accidentally writing statements early, but don't lose track of
		// them if they are sent here
		if (prettyPrintModel != null && !endRDFCalled) {
			prettyPrintModel.add(st);
			return;
		}

		try {
			Resource context = st.getContext();

			if (inActiveContext && !contextsEquals(context, currentContext)) {
				closePreviousStatement();
				closeActiveContext();
			}

			if (!inActiveContext) {
				writer.writeEOL();

				if (context != null) {
					boolean canShortenContext = false;
					if (context instanceof BNode) {
						if (prettyPrintModel != null && !prettyPrintModel.contains(context, null, null)
								&& !prettyPrintModel.contains(null, null, context)) {
							canShortenContext = true;
						}
					}
					writeResource(context, canShortenContext);
					writer.write(" ");
				}

				writer.write("{");
				writer.increaseIndentation();

				currentContext = context;
				inActiveContext = true;
			}
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}

		// If we get to this point, switch endRDFCalled to true so writing occurs
		super.handleStatementInternal(st, true, canShortenSubject, canShortenObject);
	}

	@Override
	protected void writeCommentLine(String line) throws IOException {
		// Comments can be written anywhere, so disabling this
		// closeActiveContext();
		super.writeCommentLine(line);
	}

	@Override
	protected void writeNamespace(String prefix, String name) throws IOException {
		if (currentContext != null && currentContext instanceof BNode) {
			// FIXME: No formal way to warn the user that things may break in this situation
		}
		// TriG spec requires that we close the active context before writing a namespace declaration
		closeActiveContext();
		super.writeNamespace(prefix, name);
	}

	protected void closeActiveContext() throws IOException {
		if (inActiveContext) {
			writer.decreaseIndentation();
			writer.write("}");
			writer.writeEOL();

			inActiveContext = false;
			currentContext = null;
		}
	}

	private static final boolean contextsEquals(Resource context1, Resource context2) {
		if (context1 == null) {
			return context2 == null;
		} else {
			return context1.equals(context2);
		}
	}
}
