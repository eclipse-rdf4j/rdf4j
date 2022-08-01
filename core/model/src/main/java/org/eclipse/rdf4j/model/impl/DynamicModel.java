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

package org.eclipse.rdf4j.model.impl;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.common.iterator.EmptyIterator;
import org.eclipse.rdf4j.common.iterator.SingletonIterator;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * A LinkedHashModel or a TreeModel achieves fast data access at the cost of higher indexing time. The DynamicModel
 * postpones this cost until such access is actually needed. It stores all data in a LinkedHashMap and supports adding,
 * retrieving and removing data. The model will upgrade to a full model (provided by the modelFactory) if more complex
 * operations are called, for instance removing data according to a pattern (eg. all statements with rdf:type as
 * predicate).
 *
 * DynamicModel is thread safe to the extent that the underlying LinkedHashMap or Model is. The upgrade path is
 * protected by the actual upgrade method being synchronized. The LinkedHashMap storage is not removed once upgraded, so
 * concurrent reads that have started reading from the LinkedHashMap can continue to read even during an upgrade. We do
 * make the LinkedHashMap unmodifiable to reduce the chance of there being a bug.
 *
 * @author Håvard Mikkelsen Ottestad
 */
public class DynamicModel extends AbstractSet<Statement> implements Model {

	private static final long serialVersionUID = -9162104133818983614L;

	private static final Resource[] NULL_CTX = new Resource[] { null };

	private Map<Statement, Statement> statements = new LinkedHashMap<>();
	final Set<Namespace> namespaces = new LinkedHashSet<>();

	volatile private Model model = null;

	private final ModelFactory modelFactory;

	public DynamicModel(ModelFactory modelFactory) {
		this.modelFactory = modelFactory;
	}

	@Override
	public Model unmodifiable() {
		upgrade();
		return model.unmodifiable();
	}

	@Override
	public Optional<Namespace> getNamespace(String prefix) {
		for (Namespace nextNamespace : namespaces) {
			if (prefix.equals(nextNamespace.getPrefix())) {
				return Optional.of(nextNamespace);
			}
		}
		return Optional.empty();
	}

	@Override
	public Set<Namespace> getNamespaces() {
		return namespaces;
	}

	@Override
	public Namespace setNamespace(String prefix, String name) {
		removeNamespace(prefix);
		Namespace result = new SimpleNamespace(prefix, name);
		namespaces.add(result);
		return result;
	}

	@Override
	public void setNamespace(Namespace namespace) {
		removeNamespace(namespace.getPrefix());
		namespaces.add(namespace);
	}

	@Override
	public Optional<Namespace> removeNamespace(String prefix) {
		Optional<Namespace> result = getNamespace(prefix);
		result.ifPresent(namespaces::remove);
		return result;
	}

	@Override
	public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts) {
		upgrade();
		return model.contains(subj, pred, obj, contexts);
	}

	@Override
	public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
		if (contexts.length == 0) {
			contexts = NULL_CTX;
		}

		if (model == null) {
			boolean added = false;
			for (Resource context : contexts) {
				Statement statement = SimpleValueFactory.getInstance().createStatement(subj, pred, obj, context);
				added = added
						| statements.put(statement, statement) == null;
			}
			return added;
		} else {
			return model.add(subj, pred, obj, contexts);
		}
	}

	@Override
	public boolean clear(Resource... context) {
		upgrade();
		return model.clear(context);
	}

	@Override
	public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts) {
		if (subj == null || pred == null || obj == null || contexts.length == 0) {
			upgrade();
		}

		if (model == null) {
			boolean removed = false;
			for (Resource context : contexts) {
				removed = removed
						| statements.remove(
								SimpleValueFactory.getInstance().createStatement(subj, pred, obj, context)) != null;
			}
			return removed;
		} else {
			return model.remove(subj, pred, obj, contexts);
		}
	}

	@Override
	public Model filter(Resource subj, IRI pred, Value obj, Resource... contexts) {
		upgrade();
		return model.filter(subj, pred, obj, contexts);
	}

	@Override
	public Set<Resource> subjects() {
		upgrade();
		return model.subjects();
	}

	@Override
	public Set<IRI> predicates() {
		upgrade();
		return model.predicates();
	}

	@Override
	public Set<Value> objects() {
		upgrade();
		return model.objects();
	}

	@Override
	public Set<Resource> contexts() {
		upgrade();
		return model.contexts();
	}

	@Override
	public int size() {
		if (model == null) {
			return statements.size();
		}
		return model.size();
	}

	@Override
	public boolean isEmpty() {
		if (model == null) {
			return statements.isEmpty();
		}
		return model.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		if (model == null) {
			return statements.containsKey(o);
		}
		return model.contains(o);
	}

	@Override
	public Iterator<Statement> iterator() {
		if (model == null) {
			return statements.values().iterator();
		}

		return model.iterator();
	}

	@Override
	public Object[] toArray() {
		if (model == null) {
			return statements.values().toArray();
		}
		return model.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		if (model == null) {
			return statements.values().toArray(a);
		}
		return model.toArray(a);
	}

	@Override
	public boolean add(Statement statement) {
		Objects.requireNonNull(statement);
		if (model == null) {
			return statements.put(statement, statement) == null;
		}
		return model.add(statement);
	}

	@Override
	public boolean remove(Object o) {
		Objects.requireNonNull(o);
		if (model == null) {
			return statements.remove(o) != null;
		}
		return model.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		Objects.requireNonNull(c);
		if (model == null) {
			return statements.keySet().containsAll(c);
		}
		return model.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends Statement> c) {
		Objects.requireNonNull(c);
		if (model == null) {
			return c.stream()
					.map(s -> {
						Objects.requireNonNull(s);
						return statements.put(s, s) == null;
					})
					.reduce((a, b) -> a || b)
					.orElse(false);
		}
		return model.addAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		if (model == null) {
			return statements.keySet().retainAll(c);
		}
		return model.retainAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (model == null) {
			return c
					.stream()
					.map(statements::remove)
					.map(Objects::nonNull)
					.reduce((a, b) -> a || b)
					.orElse(false);
		}
		return model.removeAll(c);
	}

	@Override
	public void clear() {
		if (model == null) {
			statements.clear();
		} else {
			model.clear();
		}
	}

	@Override
	public Iterable<Statement> getStatements(Resource subject, IRI predicate, Value object, Resource... contexts) {
		if (model == null && subject != null && predicate != null && object != null && contexts != null
				&& contexts.length == 1) {
			Statement statement = SimpleValueFactory.getInstance()
					.createStatement(subject, predicate, object, contexts[0]);
			Statement foundStatement = statements.get(statement);
			if (foundStatement == null) {
				return EmptyIterator::new;
			}
			return () -> new SingletonIterator<>(foundStatement);
		} else if (model == null && subject == null && predicate == null && object == null && contexts != null
				&& contexts.length == 0) {
			return this;
		} else {
			upgrade();
			return model.getStatements(subject, predicate, object, contexts);
		}
	}

	private void upgrade() {
		if (model == null) {
			synchronizedUpgrade();
		}
	}

	synchronized private void synchronizedUpgrade() {
		if (model == null) {
			// make statements unmodifiable first, to increase chance of an early failure if the user is doing
			// concurrent write with reads
			statements = Collections.unmodifiableMap(statements);
			Model tempModel = modelFactory.createEmptyModel();
			tempModel.addAll(statements.values());
			model = tempModel;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (model != null) {
			return model.equals(o);
		}
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		if (model != null) {
			return model.hashCode();
		}
		return super.hashCode();
	}
}
