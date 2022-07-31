/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.experimental;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.queryrender.BaseTupleExprRenderer;
import org.eclipse.rdf4j.queryrender.QueryRenderer;
import org.eclipse.rdf4j.queryrender.sparql.SPARQLQueryRenderer;

/**
 * An alternative implementation of the SPARQL query renderer (more complete than the default
 * {@link SPARQLQueryRenderer})
 *
 * @author Andriy Nikolov
 * @author Jeen Broekstra
 * @author Andreas Schwarte
 *
 * @apiNote This feature is in an experimental state: its existence, signature or behavior may change without warning
 *          from one release to the next.
 */
@Experimental
public class SparqlQueryRenderer extends BaseTupleExprRenderer implements QueryRenderer {

	public SparqlQueryRenderer() {
	}

	@Override
	public QueryLanguage getLanguage() {
		return QueryLanguage.SPARQL;
	}

	@Override
	public String render(ParsedQuery theQuery) throws Exception {
		if (theQuery instanceof ParsedTupleQuery) {
			ParsedQueryPreprocessor parserVisitor = new ParsedQueryPreprocessor();
			PreprocessedQuerySerializer serializerVisitor = new PreprocessedQuerySerializer();
			SerializableParsedTupleQuery toSerialize = parserVisitor
					.transformToSerialize((ParsedTupleQuery) theQuery);
			return serializerVisitor.serialize(toSerialize);
		} else if (theQuery instanceof ParsedBooleanQuery) {
			ParsedQueryPreprocessor parserVisitor = new ParsedQueryPreprocessor();
			PreprocessedQuerySerializer serializerVisitor = new PreprocessedQuerySerializer();
			SerializableParsedBooleanQuery toSerialize = parserVisitor
					.transformToSerialize((ParsedBooleanQuery) theQuery);
			return serializerVisitor.serialize(toSerialize);
		} else if (theQuery instanceof ParsedGraphQuery) {
			ParsedQueryPreprocessor parserVisitor = new ParsedQueryPreprocessor();
			PreprocessedQuerySerializer serializerVisitor = new PreprocessedQuerySerializer();
			SerializableParsedConstructQuery toSerialize = parserVisitor
					.transformToSerialize((ParsedGraphQuery) theQuery);
			return serializerVisitor.serialize(toSerialize);
		} else {
			throw new UnsupportedOperationException("Only SELECT, ASK, and CONSTRUCT queries are supported");
		}
	}

	public String render(ParsedOperation theOperation) throws Exception {
		if (theOperation instanceof ParsedQuery) {
			return render((ParsedQuery) theOperation);
		} else if (theOperation instanceof ParsedUpdate) {
			return renderUpdate((ParsedUpdate) theOperation);
		}

		throw new UnsupportedOperationException("Only ParsedQuery and ParsedUpdate operations are supported");
	}

	private String renderUpdate(ParsedUpdate theUpdate) throws Exception {
		StringBuilder exprBuilder = new StringBuilder();
		boolean multipleExpressions = (theUpdate.getUpdateExprs().size() > 1);

		for (UpdateExpr updateExpr : theUpdate.getUpdateExprs()) {
			ParsedQueryPreprocessor parserVisitor = new ParsedQueryPreprocessor();
			PreprocessedQuerySerializer serializerVisitor = new PreprocessedQuerySerializer();
			SerializableParsedUpdate toSerialize = parserVisitor
					.transformToSerialize((UpdateExpr) updateExpr, theUpdate.getDatasetMapping().get(updateExpr));
			exprBuilder.append(serializerVisitor.serialize(toSerialize));
			if (multipleExpressions) {
				exprBuilder.append(";\n");
			}
		}
		return exprBuilder.toString();
	}

	@Override
	public String render(TupleExpr theExpr) throws Exception {
		ParsedQueryPreprocessor parserVisitor = new ParsedQueryPreprocessor();
		PreprocessedQuerySerializer serializerVisitor = new PreprocessedQuerySerializer();
		SerializableParsedTupleQuery toSerialize = parserVisitor.transformToSerialize(theExpr);
		return serializerVisitor.serialize(toSerialize);
	}

	@Override
	public String renderValueExpr(ValueExpr theExpr) throws Exception {
		PreprocessedQuerySerializer serializerVisitor = new PreprocessedQuerySerializer();
		theExpr.visit(serializerVisitor);
		return serializerVisitor.builder.toString();
	}

}
