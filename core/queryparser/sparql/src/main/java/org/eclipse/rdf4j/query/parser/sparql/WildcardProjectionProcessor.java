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

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBind;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTConstraint;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDescribe;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDescribeQuery;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOperation;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOperationContainer;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTProjectionElem;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSelect;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSelectQuery;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTTripleRef;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTVar;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTWhereClause;
import org.eclipse.rdf4j.query.parser.sparql.ast.Node;
import org.eclipse.rdf4j.query.parser.sparql.ast.SyntaxTreeBuilderTreeConstants;
import org.eclipse.rdf4j.query.parser.sparql.ast.VisitorException;

/**
 * Processes 'wildcard' projections, making them explicit by adding the appropriate variable nodes to them.
 *
 * @author arjohn
 * @author Jeen Broekstra
 *
 * @deprecated since 3.0. This feature is for internal use only: its existence, signature or behavior may change without
 *             warning from one release to the next.
 */
@Deprecated
public class WildcardProjectionProcessor extends AbstractASTVisitor {

	public static void process(ASTOperationContainer container) throws MalformedQueryException {

		ASTOperation operation = container.getOperation();

		// scan for nested SELECT clauses in the operation's WHERE clause
		if (operation != null) {
			ASTWhereClause whereClause = operation.getWhereClause();

			// DESCRIBE queries and certain update operations can be without a WHERE clause
			if (whereClause != null) {
				SelectClauseCollector collector = new SelectClauseCollector();
				try {
					whereClause.jjtAccept(collector, null);

					Set<ASTSelect> selectClauses = collector.getSelectClauses();

					for (ASTSelect selectClause : selectClauses) {
						if (selectClause.isWildcard()) {
							ASTSelectQuery q = (ASTSelectQuery) selectClause.jjtGetParent();

							addQueryVars(q.getWhereClause(), selectClause);
							selectClause.setWildcard(false);
						}
					}

				} catch (VisitorException e) {
					throw new MalformedQueryException(e);
				}
			}
		}

		if (operation instanceof ASTSelectQuery) {
			// check for wildcard in upper SELECT query

			ASTSelectQuery selectQuery = (ASTSelectQuery) operation;
			ASTSelect selectClause = selectQuery.getSelect();
			if (selectClause.isWildcard()) {
				addQueryVars(selectQuery.getWhereClause(), selectClause);
				selectClause.setWildcard(false);
			}
		} else if (operation instanceof ASTDescribeQuery) {
			// check for possible wildcard in DESCRIBE query
			ASTDescribeQuery describeQuery = (ASTDescribeQuery) operation;
			ASTDescribe describeClause = describeQuery.getDescribe();

			if (describeClause.isWildcard()) {
				addQueryVars(describeQuery.getWhereClause(), describeClause);
				describeClause.setWildcard(false);
			}
		}
	}

	private static void addQueryVars(ASTWhereClause queryBody, Node wildcardNode) throws MalformedQueryException {
		QueryVariableCollector visitor = new QueryVariableCollector();

		try {
			// Collect variable names from query
			queryBody.jjtAccept(visitor, null);

			// Adds ASTVar nodes to the ASTProjectionElem nodes and to the parent
			for (String varName : visitor.getVariableNames()) {
				ASTVar varNode = new ASTVar(SyntaxTreeBuilderTreeConstants.JJTVAR);
				ASTProjectionElem projectionElemNode = new ASTProjectionElem(
						SyntaxTreeBuilderTreeConstants.JJTPROJECTIONELEM);

				varNode.setName(varName);
				projectionElemNode.jjtAppendChild(varNode);
				varNode.jjtSetParent(projectionElemNode);

				wildcardNode.jjtAppendChild(projectionElemNode);
				projectionElemNode.jjtSetParent(wildcardNode);

			}

		} catch (VisitorException e) {
			throw new MalformedQueryException(e);
		}
	}

	/*------------------------------------*
	 * Inner class QueryVariableCollector *
	 *------------------------------------*/

	private static class QueryVariableCollector extends AbstractASTVisitor {

		private final Set<String> variableNames = new LinkedHashSet<>();

		public Set<String> getVariableNames() {
			return variableNames;
		}

		@Override
		public Object visit(ASTSelectQuery node, Object data) throws VisitorException {
			// stop visitor from processing body of sub-select, only add variables
			// from the projection
			return visit(node.getSelect(), data);
		}

		@Override
		public Object visit(ASTProjectionElem node, Object data) throws VisitorException {
			// only include the actual alias from a projection element in a
			// subselect, not any variables used as
			// input to a function
			String alias = node.getAlias();
			if (alias != null) {
				variableNames.add(alias);
				return null;
			} else {
				return super.visit(node, data);
			}
		}

		@Override
		public Object visit(ASTBind node, Object data) throws VisitorException {
			// only include the actual alias from a BIND
			// exception: in case of ASTTRipleRef include its vars
			Node first = node.jjtGetChild(0);
			if (first instanceof ASTTripleRef) {
				ASTTripleRef triple = (ASTTripleRef) first;
				super.visit(triple, data);
			}
			Node aliasNode = node.jjtGetChild(1);
			String alias = ((ASTVar) aliasNode).getName();

			if (alias != null) {
				variableNames.add(alias);
				return null;
			} else {
				return super.visit(node, data);
			}
		}

		@Override
		public Object visit(ASTConstraint node, Object data) throws VisitorException {
			// ignore variables in filter expressions
			return null;
		}

		@Override
		public Object visit(ASTVar node, Object data) throws VisitorException {
			if (!node.isAnonymous()) {
				variableNames.add(node.getName());
			}
			return super.visit(node, data);
		}
	}

	/*------------------------------------*
	 * Inner class SelectClauseCollector  *
	 *------------------------------------*/

	private static class SelectClauseCollector extends AbstractASTVisitor {

		private final Set<ASTSelect> selectClauses = new LinkedHashSet<>();

		public Set<ASTSelect> getSelectClauses() {
			return selectClauses;
		}

		@Override
		public Object visit(ASTSelect node, Object data) throws VisitorException {
			selectClauses.add(node);
			return super.visit(node, data);
		}
	}
}
