/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqlstarjson;

import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.AbstractQueryResultIOTupleTest;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.UnsupportedQueryResultFormatException;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Pavel Mihaylov
 */
public class SPARQLStarJSONTupleBackgroundTest extends AbstractQueryResultIOTupleTest {
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

	@Override
	protected TupleQueryResult parseTupleInternal(TupleQueryResultFormat format, InputStream in) throws IOException,
			QueryResultParseException, TupleQueryResultHandlerException, UnsupportedQueryResultFormatException {
		return QueryResultIO.parseTupleBackground(in, format);
	}
}
