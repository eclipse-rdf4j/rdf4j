/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTLiteral;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTQueryContainer;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTString;
import org.eclipse.rdf4j.query.parser.serql.ast.VisitorException;

/**
 * Processes escape sequences in strings, replacing the escape sequence with their actual value. Escape sequences for
 * SPARQL are documented in section <a href="http://www.w3.org/TR/rdf-sparql-query/#grammarEscapes">A.7 Escape sequences
 * in strings</a>.
 * 
 * @author Arjohn Kampman
 */
class StringEscapesProcessor {

	/**
	 * Processes escape sequences in ASTString objects.
	 * 
	 * @param qc The query that needs to be processed.
	 * @throws MalformedQueryException If an invalid escape sequence was found.
	 */
	public static void process(ASTQueryContainer qc) throws MalformedQueryException {
		StringProcessor visitor = new StringProcessor();
		try {
			qc.jjtAccept(visitor, null);
		} catch (VisitorException e) {
			throw new MalformedQueryException(e.getMessage(), e);
		}
	}

	private static class StringProcessor extends AbstractASTVisitor {

		public StringProcessor() {
		}

		@Override
		public Object visit(ASTString stringNode, Object data) throws VisitorException {
			String value = stringNode.getValue();
			try {
				value = SeRQLUtil.decodeString(value);
				stringNode.setValue(value);
			} catch (IllegalArgumentException e) {
				// Invalid escape sequence
				throw new VisitorException(e.getMessage());
			}

			return super.visit(stringNode, data);
		}

		@Override
		public Object visit(ASTLiteral literalNode, Object data) throws VisitorException {
			String label = literalNode.getLabel();
			try {
				label = SeRQLUtil.decodeString(label);
				literalNode.setLabel(label);
			} catch (IllegalArgumentException e) {
				// Invalid escape sequence
				throw new VisitorException(e.getMessage());
			}

			return super.visit(literalNode, data);
		}
	}
}
