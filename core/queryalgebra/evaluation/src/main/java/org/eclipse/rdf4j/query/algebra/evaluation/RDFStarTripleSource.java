/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryEvaluationException;

public interface RDFStarTripleSource extends TripleSource {
	/**
	 * Gets all Triple nodes that have a specific subject, predicate and/or object. All three parameters may be null to
	 * indicate wildcards.
	 *
	 * @param subj A Resource specifying the triple's subject, or <var>null</var> for a wildcard.
	 * @param pred A URI specifying the triple's predicate, or <var>null</var> for a wildcard.
	 * @param obj  A Value specifying the triple's object, or <var>null</var> for a wildcard.
	 * @return An iterator over the relevant triples.
	 * @throws QueryEvaluationException If the rdf star triple source failed to get the statements.
	 */
	CloseableIteration<? extends Triple, QueryEvaluationException> getRdfStarTriples(Resource subj, IRI pred,
			Value obj) throws QueryEvaluationException;

}
