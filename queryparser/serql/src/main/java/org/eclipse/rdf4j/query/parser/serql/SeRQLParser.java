/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTGraphQuery;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTQuery;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTQueryContainer;
import org.eclipse.rdf4j.query.parser.serql.ast.ASTTupleQuery;
import org.eclipse.rdf4j.query.parser.serql.ast.ParseException;
import org.eclipse.rdf4j.query.parser.serql.ast.SyntaxTreeBuilder;
import org.eclipse.rdf4j.query.parser.serql.ast.TokenMgrError;
import org.eclipse.rdf4j.query.parser.serql.ast.VisitorException;

public class SeRQLParser implements QueryParser {

	@Override
	public ParsedQuery parseQuery(String queryStr, String baseURI) throws MalformedQueryException {
		try {
			ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(queryStr);

			// Replace deprecated NULL nodes with semantically equivalent
			// alternatives
			NullProcessor.process(qc);

			StringEscapesProcessor.process(qc);
			Map<String, String> namespaces = NamespaceDeclProcessor.process(qc);
			ProjectionProcessor.process(qc);
			qc.jjtAccept(new ProjectionAliasProcessor(), null);
			qc.jjtAccept(new AnonymousVarGenerator(), null);

			// TODO: check use of unbound variables?

			TupleExpr tupleExpr = QueryModelBuilder.buildQueryModel(qc, SimpleValueFactory.getInstance());

			ASTQuery queryNode = qc.getQuery();
			ParsedQuery query;
			if (queryNode instanceof ASTTupleQuery) {
				query = new ParsedTupleQuery(tupleExpr);
			} else if (queryNode instanceof ASTGraphQuery) {
				query = new ParsedGraphQuery(tupleExpr, namespaces);
			} else {
				throw new RuntimeException("Unexpected query type: " + queryNode.getClass());
			}

			return query;
		} catch (ParseException e) {
			throw new MalformedQueryException(e.getMessage(), e);
		} catch (TokenMgrError e) {
			throw new MalformedQueryException(e.getMessage(), e);
		} catch (VisitorException e) {
			throw new MalformedQueryException(e.getMessage(), e);
		}
	}

	public static void main(String[] args) throws java.io.IOException {
		System.out.println("Your SeRQL query:");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		StringBuilder buf = new StringBuilder();
		String line = null;
		while ((line = in.readLine()) != null) {
			if (line.length() > 0) {
				buf.append(' ').append(line).append('\n');
			} else {
				String queryStr = buf.toString().trim();
				if (queryStr.length() > 0) {
					try {
						SeRQLParser parser = new SeRQLParser();
						parser.parseQuery(queryStr, null);
					} catch (Exception e) {
						System.err.println(e.getMessage());
						e.printStackTrace();
					}
				}
				buf.setLength(0);
			}
		}
	}

	@Override
	public ParsedUpdate parseUpdate(String updateStr, String baseURI) throws MalformedQueryException {
		throw new UnsupportedOperationException("SeRQL does not support update operations");
	}
}
