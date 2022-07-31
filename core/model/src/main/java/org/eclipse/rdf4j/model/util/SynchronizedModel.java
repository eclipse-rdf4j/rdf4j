/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.model.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

class SynchronizedModel implements Model {

	private final Model delegate;

	SynchronizedModel(Model delegate) {
		this.delegate = delegate;
	}

	@Override
	synchronized public Model unmodifiable() {
		return delegate.unmodifiable();
	}

	@Override
	synchronized public Namespace setNamespace(String prefix, String name) {
		return delegate.setNamespace(prefix, name);
	}

	@Override
	synchronized public void setNamespace(Namespace namespace) {
		delegate.setNamespace(namespace);
	}

	@Override
	synchronized public Optional<Namespace> removeNamespace(String prefix) {
		return delegate.removeNamespace(prefix);
	}

	@Override
	synchronized public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return delegate.contains(subj, pred, obj, contexts);
	}

	@Override
	synchronized public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return delegate.add(subj, pred, obj, contexts);
	}

	@Override
	synchronized public boolean clear(Resource... context) {
		return delegate.clear(context);
	}

	@Override
	synchronized public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return delegate.remove(subj, pred, obj, contexts);
	}

	@Override
	synchronized public Model filter(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return delegate.filter(subj, pred, obj, contexts);
	}

	@Override
	synchronized public Set<Resource> subjects() {
		return delegate.subjects();
	}

	@Override
	synchronized public Set<IRI> predicates() {
		return delegate.predicates();
	}

	@Override
	synchronized public Set<Value> objects() {
		return delegate.objects();
	}

	@Override
	synchronized public Set<Resource> contexts() {
		return delegate.contexts();
	}

	@Override
	synchronized public boolean removeIf(Predicate<? super Statement> filter) {
		return delegate.removeIf(filter);
	}

	@Override
	synchronized public Stream<Statement> stream() {
		return delegate.stream();
	}

	@Override
	synchronized public Stream<Statement> parallelStream() {
		return delegate.parallelStream();
	}

	@Override
	synchronized public void forEach(Consumer<? super Statement> action) {
		delegate.forEach(action);
	}

	@Override
	synchronized public Set<Namespace> getNamespaces() {
		return delegate.getNamespaces();
	}

	@Override
	synchronized public Optional<Namespace> getNamespace(String prefix) {
		return delegate.getNamespace(prefix);
	}

	@Override
	synchronized public int size() {
		return delegate.size();
	}

	@Override
	synchronized public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	synchronized public boolean contains(Object o) {
		return delegate.contains(o);
	}

	@Override
	synchronized public Iterator<Statement> iterator() {
		return delegate.iterator();
	}

	@Override
	synchronized public Object[] toArray() {
		return delegate.toArray();
	}

	@Override
	synchronized public <T> T[] toArray(T[] a) {
		return delegate.toArray(a);
	}

	@Override
	synchronized public boolean add(Statement statement) {
		return delegate.add(statement);
	}

	@Override
	synchronized public boolean remove(Object o) {
		return delegate.remove(o);
	}

	@Override
	synchronized public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c);
	}

	@Override
	synchronized public boolean addAll(Collection<? extends Statement> c) {
		return delegate.addAll(c);
	}

	@Override
	synchronized public boolean retainAll(Collection<?> c) {
		return delegate.retainAll(c);
	}

	@Override
	synchronized public boolean removeAll(Collection<?> c) {
		return delegate.removeAll(c);
	}

	@Override
	synchronized public void clear() {
		delegate.clear();
	}

	@Override
	synchronized public boolean equals(Object o) {
		return delegate.equals(o);
	}

	@Override
	synchronized public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	synchronized public Spliterator<Statement> spliterator() {
		return delegate.spliterator();
	}

}
