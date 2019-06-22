/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import org.eclipse.rdf4j.query.resultio.AbstractQueryResultIOTupleTest;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;

/**
 * @author Peter Ansell
 */
public class SPARQLXMLTupleTest extends AbstractQueryResultIOTupleTest {

	@Override
	protected String getFileName() {
		return "test.srx";
	}

	@Override
	protected TupleQueryResultFormat getTupleFormat() {
		return TupleQueryResultFormat.SPARQL;
	}

	@Override
	protected BooleanQueryResultFormat getMatchingBooleanFormatOrNull() {
		return BooleanQueryResultFormat.SPARQL;
	}
}
