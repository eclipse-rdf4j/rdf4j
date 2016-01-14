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

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;

/**
 * An extension of {@link TurtleWriter} that writes RDF documents in <a
 * href="http://www.wiwiss.fu-berlin.de/suhl/bizer/TriG/Spec/">TriG</a> format
 * by adding graph scopes to the Turtle document.
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
	 * @param out
	 *        The OutputStream to write the TriG document to.
	 */
	public TriGWriter(OutputStream out) {
		super(out);
	}

	/**
	 * Creates a new TriGWriter that will write to the supplied Writer.
	 * 
	 * @param writer
	 *        The Writer to write the TriG document to.
	 */
	public TriGWriter(Writer writer) {
		super(writer);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public RDFFormat getRDFFormat()
	{
		return RDFFormat.TRIG;
	}

	@Override
	public void startRDF()
		throws RDFHandlerException
	{
		super.startRDF();

		inActiveContext = false;
		currentContext = null;
	}

	@Override
	public void endRDF()
		throws RDFHandlerException
	{
		super.endRDF();

		try {
			closeActiveContext();
			writer.flush();
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
		if (!writingStarted) {
			throw new RuntimeException("Document writing has not yet been started");
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
					writeResource(context);
					writer.write(" ");
				}

				writer.write("{");
				writer.increaseIndentation();

				currentContext = context;
				inActiveContext = true;
			}
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}

		super.handleStatement(st);
	}

	@Override
	protected void writeCommentLine(String line)
		throws IOException
	{
		closeActiveContext();
		super.writeCommentLine(line);
	}

	@Override
	protected void writeNamespace(String prefix, String name)
		throws IOException
	{
		closeActiveContext();
		super.writeNamespace(prefix, name);
	}

	protected void closeActiveContext()
		throws IOException
	{
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
		}
		else {
			return context1.equals(context2);
		}
	}
}
