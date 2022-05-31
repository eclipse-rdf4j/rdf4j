/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;

/**
 * Utility methods for working with {@link TripleSource}s.
 */
// TODO this duplicates a lot of functionality already found in classes like Models and RDFCollections.
// Closer integration and/or rewriting of these utilities may be required.
@Deprecated(since = "4.1.0", forRemoval = true)
public class TripleSources {

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	TripleSources() {
	}

	public static Iteration<Resource, QueryEvaluationException> listResources(final Resource subj,
			final TripleSource store) throws QueryEvaluationException {
		return new ConvertingIteration<Value, Resource, QueryEvaluationException>(
				new FilterIteration<Value, QueryEvaluationException>(list(subj, store)) {

					@Override
					protected boolean accept(Value v) throws QueryEvaluationException {
						return (v instanceof Resource);
					}
				}) {

			@Override
			protected Resource convert(Value v) throws QueryEvaluationException {
				return (Resource) v;
			}
		};
	}

	public static Iteration<Value, QueryEvaluationException> list(final Resource subj,
			final TripleSource store) throws QueryEvaluationException {
		if (subj == null) {
			throw new NullPointerException("RDF list subject cannot be null");
		}
		return new Iteration<Value, QueryEvaluationException>() {

			Resource list = subj;

			@Override
			public boolean hasNext() throws QueryEvaluationException {
				return !RDF.NIL.equals(list);
			}

			@Override
			public Value next() throws QueryEvaluationException {
				Value v = singleValue(list, RDF.FIRST, store);
				if (v == null) {
					throw new QueryEvaluationException("List missing rdf:first: " + list);
				}
				Resource nextList = (Resource) singleValue(list, RDF.REST, store);
				if (nextList == null) {
					throw new QueryEvaluationException("List missing rdf:rest: " + list);
				}
				list = nextList;
				return v;
			}

			@Override
			public void remove() throws QueryEvaluationException {
				throw new UnsupportedOperationException();
			}
		};
	}

	public static boolean booleanValue(Resource subj, IRI pred, TripleSource store) throws QueryEvaluationException {
		Value v = TripleSources.singleValue(subj, pred, store);
		if (v == null) {
			return false;
		} else if (v instanceof Literal) {
			try {
				return ((Literal) v).booleanValue();
			} catch (IllegalArgumentException e) {
				throw new QueryEvaluationException(
						"Value for " + pred + " must be of datatype " + XSD.BOOLEAN + ": " + subj);
			}
		} else {
			throw new QueryEvaluationException("Non-literal value for " + pred + ": " + subj);
		}
	}

	public static Value singleValue(Resource subj, IRI pred, TripleSource store) throws QueryEvaluationException {
		Statement stmt = single(subj, pred, null, store);
		return (stmt != null) ? stmt.getObject() : null;
	}

	/**
	 * Returns the single statement with the given subject, predicate and object or null if none exists. Context
	 * information is disregarded.
	 *
	 * @param subj null for any.
	 * @param pred null for any.
	 * @param obj  null for any.
	 * @throws QueryEvaluationException If there is more than one such statement.
	 */
	public static Statement single(Resource subj, IRI pred, Value obj, TripleSource store)
			throws QueryEvaluationException {
		Statement stmt;
		try (CloseableIteration<? extends Statement, QueryEvaluationException> stmts = store.getStatements(subj, pred,
				obj)) {
			if (stmts.hasNext()) {
				stmt = stmts.next();
				while (stmts.hasNext()) {
					Statement nextStmt = stmts.next();
					if (!org.eclipse.rdf4j.model.util.Statements.isSameTriple(stmt, nextStmt)) {
						throw new QueryEvaluationException(
								"Multiple statements for pattern: " + subj + " " + pred + " " + obj);
					}
				}
			} else {
				stmt = null;
			}
		}
		return stmt;
	}

	public static CloseableIteration<IRI, QueryEvaluationException> getSubjectURIs(IRI predicate,
			Value object, TripleSource store) throws QueryEvaluationException {
		return new ConvertingIteration<Statement, IRI, QueryEvaluationException>(
				new FilterIteration<Statement, QueryEvaluationException>(store.getStatements(null, predicate, object)) {

					@Override
					protected boolean accept(Statement stmt) throws QueryEvaluationException {
						return (stmt.getSubject() instanceof IRI);
					}
				}) {

			@Override
			protected IRI convert(Statement stmt) throws QueryEvaluationException {
				return (IRI) stmt.getSubject();
			}
		};
	}

	public static CloseableIteration<Resource, QueryEvaluationException> getObjectResources(Resource subject,
			IRI predicate, TripleSource store) throws QueryEvaluationException {
		return new ConvertingIteration<Statement, Resource, QueryEvaluationException>(
				new FilterIteration<Statement, QueryEvaluationException>(
						store.getStatements(subject, predicate, null)) {

					@Override
					protected boolean accept(Statement stmt) throws QueryEvaluationException {
						return (stmt.getObject() instanceof Resource);
					}
				}) {

			@Override
			protected Resource convert(Statement stmt) throws QueryEvaluationException {
				return (Resource) stmt.getObject();
			}
		};
	}

	public static CloseableIteration<IRI, QueryEvaluationException> getObjectURIs(Resource subject,
			IRI predicate, TripleSource store) throws QueryEvaluationException {
		return new ConvertingIteration<Statement, IRI, QueryEvaluationException>(
				new FilterIteration<Statement, QueryEvaluationException>(
						store.getStatements(subject, predicate, null)) {

					@Override
					protected boolean accept(Statement stmt) throws QueryEvaluationException {
						return (stmt.getObject() instanceof IRI);
					}
				}) {

			@Override
			protected IRI convert(Statement stmt) throws QueryEvaluationException {
				return (IRI) stmt.getObject();
			}
		};
	}

	public static CloseableIteration<Literal, QueryEvaluationException> getObjectLiterals(Resource subject,
			IRI predicate, TripleSource store) throws QueryEvaluationException {
		return new ConvertingIteration<Statement, Literal, QueryEvaluationException>(
				new FilterIteration<Statement, QueryEvaluationException>(
						store.getStatements(subject, predicate, null)) {

					@Override
					protected boolean accept(Statement stmt) throws QueryEvaluationException {
						return (stmt.getObject() instanceof Literal);
					}
				}) {

			@Override
			protected Literal convert(Statement stmt) throws QueryEvaluationException {
				return (Literal) stmt.getObject();
			}
		};
	}
}
