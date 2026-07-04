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
package org.eclipse.rdf4j.queryrender.sparql;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.queryrender.QueryRenderer;

/**
 * <p>
 * Implementation of the {@link QueryRenderer} interface which renders queries into the SPARQL syntax.
 * </p>
 *
 * @author Michael Grove
 */
public class SPARQLQueryRenderer implements QueryRenderer {

	/**
	 * The query renderer
	 */
	private final SparqlTupleExprRenderer mRenderer = new SparqlTupleExprRenderer();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public QueryLanguage getLanguage() {
		return QueryLanguage.SPARQL;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String render(final ParsedQuery theQuery) throws Exception {
		mRenderer.reset();

		TupleExpr tupleExpr = theQuery.getTupleExpr();
		if (tupleExpr instanceof QueryRoot) {
			tupleExpr = ((QueryRoot) tupleExpr).getArg();
		}
		StringBuffer aBody = new StringBuffer(mRenderer.render(tupleExpr));

		boolean aFirst;

		StringBuilder aQuery = new StringBuilder();

		if (theQuery instanceof ParsedTupleQuery) {
			aQuery.append("select ");
		} else if (theQuery instanceof ParsedBooleanQuery) {
			aQuery.append("ask").append(System.lineSeparator());
		} else {
			aQuery.append("construct ");
		}

		if (mRenderer.isDistinct()) {
			aQuery.append("distinct ");
		}

		if (mRenderer.isReduced() && theQuery instanceof ParsedTupleQuery) {
			aQuery.append("reduced ");
		}

		if (!mRenderer.getProjection().isEmpty() && !(theQuery instanceof ParsedBooleanQuery)) {

			aFirst = true;

			if (!(theQuery instanceof ParsedTupleQuery)) {
				aQuery.append(" {").append(System.lineSeparator());
			}

			for (ProjectionElemList aList : mRenderer.getProjection()) {
				if (SparqlTupleExprRenderer.isSPOElemList(aList)) {
					if (!aFirst) {
						aQuery.append(System.lineSeparator());
					} else {
						aFirst = false;
					}

					aQuery.append("  ").append(mRenderer.renderPattern(mRenderer.toStatementPattern(aList)));
				} else {
					for (ProjectionElem aElem : aList.getElements()) {
						if (!aFirst) {
							aQuery.append(" ");
						} else {
							aFirst = false;
						}

						aQuery.append("?" + aElem.getName());
					}
				}
			}

			if (!(theQuery instanceof ParsedTupleQuery)) {
				aQuery.append("}");
			}

			aQuery.append(System.lineSeparator());
		} else if (mRenderer.getProjection().isEmpty()) {
			if (theQuery instanceof ParsedGraphQuery) {
				aQuery.append("{ }").append(System.lineSeparator());
			} else if (theQuery instanceof ParsedTupleQuery) {
				aQuery.append("*").append(System.lineSeparator());
			}
		}

		if (theQuery.getDataset() != null) {
			for (IRI aURI : theQuery.getDataset().getDefaultGraphs()) {
				aQuery.append("from <").append(aURI).append(">").append(System.lineSeparator());
			}

			for (IRI aURI : theQuery.getDataset().getNamedGraphs()) {
				aQuery.append("from named <").append(aURI).append(">").append(System.lineSeparator());
			}
		}

		if (aBody.length() > 0) {

			// this removes any superflous trailing commas, i think this is just an
			// artifact of this code's history
			// from initially being a serql renderer. i'll leave it for now, but i
			// think this is to be removed.
			// test cases to prove these things work would be lovely.
			if (aBody.toString().trim().lastIndexOf(',') == aBody.length() - 1) {
				aBody.setCharAt(aBody.lastIndexOf(","), ' ');
			}

			if (!(theQuery instanceof ParsedBooleanQuery)) {
				aQuery.append("where ");
			}

			aQuery.append("{").append(System.lineSeparator());
			aQuery.append(aBody);
			aQuery.append("}");
		}

		if (!mRenderer.getOrdering().isEmpty()) {
			aQuery.append(System.lineSeparator()).append("order by ");

			aFirst = true;
			for (OrderElem aOrder : mRenderer.getOrdering()) {
				if (!aFirst) {
					aQuery.append(" ");
				} else {
					aFirst = false;
				}

				if (aOrder.isAscending()) {
					aQuery.append(mRenderer.renderValueExpr(aOrder.getExpr()));
				} else {
					aQuery.append("desc(");
					aQuery.append(mRenderer.renderValueExpr(aOrder.getExpr()));
					aQuery.append(")");
				}
			}
		}

		if (mRenderer.getLimit() != -1 && !(theQuery instanceof ParsedBooleanQuery)) {
			aQuery.append(System.lineSeparator()).append("limit ").append(mRenderer.getLimit());
		}

		if (mRenderer.getOffset() != -1) {
			aQuery.append(System.lineSeparator()).append("offset ").append(mRenderer.getOffset());
		}

		return aQuery.toString();
	}
}
