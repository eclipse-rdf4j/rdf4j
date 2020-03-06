/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Models;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A LinkedHashModel or a TreeModel achieves fast data access at the cost of higher indexing time. The DynamicModel
 * postpones this cost until such access is actually needed. It stores all data in a LinkedHashSet and supports adding,
 * retrieving and removing data. The model will upgrade to a full model (provided by the modelFactory) if more complex
 * operations are called, for instance removing data according to a pattern (eg. all statements with rdf:type as
 * predicate).
 *
 * @author Håvard Mikkelsen Ottestad
 */
public class DynamicModel implements Model {

	private static final long serialVersionUID = -9162104133818983614L;

	private static final Resource[] NULL_CTX = new Resource[] { null };

	private Set<Statement> statements = new LinkedHashSet<>();
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
				added = added
						| statements.add(SimpleValueFactory.getInstance().createStatement(subj, pred, obj, context));
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
						| statements.remove(SimpleValueFactory.getInstance().createStatement(subj, pred, obj, context));
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
			return statements.contains(o);
		}
		return model.contains(o);
	}

	@Override
	public Iterator<Statement> iterator() {
		if (model == null) {
			return statements.iterator();
		}

		return model.iterator();
	}

	@Override
	public Object[] toArray() {
		if (model == null) {
			return statements.toArray();
		}
		return model.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		if (model == null) {
			return statements.toArray(a);
		}
		return model.toArray(a);
	}

	@Override
	public boolean add(Statement statement) {
		Objects.requireNonNull(statement);
		if (model == null) {
			return statements.add(statement);
		}
		return model.add(statement);
	}

	@Override
	public boolean remove(Object o) {
		Objects.requireNonNull(o);
		if (model == null) {
			return statements.remove(o);
		}
		return model.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		Objects.requireNonNull(c);
		if (model == null) {
			return statements.containsAll(c);
		}
		return model.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends Statement> c) {
		Objects.requireNonNull(c);
		if (model == null) {
			c.forEach(Objects::requireNonNull);
			return statements.addAll(c);
		}
		return model.addAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		if (model == null) {
			return statements.retainAll(c);
		}
		return model.retainAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (model == null) {
			return statements.removeAll(c);
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

	private void upgrade() {
		if (model == null) {
			synchronized (this) {
				if (model == null) {
					Model tempModel = modelFactory.createEmptyModel();
					tempModel.addAll(statements);
					statements = null;
					model = tempModel;
				}
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof Model) {
			Model model = (Model) o;
			return Models.isomorphic(this, model);
		} else if (o instanceof Set) {
			if (this.size() != ((Set<?>) o).size()) {
				return false;
			}
			try {
				return Models.isomorphic(this, (Iterable<? extends Statement>) o);
			} catch (ClassCastException e) {
				return false;
			}
		}

		return false;

	}

	@Override
	public int hashCode() {
		if (model != null) {
			return model.hashCode();
		} else {
			int h = 0;
			for (Statement obj : this) {
				if (obj != null) {
					h += obj.hashCode();
				}
			}
			return h;

		}

	}
}
