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
package org.eclipse.rdf4j.query.parser.sparql;

import java.util.List;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDatasetClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIRI;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTModify;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOperation;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOperationContainer;

/**
 * Extracts a SPARQL {@link Dataset} from an ASTQueryContainer, if one is contained.
 *
 * @author Simon Schenk
 * @author Arjohn Kampman
 *
 * @apiNote This feature is for internal use only: its existence, signature or behavior may change without warning from
 *          one release to the next.
 */
@InternalUseOnly
public class DatasetDeclProcessor {

	/**
	 * Extracts a SPARQL {@link Dataset} from an ASTQueryContainer, if one is contained. Returns null otherwise.
	 *
	 * @param qc The query model to resolve relative URIs in.
	 * @throws MalformedQueryException If DatasetClause does not contain a valid URI.
	 */
	public static Dataset process(ASTOperationContainer qc) throws MalformedQueryException {
		SimpleDataset dataset = null;

		ASTOperation op = qc.getOperation();
		if (op != null) {

			List<ASTDatasetClause> datasetClauses = op.getDatasetClauseList();

			if (!datasetClauses.isEmpty()) {
				dataset = new SimpleDataset();

				for (ASTDatasetClause dc : datasetClauses) {

					ASTIRI astIri = dc.jjtGetChild(ASTIRI.class);

					try {
						IRI uri = RDF4J.NIL;

						if (astIri != null) {
							uri = SimpleValueFactory.getInstance().createIRI(astIri.getValue());
						}

						boolean withClause = false;
						if (op instanceof ASTModify) {
							if (dc.equals(((ASTModify) op).getWithClause())) {
								withClause = true;
								dataset.setDefaultInsertGraph(uri);
								dataset.addDefaultRemoveGraph(uri);
							}
						}

						// set graphs to read from if this is not a WITH clause,
						// or (if it is), it's not overridden by other dataset
						// clauses.
						if (!withClause || datasetClauses.size() == 1) {
							if (dc.isNamed()) {
								dataset.addNamedGraph(uri);
							} else {
								dataset.addDefaultGraph(uri);
							}
						}
					} catch (IllegalArgumentException e) {
						throw new MalformedQueryException(e.getMessage(), e);
					}
				}
			}
		}

		return dataset;
	}
}
