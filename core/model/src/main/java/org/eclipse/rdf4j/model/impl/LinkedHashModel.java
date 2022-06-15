/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.PatternIterator;

/**
 * Hash table based implementation of the <var>{@link Model}</var> interface.
 * <p>
 * This implementation provides constant-time performance for filters using a single term, assuming the hash function
 * disperses the elements properly among the buckets. Each term is indexed using a {@link HashMap}. When multiple terms
 * are provided in a filter the index, of the term that reduces the possible {@link Statement}s the most, is used and a
 * sequential scan is used to filter additional terms.
 * <p>
 * <b>Note that this implementation is not synchronized.</b> If multiple threads access a model concurrently, and at
 * least one of the threads modifies the model, it must be synchronized externally. This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the model. If no such object exists, the set should be
 * "wrapped" using the * Models.synchronizedModel method.
 * </p>
 *
 * @author James Leigh
 */
@SuppressWarnings("unchecked")
public class LinkedHashModel extends AbstractModel {

	private static final long serialVersionUID = -9161104123818983614L;

	private static final Resource[] NULL_CTX = new Resource[] { null };

	private final Set<Namespace> namespaces = new LinkedHashSet<>();

	private transient Map<Value, ModelNode> values;

	private transient Set<LinkedHashModelStatement> statements;

	public LinkedHashModel() {
		this(128);
	}

	public LinkedHashModel(Model model) {
		this(model.getNamespaces(), model.size());
		addAll(model);
	}

	public LinkedHashModel(Collection<? extends Statement> c) {
		this(c.size());
		addAll(c);
	}

	public LinkedHashModel(int size) {
		super();

		// assume fewer unique values than statements (eg. some reuse of nodes instatements)
		values = new HashMap<>(size, 0.75f);

		// compensate for the loadfactor by multiplying by 2
		statements = new LinkedHashSet<>(size * 2, 0.75f);
	}

	public LinkedHashModel(Set<Namespace> namespaces, Collection<? extends Statement> c) {
		this(c);
		this.namespaces.addAll(namespaces);
	}

	public LinkedHashModel(Set<Namespace> namespaces) {
		this();
		this.namespaces.addAll(namespaces);
	}

	public LinkedHashModel(Set<Namespace> namespaces, int size) {
		this(size);
		this.namespaces.addAll(namespaces);
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
	public int size() {
		return statements.size();
	}

	@Override
	public boolean add(Statement statement) {
		Resource subj = statement.getSubject();
		IRI pred = statement.getPredicate();
		Value obj = statement.getObject();
		Resource context = statement.getContext();

		ModelNode<Resource> s = asNode(subj);
		ModelNode<IRI> p = asNode(pred);
		ModelNode<Value> o = asNode(obj);
		ModelNode<Resource> c = asNode(context);
		return addModelStatement(new LinkedHashModelStatement(s, p, o, c, statement));
	}

	@Override
	public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
		if (subj == null || pred == null || obj == null) {
			throw new UnsupportedOperationException("Incomplete statement");
		}
		Value[] ctxs = notNull(contexts);
		if (ctxs.length == 0) {
			ctxs = NULL_CTX;
		}
		boolean changed = false;
		for (Value ctx : ctxs) {
			ModelNode<Resource> s = asNode(subj);
			ModelNode<IRI> p = asNode(pred);
			ModelNode<Value> o = asNode(obj);
			ModelNode<Resource> c = asNode((Resource) ctx);
			LinkedHashModelStatement st = new LinkedHashModelStatement(s, p, o, c);
			changed |= addModelStatement(st);
		}
		return changed;
	}

	@Override
	public void clear() {
		values.clear();
		statements.clear();
	}

	@Override
	public boolean contains(Object o) {
		if (o instanceof Statement) {
			if (o instanceof LinkedHashModelStatement) {
				return statements.contains(o);
			}

			return find((Statement) o).hasNext();
		}
		return false;
	}

	@Override
	public Iterator iterator() {
		return matchPattern(null, null, null);
	}

