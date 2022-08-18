/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.wrapper.data;

import static org.eclipse.rdf4j.model.util.Statements.statement;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.SailConnectionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Very simple RDFS backwardschaining connection that supports type inference on hasStatement and getStatement. It does
 * not support inference for SPARQL queries.
 */
public class VerySimpleRdfsBackwardsChainingConnection extends SailConnectionWrapper {

	private final RdfsSubClassOfReasoner rdfsSubClassOfReasoner;
	private static final Logger logger = LoggerFactory.getLogger(VerySimpleRdfsBackwardsChainingConnection.class);

	public VerySimpleRdfsBackwardsChainingConnection(SailConnection wrappedCon,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		super(wrappedCon);
		this.rdfsSubClassOfReasoner = rdfsSubClassOfReasoner;
	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws SailException {

		boolean hasStatement = super.hasStatement(subj, pred, obj, false, contexts);

		if (hasStatement) {
			return true;
		}

		if (rdfsSubClassOfReasoner != null && includeInferred && obj != null && obj.isResource()
				&& RDF.TYPE.equals(pred)) {

			Set<Resource> types = rdfsSubClassOfReasoner.backwardsChain((Resource) obj);
			if (types.size() == 1) {
				return hasStatement;
			}
			if (types.size() > 10) {
				try (CloseableIteration<? extends Statement, SailException> statements = super.getStatements(subj,
						RDF.TYPE, null, false, contexts)) {
					return statements.stream()
							.map(Statement::getObject)
							.filter(Value::isResource)
							.map(type -> ((Resource) type))
							.anyMatch(types::contains);
				}
			} else {
				return types
						.stream()
						.anyMatch(type -> super.hasStatement(subj, pred, type, false, contexts));
			}

		}

		return false;
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
			boolean includeInferred, Resource... contexts) throws SailException {

		if (rdfsSubClassOfReasoner != null && includeInferred && obj != null && obj.isResource()
				&& RDF.TYPE.equals(pred)) {
			Set<Resource> inferredTypes = rdfsSubClassOfReasoner.backwardsChain((Resource) obj);
			if (inferredTypes.size() > 1) {

				CloseableIteration<Statement, SailException>[] statementsMatchingInferredTypes = inferredTypes.stream()
						.map(r -> super.getStatements(subj, pred, r, false, contexts))
						.toArray(CloseableIteration[]::new);

				return new LookAheadIteration<>() {

					final UnionIteration<Statement, SailException> unionIteration = new UnionIteration<>(
							statementsMatchingInferredTypes);

					final HashSet<Statement> dedupe = new HashSet<>();

					Value localObject = obj;

					@Override
					protected Statement getNextElement() throws SailException {
						Statement next = null;

						while (next == null && unionIteration.hasNext()) {
							Statement statement = unionIteration.next();

							if (!localObject.equals(statement.getObject())) {
								statement = statement(statement.getSubject(), statement.getPredicate(), localObject,
										statement.getContext());
							} else {
								localObject = statement.getObject();
							}

							if (!dedupe.isEmpty()) {
								boolean contains = dedupe.contains(statement);
								if (!contains) {
									next = statement;
									dedupe.add(statement);
								}
							} else {
								next = statement;
								dedupe.add(statement);
							}

						}

						return next;
					}

					@Override
					public void remove() throws SailException {
						throw new IllegalStateException("Not implemented");
					}

					@Override
					protected void handleClose() throws SailException {
						try {
							unionIteration.close();
						} finally {
							super.handleClose();
						}
					}

				};

			}
		}

		return super.getStatements(subj, pred, obj, includeInferred, contexts);
	}
}
