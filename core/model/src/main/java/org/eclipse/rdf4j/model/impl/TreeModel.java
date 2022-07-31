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

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.LexicalValueComparator;
import org.eclipse.rdf4j.model.util.PatternIterator;

/**
 * A Red-Black tree based {@link Model} implementation. The model is sorted according to the lexical ordering of terms.
 * <p>
 * This implementation provides guaranteed log(n) time cost for filtered access by any number of terms. If an index is
 * not yet available for a set of positions, it is created at runtime using a {@link TreeSet}.
 * <p>
 * <b>Note that this implementation is not synchronized.</b> If multiple threads access a model concurrently, even if
 * all of them are read operations, it must be synchronized externally. This is typically accomplished by synchronizing
 * on some object that naturally encapsulates the model. If no such object exists, the set should be "wrapped" using the
 * Models.synchronizedModel method.
 * </p>
 *
 * @author James Leigh
 */
public class TreeModel extends AbstractModel implements SortedSet<Statement> {

	private static final long serialVersionUID = 7893197431354524479L;

	static final IRI BEFORE = new SimpleIRI("urn:from");

	static final IRI AFTER = new SimpleIRI("urn:to");

	private final LexicalValueComparator vc = new LexicalValueComparator();

	final Set<Namespace> namespaces = new TreeSet<>();

	final List<StatementTree> trees = new ArrayList<>();

	public TreeModel() {
		trees.add(new StatementTree("spog".toCharArray()));
	}

	public TreeModel(Model model) {
		this(model.getNamespaces());
		addAll(model);
	}

	public TreeModel(Collection<? extends Statement> c) {
		this();
		addAll(c);
	}

	public TreeModel(Set<Namespace> namespaces, Collection<? extends Statement> c) {
		this(c);
		this.namespaces.addAll(namespaces);
	}

	public TreeModel(Set<Namespace> namespaces) {
		this();
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
		return trees.get(0).size();
	}

	@Override
	public void clear() {
		for (StatementTree tree : trees) {
			tree.clear();
		}
	}

	@Override
	public Comparator<? super Statement> comparator() {
		return trees.get(0).tree.comparator();
	}

	@Override
	public Statement first() {
		return trees.get(0).tree.first();
	}

	@Override
	public Statement last() {
		return trees.get(0).tree.last();
	}

	public Statement lower(Statement e) {
		return trees.get(0).tree.lower(e);
	}

	public Statement floor(Statement e) {
		return trees.get(0).tree.floor(e);
	}

	public Statement ceiling(Statement e) {
		return trees.get(0).tree.ceiling(e);
	}

	public Statement higher(Statement e) {
		return trees.get(0).tree.higher(e);
	}

