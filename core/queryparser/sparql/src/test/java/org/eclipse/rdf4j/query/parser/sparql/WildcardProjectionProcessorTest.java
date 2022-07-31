/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.rdf4j.query.parser.sparql.ast.ASTProjectionElem;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTQueryContainer;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTSelectQuery;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTVar;
import org.eclipse.rdf4j.query.parser.sparql.ast.SyntaxTreeBuilder;
import org.junit.Test;

public class WildcardProjectionProcessorTest {

	@Test
	public void testVarInFilter() throws Exception {
		String queryStr = "SELECT * {\n" + "    FILTER (!bound(?a))\n" + "}";
		ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(queryStr);
		WildcardProjectionProcessor.process(qc);

		List<ASTProjectionElem> projection = ((ASTSelectQuery) qc.getQuery()).getSelect().getProjectionElemList();

		assertThat(projection).isEmpty();
	}

	@Test
	public void testVarInBGP() throws Exception {
		String queryStr = "SELECT * {\n" + "    ?a <ex:p> <ex:o> . \n" + "    FILTER (!bound(?a))\n" + "}";
		ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(queryStr);
		WildcardProjectionProcessor.process(qc);

		List<ASTProjectionElem> projection = ((ASTSelectQuery) qc.getQuery()).getSelect().getProjectionElemList();

		assertThat(projection.size()).isEqualTo(1);
		assertThat(((ASTVar) projection.get(0).jjtGetChild(0)).getName()).isEqualTo("a");
	}

	@Test
	public void testVarInBind() throws Exception {
		String queryStr = "SELECT * {\n" + "    BIND (?a AS ?b)\n" + "}";
		ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(queryStr);
		WildcardProjectionProcessor.process(qc);

		List<ASTProjectionElem> projection = ((ASTSelectQuery) qc.getQuery()).getSelect().getProjectionElemList();

		assertThat(projection.size()).isEqualTo(1);
		assertThat(((ASTVar) projection.get(0).jjtGetChild(0)).getName()).isEqualTo("b");
	}

	@Test
	public void testVarInSubselect() throws Exception {
		String queryStr = "SELECT * {\n" + "    { SELECT ?a { ?a ?p ?o } }\n" + "}";
		ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(queryStr);
		WildcardProjectionProcessor.process(qc);

		List<ASTProjectionElem> projection = ((ASTSelectQuery) qc.getQuery()).getSelect().getProjectionElemList();

		assertThat(projection.size()).isEqualTo(1);
		assertThat(((ASTVar) projection.get(0).jjtGetChild(0)).getName()).isEqualTo("a");
	}

}
