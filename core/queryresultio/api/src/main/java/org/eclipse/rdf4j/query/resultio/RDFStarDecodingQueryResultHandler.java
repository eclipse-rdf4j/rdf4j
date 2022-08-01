/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio;

import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQueryResultHandler;
import org.eclipse.rdf4j.query.QueryResultHandler;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.rio.helpers.RDFStarUtil;

/**
 * A {@link QueryResultHandler} that delegates all results to another handler and processes RDF-star triples encoded as
 * special IRIs back to RDF-star triple values.
 *
 * @author Pavel Mihaylov
 */
class RDFStarDecodingQueryResultHandler implements TupleQueryResultHandler, BooleanQueryResultHandler {
	private final QueryResultHandler delegate;

	RDFStarDecodingQueryResultHandler(QueryResultHandler delegate) {
		this.delegate = delegate;
	}

	@Override
	public void handleBoolean(boolean value) throws QueryResultHandlerException {
		delegate.handleBoolean(value);
	}

	@Override
	public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
		delegate.handleLinks(linkUrls);
	}

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
		delegate.startQueryResult(bindingNames);
	}

	@Override
	public void endQueryResult() throws TupleQueryResultHandlerException {
		delegate.endQueryResult();
	}

	@Override
	public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
		delegate.handleSolution(new ValueMappingBindingSet(bindingSet, RDFStarUtil::fromRDFEncodedValue));
	}
}
