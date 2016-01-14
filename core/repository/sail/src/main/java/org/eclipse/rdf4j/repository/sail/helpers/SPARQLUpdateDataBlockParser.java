/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.helpers;

import java.io.IOException;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.trig.TriGParser;

/**
 * An extension of {@link TriGParser} that processes data in the format
 * specified in the SPARQL 1.1 grammar for Quad data (assuming no variables, as
 * is the case for INSERT DATA and DELETE DATA operations). This format is
 * almost completely compatible with TriG, except for three differences:
 * <ul>
 * <li>it introduces the 'GRAPH' keyword in front of each named graph identifier
 * <li>it does not allow the occurrence of blank nodes.
 * <li>it does not require curly braces around the default graph.
 * </ul>
 * 
 * @author Jeen Broekstra
 * @see <a href="http://www.w3.org/TR/sparql11-query/#rInsertData">SPARQL 1.1
 *      Grammar production for INSERT DATA</a>
 * @see <a href="http://www.w3.org/TR/sparql11-query/#rDeleteData">SPARQL 1.1
 *      Grammar production for DELETE DATA</a>
 */
public class SPARQLUpdateDataBlockParser extends TriGParser {

	private boolean allowBlankNodes = true;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new parser that will use a {@link SimpleValueFactory} to create
	 * RDF model objects.
	 */
	public SPARQLUpdateDataBlockParser() {
		super();
	}

	/**
	 * Creates a new parser that will use the supplied ValueFactory to create RDF
	 * model objects.
	 * 
	 * @param valueFactory
	 *        A ValueFactory.
	 */
	public SPARQLUpdateDataBlockParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public RDFFormat getRDFFormat() {
		// TODO for now, we do not implement this as a fully compatible Rio
		// parser, and we're not introducing a new RDFFormat constant.
		return null;
	}

	@Override
	protected void parseGraph() throws RDFParseException, RDFHandlerException, IOException {
		super.parseGraph();
		skipOptionalPeriod();
	}
	
	@Override
	protected Resource parseImplicitBlank()
		throws IOException, RDFParseException, RDFHandlerException
	{
		if (isAllowBlankNodes()) {
			return super.parseImplicitBlank();
		}
		else {
			throw new RDFParseException("blank nodes not allowed in data block");
		}
	}

	@Override
	protected BNode parseNodeID()
		throws IOException, RDFParseException
	{
		if (isAllowBlankNodes()) {
			return super.parseNodeID();
		}
		else {
			throw new RDFParseException("blank nodes not allowed in data block");
		}
	}

	/**
	 * @return Returns the allowBlankNodes.
	 */
	public boolean isAllowBlankNodes() {
		return allowBlankNodes;
	}

	/**
	 * @param allowBlankNodes
	 *        The allowBlankNodes to set.
	 */
	public void setAllowBlankNodes(boolean allowBlankNodes) {
		this.allowBlankNodes = allowBlankNodes;
	}

	private void skipOptionalPeriod()
		throws RDFHandlerException, IOException
	{
		skipWSC();
		int c = peekCodePoint();
		if (c == '.') {
			readCodePoint();
		}
	}
}
