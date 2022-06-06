/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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
 * Blocks access to the statements of the model, allowing only changes to the model's namespaces.
 *
 * @author James Leigh
 */
public class EmptyModel extends AbstractModel {

	private final Model model;

	public EmptyModel(Model model) {
		this.model = model;
	}

	private static final long serialVersionUID = 3123007631452759092L;

	private final Set<Statement> emptySet = Collections.emptySet();

	@Override
	public Optional<Namespace> getNamespace(String prefix) {
		return this.model.getNamespace(prefix);
	}

	@Override
	public Set<Namespace> getNamespaces() {
		return this.model.getNamespaces();
	}

	@Override
	public Namespace setNamespace(String prefix, String name) {
		return this.model.setNamespace(prefix, name);
	}

	@Override
	public void setNamespace(Namespace namespace) {
		this.model.setNamespace(namespace);
	}

	@Override
	public Optional<Namespace> removeNamespace(String prefix) {
		return this.model.removeNamespace(prefix);
	}

	@Override
	public Iterator<Statement> iterator() {
		return emptySet.iterator();
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
		throw new UnsupportedOperationException("All statements are filtered out of view");
	}

	@Override
	public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return false;
	}

	@Override
	public Model filter(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return this;
	}

	@Override
	public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return false;
	}

	@Override
	public void removeTermIteration(Iterator<Statement> iter, Resource subj, IRI pred, Value obj,
			Resource... contexts) {
		// remove nothing
	}

}
