/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.resultcache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * @author Florian Kleedorfer
 * @since 4.0.0
 */
public class CachedGraphQueryResult implements GraphQueryResult {
	private final List<Statement> statements;
	private Iterator<Statement> replayingIterator;
	private final Map<String, String> namespaces;

	CachedGraphQueryResult(List<Statement> statements, Map<String, String> namespaces) {
		this.statements = new ArrayList<>(statements);
		this.namespaces = new HashMap<>();
		this.namespaces.putAll(namespaces);
		this.replayingIterator = statements.iterator();
	}

	@Override
	public Map<String, String> getNamespaces() {
		return namespaces;
	}

	@Override
	public Iterator<Statement> iterator() {
		return replayingIterator;
	}

	@Override
	public void close() {
		this.replayingIterator = null;
	}

	@Override
	public boolean hasNext() {
		return replayingIterator.hasNext();
	}

	@Override
	public Statement next() {
		return this.replayingIterator.next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove is not supported");
	}

	@Override
	public Stream<Statement> stream() {
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(this.replayingIterator, Spliterator.ORDERED),
				false);
	}

	@Override
	public void forEach(Consumer<? super Statement> action) {
		statements.forEach(action);
	}

	@Override
	public Spliterator<Statement> spliterator() {
		return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED);
	}
}
