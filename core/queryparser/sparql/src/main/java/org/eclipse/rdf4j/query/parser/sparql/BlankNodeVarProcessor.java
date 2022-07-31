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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBasicGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBlankNode;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBlankNodePropertyList;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTCollection;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOperationContainer;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTVar;
import org.eclipse.rdf4j.query.parser.sparql.ast.SyntaxTreeBuilderTreeConstants;
import org.eclipse.rdf4j.query.parser.sparql.ast.VisitorException;

/**
 * Processes blank nodes in the query body, replacing them with variables while retaining scope.
 *
 * @author Arjohn Kampman
 *
 * @apiNote This feature is for internal use only: its existence, signature or behavior may change without warning from
 *          one release to the next.
 */
@InternalUseOnly
public class BlankNodeVarProcessor extends AbstractASTVisitor {

	public static Set<String> process(ASTOperationContainer qc) throws MalformedQueryException {
		try {
			BlankNodeToVarConverter converter = new BlankNodeToVarConverter();
			qc.jjtAccept(converter, null);
			return converter.getUsedBNodeIDs();
		} catch (VisitorException e) {
			throw new MalformedQueryException(e);
		}
	}

	/*-------------------------------------*
	 * Inner class BlankNodeToVarConverter *
	 *-------------------------------------*/

	private static class BlankNodeToVarConverter extends AbstractASTVisitor {

		private int anonVarNo = 1;

		private final Map<String, String> conversionMap = new HashMap<>();

		private final Set<String> usedBNodeIDs = new HashSet<>();

		private String createAnonVarName() {
			return "_anon_" + anonVarNo++;
		}

		public Set<String> getUsedBNodeIDs() {
			usedBNodeIDs.addAll(conversionMap.keySet());
			return Collections.unmodifiableSet(usedBNodeIDs);
		}

		@Override
		public Object visit(ASTBasicGraphPattern node, Object data) throws VisitorException {
			// The same Blank node ID cannot be used across Graph Patterns
			usedBNodeIDs.addAll(conversionMap.keySet());

			// Blank nodes are scoped to Basic Graph Patterns
			conversionMap.clear();

			return super.visit(node, data);
		}

		@Override
		public Object visit(ASTBlankNode node, Object data) throws VisitorException {
			String bnodeID = node.getID();
			String varName = findVarName(bnodeID);

			if (varName == null) {
				varName = createAnonVarName();

				if (bnodeID != null) {
					conversionMap.put(bnodeID, varName);
				}
			}

			ASTVar varNode = new ASTVar(SyntaxTreeBuilderTreeConstants.JJTVAR);
			varNode.setName(varName);
			varNode.setAnonymous(true);

			node.jjtReplaceWith(varNode);

			return super.visit(node, data);
		}

		private String findVarName(String bnodeID) throws VisitorException {
			if (bnodeID == null) {
				return null;
			}
			String varName = conversionMap.get(bnodeID);
			if (varName == null && usedBNodeIDs.contains(bnodeID)) {
				throw new VisitorException("BNodeID already used in another scope: " + bnodeID);
			}
			return varName;
		}

		@Override
		public Object visit(ASTBlankNodePropertyList node, Object data) throws VisitorException {
			node.setVarName(createAnonVarName());
			return super.visit(node, data);
		}

		@Override
		public Object visit(ASTCollection node, Object data) throws VisitorException {
			node.setVarName(createAnonVarName());
			return super.visit(node, data);
		}
	}
}
