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
package org.eclipse.rdf4j.sail.inferencer.fc;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnection;
import org.eclipse.rdf4j.sail.inferencer.InferencerConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A forward-chaining inferencer that infers the direct-type hierarchy relations {@link SESAME#DIRECTSUBCLASSOF
 * sesame:directSubClassOf}, {@link SESAME#DIRECTSUBPROPERTYOF sesame:directSubPropertyOf} and {@link SESAME#DIRECTTYPE
 * sesame:directType}.
 * <p>
 * The semantics of this inferencer are defined as follows:
 *
 * <pre>
 *    Class A is a direct subclass of B iff:
 *       1. A is a subclass of B and;
 *       2. A and B are not equa and;
 *       3. there is no class C (unequal A and B) such that
 *          A is a subclass of C and C of B.
 *
 *    Property P is a direct subproperty of Q iff:
 *       1. P is a subproperty of Q and;
 *       2. P and Q are not equal and;
 *       3. there is no property R (unequal P and Q) such that
 *          P is a subproperty of R and R of Q.
 *
 *    Resource I is of direct type T iff:
 *       1. I is of type T and
 *       2. There is no class U (unequal T) such that:
 *           a. U is a subclass of T and;
 *           b. I is of type U.
 * </pre>
 */
public class DirectTypeHierarchyInferencer extends NotifyingSailWrapper {

	private static final Logger logger = LoggerFactory.getLogger(DirectTypeHierarchyInferencer.class);

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final ParsedGraphQuery DIRECT_SUBCLASSOF_MATCHER;

	private static final ParsedGraphQuery DIRECT_SUBCLASSOF_QUERY;

	private static final ParsedGraphQuery DIRECT_SUBPROPERTYOF_MATCHER;

	private static final ParsedGraphQuery DIRECT_SUBPROPERTYOF_QUERY;

	private static final ParsedGraphQuery DIRECT_TYPE_MATCHER;

	private static final ParsedGraphQuery DIRECT_TYPE_QUERY;

	static {
		try {
			DIRECT_SUBCLASSOF_MATCHER = QueryParserUtil.parseGraphQuery(QueryLanguage.SPARQL,
					"CONSTRUCT WHERE { ?X sesame:directSubClassOf ?Y } ", null);

			DIRECT_SUBPROPERTYOF_MATCHER = QueryParserUtil.parseGraphQuery(QueryLanguage.SPARQL,
					"CONSTRUCT WHERE { ?X sesame:directType ?Y }", null);

			DIRECT_TYPE_MATCHER = QueryParserUtil.parseGraphQuery(QueryLanguage.SPARQL,
					"CONSTRUCT WHERE { ?X sesame:directSubPropertyOf ?Y}", null);

			DIRECT_SUBCLASSOF_QUERY = QueryParserUtil.parseGraphQuery(QueryLanguage.SPARQL,
					"CONSTRUCT { ?X sesame:directSubClassOf ?Y } "
							+ "WHERE { ?X rdfs:subClassOf ?Y . "
							+ "FILTER X != Y AND "
							+ "NOT EXISTS { SELECT ?Z WHERE { ?X rdfs:subClassOf ?Z. ?Z rdfs:subClassOf ?Y . FILTER ?X != ?Z AND ?Z != ?Y }}}",
					null);

			DIRECT_SUBPROPERTYOF_QUERY = QueryParserUtil.parseGraphQuery(QueryLanguage.SPARQL,
					"CONSTRUCT { ?X sesame:directSubPropertyOf ?Y } "
							+ "WHERE { ?X rdfs:subPropertyOf ?Y . "
							+ "FILTER X != Y AND "
							+ "NOT EXISTS { SELECT ?Z WHERE { ?X rdfs:subPropertyOf ?Z. ?Z rdfs:subPropertyOf ?Y . FILTER ?X != ?Z AND ?Z != ?Y }}}",
					null);

			DIRECT_TYPE_QUERY = QueryParserUtil.parseGraphQuery(QueryLanguage.SPARQL,
					"CONSTRUCT { ?X sesame:directType ?Y } WHERE { ?X rdf:type ?Y . \n "
							+ "FILTER NOT EXISTS { SELECT ?Z WHERE { ?X rdf:type ?Z. ?Z rdfs:subClassOf ?Y . FILTER ?Z != ?Y }}}",
					null);
		} catch (MalformedQueryException e) {
			// Can only occur due to a bug in this code
			throw new RuntimeException(e);
		}
	}

	/*--------------*
	 * Constructors *
	 *--------------*/

	public DirectTypeHierarchyInferencer() {
		super();
	}

