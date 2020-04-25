/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail;

import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.QueryModelTreeToGenericPlanNode;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.query.explanation.GenericPlanNode;
import org.eclipse.rdf4j.query.impl.AbstractParserQuery;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.sail.SailConnection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Arjohn Kampman
 */
public abstract class SailQuery extends AbstractParserQuery {

	private final SailRepositoryConnection con;
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	protected SailQuery(ParsedQuery parsedQuery, SailRepositoryConnection con) {
		super(parsedQuery);
		this.con = con;
	}

	protected SailRepositoryConnection getConnection() {
		return con;
	}

	@Override
	public Explanation explain(Explanation.Level level) {

		TupleExpr tupleExpr = getParsedQuery().getTupleExpr();

		SailConnection sailCon = getConnection().getSailConnection();

		QueryModelNode explainedTupleExpr = sailCon.explain(level, tupleExpr, getActiveDataset(), getBindings(),
				getIncludeInferred());

		return new Explanation() {
			@Override
			public String toString() {
				return toGenericPlanNode().toString();
			}

			@Override
			public GenericPlanNode toGenericPlanNode() {
				QueryModelTreeToGenericPlanNode queryModelTreeToGenericPlanNode = new QueryModelTreeToGenericPlanNode(
						explainedTupleExpr);
				explainedTupleExpr.visit(queryModelTreeToGenericPlanNode);
				return queryModelTreeToGenericPlanNode.getGenericPlanNode();
			}

			@Override
			public String toJson() {
				try {
					// TODO: Consider removing pretty printer
					return JSON_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL)
							.writerWithDefaultPrettyPrinter()
							.writeValueAsString(toGenericPlanNode());
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}
		};

	}
}
