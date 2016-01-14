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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.PatternIterator;

/**
 * Hash table based implementation of the <tt>{@link Model}</tt> interface.
 * <p>
 * This implementation provides constant-time performance for filters using a
 * single term, assuming the hash function disperses the elements properly among
 * the buckets. Each term is indexed using a {@link HashMap}. When multiple
 * terms are provided in a filter the index, of the term that reduces the
 * possible {@link Statement}s the most, is used and a sequential scan is used
 * to filter additional terms.
 * <p>
 * <b>Note that this implementation is not synchronized.</b> If multiple threads
 * access a model concurrently, and at least one of the threads modifies the
 * model, it must be synchronized externally. This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the model. If no
 * such object exists, the set should be "wrapped" using the
 * Collections.synchronizedSet method. This is best done at creation time, to
 * prevent accidental unsynchronized access to the LinkedHashModel instance
 * (though the synchronization guarantee is only when accessing via the Set
 * interface methods):
 * </p>
 * 
 * <pre>
 * Set<Statement> s = Collections.synchronizedSet(new LinkedHashModel(...));
 * </pre>
 * 
 * @author James Leigh
 * @since 2.7.0
 */
@SuppressWarnings("unchecked")
public class LinkedHashModel extends AbstractModel {

	private static final long serialVersionUID = -9161104123818983614L;

	static final Resource[] NULL_CTX = new Resource[] { null };

	Set<Namespace> namespaces = new LinkedHashSet<Namespace>();

	transient Map<Value, ModelNode> values;

	transient Set<ModelStatement> statements;

	public LinkedHashModel() {
		this(128);
	}

	public LinkedHashModel(Model model) {
		this(model.getNamespaces());
		addAll(model);
	}

	public LinkedHashModel(Collection<? extends Statement> c) {
		this(c.size());
		addAll(c);
	}

	public LinkedHashModel(int size) {
		super();
		values = new HashMap<Value, ModelNode>(size * 2);
		statements = new LinkedHashSet<ModelStatement>(size);
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
		if (result.isPresent()) {
			namespaces.remove(result.get());
		}
		return result;
	}

	@Override
	public int size() {
		return statements.size();
	}

