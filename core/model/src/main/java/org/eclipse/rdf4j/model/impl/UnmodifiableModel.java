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
package org.eclipse.rdf4j.model.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * A Model wrapper that prevents modification to the underlying model.
 */
class UnmodifiableModel extends AbstractModel {

	private static final long serialVersionUID = 6335569454318096059L;

	private final Model model;

	public UnmodifiableModel(Model delegate) {
		this.model = delegate;
	}

	@Override
	public Set<Namespace> getNamespaces() {
		return Collections.unmodifiableSet(model.getNamespaces());
	}

	@Override
	public Optional<Namespace> getNamespace(String prefix) {
		return model.getNamespace(prefix);
	}

	@Override
	public Namespace setNamespace(String prefix, String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setNamespace(Namespace name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Namespace> removeNamespace(String prefix) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return model.contains(subj, pred, obj, contexts);
	}

	@Override
	public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Model filter(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return model.filter(subj, pred, obj, contexts).unmodifiable();
	}

	@Override
	public Iterator<Statement> iterator() {
		return Collections.unmodifiableSet(model).iterator();
	}

	@Override
	public int size() {
		return model.size();
	}

	@Override
	public void removeTermIteration(Iterator<Statement> iter, Resource subj, IRI pred, Value obj,
			Resource... contexts) {
		throw new UnsupportedOperationException();
	}

}
