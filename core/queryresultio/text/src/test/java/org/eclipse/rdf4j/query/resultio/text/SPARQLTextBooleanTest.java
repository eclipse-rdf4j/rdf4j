/*****************************************************************************
 *  Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 * *****************************************************************************
 */
package org.eclipse.rdf4j.query.resultio.text;

import org.eclipse.rdf4j.query.resultio.AbstractQueryResultIOBooleanTest;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;

/**
 * @author Peter Ansell
 */
public class SPARQLTextBooleanTest extends AbstractQueryResultIOBooleanTest {

	@Override
	protected String getFileName() {
		return "test.txt";
	}

	@Override
	protected BooleanQueryResultFormat getBooleanFormat() {
		return BooleanQueryResultFormat.TEXT;
	}

	@Override
	protected TupleQueryResultFormat getMatchingTupleFormatOrNull() {
		return null;
	}

}