	@Override
	public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
		if (subj == null || pred == null || obj == null)
			throw new UnsupportedOperationException("Incomplete statement");
		Value[] ctxs = notNull(contexts);
		if (ctxs.length == 0) {
			ctxs = NULL_CTX;
		}
		boolean changed = false;
		for (Value ctx : ctxs) {
			ModelNode<Resource> s = asNode(subj);
			ModelNode<IRI> p = asNode(pred);
			ModelNode<Value> o = asNode(obj);
			ModelNode<Resource> c = asNode((Resource)ctx);
			ModelStatement st = new ModelStatement(s, p, o, c);
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
	public boolean remove(Object o) {
		if (o instanceof Statement) {
			Iterator iter = find((Statement)o);
			if (iter.hasNext()) {
				iter.next();
				iter.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean contains(Object o) {
		if (o instanceof Statement) {
			return find((Statement)o).hasNext();
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
		Iterator iter = matchPattern(subj, pred, obj, contexts);
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
			protected void removeFilteredTermIteration(Iterator<Statement> iter, Resource subj, IRI pred,
					Value obj, Resource... contexts)
			{
				LinkedHashModel.this.removeTermIteration(iter, subj, pred, obj, contexts);
			}
		};
	}

	@Override
	public void removeTermIteration(Iterator iterator, Resource subj, IRI pred, Value obj,
			Resource... contexts)
	{
		Set<ModelStatement> owner = ((ModelIterator)iterator).getOwner();
		Set<ModelStatement> chosen = choose(subj, pred, obj, contexts);
		Iterator<ModelStatement> iter = chosen.iterator();
		iter = new PatternIterator(iter, subj, pred, obj, contexts);
		while (iter.hasNext()) {
			ModelStatement last = iter.next();
			if (statements == owner) {
				statements = new LinkedHashSet<ModelStatement>(statements);
				statements.remove(last);
			}
			else if (statements != chosen) {
				statements.remove(last);
			}
			if (last.subj.subjects == owner) {
				last.subj.subjects = new LinkedHashSet<ModelStatement>(last.subj.subjects);
				last.subj.subjects.remove(last);
			}
			else if (last.subj.subjects != chosen) {
				last.subj.subjects.remove(last);
			}
			if (last.pred.predicates == owner) {
				last.pred.predicates = new LinkedHashSet<ModelStatement>(statements);
				last.pred.predicates.remove(last);
			}
			else if (last.pred.predicates != chosen) {
				last.pred.predicates.remove(last);
			}
			if (last.obj.objects == owner) {
				last.obj.objects = new LinkedHashSet<ModelStatement>(statements);
				last.obj.objects.remove(last);
			}
			else if (last.obj.objects != chosen) {
				last.obj.objects.remove(last);
			}
			if (last.ctx.contexts == owner) {
				last.ctx.contexts = new LinkedHashSet<ModelStatement>(statements);
				last.ctx.contexts.remove(last);
			}
			else if (last.ctx.contexts != chosen) {
				last.ctx.contexts.remove(last);
			}
			if (owner != chosen) {
				iter.remove(); // remove from chosen
			}
		}
	}

	private class ModelIterator implements Iterator<ModelStatement> {

		private Iterator<ModelStatement> iter;

		private Set<ModelStatement> owner;

		private ModelStatement last;

		public ModelIterator(Iterator<ModelStatement> iter, Set<ModelStatement> owner) {
			this.iter = iter;
			this.owner = owner;
		}

		public Set<ModelStatement> getOwner() {
			return owner;
		}

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public ModelStatement next() {
			return last = iter.next();
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
			iter.remove(); // remove from owner
		}

		private void removeFrom(Set<ModelStatement> subjects) {
			if (subjects != owner) {
				subjects.remove(last);
			}
		}
	}

	private static class ModelNode<V extends Value> implements Serializable {

		private static final long serialVersionUID = -1205676084606998540L;

		Set<ModelStatement> subjects = new LinkedHashSet<ModelStatement>();

		Set<ModelStatement> predicates = new LinkedHashSet<ModelStatement>();

		Set<ModelStatement> objects = new LinkedHashSet<ModelStatement>();

		Set<ModelStatement> contexts = new LinkedHashSet<ModelStatement>();

		private V value;

		public ModelNode(V value) {
			this.value = value;
		}

		public V getValue() {
			return value;
		}
	}

	private static class ModelStatement extends ContextStatement {

		private static final long serialVersionUID = 2200404772364346279L;

		ModelNode<Resource> subj;

		ModelNode<IRI> pred;

		ModelNode<Value> obj;

		ModelNode<Resource> ctx;

		public ModelStatement(ModelNode<Resource> subj, ModelNode<IRI> pred, ModelNode<Value> obj,
				ModelNode<Resource> ctx)
		{
			super(subj.getValue(), pred.getValue(), obj.getValue(), ctx.getValue());
			assert subj != null;
			assert pred != null;
			assert obj != null;
			assert ctx != null;
			this.subj = subj;
			this.pred = pred;
			this.obj = obj;
			this.ctx = ctx;
		}

		@Override
		public Resource getSubject() {
			return subj.getValue();
		}

		@Override
		public IRI getPredicate() {
			return pred.getValue();
		}

		@Override
		public Value getObject() {
			return obj.getValue();
		}

		@Override
		public Resource getContext() {
			return ctx.getValue();
		}

		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (!super.equals(other))
				return false;
			if (getContext() == null)
				return ((Statement)other).getContext() == null;
			return getContext().equals(((Statement)other).getContext());
		}
	}

	private void writeObject(ObjectOutputStream s)
		throws IOException
	{
		// Write out any hidden serialization magic
		s.defaultWriteObject();
		// Write in size
		s.writeInt(statements.size());
		// Write in all elements
		for (ModelStatement st : statements) {
			Resource subj = st.getSubject();
			IRI pred = st.getPredicate();
			Value obj = st.getObject();
			Resource ctx = st.getContext();
			s.writeObject(new ContextStatement(subj, pred, obj, ctx));
		}
	}

	private void readObject(ObjectInputStream s)
		throws IOException, ClassNotFoundException
	{
		// Read in any hidden serialization magic
		s.defaultReadObject();
		// Read in size
		int size = s.readInt();
		values = new HashMap<Value, ModelNode>(size * 2);
		statements = new LinkedHashSet<ModelStatement>(size);
		// Read in all elements
		for (int i = 0; i < size; i++) {
			Statement st = (Statement)s.readObject();
			add(st);
		}
	}

	private ModelIterator matchPattern(Resource subj, IRI pred, Value obj, Resource... contexts) {
		Set<ModelStatement> set = choose(subj, pred, obj, contexts);
		Iterator<ModelStatement> it = set.iterator();
		Iterator<ModelStatement> iter;
		iter = new PatternIterator(it, subj, pred, obj, contexts);
		return new ModelIterator(iter, set);
	}

	private Set<ModelStatement> choose(Resource subj, IRI pred, Value obj, Resource... contexts) {
		contexts = notNull(contexts);
		Set<ModelStatement> s = null;
		Set<ModelStatement> p = null;
		Set<ModelStatement> o = null;
		if (subj != null) {
			if (!values.containsKey(subj))
				return Collections.emptySet();
			s = values.get(subj).subjects;
		}
		if (pred != null) {
			if (!values.containsKey(pred))
				return Collections.emptySet();
			p = values.get(pred).predicates;
		}
		if (obj != null) {
			if (!values.containsKey(obj))
				return Collections.emptySet();
			o = values.get(obj).objects;
		}
		if (contexts.length == 1) {
			if (!values.containsKey(contexts[0]))
				return Collections.emptySet();
			Set<ModelStatement> c = values.get(contexts[0]).contexts;
			return smallest(statements, s, p, o, c);
		}
		else {
			return smallest(statements, s, p, o);
		}
	}

	private Resource[] notNull(Resource[] contexts) {
		if (contexts == null) {
			return new Resource[] { null };
		}
		return contexts;
	}

	private Iterator find(Statement st) {
		Resource subj = st.getSubject();
		IRI pred = st.getPredicate();
		Value obj = st.getObject();
		Resource ctx = st.getContext();
		return matchPattern(subj, pred, obj, ctx);
	}

	private boolean addModelStatement(ModelStatement st) {
		Set<ModelStatement> subj = st.subj.subjects;
		Set<ModelStatement> pred = st.pred.predicates;
		Set<ModelStatement> obj = st.obj.objects;
		Set<ModelStatement> ctx = st.ctx.contexts;
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

	private Set<ModelStatement> smallest(Set<ModelStatement>... sets) {
		int minSize = Integer.MAX_VALUE;
		Set<ModelStatement> minSet = null;
		for (Set<ModelStatement> set : sets) {
			if (set != null && set.size() < minSize) {
				minSet = set;
			}
		}
		return minSet;
	}

	private <V extends Value> ModelNode<V> asNode(V value) {
		ModelNode node = values.get(value);
		if (node != null)
			return node;
		node = new ModelNode<V>(value);
		values.put(value, node);
		return node;
	}
}
