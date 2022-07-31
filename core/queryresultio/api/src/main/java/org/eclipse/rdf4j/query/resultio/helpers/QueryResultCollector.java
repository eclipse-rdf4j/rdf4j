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
package org.eclipse.rdf4j.query.resultio.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQueryResultHandler;
import org.eclipse.rdf4j.query.QueryResultHandler;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;

/**
 * An implementation of the {@link QueryResultHandler} interface that is able to collect a single result from either
 * Boolean or Tuple results simultaneously.
 * <p>
 * The {@link List}s that are returned by this interface are immutable.
 *
 * @author Peter Ansell
 */
public class QueryResultCollector implements TupleQueryResultHandler, BooleanQueryResultHandler {

	private boolean hasBooleanSet = false;

	private Boolean value = null;

	private boolean endQueryResultFound = false;

	private List<String> bindingNames = Collections.emptyList();

	private List<BindingSet> bindingSets = Collections.emptyList();

	private final List<String> links = new ArrayList<>();

	public QueryResultCollector() {
	}

	@Override
	public void handleBoolean(boolean value) throws QueryResultHandlerException {
		hasBooleanSet = true;
		this.value = value;
	}

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
		endQueryResultFound = false;
		this.bindingNames = Collections.unmodifiableList(new ArrayList<>(bindingNames));
		bindingSets = new ArrayList<>();
	}

	@Override
	public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
		endQueryResultFound = false;
		bindingSets.add(bindingSet);
	}

	@Override
	public void endQueryResult() throws TupleQueryResultHandlerException {
		endQueryResultFound = true;
		// the binding sets cannot be modified after this point without a call to
		// startQueryResult which will reset the bindingsets
		bindingSets = Collections.unmodifiableList(bindingSets);
		// reset the start query result found variable at this point
	}

	@Override
	public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
		this.links.addAll(linkUrls);
	}

	/**
	 * Determines whether {@link #handleBoolean(boolean)} was called for this collector.
	 *
	 * @return True if there was a boolean handled by this collector.
	 */
	public boolean getHandledBoolean() {
		return hasBooleanSet;
	}

	/**
	 * If {@link #getHandledBoolean()} returns true this method returns the boolean that was last found using
	 * {@link #handleBoolean(boolean)}
	 * <p>
	 * If {@link #getHandledBoolean()} returns false this method throws a {@link QueryResultHandlerException} indicating
	 * that a response could not be provided.
	 *
	 * @return The boolean value that was collected.
	 * @throws QueryResultHandlerException If there was no boolean value collected.
	 */
	public boolean getBoolean() throws QueryResultHandlerException {
		if (!hasBooleanSet) {
			throw new QueryResultHandlerException("Did not collect a boolean value");
		} else {
			return this.value;
		}
	}

	/**
	 * Determines whether {@link #endQueryResult()} was called after the last calls to {@link #startQueryResult(List)}
	 * and optionally calls to {@link #handleSolution(BindingSet)}.
	 *
	 * @return True if there was a call to {@link #endQueryResult()} after the last calls to
	 *         {@link #startQueryResult(List)} and {@link #handleSolution(BindingSet)}.
	 */
	public boolean getHandledTuple() {
		return endQueryResultFound;
	}

	/**
	 * Returns a collection of binding names collected.
	 *
	 * @return An immutable list of {@link String}s that were collected as the binding names.
	 * @throws QueryResultHandlerException If the tuple results set was not successfully collected, as signalled by a
	 *                                     call to {@link #endQueryResult()}.
	 */
	public List<String> getBindingNames() throws QueryResultHandlerException {
		if (!endQueryResultFound) {
			throw new QueryResultHandlerException("Did not successfully collect a tuple results set.");
		} else {
			return bindingNames;
		}
	}

	/**
	 * @return An immutable list of {@link BindingSet}s that were collected as the tuple results.
	 * @throws QueryResultHandlerException If the tuple results set was not successfully collected, as signalled by a
	 *                                     call to {@link #endQueryResult()}.
	 */
	public List<BindingSet> getBindingSets() throws QueryResultHandlerException {
		if (!endQueryResultFound) {
			throw new QueryResultHandlerException("Did not successfully collect a tuple results set.");
		} else {
			return bindingSets;
		}
	}

	/**
	 * @return A list of links accumulated from calls to {@link #handleLinks(List)}.
	 */
	public List<String> getLinks() {
		return Collections.unmodifiableList(links);
	}
}
