/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.query.parser.serql.ast.ASTProjectionElem;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTSelect;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTString;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTVar;
import org.eclipse.rdf4j.query.parser.serql.ast.Node;
import org.eclipse.rdf4j.query.parser.serql.ast.SyntaxTreeBuilderTreeConstants;
import org.eclipse.rdf4j.query.parser.serql.ast.VisitorException;

/**
 * Processes projection aliases, verifying that the specified aliases are unique and generating aliases for
 * the elements for which no alias has been specified but that do require one.
 * 
 * @author Arjohn Kampman
 */
class ProjectionAliasProcessor extends AbstractASTVisitor {

	@Override
	public Object visit(ASTSelect node, Object data)
		throws VisitorException
	{
		// Iterate over all projection elements to retrieve the defined aliases
		Set<String> aliases = new HashSet<>();
		List<Node> unaliasedNodes = new ArrayList<>();

		for (int i = 0; i < node.jjtGetNumChildren(); i++) {
			ASTProjectionElem projElem = (ASTProjectionElem)node.jjtGetChild(i);

			String alias = projElem.getAlias();
			if (alias == null && projElem.getValueExpr() instanceof ASTVar) {
				alias = ((ASTVar)projElem.getValueExpr()).getName();
			}

			if (alias != null) {
				boolean isUnique = aliases.add(alias);

				if (!isUnique) {
					throw new VisitorException("Duplicate projection element names: '" + alias + "'");
				}
			}
			else {
				unaliasedNodes.add(projElem);
			}
		}

		// Iterate over the unaliased nodes and generate aliases for them
		int aliasNo = 1;
		for (Node projElem : unaliasedNodes) {
			// Generate unique alias for projection element
			String alias;
			while (aliases.contains(alias = "_" + aliasNo++)) {
				// try again
			}

			aliases.add(alias);

			ASTString aliasNode = new ASTString(SyntaxTreeBuilderTreeConstants.JJTSTRING);
			aliasNode.setValue(alias);
			aliasNode.jjtSetParent(projElem);
			projElem.jjtAppendChild(aliasNode);
		}

		return data;
	}
}
