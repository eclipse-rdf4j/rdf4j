/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.OrderElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
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
	private SparqlTupleExprRenderer mRenderer = new SparqlTupleExprRenderer();

	/**
	 * @inheritDoc
	 */
	@Override
	public QueryLanguage getLanguage() {
		return QueryLanguage.SPARQL;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public String render(final ParsedQuery theQuery)
		throws Exception
	{
		mRenderer.reset();

		StringBuffer aBody = new StringBuffer(mRenderer.render(theQuery.getTupleExpr()));

		boolean aFirst = true;

		StringBuffer aQuery = new StringBuffer();

		if (theQuery instanceof ParsedTupleQuery) {
			aQuery.append("select ");
		}
		else if (theQuery instanceof ParsedBooleanQuery) {
			aQuery.append("ask\n");
		}
		else {
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
				aQuery.append(" {\n");
			}

			for (ProjectionElemList aList : mRenderer.getProjection()) {
				if (SparqlTupleExprRenderer.isSPOElemList(aList)) {
					if (!aFirst) {
						aQuery.append("\n");
					}
					else {
						aFirst = false;
					}

					aQuery.append("  ").append(mRenderer.renderPattern(mRenderer.toStatementPattern(aList)));
				}
				else {
					for (ProjectionElem aElem : aList.getElements()) {
						if (!aFirst) {
							aQuery.append(" ");
						}
						else {
							aFirst = false;
						}

						aQuery.append("?" + aElem.getSourceName());

						// SPARQL does not support this, its an artifact of copy and
						// paste from the serql stuff
						// aQuery.append(mRenderer.getExtensions().containsKey(aElem.getSourceName())
						// ?
						// mRenderer.renderValueExpr(mRenderer.getExtensions().get(aElem.getSourceName()))
						// : "?"+aElem.getSourceName());
						//
						// if (!aElem.getSourceName().equals(aElem.getTargetName()) ||
						// (mRenderer.getExtensions().containsKey(aElem.getTargetName())
						// &&
						// !mRenderer.getExtensions().containsKey(aElem.getSourceName())))
						// {
						// aQuery.append(" as ").append(mRenderer.getExtensions().containsKey(aElem.getTargetName())
						// ?
						// mRenderer.renderValueExpr(mRenderer.getExtensions().get(aElem.getTargetName()))
						// : aElem.getTargetName());
						// }
					}
				}
			}

			if (!(theQuery instanceof ParsedTupleQuery)) {
				aQuery.append("}");
			}

			aQuery.append("\n");
		}
		else if (mRenderer.getProjection().isEmpty()) {
			if (theQuery instanceof ParsedGraphQuery) {
				aQuery.append("{ }\n");
			}
			else if (theQuery instanceof ParsedTupleQuery) {
				aQuery.append("*\n");
			}
		}

		if (theQuery.getDataset() != null) {
			for (IRI aURI : theQuery.getDataset().getDefaultGraphs()) {
				aQuery.append("from <").append(aURI).append(">\n");
			}

			for (IRI aURI : theQuery.getDataset().getNamedGraphs()) {
				aQuery.append("from named <").append(aURI).append(">\n");
			}
		}

		if (aBody.length() > 0) {

			// this removes any superflous trailing commas, i think this is just an
			// artifact of this code's history
			// from initially being a serql renderer. i'll leave it for now, but i
			// think this is to be removed.
			// test cases to prove these things work would be lovely.
			if (aBody.toString().trim().lastIndexOf(",") == aBody.length() - 1) {
				aBody.setCharAt(aBody.lastIndexOf(","), ' ');
			}

			if (!(theQuery instanceof ParsedBooleanQuery)) {
				aQuery.append("where ");
			}

			aQuery.append("{\n");
			aQuery.append(aBody);
			aQuery.append("}");
		}

		if (!mRenderer.getOrdering().isEmpty()) {
			aQuery.append("\norder by ");

			aFirst = true;
			for (OrderElem aOrder : mRenderer.getOrdering()) {
				if (!aFirst) {
					aQuery.append(" ");
				}
				else {
					aFirst = false;
				}

				if (aOrder.isAscending()) {
					aQuery.append(mRenderer.renderValueExpr(aOrder.getExpr()));
				}
				else {
					aQuery.append("desc(");
					aQuery.append(mRenderer.renderValueExpr(aOrder.getExpr()));
					aQuery.append(")");
				}
			}
		}

		if (mRenderer.getLimit() != -1 && !(theQuery instanceof ParsedBooleanQuery)) {
			aQuery.append("\nlimit ").append(mRenderer.getLimit());
		}

		if (mRenderer.getOffset() != -1) {
			aQuery.append("\noffset ").append(mRenderer.getOffset());
		}

		return aQuery.toString();
	}
}
