/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.resultcache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class CachedTupleQueryResult implements TupleQueryResult {
	private final List<BindingSet> bindingSets;
	private Iterator<BindingSet> replayingIterator;
	private final List<String> bindingNames;

	CachedTupleQueryResult(List<BindingSet> bindingSets, List<String> bindingNames) {
		this.bindingSets = new LinkedList<>(bindingSets);
		this.bindingNames = new ArrayList<>(bindingNames);
		this.replayingIterator = bindingSets.iterator();
	}

	@Override
	public List<String> getBindingNames() throws QueryEvaluationException {
		return bindingNames;
	}

	@Override
	public Iterator<BindingSet> iterator() {
		return replayingIterator;
	}

	@Override
	public void close() throws QueryEvaluationException {
		this.replayingIterator = null;
	}

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		return replayingIterator.hasNext();
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		return this.replayingIterator.next();
	}

	@Override
	public void remove() throws QueryEvaluationException {
		throw new UnsupportedOperationException("Remove is not supported");
	}

	@Override
	public Stream<BindingSet> stream() {
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(this.replayingIterator, Spliterator.ORDERED),
				false);
	}

	@Override
	public void forEach(Consumer<? super BindingSet> action) {
		bindingSets.forEach(action);
	}

	@Override
	public Spliterator<BindingSet> spliterator() {
		return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED);
	}
}
