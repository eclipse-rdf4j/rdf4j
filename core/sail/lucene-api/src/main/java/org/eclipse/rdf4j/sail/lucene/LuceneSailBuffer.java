/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * A buffer collecting all transaction operations (triples that need to be added, removed, clear operations) so that
 * they can be executed at once during commit.
 *
 * @author sauermann
 * @author andriy.nikolov
 *
 * @deprecated since 3.0. This feature is for internal use only: its existence, signature or behavior may change without
 *             warning from one release to the next.
 */
@Deprecated
@InternalUseOnly
public class LuceneSailBuffer {

	private static class ContextAwareStatementImpl implements Statement {

		private static final long serialVersionUID = -2976244503679342649L;

		private final Statement delegate;

		public ContextAwareStatementImpl(Statement delegate) {
			if (delegate == null) {
				throw new RuntimeException("Trying to add/remove a null statement");
			}
			this.delegate = delegate;
		}

		@Override
		public Resource getSubject() {
			return delegate.getSubject();
		}

		@Override
		public IRI getPredicate() {
			return delegate.getPredicate();
		}

		@Override
		public Value getObject() {
			return delegate.getObject();
		}

		@Override
		public Resource getContext() {
			return delegate.getContext();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof Statement) {
				Statement other = (Statement) obj;

				return this.delegate.equals(other)
						&& ((this.getContext() == null && other.getContext() == null) || (this.getContext() != null
								&& other.getContext() != null && this.getContext().equals(other.getContext())));
			}
			return false;
		}

		@Override
		public int hashCode() {
			return delegate.hashCode() + ((getContext() == null) ? 0 : 29791 * getContext().hashCode());
		}

		@Override
		public String toString() {
			return delegate.toString();
		}

	}

	public static class Operation {

	}

	public static class AddRemoveOperation extends Operation {

		HashSet<Statement> added = new HashSet<>();

		HashSet<Statement> removed = new HashSet<>();

		Map<Resource, Boolean> typeAdded;

		Set<Resource> typeRemoved;

		public AddRemoveOperation() {
			this(false);
		}

		public AddRemoveOperation(boolean useType) {
			if (useType) {
				typeAdded = new HashMap<>();
				typeRemoved = new HashSet<>();
			}
		}

		public void add(Statement s) {
			if (!removed.remove(s)) {
				added.add(s);
			}
		}

		public void addType(Statement s, boolean rightType) {
			if (!typeRemoved.remove(s.getSubject())) {
				typeAdded.put(s.getSubject(), rightType);
			}
		}

		public void remove(Statement s) {
			if (!added.remove(s)) {
				removed.add(s);
			}
		}

		public void removeType(Statement s) {
			if (typeAdded.remove(s.getSubject()) == null) {
				typeRemoved.add(s.getSubject());
			}
		}

		/**
		 * @return Returns the added.
		 */
		public HashSet<Statement> getAdded() {
			return added;
		}

		/**
		 * @return Returns the removed.
		 */
		public HashSet<Statement> getRemoved() {
			return removed;
		}

		/**
		 * @return Returns the added type
		 */
		public Map<Resource, Boolean> getTypeAdded() {
			return typeAdded;
		}

		/**
		 * @return Returns the removed type
		 */
		public Set<Resource> getTypeRemoved() {
			return typeRemoved;
		}
	}

	public static class ClearContextOperation extends Operation {

		Resource[] contexts;

		public ClearContextOperation(Resource[] contexts) {
			this.contexts = contexts;
		}

		/**
		 * @return Returns the contexts.
		 */
		public Resource[] getContexts() {
			return contexts;
		}

	}

	public static class ClearOperation extends Operation {

	}

	private final ArrayList<Operation> operations = new ArrayList<>();

	private final boolean useType;

	public LuceneSailBuffer() {
		this(false);
	}

	public LuceneSailBuffer(boolean useType) {
		this.useType = useType;
	}

	/**
	 * Add this statement to the buffer
	 *
	 * @param s the statement
	 */
	public synchronized void add(Statement s) {
		// check if the last operation was adding/Removing triples
		Operation o = (operations.isEmpty()) ? null : operations.get(operations.size() - 1);
		if ((o == null) || !(o instanceof AddRemoveOperation)) {
			o = new AddRemoveOperation(useType);
			operations.add(o);
		}
		AddRemoveOperation aro = (AddRemoveOperation) o;
		aro.add(new ContextAwareStatementImpl(s));
	}

	/**
	 * Add this type statement to the buffer
	 *
	 * @param s the statement
	 */
	public synchronized void addTypeStatement(Statement s, boolean rightType) {
		// check if the last operation was adding/Removing triples
		Operation o = (operations.isEmpty()) ? null : operations.get(operations.size() - 1);
		if (!(o instanceof AddRemoveOperation)) {
			o = new AddRemoveOperation(useType);
			operations.add(o);
		}
		AddRemoveOperation aro = (AddRemoveOperation) o;
		aro.addType(new ContextAwareStatementImpl(s), rightType);
	}

	/**
	 * Remove this statement to the buffer
	 *
	 * @param s the statement
	 */
	public synchronized void remove(Statement s) {
		// check if the last operation was adding/Removing triples
		Operation o = (operations.isEmpty()) ? null : operations.get(operations.size() - 1);
		if ((o == null) || !(o instanceof AddRemoveOperation)) {
			o = new AddRemoveOperation(useType);
			operations.add(o);
		}
		AddRemoveOperation aro = (AddRemoveOperation) o;
		aro.remove(new ContextAwareStatementImpl(s));
	}

	/**
	 * Remove this type statement to the buffer
	 *
	 * @param s the statement
	 */
	public synchronized void removeTypeStatement(Statement s) {
		// check if the last operation was adding/Removing triples
		Operation o = (operations.isEmpty()) ? null : operations.get(operations.size() - 1);
		if (!(o instanceof AddRemoveOperation)) {
			o = new AddRemoveOperation(useType);
			operations.add(o);
		}
		AddRemoveOperation aro = (AddRemoveOperation) o;
		aro.removeType(new ContextAwareStatementImpl(s));
	}

	public synchronized void clear(Resource[] contexts) {
		if ((contexts == null) || (contexts.length == 0)) {
			operations.add(new ClearOperation());
		} else {
			operations.add(new ClearContextOperation(contexts));
		}
	}

	/**
	 * Iterator over the operations
	 */
	public synchronized Iterator<Operation> operationsIterator() {
		return operations.iterator();
	}

	/**
	 * the list of operations. You must not change it
	 */
	public synchronized List<Operation> operations() {
		return operations;
	}

	/**
	 * Optimize will remove any changes that are done before a clear()
	 */
	public void optimize() {
		for (int i = operations.size() - 1; i >= 0; i--) {
			Operation o = operations.get(i);
			if (o instanceof ClearOperation) {
				// remove everything before
				// is is now the size of the operations to be removed
				while (i > 0) {
					operations.remove(i);
					i--;
				}
				return;
			}
		}
	}

	/**
	 * reset the buffer, empty the operations list
	 */
	public void reset() {
		operations.clear();
	}

}
