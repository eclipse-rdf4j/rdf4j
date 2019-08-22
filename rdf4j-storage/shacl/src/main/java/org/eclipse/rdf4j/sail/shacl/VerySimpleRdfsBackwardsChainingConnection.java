/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.SailConnectionWrapper;

import java.util.HashSet;
import java.util.Set;

/**
 * Very simple RDFS backwardschaining connection that supports type inference on hasStatement and getStatement. It does
 * not support inference for SPARQL queries.
 */
class VerySimpleRdfsBackwardsChainingConnection extends SailConnectionWrapper {

	private final RdfsSubClassOfReasoner rdfsSubClassOfReasoner;

	VerySimpleRdfsBackwardsChainingConnection(SailConnection wrappedCon,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		super(wrappedCon);
		this.rdfsSubClassOfReasoner = rdfsSubClassOfReasoner;
	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws SailException {

		boolean hasStatement = super.hasStatement(subj, pred, obj, includeInferred, contexts);

		if (rdfsSubClassOfReasoner != null && includeInferred && obj instanceof Resource
				&& RDF.TYPE.equals(pred)) {
			return hasStatement | rdfsSubClassOfReasoner.backwardsChain((Resource) obj)
					.stream()
					.map(type -> super.hasStatement(subj, pred, type, false, contexts))
					.reduce((a, b) -> a || b)
					.orElse(false);
		}
		return hasStatement;
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
			boolean includeInferred, Resource... contexts) throws SailException {

		if (rdfsSubClassOfReasoner != null && includeInferred && obj instanceof Resource
				&& RDF.TYPE.equals(pred)) {
			Set<Resource> inferredTypes = rdfsSubClassOfReasoner.backwardsChain((Resource) obj);
			if (!inferredTypes.isEmpty()) {

				CloseableIteration<Statement, SailException>[] statementsMatchingInferredTypes = inferredTypes.stream()
						.map(r -> super.getStatements(subj, pred, r, false, contexts))
						.toArray(CloseableIteration[]::new);

				return new LookAheadIteration<Statement, SailException>() {

					UnionIteration<Statement, SailException> unionIteration = new UnionIteration<>(
							statementsMatchingInferredTypes);

					HashSet<Statement> dedupe = new HashSet<>();

					@Override
					protected Statement getNextElement() throws SailException {
						Statement next = null;

						while (next == null && unionIteration.hasNext()) {
							Statement temp = unionIteration.next();
							temp = SimpleValueFactory.getInstance()
									.createStatement(temp.getSubject(), temp.getPredicate(), obj, temp.getContext());

							if (!dedupe.isEmpty()) {
								boolean contains = dedupe.contains(temp);
								if (!contains) {
									next = temp;
									dedupe.add(next);
								}
							} else {
								next = temp;
								dedupe.add(next);
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
