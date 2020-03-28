/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqlstarjson;

import org.eclipse.rdf4j.query.resultio.AbstractQueryResultIOTupleTest;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;

/**
 * @author Pavel Mihaylov
 */
public class SPARQLStarJSONTupleTest extends AbstractQueryResultIOTupleTest {
	@Override
	protected String getFileName() {
		return "test.srjs";
	}

	@Override
	protected TupleQueryResultFormat getTupleFormat() {
		return TupleQueryResultFormat.JSON_STAR;
	}

	@Override
	protected BooleanQueryResultFormat getMatchingBooleanFormatOrNull() {
		return BooleanQueryResultFormat.JSON;
	}
}