	public Statement pollFirst() {
		try {
			Statement first = trees.get(0).tree.first();
			remove(first);
			return first;
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	public Statement pollLast() {
		try {
			Statement last = trees.get(0).tree.last();
			remove(last);
			return last;
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	@Override
	public SortedSet<Statement> subSet(Statement fromElement, Statement toElement) {
		return subSet(fromElement, true, toElement, false);
	}

	@Override
	public SortedSet<Statement> headSet(Statement toElement) {
		return subSet(before(null, null, null, null), true, toElement, false);
	}

	@Override
	public SortedSet<Statement> tailSet(Statement fromElement) {
		return subSet(fromElement, true, after(null, null, null, null), true);
	}

	@Override
	public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
		if (subj == null || pred == null || obj == null) {
			throw new UnsupportedOperationException("Incomplete statement");
		}
		boolean changed = false;
		for (Value ctx : notEmpty(contexts)) {
			if (ctx == null || ctx instanceof Resource) {
				Statement st = new TreeStatement(subj, pred, obj, (Resource) ctx);
				for (StatementTree tree : trees) {
					changed |= tree.add(st);
				}
			}
		}
		return changed;
	}

	@Override
	public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts) {
		if (contexts == null || contexts.length == 1 && contexts[0] == null) {
			Iterator<Statement> iter = matchPattern(subj, pred, obj, null);
			while (iter.hasNext()) {
				if (iter.next().getContext() == null) {
					return true;
				}
			}
			return false;
		} else if (contexts.length == 0) {
			return matchPattern(subj, pred, obj, null).hasNext();
		} else {
			for (Resource ctx : contexts) {
				if (ctx == null) {
					if (contains(subj, pred, obj, (Resource[]) null)) {
						return true;
					}
				} else if (matchPattern(subj, pred, obj, ctx).hasNext()) {
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts) {
		if (isEmpty()) {
			return false;
		}

		boolean changed = false;
		if (contexts == null || contexts.length == 1 && contexts[0] == null) {
			Iterator<Statement> iter = matchPattern(subj, pred, obj, null);
			while (iter.hasNext()) {
				if (iter.next().getContext() == null) {
					iter.remove();
					changed = true;
				}
			}
		} else if (contexts.length == 0) {
			Iterator<Statement> iter = matchPattern(subj, pred, obj, null);
			while (iter.hasNext()) {
				iter.next();
				iter.remove();
				changed = true;
			}
		} else {
			for (Resource ctx : contexts) {
				if (ctx == null) {
					changed |= remove(subj, pred, obj, (Resource[]) null);
				} else {
					Iterator<Statement> iter = matchPattern(subj, pred, obj, ctx);
					while (iter.hasNext()) {
						iter.next();
						iter.remove();
						changed = true;
					}
				}
			}
		}
		return changed;
	}

	@Override
	public Iterator<Statement> iterator() {
		return matchPattern(null, null, null, null);
	}

	@Override
	public Model filter(final Resource subj, final IRI pred, final Value obj, final Resource... contexts) {
		if (contexts != null && contexts.length == 0) {
			return new FilteredModel(this, subj, pred, obj, contexts) {

				private static final long serialVersionUID = 396293781006255959L;

				@Override
				public Iterator<Statement> iterator() {
					return matchPattern(subj, pred, obj, null);
				}

				@Override
				protected void removeFilteredTermIteration(Iterator<Statement> iter, Resource subj, IRI pred, Value obj,
						Resource... contexts) {
					TreeModel.this.removeTermIteration(iter, subj, pred, obj, contexts);
				}
			};
		} else if (contexts != null && contexts.length == 1 && contexts[0] != null) {
			return new FilteredModel(this, subj, pred, obj, contexts) {

				@Override
				public Iterator<Statement> iterator() {
					return matchPattern(subj, pred, obj, contexts[0]);
				}

				@Override
				protected void removeFilteredTermIteration(Iterator<Statement> iter, Resource subj, IRI pred, Value obj,
						Resource... contexts) {
					TreeModel.this.removeTermIteration(iter, subj, pred, obj, contexts);
				}
			};
		} else {
			return new FilteredModel(this, subj, pred, obj, contexts) {

				private static final long serialVersionUID = 396293781006255959L;

				@Override
				public Iterator<Statement> iterator() {
					return new PatternIterator<>(matchPattern(subj, pred, obj, null), subj, pred, obj, contexts);
				}

				@Override
				protected void removeFilteredTermIteration(Iterator<Statement> iter, Resource subj, IRI pred, Value obj,
						Resource... contexts) {
					TreeModel.this.removeTermIteration(iter, subj, pred, obj, contexts);
				}
			};
		}
	}

	@Override
	public void removeTermIteration(Iterator<Statement> iterator, Resource subj, IRI pred, Value obj,
			Resource... contexts) {
		TreeSet<Statement> owner = ((ModelIterator) iterator).getOwner();
		if (contexts == null || contexts.length == 1 && contexts[0] == null) {
			StatementTree chosen = choose(subj, pred, obj, null);
			Iterator<Statement> iter = chosen.subIterator(before(subj, pred, obj, null), true,
					after(subj, pred, obj, null), true);
			iter = new PatternIterator<>(iter, subj, pred, obj, contexts);
			removeAll(owner, chosen, iter);
		} else if (contexts.length == 0) {
			StatementTree chosen = choose(subj, pred, obj, null);
			Iterator<Statement> iter = chosen.subIterator(before(subj, pred, obj, null), true,
					after(subj, pred, obj, null), true);
			removeAll(owner, chosen, iter);
		} else {
			for (Value ctx : notEmpty(contexts)) {
				if (ctx == null) {
					removeTermIteration(iterator, subj, pred, obj, (Resource[]) null);
				} else {
					StatementTree chosen = choose(subj, pred, obj, ctx);
					Iterator<Statement> iter = chosen.subIterator(before(subj, pred, obj, ctx), true,
							after(subj, pred, obj, ctx), true);
					removeAll(owner, chosen, iter);
				}
			}
		}
	}

	Iterator<Statement> matchPattern(Resource subj, IRI pred, Value obj, Resource ctx) {
		if (!isResourceURIResource(subj, pred, ctx)) {
			Set<Statement> emptySet = Collections.emptySet();
			return emptySet.iterator();
		}
		StatementTree tree = choose(subj, pred, obj, ctx);
		Iterator<Statement> it = tree.subIterator(before(subj, pred, obj, ctx), true, after(subj, pred, obj, ctx),
				true);
		return new ModelIterator(it, tree);
	}

	int compareValue(Value o1, Value o2) {
		if (o1 == o2) {
			return 0;
		}
		if (o1 == BEFORE) {
			return -1;
		}
		if (o2 == BEFORE) {
			return 1;
		}
		if (o1 == AFTER) {
			return 1;
		}
		if (o2 == AFTER) {
			return -1;
		}
		return vc.compare(o1, o2);
	}

	SortedSet<Statement> subSet(Statement lo, boolean loInclusive, Statement hi, boolean hiInclusive) {
		return new SubSet(this, new TreeStatement(lo), loInclusive, new TreeStatement(hi), hiInclusive);
	}

	private void removeAll(TreeSet<Statement> owner, StatementTree chosen, Iterator<Statement> iter) {
		while (iter.hasNext()) {
			Statement last = iter.next();
			for (StatementTree tree : trees) {
				if (tree.owns(owner)) {
					tree.reindex();
					tree.remove(last);
				} else if (tree != chosen) {
					tree.remove(last);
				}
			}
			iter.remove(); // remove from chosen
		}
	}

	private boolean isResourceURIResource(Value subj, Value pred, Value ctx) {
		return (subj == null || subj instanceof Resource) && (pred == null || pred instanceof IRI)
				&& (ctx == null || ctx instanceof Resource);
	}

	private Value[] notEmpty(Value[] contexts) {
		if (contexts == null || contexts.length == 0) {
			return new Resource[] { null };
		}
		return contexts;
	}

	private Statement before(Value subj, Value pred, Value obj, Value ctx) {
		Resource s = subj instanceof Resource ? (Resource) subj : BEFORE;
		IRI p = pred instanceof IRI ? (IRI) pred : BEFORE;
		Value o = obj instanceof Value ? obj : BEFORE;
		Resource c = ctx instanceof Resource ? (Resource) ctx : BEFORE;
		return new TreeStatement(s, p, o, c);
	}

	private Statement after(Value subj, Value pred, Value obj, Value ctx) {
		Resource s = subj instanceof Resource ? (Resource) subj : AFTER;
		IRI p = pred instanceof IRI ? (IRI) pred : AFTER;
		Value o = obj instanceof Value ? obj : AFTER;
		Resource c = ctx instanceof Resource ? (Resource) ctx : AFTER;
		return new TreeStatement(s, p, o, c);
	}

	private StatementTree choose(Value subj, Value pred, Value obj, Value ctx) {
		for (StatementTree tree : trees) {
			if (tree.isIndexed(subj, pred, obj, ctx)) {
				return tree;
			}
		}
		return index(subj, pred, obj, ctx);
	}

	private StatementTree index(Value subj, Value pred, Value obj, Value ctx) {
		int idx = 0;
		char[] index = new char[4];
		if (subj != null) {
			index[idx++] = 's';
		}
		if (pred != null) {
			index[idx++] = 'p';
		}
		if (obj != null) {
			index[idx++] = 'o';
		}
		if (ctx != null) {
			index[idx++] = 'g';
		}
		if (pred == null) {
			index[idx++] = 'p';
		}
		if (obj == null) {
			index[idx++] = 'o';
		}
		if (ctx == null) {
			index[idx++] = 'g';
		}
		if (subj == null) {
			index[idx++] = 's';
		}
		StatementTree tree = new StatementTree(index);

		tree.addAll(trees.get(0));
		trees.add(tree);
		return tree;
	}

	@Override
	public boolean isEmpty() {
		if (trees.isEmpty()) {
			return true;
		}
		return trees.get(0).isEmpty();
	}

	private class ModelIterator implements Iterator<Statement> {

		private final Iterator<Statement> iter;

		private final TreeSet<Statement> owner;

		private Statement last;

		public ModelIterator(Iterator<Statement> iter, StatementTree owner) {
			this.iter = iter;
			this.owner = owner.tree;
		}

		public TreeSet<Statement> getOwner() {
			return owner;
		}

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public Statement next() {
			return last = iter.next();
		}

		@Override
		public void remove() {
			if (last == null) {
				throw new IllegalStateException();
			}
			for (StatementTree tree : trees) {
				removeFrom(tree);
			}
			iter.remove(); // remove from owner
		}

		private void removeFrom(StatementTree subjects) {
			if (!subjects.owns(owner)) {
				subjects.remove(last);
			}
		}
	}

	static class TreeStatement extends ContextStatement {

		private static final long serialVersionUID = -7720419322256724495L;

		public TreeStatement(Statement st) {
			super(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
		}

		public TreeStatement(Resource subject, IRI predicate, Value object, Resource ctx) {
			super(subject, predicate, object, ctx);
		}
	}

	class StatementTree implements Serializable {

		private static final long serialVersionUID = -7580746419791799953L;

		private final char[] index;

		TreeSet<Statement> tree;

		public StatementTree(char[] index) {
			this.index = index;
			Comparator<Statement>[] comparators = new Comparator[index.length];
			for (int i = 0; i < index.length; i++) {
				switch (index[i]) {
				case 's':
					comparators[i] = new SubjectComparator();
					break;
				case 'p':
					comparators[i] = new PredicateComparator();
					break;
				case 'o':
					comparators[i] = new ObjectComparator();
					break;
				case 'g':
					comparators[i] = new GraphComparator();
					break;
				default:
					throw new AssertionError();
				}
			}
			tree = new TreeSet<>(new StatementComparator(comparators));
		}

		public boolean owns(TreeSet<Statement> set) {
			return tree == set;
		}

		public boolean isIndexed(Value subj, Value pred, Value obj, Value ctx) {
			boolean wild = false;
			for (int i = 0; i < index.length; i++) {
				switch (index[i]) {
				case 's':
					if (subj == null) {
						wild = true;
					} else if (wild) {
						return false;
					}
					break;
				case 'p':
					if (pred == null) {
						wild = true;
					} else if (wild) {
						return false;
					}
					break;
				case 'o':
					if (obj == null) {
						wild = true;
					} else if (wild) {
						return false;
					}
					break;
				case 'g':
					if (ctx == null) {
						wild = true;
					} else if (wild) {
						return false;
					}
					break;
				default:
					throw new AssertionError();
				}
			}
			return true;
		}

		public void reindex() {
			TreeSet<Statement> treeSet = new TreeSet<>(tree.comparator());
			treeSet.addAll(tree);
			tree = treeSet;
		}

		public boolean add(Statement e) {
			return tree.add(e);
		}

		public boolean addAll(StatementTree c) {
			return tree.addAll(c.tree);
		}

		public int size() {
			return tree.size();
		}

		public void clear() {
			tree.clear();
		}

		public boolean remove(Object o) {
			return tree.remove(o);
		}

		public Iterator<Statement> subIterator(Statement fromElement, boolean fromInclusive, Statement toElement,
				boolean toInclusive) {
			return tree.subSet(fromElement, true, toElement, true).iterator();
		}

		public boolean isEmpty() {
			return tree.isEmpty();
		}
	}

	class SubjectComparator implements Serializable, Comparator<Statement> {

		private static final long serialVersionUID = 5275239384134217143L;

		@Override
		public int compare(Statement s1, Statement s2) {
			return compareValue(s1.getSubject(), s2.getSubject());
		}
	}

	class PredicateComparator implements Serializable, Comparator<Statement> {

		private static final long serialVersionUID = -883414941022127103L;

		@Override
		public int compare(Statement s1, Statement s2) {
			return compareValue(s1.getPredicate(), s2.getPredicate());
		}
	}

	class ObjectComparator implements Serializable, Comparator<Statement> {

		private static final long serialVersionUID = 1768294714884456242L;

		@Override
		public int compare(Statement s1, Statement s2) {
			return compareValue(s1.getObject(), s2.getObject());
		}
	}

	class GraphComparator implements Serializable, Comparator<Statement> {

		private static final long serialVersionUID = 7027824614533897706L;

		@Override
		public int compare(Statement s1, Statement s2) {
			return compareValue(s1.getContext(), s2.getContext());
		}
	}

	static class StatementComparator implements Serializable, Comparator<Statement> {

		private static final long serialVersionUID = -5602364720279633641L;

		private final Comparator<Statement>[] comparators;

		public StatementComparator(Comparator<Statement>... comparators) {
			this.comparators = comparators;
		}

		@Override
		public int compare(Statement s1, Statement s2) {
			for (Comparator<Statement> c : comparators) {
				int r1 = c.compare(s1, s2);
				if (r1 != 0) {
					return r1;
				}
			}
			return 0;
		}
	}

	static class SubSet extends AbstractSet<Statement> implements Serializable, SortedSet<Statement> {

		private static final long serialVersionUID = 6362727792092563793L;

		private final TreeModel model;

		private final TreeStatement lo, hi;

		private final boolean loInclusive, hiInclusive;

		public SubSet(TreeModel model, TreeStatement lo, boolean loInclusive, TreeStatement hi, boolean hiInclusive) {
			this.model = model;
			this.lo = lo;
			this.loInclusive = loInclusive;
			this.hi = hi;
			this.hiInclusive = hiInclusive;
		}

		public Optional<Namespace> getNamespace(String prefix) {
			return model.getNamespace(prefix);
		}

		public Set<Namespace> getNamespaces() {
			return model.getNamespaces();
		}

		public Namespace setNamespace(String prefix, String name) {
			return model.setNamespace(prefix, name);
		}

		public void setNamespace(Namespace namespace) {
			model.setNamespace(namespace);
		}

		public Optional<Namespace> removeNamespace(String prefix) {
			return model.removeNamespace(prefix);
		}

		@Override
		public int size() {
			return subSet().size();
		}

		@Override
		public void clear() {
			StatementTree tree = model.trees.get(0);
			Iterator<Statement> it = tree.subIterator(lo, loInclusive, hi, hiInclusive);
			it = model.new ModelIterator(it, tree);
			while (it.hasNext()) {
				it.remove();
			}
		}

		@Override
		public Comparator<? super Statement> comparator() {
			return model.comparator();
		}

		@Override
		public Statement first() {
			return subSet().first();
		}

		@Override
		public Statement last() {
			return subSet().last();
		}

		public Statement lower(Statement e) {
			return subSet().lower(e);
		}

		@Override
		public boolean isEmpty() {
			return subSet().isEmpty();
		}

		public Statement floor(Statement e) {
			return subSet().floor(e);
		}

		public Statement ceiling(Statement e) {
			return subSet().ceiling(e);
		}

		public Statement higher(Statement e) {
			return subSet().higher(e);
		}

		public Statement pollFirst() {
			try {
				Statement first = subSet().first();
				model.remove(first);
				return first;
			} catch (NoSuchElementException e) {
				return null;
			}
		}

		public Statement pollLast() {
			try {
				Statement last = subSet().last();
				model.remove(last);
				return last;
			} catch (NoSuchElementException e) {
				return null;
			}
		}

		@Override
		public SortedSet<Statement> subSet(Statement fromElement, Statement toElement) {
			boolean fromInclusive = true;
			boolean toInclusive = false;
			if (comparator().compare(fromElement, lo) < 0) {
				fromElement = lo;
				fromInclusive = loInclusive;
			}
			if (comparator().compare(hi, toElement) < 0) {
				toElement = hi;
				toInclusive = hiInclusive;
			}
			return model.subSet(fromElement, fromInclusive, toElement, toInclusive);
		}

		@Override
		public SortedSet<Statement> headSet(Statement toElement) {
			boolean toInclusive = false;
			if (comparator().compare(hi, toElement) < 0) {
				toElement = hi;
				toInclusive = hiInclusive;
			}
			return model.subSet(lo, loInclusive, toElement, toInclusive);
		}

		@Override
		public SortedSet<Statement> tailSet(Statement fromElement) {
			boolean fromInclusive = true;
			if (comparator().compare(fromElement, lo) < 0) {
				fromElement = lo;
				fromInclusive = loInclusive;
			}
			return model.subSet(fromElement, fromInclusive, hi, hiInclusive);
		}

		@Override
		public Iterator<Statement> iterator() {
			StatementTree tree = model.trees.get(0);
			Iterator<Statement> it = tree.subIterator(lo, loInclusive, hi, hiInclusive);
			return model.new ModelIterator(it, tree);
		}

		private NavigableSet<Statement> subSet() {
			return model.trees.get(0).tree.subSet(lo, loInclusive, hi, hiInclusive);
		}
	}
}