	public DirectTypeHierarchyInferencer(NotifyingSail baseSail) {
		super(baseSail);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public InferencerConnection getConnection() throws SailException {
		try {
			InferencerConnection con = (InferencerConnection) super.getConnection();
			return new DirectTypeHierarchyInferencerConnection(con);
		} catch (ClassCastException e) {
			throw new SailException(e.getMessage(), e);
		}
	}

	@Override
	public void init() throws SailException {
		super.init();

		try (InferencerConnection con = getConnection()) {
			con.begin();
			con.flushUpdates();
			con.commit();
		}
	}

	/*-----------------------------------------------------*
	 * Inner class DirectTypeHierarchyInferencerConnection *
	 *-----------------------------------------------------*/

	private class DirectTypeHierarchyInferencerConnection extends InferencerConnectionWrapper
			implements SailConnectionListener {

		/**
		 * Flag indicating whether an update of the inferred statements is needed.
		 */
		private boolean updateNeeded = false;

		public DirectTypeHierarchyInferencerConnection(InferencerConnection con) {
			super(con);
			con.addConnectionListener(this);
		}

		// called by base sail
		@Override
		public void statementAdded(Statement st) {
			checkUpdatedStatement(st);
		}

		// called by base sail
		@Override
		public void statementRemoved(Statement st) {
			checkUpdatedStatement(st);
		}

		private void checkUpdatedStatement(Statement st) {
			IRI pred = st.getPredicate();

			if (pred.equals(RDF.TYPE) || pred.equals(RDFS.SUBCLASSOF) || pred.equals(RDFS.SUBPROPERTYOF)) {
				updateNeeded = true;
			}
		}

		@Override
		public void rollback() throws SailException {
			super.rollback();
			updateNeeded = false;
		}

		@Override
		public void flushUpdates() throws SailException {
			super.flushUpdates();

			while (updateNeeded) {
				try {
					// Determine which statements should be added and which should be
					// removed
					Collection<Statement> oldStatements = new HashSet<>(256);
					Collection<Statement> newStatements = new HashSet<>(256);

					evaluateIntoStatements(DIRECT_SUBCLASSOF_MATCHER, oldStatements);
					evaluateIntoStatements(DIRECT_SUBPROPERTYOF_MATCHER, oldStatements);
					evaluateIntoStatements(DIRECT_TYPE_MATCHER, oldStatements);

					evaluateIntoStatements(DIRECT_SUBCLASSOF_QUERY, newStatements);
					evaluateIntoStatements(DIRECT_SUBPROPERTYOF_QUERY, newStatements);
					evaluateIntoStatements(DIRECT_TYPE_QUERY, newStatements);

					logger.debug("existing virtual properties: {}", oldStatements.size());
					logger.debug("new virtual properties: {}", newStatements.size());

					// Remove the statements that should be retained from both sets
					Collection<Statement> unchangedStatements = new HashSet<>(oldStatements);
					unchangedStatements.retainAll(newStatements);

					oldStatements.removeAll(unchangedStatements);
					newStatements.removeAll(unchangedStatements);

					logger.debug("virtual properties to remove: {}", oldStatements.size());
					logger.debug("virtual properties to add: {}", newStatements.size());

					Resource[] contexts = new Resource[] { null };

					for (Statement st : oldStatements) {
						removeInferredStatement(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
					}

					for (Statement st : newStatements) {
						addInferredStatement(st.getSubject(), st.getPredicate(), st.getObject(), contexts);
					}

					updateNeeded = false;
				} catch (RDFHandlerException e) {
					Throwable t = e.getCause();
					if (t instanceof SailException) {
						throw (SailException) t;
					} else {
						throw new SailException(t);
					}
				} catch (QueryEvaluationException e) {
					throw new SailException(e);
				}

				super.flushUpdates();
			}
		}

		private void evaluateIntoStatements(ParsedGraphQuery query, Collection<Statement> statements)
				throws SailException, RDFHandlerException, QueryEvaluationException {
			try (CloseableIteration<? extends BindingSet, QueryEvaluationException> bindingsIter = getWrappedConnection()
					.evaluate(query.getTupleExpr(), null, EmptyBindingSet.getInstance(), true)) {
				ValueFactory vf = getValueFactory();

				while (bindingsIter.hasNext()) {
					BindingSet bindings = bindingsIter.next();

					Value subj = bindings.getValue("subject");
					Value pred = bindings.getValue("predicate");
					Value obj = bindings.getValue("object");

					if (subj instanceof Resource && pred instanceof IRI && obj != null) {
						statements.add(vf.createStatement((Resource) subj, (IRI) pred, obj));
					}
				}
			}
		}
	} // end inner class DirectTypeHierarchyInferencerConnection
}
