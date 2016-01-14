/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleIRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDatasetClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIRI;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOperation;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOperationContainer;

/**
 * Extracts a SPARQL {@link Dataset} from an ASTQueryContainer, if one is
 * contained.
 * 
 * @author Simon Schenk
 * @author Arjohn Kampman
 */
public class DatasetDeclProcessor {

	/**
	 * Extracts a SPARQL {@link Dataset} from an ASTQueryContainer, if one is
	 * contained. Returns null otherwise.
	 * 
	 * @param qc
	 *        The query model to resolve relative URIs in.
	 * @throws MalformedQueryException
	 *         If DatasetClause does not contain a valid URI.
	 */
	public static Dataset process(ASTOperationContainer qc)
		throws MalformedQueryException
	{
		SimpleDataset dataset = null;

		ASTOperation op = qc.getOperation();
		if (op != null) {

			List<ASTDatasetClause> datasetClauses = op.getDatasetClauseList();

			if (!datasetClauses.isEmpty()) {
				dataset = new SimpleDataset();

				for (ASTDatasetClause dc : datasetClauses) {

					ASTIRI astIri = dc.jjtGetChild(ASTIRI.class);

					try {
						IRI uri = SESAME.NIL;
						
						if (astIri != null) {
							uri = SimpleValueFactory.getInstance().createIRI(astIri.getValue());
						}
						
						if (dc.isNamed()) {
							dataset.addNamedGraph(uri);
						}
						else {
							dataset.addDefaultGraph(uri);
						}
					}
					catch (IllegalArgumentException e) {
						throw new MalformedQueryException(e.getMessage(), e);
					}
				}
			}
		}

		return dataset;
	}
}
