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
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTModify;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOperationContainer;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTReifiedTriple;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTTripleTerm;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTVar;
import org.eclipse.rdf4j.query.parser.sparql.ast.SimpleNode;
import org.eclipse.rdf4j.query.parser.sparql.ast.SyntaxTreeBuilderTreeConstants;
import org.eclipse.rdf4j.query.parser.sparql.ast.VisitorException;

/**
 * Processes blank nodes in the query body, replacing them with variables while retaining scope.
 *
 * @author Arjohn Kampman
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

		// Separate map for reifier blank nodes — intentionally shared across INSERT and WHERE
		// scopes because a named reifier bnode (_:x) in INSERT refers to the SAME reifier
		// matched in WHERE, not a fresh blank node. This is different from regular bnodes
		// which are existential and scope-local.
		private final Map<String, String> reifierConversionMap = new HashMap<>();

		// When a bnode is identified as a reifier, mark its ID
		private final Set<String> reifierBNodeIDs = new HashSet<>();

		private final Set<String> usedBNodeIDs = new HashSet<>();

		private String createAnonVarName() {
			return "_anon_bnode_" + anonVarNo++;
		}

		private String createAnonUserVarName() {
			return "_anon_user_bnode_" + anonVarNo++;
		}

		private String createAnonCollectionVarName() {
			return "_anon_collection_" + anonVarNo++;
		}

		public Set<String> getUsedBNodeIDs() {
			usedBNodeIDs.addAll(conversionMap.keySet());
			return Collections.unmodifiableSet(usedBNodeIDs);
		}

		@Override
		public Object visit(ASTModify node, Object data) throws VisitorException {
			reifierConversionMap.clear();
			reifierBNodeIDs.clear();
			return super.visit(node, data);
		}

		@Override
		public Object visit(ASTBasicGraphPattern node, Object data) throws VisitorException {
			// The same Blank node ID cannot be used across Graph Patterns
			usedBNodeIDs.addAll(conversionMap.keySet());

			// Copy reifier bnodes before clearing — they span BGP boundaries intentionally
			conversionMap.entrySet()
					.stream()
					.filter(e -> reifierBNodeIDs.contains(e.getKey()))
					.forEach(e -> reifierConversionMap.putIfAbsent(e.getKey(), e.getValue()));

			// Blank nodes are scoped to Basic Graph Patterns
			conversionMap.clear();

			return super.visit(node, data);
		}

		@Override
		public Object visit(ASTBlankNode node, Object data) throws VisitorException {
			String bnodeID = node.getID();
			String varName = findVarName(bnodeID);

			if (varName == null) {
				if (bnodeID == null) {
					varName = createAnonVarName();

				} else {
					varName = createAnonUserVarName();

				}

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

		@Override
		public Object visit(ASTReifiedTriple node, Object data) throws VisitorException {
			processBlankNodeOrRecurse(node.getSubj(), data);
			// Visit predicate normally
			node.getPred().jjtAccept(this, data);
			processBlankNodeOrRecurse(node.getObj(), data);

			// Handle reifier blank node specially - bypass cross-scope check
			SimpleNode reifier = node.getReifier();
			if (reifier != null) {
				if (reifier instanceof ASTBlankNode) {
					ASTBlankNode bn = (ASTBlankNode) reifier;
					String bnodeID = bn.getID();
					String varName = conversionMap.get(bnodeID);
					if (varName == null) {
						// Check if this reifier bnode was seen in a previous BGP —
						// if so, reuse the same var name to maintain cross-BGP identity
						varName = reifierConversionMap.get(bnodeID);
						if (varName == null) {
							// First time seeing this reifier bnode — create a new var name
							varName = createAnonVarName();
						}
						if (bnodeID != null) {
							reifierBNodeIDs.add(bnodeID);
							conversionMap.put(bnodeID, varName);
						}
					}
					ASTVar varNode = new ASTVar(SyntaxTreeBuilderTreeConstants.JJTVAR);
					varNode.setName(varName);
					varNode.setAnonymous(true);
					// Mark nested reifier as blank node so it gets collected
					// by TripleRefBNodeVarCollector and bound to a BNodeGenerator in INSERT/WHERE
					varNode.setIsBNode(true);
					bn.jjtReplaceWith(varNode);
				} else {
					reifier.jjtAccept(this, data);
				}
			}

			return null;
		}

		/**
		 * Processes blank nodes within a {@link ASTTripleTerm} node by converting them to anonymous variables.
		 * <p>
		 * Per RDF 1.2 and SPARQL 1.2, blank nodes inside triple terms are only permitted in update operations (e.g.,
		 * INSERT/DELETE templates). In query contexts (e.g., WHERE clauses), blank nodes inside triple terms are not
		 * permitted and will be left unprocessed.
		 *
		 * @param node the {@link ASTTripleTerm} AST node whose subject and object positions are inspected for blank
		 *             nodes
		 * @param data visitor data passed through the traversal
		 * @return {@code null}
		 * @throws VisitorException if an error occurs during visitation of child nodes
		 */
		@Override
		public Object visit(ASTTripleTerm node, Object data) throws VisitorException {
			var subj = node.getSubj();
			// Visit predicate normally
			node.getPred().jjtAccept(this, data);
			var object = node.getObj();

			processBlankNodeOrRecurse(subj, data);
			processBlankNodeOrRecurse(object, data);

			return null;
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
			node.setVarName(createAnonCollectionVarName());
			return super.visit(node, data);
		}

		private void processBlankNodeOrRecurse(SimpleNode bn, Object data) throws VisitorException {
			if (bn instanceof ASTBlankNode) {
				var bNodeVar = convertBlankNodeToVar((ASTBlankNode) bn);
				// Mark var as blank node so it gets collected by TripleRefBNodeVarCollector
				// and bound to a BNodeGenerator in INSERT/WHERE
				bNodeVar.setIsBNode(true);
			} else {
				bn.jjtAccept(this, data);
			}
		}

		private ASTVar convertBlankNodeToVar(ASTBlankNode node) throws VisitorException {
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

			return varNode;
		}
	}
}
