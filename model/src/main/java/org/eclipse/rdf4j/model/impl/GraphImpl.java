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
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.rdf4j.OpenRDFUtil;
import org.eclipse.rdf4j.model.Graph;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.util.iterators.FilterIterator;

/**
 * Basic implementation of Graph.
 * 
 * @deprecated since release 2.7.0. Use a {@link org.eclipse.rdf4j.model.Model} implementation (e.g. {@link TreeModel}
 *             or {@link LinkedHashModel} instead.
 * @author Arjohn Kampman
 */
@Deprecated
public class GraphImpl extends AbstractCollection<Statement> implements Graph {

	private static final long serialVersionUID = -5307095904382050478L;

	protected LinkedList<Statement> statements;

	transient protected ValueFactory valueFactory;

	public GraphImpl(ValueFactory valueFactory) {
		super();
		statements = new LinkedList<>();
		setValueFactory(valueFactory);
	}

	public GraphImpl() {
		this(SimpleValueFactory.getInstance());
	}

	public GraphImpl(ValueFactory valueFactory, Collection<? extends Statement> statements) {
		this(valueFactory);
		addAll(statements);
	}

	public GraphImpl(Collection<? extends Statement> statements) {
		this(SimpleValueFactory.getInstance(), statements);
	}

	@Override
	public ValueFactory getValueFactory() {
		return valueFactory;
	}

	public void setValueFactory(ValueFactory valueFactory) {
		this.valueFactory = valueFactory;
	}

	@Override
	public Iterator<Statement> iterator() {
		return statements.iterator();
	}

	@Override
	public int size() {
		return statements.size();
	}

	@Override
	public boolean add(Statement st) {
		return statements.add(st);
	}

	@Override
	public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
		OpenRDFUtil.verifyContextNotNull(contexts);

		boolean graphChanged = false;

		if (contexts.length == 0) {
			graphChanged = add(valueFactory.createStatement(subj, pred, obj));
		} else {
			for (Resource context : contexts) {
				graphChanged |= add(valueFactory.createStatement(subj, pred, obj, context));
			}
		}

		return graphChanged;
	}

	@Override
	public Iterator<Statement> match(Resource subj, IRI pred, Value obj, Resource... contexts) {
		OpenRDFUtil.verifyContextNotNull(contexts);
		return new PatternIterator(iterator(), subj, pred, obj, contexts);
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		setValueFactory(SimpleValueFactory.getInstance());
	}

	/*-----------------------------*
	 * Inner class PatternIterator *
	 *-----------------------------*/

	private static class PatternIterator extends FilterIterator<Statement> {

		private Resource subj;

		private IRI pred;

		private Value obj;

		private Resource[] contexts;

		public PatternIterator(Iterator<? extends Statement> iter, Resource subj, IRI pred, Value obj,
				Resource... contexts) {
			super(iter);
			this.subj = subj;
			this.pred = pred;
			this.obj = obj;
			this.contexts = contexts;
		}

		@Override
		protected boolean accept(Statement st) {
			if (subj != null && !subj.equals(st.getSubject())) {
				return false;
			}
			if (pred != null && !pred.equals(st.getPredicate())) {
				return false;
			}
			if (obj != null && !obj.equals(st.getObject())) {
				return false;
			}

			if (contexts.length == 0) {
				// Any context matches
				return true;
			} else {
				// Accept if one of the contexts from the pattern matches
				Resource stContext = st.getContext();

				for (Resource context : contexts) {
					if (context == null && stContext == null) {
						return true;
					}
					if (context != null && context.equals(stContext)) {
						return true;
					}
				}

				return false;
			}
		}
	}
}
