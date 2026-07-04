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
package org.eclipse.rdf4j.query;

import java.util.List;

/**
 * Base class for {@link TupleQueryResultHandler}s with dummy implementations of all methods. This class is a useful
 * superclass for classes that implement only one or two TupleQueryResultHandler methods.
 */
public abstract class AbstractTupleQueryResultHandler implements TupleQueryResultHandler {

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
	}

	@Override
	public void endQueryResult() throws TupleQueryResultHandlerException {
	}

	@Override
	public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
	}

	@Override
	public void handleBoolean(boolean value) throws QueryResultHandlerException {
		// This is a base class for handling tuple results
		throw new UnsupportedOperationException("Cannot handle boolean results");
	}

	@Override
	public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
	}
}
