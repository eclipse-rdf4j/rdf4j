/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import java.net.URISyntaxException;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTBaseDecl;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTDeleteData;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIRI;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTIRIFunc;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTInsertData;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTOperationContainer;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTServiceGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUnparsedQuadDataBlock;
import org.eclipse.rdf4j.query.parser.sparql.ast.VisitorException;

/**
 * Resolves relative URIs in a query model using either an external base URI or using the base URI specified in the
 * query model itself. The former takes precedence over the latter.
 *
 * @author Arjohn Kampman
 *
 * @apiNote This feature is for internal use only: its existence, signature or behavior may change without warning from
 *          one release to the next.
 */
@InternalUseOnly
public class BaseDeclProcessor {

	/**
	 * Resolves relative URIs in the supplied query model using either the specified <var>externalBaseURI</var> or, if
	 * this parameter is <var>null</var>, the base URI specified in the query model itself.
	 *
	 * @param qc              The query model to resolve relative URIs in.
	 * @param externalBaseURI The external base URI to use for resolving relative URIs, or <var>null</var> if the base
	 *                        URI that is specified in the query model should be used.
	 * @throws IllegalArgumentException If an external base URI is specified that is not an absolute URI.
	 * @throws MalformedQueryException  If the base URI specified in the query model is not an absolute URI.
	 */
	public static void process(ASTOperationContainer qc, String externalBaseURI) throws MalformedQueryException {
		ParsedIRI parsedBaseURI = null;

		// Use the query model's own base URI, if available
		ASTBaseDecl baseDecl = qc.getBaseDecl();
		if (baseDecl != null) {
			try {
				parsedBaseURI = new ParsedIRI(baseDecl.getIRI());
			} catch (URISyntaxException e) {
				throw new MalformedQueryException(e);
			}

			if (!parsedBaseURI.isAbsolute()) {
				throw new MalformedQueryException("BASE IRI is not an absolute IRI: " + externalBaseURI);
			}
		} else if (externalBaseURI != null) {
			// Use external base URI if the query doesn't contain one itself
			try {
				parsedBaseURI = new ParsedIRI(externalBaseURI);
			} catch (URISyntaxException e) {
				throw new MalformedQueryException(e);
			}

			if (!parsedBaseURI.isAbsolute()) {
				throw new IllegalArgumentException("Supplied base URI is not an absolute IRI: " + externalBaseURI);
			}
		} else {
			// FIXME: use the "Default Base URI"?
		}

		if (parsedBaseURI != null) {
			ASTUnparsedQuadDataBlock dataBlock = null;
			if (qc.getOperation() instanceof ASTInsertData) {
				ASTInsertData insertData = (ASTInsertData) qc.getOperation();
				dataBlock = insertData.jjtGetChild(ASTUnparsedQuadDataBlock.class);

			} else if (qc.getOperation() instanceof ASTDeleteData) {
				ASTDeleteData deleteData = (ASTDeleteData) qc.getOperation();
				dataBlock = deleteData.jjtGetChild(ASTUnparsedQuadDataBlock.class);
			}

			if (dataBlock != null) {
				final String baseURIDeclaration = "BASE <" + parsedBaseURI + "> \n";
				dataBlock.setDataBlock(baseURIDeclaration + dataBlock.getDataBlock());
			} else {
				RelativeIRIResolver visitor = new RelativeIRIResolver(parsedBaseURI);
				try {
					qc.jjtAccept(visitor, null);
				} catch (VisitorException e) {
					throw new MalformedQueryException(e);
				}
			}
		}
	}

	private static class RelativeIRIResolver extends AbstractASTVisitor {

		private final ParsedIRI parsedBaseURI;

		public RelativeIRIResolver(ParsedIRI parsedBaseURI) {
			this.parsedBaseURI = parsedBaseURI;
		}

		@Override
		public Object visit(ASTIRI node, Object data) throws VisitorException {
			node.setValue(parsedBaseURI.resolve(node.getValue()));

			return super.visit(node, data);
		}

		@Override
		public Object visit(ASTIRIFunc node, Object data) throws VisitorException {
			node.setBaseURI(parsedBaseURI.toString());
			return super.visit(node, data);
		}

		@Override
		public Object visit(ASTServiceGraphPattern node, Object data) throws VisitorException {
			node.setBaseURI(parsedBaseURI.toString());
			return super.visit(node, data);
		}
	}
}