	@Override
	public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return matchPattern(subj, pred, obj, contexts).hasNext();
	}

	@Override
	public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts) {
		if (isEmpty()) {
			return false;
		}

		var iter = matchPattern(subj, pred, obj, contexts);
		if (!iter.hasNext()) {
			return false;
		}
		while (iter.hasNext()) {
			iter.next();
			iter.remove();
		}
		return true;
	}

	@Override
	public Model filter(final Resource subj, final IRI pred, final Value obj, final Resource... contexts) {
		return new FilteredModel(this, subj, pred, obj, contexts) {

			private static final long serialVersionUID = 396293781006255959L;

			@Override
			public Iterator iterator() {
				return matchPattern(subj, pred, obj, contexts);
			}

			@Override
			protected void removeFilteredTermIteration(Iterator<Statement> iter, Resource subj, IRI pred, Value obj,
					Resource... contexts) {
				LinkedHashModel.this.removeTermIteration(iter, subj, pred, obj, contexts);
			}
		};
	}

	@Override
	public void removeTermIteration(Iterator iterator, Resource subj, IRI pred, Value obj, Resource... contexts) {
		Set<LinkedHashModelStatement> owner = ((ModelIterator) iterator).getOwner();
		Set<LinkedHashModelStatement> chosen = choose(subj, pred, obj, contexts);
		Iterator<LinkedHashModelStatement> iter = chosen.iterator();
		iter = new PatternIterator<>(iter, subj, pred, obj, contexts);
		while (iter.hasNext()) {
			LinkedHashModelStatement last = iter.next();
			if (statements == owner) {
				statements = new LinkedHashSet<>(statements);
				statements.remove(last);
			} else if (statements != chosen) {
				statements.remove(last);
			}
			if (last.subj.subjects == owner) {
				last.subj.subjects = new LinkedHashSet<>(last.subj.subjects);
				last.subj.subjects.remove(last);
			} else if (last.subj.subjects != chosen) {
				last.subj.subjects.remove(last);
			}
			if (last.pred.predicates == owner) {
				last.pred.predicates = new LinkedHashSet<>(statements);
				last.pred.predicates.remove(last);
			} else if (last.pred.predicates != chosen) {
				last.pred.predicates.remove(last);
			}
			if (last.obj.objects == owner) {
				last.obj.objects = new LinkedHashSet<>(statements);
				last.obj.objects.remove(last);
			} else if (last.obj.objects != chosen) {
				last.obj.objects.remove(last);
			}
			if (last.ctx.contexts == owner) {
				last.ctx.contexts = new LinkedHashSet<>(statements);
				last.ctx.contexts.remove(last);
			} else if (last.ctx.contexts != chosen) {
				last.ctx.contexts.remove(last);
			}
			if (owner != chosen) {
				iter.remove(); // remove from chosen
			}
		}
	}

	@Override
	public boolean isEmpty() {
		return statements.isEmpty();
	}

	private class ModelIterator implements Iterator<LinkedHashModelStatement> {

		private final Iterator<LinkedHashModelStatement> iterator;

		private final Set<LinkedHashModelStatement> owner;

		private LinkedHashModelStatement last;

		public ModelIterator(Iterator<LinkedHashModelStatement> iterator, Set<LinkedHashModelStatement> owner) {
			this.iterator = iterator;
			this.owner = owner;
		}

		public Set<LinkedHashModelStatement> getOwner() {
			return owner;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public LinkedHashModelStatement next() {
			return last = iterator.next();
		}

		@Override
		public void remove() {
			if (last == null) {
				throw new IllegalStateException();
			}
			removeFrom(statements);
			removeFrom(last.subj.subjects);
			removeFrom(last.pred.predicates);
			removeFrom(last.obj.objects);
			removeFrom(last.ctx.contexts);
			iterator.remove(); // remove from owner
		}

		private void removeFrom(Set<LinkedHashModelStatement> subjects) {
			if (subjects != owner) {
				subjects.remove(last);
			}
		}
	}

	private static class ModelNode<V extends Value> implements Serializable {

		private static final long serialVersionUID = -1205676084606998540L;

		private Set<LinkedHashModelStatement> subjects = new LinkedHashSet<>();
		private Set<LinkedHashModelStatement> predicates = new LinkedHashSet<>();
		private Set<LinkedHashModelStatement> objects = new LinkedHashSet<>();
		private Set<LinkedHashModelStatement> contexts = new LinkedHashSet<>();

		private final V value;

		public ModelNode(V value) {
			this.value = value;
		}

		public V getValue() {
			return value;
		}
	}

	@InternalUseOnly
	public static class LinkedHashModelStatement extends GenericStatement<Resource, IRI, Value> {

		private static final long serialVersionUID = 2200404772364346279L;

		private final ModelNode<Resource> subj;
		private final ModelNode<IRI> pred;
		private final ModelNode<Value> obj;
		private final ModelNode<Resource> ctx;

		private final Statement statement;

		private final int hashCode;

		public LinkedHashModelStatement(ModelNode<Resource> subj, ModelNode<IRI> pred, ModelNode<Value> obj,
				ModelNode<Resource> ctx) {
			super(subj.getValue(), pred.getValue(), obj.getValue(), ctx.getValue());
			this.subj = subj;
			this.pred = pred;
			this.obj = obj;
			this.ctx = ctx;
			this.statement = null;
			this.hashCode = calculateHashCode();
		}

		public LinkedHashModelStatement(ModelNode<Resource> subj, ModelNode<IRI> pred, ModelNode<Value> obj,
				ModelNode<Resource> ctx, Statement statement) {
			super(subj.getValue(), pred.getValue(), obj.getValue(), ctx.getValue());
			this.subj = subj;
			this.pred = pred;
			this.obj = obj;
			this.ctx = ctx;
			this.statement = statement;
			this.hashCode = calculateHashCode();
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}

			if (other instanceof LinkedHashModelStatement) {
				if (hashCode != ((LinkedHashModelStatement) other).hashCode) {
					return false;
				}
				if (statement != null && statement == ((LinkedHashModelStatement) other).statement) {
					return true;
				}
			}

			if (statement != null && statement == other) {
				return true;
			}

			if (!super.equals(other)) {
				return false;
			}

			if (getContext() == null) {
				return ((Statement) other).getContext() == null;
			}

			return getContext().equals(((Statement) other).getContext());
		}

		public Statement getStatement() {
			return statement;
		}

		private int calculateHashCode() {
			int result = 1;
			result = 31 * result + (subject == null ? 0 : subject.hashCode());
			result = 31 * result + (predicate == null ? 0 : predicate.hashCode());
			result = 31 * result + (object == null ? 0 : object.hashCode());
			result = 31 * result + (context == null ? 0 : context.hashCode());
			return result;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}
	}

	private void writeObject(ObjectOutputStream s) throws IOException {
		// Write out any hidden serialization magic
		s.defaultWriteObject();
		// Write in size
		s.writeInt(statements.size());
		// Write in all elements
		for (LinkedHashModelStatement st : statements) {
			Resource subj = st.getSubject();
			IRI pred = st.getPredicate();
			Value obj = st.getObject();
			Resource ctx = st.getContext();
			s.writeObject(new ContextStatement(subj, pred, obj, ctx));
		}
	}

	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		// Read in any hidden serialization magic
		s.defaultReadObject();
		// Read in size
		int size = s.readInt();
		values = new HashMap<>(size * 2);
		statements = new LinkedHashSet<>(size);
		// Read in all elements
		for (int i = 0; i < size; i++) {
			Statement st = (Statement) s.readObject();
			add(st);
		}
	}

	private ModelIterator matchPattern(Resource subj, IRI pred, Value obj, Resource... contexts) {
		Set<LinkedHashModelStatement> set = choose(subj, pred, obj, contexts);
		Iterator<LinkedHashModelStatement> it = set.iterator();
		Iterator<LinkedHashModelStatement> iter;
		iter = new PatternIterator<>(it, subj, pred, obj, contexts);
		return new ModelIterator(iter, set);
	}

	private Set<LinkedHashModelStatement> choose(Resource subj, IRI pred, Value obj, Resource... contexts) {
		contexts = notNull(contexts);
		Set<LinkedHashModelStatement> s = null;
		Set<LinkedHashModelStatement> p = null;
		Set<LinkedHashModelStatement> o = null;
		if (subj != null) {
			var modelNode = values.get(subj);
			if (modelNode == null) {
				return Collections.emptySet();
			}
			s = modelNode.subjects;
		}
		if (pred != null) {
			var modelNode = values.get(pred);
			if (modelNode == null) {
				return Collections.emptySet();
			}
			p = modelNode.predicates;
		}
		if (obj != null) {
			var modelNode = values.get(obj);
			if (modelNode == null) {
				return Collections.emptySet();
			}
			o = modelNode.objects;
		}
		if (contexts.length == 1) {
			var modelNode = values.get(contexts[0]);
			if (modelNode == null) {
				return Collections.emptySet();
			}
			Set<LinkedHashModelStatement> c = modelNode.contexts;
			return smallest(statements, s, p, o, c);
		} else {
			return smallest(statements, s, p, o);
		}
	}

	private Resource[] notNull(Resource[] contexts) {
		if (contexts == null) {
			return new Resource[] { null };
		}
		return contexts;
	}

	private Iterator<LinkedHashModelStatement> find(Statement st) {
		Resource subj = st.getSubject();
		IRI pred = st.getPredicate();
		Value obj = st.getObject();
		Resource ctx = st.getContext();
		return matchPattern(subj, pred, obj, ctx);
	}

	private boolean addModelStatement(LinkedHashModelStatement st) {
		Set<LinkedHashModelStatement> subj = st.subj.subjects;
		Set<LinkedHashModelStatement> pred = st.pred.predicates;
		Set<LinkedHashModelStatement> obj = st.obj.objects;
		Set<LinkedHashModelStatement> ctx = st.ctx.contexts;
		if (smallest(subj, pred, obj, ctx).contains(st)) {
			return false;
		}
		statements.add(st);
		subj.add(st);
		pred.add(st);
		obj.add(st);
		ctx.add(st);
		return true;
	}

	private Set<LinkedHashModelStatement> smallest(Set<LinkedHashModelStatement>... sets) {
		int minSize = Integer.MAX_VALUE;
		Set<LinkedHashModelStatement> minSet = null;
		for (Set<LinkedHashModelStatement> set : sets) {
			if (set != null && set.size() < minSize) {
				minSet = set;
				minSize = set.size();
			}
		}
		return minSet;
	}

	private <V extends Value> ModelNode<V> asNode(V value) {
		ModelNode<V> node = values.get(value);
		if (node != null) {
			return node;
		}
		node = new ModelNode<>(value);
		values.put(value, node);
		return node;
	}

	/**
	 * This class was only made public in order to improve the performance of the MemoryStore. It has been replaced by
	 * LinkedHashModelStatement which is for {@link InternalUseOnly}.
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	public static class ModelStatement extends ContextStatement {

		public ModelStatement(ModelNode<Resource> subj, ModelNode<IRI> pred, ModelNode<Value> obj,
				ModelNode<Resource> ctx) {
			super(subj.getValue(), pred.getValue(), obj.getValue(), ctx.getValue());
			throw new IllegalStateException("This class is no longer in use!");
		}

		public ModelStatement(ModelNode<Resource> subj, ModelNode<IRI> pred, ModelNode<Value> obj,
				ModelNode<Resource> ctx, Statement statement) {
			super(subj.getValue(), pred.getValue(), obj.getValue(), ctx.getValue());
			throw new IllegalStateException("This class is no longer in use!");
		}

		public Statement getStatement() {
			throw new IllegalStateException("This class is no longer in use!");
		}
	}

}
