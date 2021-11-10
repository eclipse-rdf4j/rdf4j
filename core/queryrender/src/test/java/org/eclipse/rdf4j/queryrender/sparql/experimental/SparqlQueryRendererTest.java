/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.experimental;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.Modify;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Sets;

/**
 * Tests for the {@link SparqlQueryRenderer}.
 * 
 * @author Andriy Nikolov
 * @author Jeen Broekstra
 * @author Andreas Schwarte
 */
public class SparqlQueryRendererTest {

	private Pattern patternAnonymousVar = Pattern.compile("(_anon_[a-z0-9_]+)");
	private Pattern patternDescribeId = Pattern.compile("(_describe_[a-z0-9_]+)");

	private String loadQuery(String queryId) throws Exception {
		return loadClasspathResourceAsUtf8String("/sparql-render/" + queryId + ".sq");
	}

	private String loadSparqlJsQuery(String queryId) throws Exception {
		return loadClasspathResourceAsUtf8String("/sparql-render/sparqljs/" + queryId + ".sparql");
	}

	private String loadClasspathResourceAsUtf8String(String resourceFile) throws Exception {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(this.getClass().getResourceAsStream(resourceFile), "UTF-8"));
		StringBuilder textBuilder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			textBuilder.append(line + "\n");
		}
		return textBuilder.toString().trim();
	}

	@Test
	public void testSparqlJsQuery1() throws Exception {
		String query = loadSparqlJsQuery("all");
		testSingleQuery("all", query);
	}

	@Test
	public void testSparqlJsQuery2() throws Exception {
		String query = loadSparqlJsQuery("artists-ghent");
		testSingleQuery("artists-ghent", query);
	}

	@Test
	public void testSparqlJsQuery3() throws Exception {
		String query = loadSparqlJsQuery("artists-york");
		testSingleQuery("artists-york", query);
	}

	@Test
	public void testSparqlJsQuery4() throws Exception {
		String query = loadSparqlJsQuery("blanks");
		testSingleQuery("blanks", query);
	}

	@Test
	public void testSparqlJsQuery5() throws Exception {
		String query = loadSparqlJsQuery("bsbm1");
		testSingleQuery("bsbm1", query);
	}

	@Test
	public void testSparqlJsQuery6() throws Exception {
		String query = loadSparqlJsQuery("bsbm10");
		testSingleQuery("bsbm10", query);
	}

	@Test
	public void testSparqlJsQuery7() throws Exception {
		String query = loadSparqlJsQuery("bsbm3");
		testSingleQuery("bsbm3", query);
	}

	@Test
	public void testSparqlJsQuery8() throws Exception {
		String query = loadSparqlJsQuery("bsbm4");
		testSingleQuery("bsbm4", query);
	}

	@Test
	public void testSparqlJsQuery9() throws Exception {
		String query = loadSparqlJsQuery("bsbm5");
		testSingleQuery("bsbm5", query);
	}

	@Test
	public void testSparqlJsQuery10() throws Exception {
		String query = loadSparqlJsQuery("bsbm6");
		testSingleQuery("bsbm6", query);
	}

	@Test
	@Ignore("The order of operands changed by the parser (preserving the semantics). "
			+ "The rendered query adequately matches the parsed operation.")
	public void testSparqlJsQuery11() throws Exception {
		String query = loadSparqlJsQuery("bsbm8");
		testSingleQuery("bsbm8", query);
	}

	@Test
	public void testSparqlJsQuery12() throws Exception {
		String query = loadSparqlJsQuery("bsbm9");
		testSingleQuery("bsbm9", query);
	}

	@Test
	public void testSparqlJsQuery13() throws Exception {
		String query = loadSparqlJsQuery("construct-without-template");
		testSingleQuery("construct-without-template", query);
	}

	@Test
	public void testSparqlJsQuery14() throws Exception {
		String query = loadSparqlJsQuery("construct");
		testSingleQuery("construct", query);
	}

	@Test
	public void testSparqlJsQuery15() throws Exception {
		String query = loadSparqlJsQuery("fedbench-cd1");
		testSingleQuery("fedbench-cd1", query);
	}

	@Test
	public void testSparqlJsQuery16() throws Exception {
		String query = loadSparqlJsQuery("fedbench-cd2");
		testSingleQuery("fedbench-cd2", query);
	}

	@Test
	public void testSparqlJsQuery17() throws Exception {
		String query = loadSparqlJsQuery("fedbench-cd3");
		testSingleQuery("fedbench-cd3", query);
	}

	@Test
	public void testSparqlJsQuery18() throws Exception {
		String query = loadSparqlJsQuery("group-concat");
		testSingleQuery("group-concat", query);
	}

	@Test
	public void testSparqlJsQuery19() throws Exception {
		String query = loadSparqlJsQuery("group-variable");
		testSingleQuery("group-variable", query);
	}

	@Test
	public void testSparqlJsQuery20() throws Exception {
		String query = loadSparqlJsQuery("in");
		testSingleQuery("in", query);
	}

	@Test
	public void testSparqlJsQuery21() throws Exception {
		String query = loadSparqlJsQuery("lists");
		testSingleQuery("lists", query);
	}

	@Test
	public void testSparqlJsQuery22() throws Exception {
		String query = loadSparqlJsQuery("load-into");
		testSingleQuery("load-into", query);
	}

	@Test
	public void testSparqlJsQuery23() throws Exception {
		String query = loadSparqlJsQuery("load");
		testSingleQuery("load", query);
	}

	@Test
	public void testSparqlJsQuery24() throws Exception {
		String query = loadSparqlJsQuery("multiline");
		testSingleQuery("multiline", query);
	}

	@Test
	public void testSparqlJsQuery25() throws Exception {
		String query = loadSparqlJsQuery("nested-path");
		testSingleQuery("nested-path", query);
	}

	@Test
	public void testSparqlJsQuery26() throws Exception {
		String query = loadSparqlJsQuery("optional-subquery");
		testSingleQuery("optional-subquery", query);
	}

	@Test
	public void testSparqlJsQuery27() throws Exception {
		String query = loadSparqlJsQuery("optional");
		testSingleQuery("optional", query);
	}

	@Test
	public void testSparqlJsQuery28() throws Exception {
		String query = loadSparqlJsQuery("or");
		testSingleQuery("or", query);
	}

	@Test
	public void testSparqlJsQuery29() throws Exception {
		String query = loadSparqlJsQuery("order-operator");
		testSingleQuery("order-operator", query);
	}

	@Test
	public void testSparqlJsQuery30() throws Exception {
		String query = loadSparqlJsQuery("sparql-10-1a");
		testSingleQuery("sparql-10-1a", query);
	}

	@Test
	public void testSparqlJsQuery31() throws Exception {
		String query = loadSparqlJsQuery("sparql-10-1b");
		testSingleQuery("sparql-10-1b", query);
	}

	@Test
	public void testSparqlJsQuery32() throws Exception {
		String query = loadSparqlJsQuery("sparql-10-2-2a");
		testSingleQuery("sparql-10-2-2a", query);
	}

	@Test
	public void testSparqlJsQuery33() throws Exception {
		String query = loadSparqlJsQuery("sparql-10-2-2b");
		testSingleQuery("sparql-10-2-2b", query);
	}

	@Test
	public void testSparqlJsQuery34() throws Exception {
		String query = loadSparqlJsQuery("sparql-10-2-2c");
		testSingleQuery("sparql-10-2-2c", query);
	}

	@Test
	public void testSparqlJsQuery35() throws Exception {
		String query = loadSparqlJsQuery("sparql-11-1");
		testSingleQuery("sparql-11-1", query);
	}

	@Test
	public void testSparqlJsQuery36() throws Exception {
		String query = loadSparqlJsQuery("sparql-11-2");
		testSingleQuery("sparql-11-2", query);
	}

	@Test
	public void testSparqlJsQuery37() throws Exception {
		String query = loadSparqlJsQuery("sparql-11-3");
		testSingleQuery("sparql-11-3", query);
	}

	@Test
	public void testSparqlJsQuery38() throws Exception {
		String query = loadSparqlJsQuery("sparql-11-4");
		testSingleQuery("sparql-11-4", query);
	}

	@Test
	public void testSparqlJsQuery39() throws Exception {
		String query = loadSparqlJsQuery("sparql-11-5");
		testSingleQuery("sparql-11-5", query);
	}

	/**
	 * Multiple prefix declarations: fails with RDF4J parser
	 * 
	 * @throws Exception
	 */
	// @Test
	public void testSparqlJsQuery40() throws Exception {
		String query = loadSparqlJsQuery("sparql-12");
		testSingleQuery("sparql-12", query);
	}

	@Test
	public void testSparqlJsQuery41() throws Exception {
		String query = loadSparqlJsQuery("sparql-13-2-1");
		testSingleQuery("sparql-13-2-1", query);
	}

	@Test
	public void testSparqlJsQuery42() throws Exception {
		String query = loadSparqlJsQuery("sparql-13-2-2");
		testSingleQuery("sparql-13-2-2", query);
	}

	@Test
	public void testSparqlJsQuery43() throws Exception {
		String query = loadSparqlJsQuery("sparql-13-2-3");
		testSingleQuery("sparql-13-2-3", query);
	}

	@Test
	public void testSparqlJsQuery44() throws Exception {
		String query = loadSparqlJsQuery("sparql-13-3-1");
		testSingleQuery("sparql-13-3-1", query);
	}

	@Test
	public void testSparqlJsQuery45() throws Exception {
		String query = loadSparqlJsQuery("sparql-13-3-2");
		testSingleQuery("sparql-13-3-2", query);
	}

	@Test
	public void testSparqlJsQuery46() throws Exception {
		String query = loadSparqlJsQuery("sparql-13-3-3");
		testSingleQuery("sparql-13-3-3", query);
	}

	@Test
	public void testSparqlJsQuery47() throws Exception {
		String query = loadSparqlJsQuery("sparql-13-3-4");
		testSingleQuery("sparql-13-3-4", query);
	}

	@Test
	public void testSparqlJsQuery48() throws Exception {
		String query = loadSparqlJsQuery("sparql-15-1a");
		testSingleQuery("sparql-15-1a", query);
	}

	@Test
	public void testSparqlJsQuery49() throws Exception {
		String query = loadSparqlJsQuery("sparql-15-1b");
		testSingleQuery("sparql-15-1b", query);
	}

	@Test
	public void testSparqlJsQuery50() throws Exception {
		String query = loadSparqlJsQuery("sparql-15-1c");
		testSingleQuery("sparql-15-1c", query);
	}

	@Test
	public void testSparqlJsQuery51() throws Exception {
		String query = loadSparqlJsQuery("sparql-15-2");
		testSingleQuery("sparql-15-2", query);
	}

	@Test
	public void testSparqlJsQuery52() throws Exception {
		String query = loadSparqlJsQuery("sparql-15-3-1");
		testSingleQuery("sparql-15-3-1", query);
	}

	@Test
	public void testSparqlJsQuery53() throws Exception {
		String query = loadSparqlJsQuery("sparql-15-3-2");
		testSingleQuery("sparql-15-3-2", query);
	}

	@Test
	public void testSparqlJsQuery54() throws Exception {
		String query = loadSparqlJsQuery("sparql-15-3");
		testSingleQuery("sparql-15-3", query);
	}

	@Test
	public void testSparqlJsQuery55() throws Exception {
		String query = loadSparqlJsQuery("sparql-15-4");
		testSingleQuery("sparql-15-4", query);
	}

	@Test
	public void testSparqlJsQuery56() throws Exception {
		String query = loadSparqlJsQuery("sparql-15-5");
		testSingleQuery("sparql-15-5", query);
	}

	@Test
	public void testSparqlJsQuery57() throws Exception {
		String query = loadSparqlJsQuery("sparql-16-1-1");
		testSingleQuery("sparql-16-1-1", query);
	}

	@Test
	public void testSparqlJsQuery58() throws Exception {
		String query = loadSparqlJsQuery("sparql-16-1-2a");
		testSingleQuery("sparql-16-1-2a", query);
	}

	@Test
	public void testSparqlJsQuery59() throws Exception {
		String query = loadSparqlJsQuery("sparql-16-1-2b");
		testSingleQuery("sparql-16-1-2b", query);
	}

	@Test
	public void testSparqlJsQuery60() throws Exception {
		String query = loadSparqlJsQuery("sparql-16-2-1");
		testSingleQuery("sparql-16-2-1", query);
	}

	@Test
	public void testSparqlJsQuery61() throws Exception {
		String query = loadSparqlJsQuery("sparql-16-2-2");
		testSingleQuery("sparql-16-2-2", query);
	}

	@Test
	public void testSparqlJsQuery62() throws Exception {
		String query = loadSparqlJsQuery("sparql-16-2-3");
		testSingleQuery("sparql-16-2-3", query);
	}

	@Test
	public void testSparqlJsQuery63() throws Exception {
		String query = loadSparqlJsQuery("sparql-16-2-4");
		testSingleQuery("sparql-16-2-4", query);
	}

	@Test
	public void testSparqlJsQuery64() throws Exception {
		String query = loadSparqlJsQuery("sparql-16-2");
		testSingleQuery("sparql-16-2", query);
	}

	@Test
	public void testSparqlJsQuery65() throws Exception {
		String query = loadSparqlJsQuery("sparql-16-3");
		testSingleQuery("sparql-16-3", query);
	}

	@Test
	public void testSparqlJsQuery66() throws Exception {
		String query = loadSparqlJsQuery("sparql-16-4-1");
		testSingleQuery("sparql-16-4-1", query);
	}

	@Test
	public void testSparqlJsQuery67() throws Exception {
		String query = loadSparqlJsQuery("sparql-16-4-2a");
		testSingleQuery("sparql-16-4-2a", query);
	}

	@Test
	public void testSparqlJsQuery68() throws Exception {
		String query = loadSparqlJsQuery("sparql-16-4-2b");
		testSingleQuery("sparql-16-4-2b", query);
	}

	@Test
	public void testSparqlJsQuery69() throws Exception {
		String query = loadSparqlJsQuery("sparql-16-4-2c");
		testSingleQuery("sparql-16-4-2c", query);
	}

	@Test
	public void testSparqlJsQuery70() throws Exception {
		String query = loadSparqlJsQuery("sparql-16-4-3");
		testSingleQuery("sparql-16-4-3", query);
	}

	@Test
	public void testSparqlJsQuery71() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-1-1a");
		testSingleQuery("sparql-17-4-1-1a", query);
	}

	@Test
	public void testSparqlJsQuery72() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-1-1b");
		testSingleQuery("sparql-17-4-1-1b", query);
	}

	@Test
	public void testSparqlJsQuery73() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-1-7a");
		testSingleQuery("sparql-17-4-1-7a", query);
	}

	@Test
	public void testSparqlJsQuery74() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-1-7b");
		testSingleQuery("sparql-17-4-1-7b", query);
	}

	@Test
	public void testSparqlJsQuery75() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-1-8a");
		testSingleQuery("sparql-17-4-1-8a", query);
	}

	@Test
	public void testSparqlJsQuery76() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-1-8b");
		testSingleQuery("sparql-17-4-1-8b", query);
	}

	@Test
	public void testSparqlJsQuery77() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-2-1");
		testSingleQuery("sparql-17-4-2-1", query);
	}

	@Test
	public void testSparqlJsQuery78() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-2-2");
		testSingleQuery("sparql-17-4-2-2", query);
	}

	@Test
	public void testSparqlJsQuery79() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-2-3");
		testSingleQuery("sparql-17-4-2-3", query);
	}

	@Test
	public void testSparqlJsQuery80() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-2-5");
		testSingleQuery("sparql-17-4-2-5", query);
	}

	@Test
	public void testSparqlJsQuery81() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-2-6");
		testSingleQuery("sparql-17-4-2-6", query);
	}

	@Test
	public void testSparqlJsQuery82() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-2-7");
		testSingleQuery("sparql-17-4-2-7", query);
	}

	@Test
	public void testSparqlJsQuery83() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-3-13a");
		testSingleQuery("sparql-17-4-3-13a", query);
	}

	@Test
	public void testSparqlJsQuery84() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-3-13b");
		testSingleQuery("sparql-17-4-3-13b", query);
	}

	@Test
	public void testSparqlJsQuery85() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-4-3-14");
		testSingleQuery("sparql-17-4-3-14", query);
	}

	@Test
	public void testSparqlJsQuery86() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-6a");
		testSingleQuery("sparql-17-6a", query);
	}

	@Test
	public void testSparqlJsQuery87() throws Exception {
		String query = loadSparqlJsQuery("sparql-17-6b");
		testSingleQuery("sparql-17-6b", query);
	}

	@Test
	public void testSparqlJsQuery88() throws Exception {
		String query = loadSparqlJsQuery("sparql-17");
		testSingleQuery("sparql-17", query);
	}

	@Test
	public void testSparqlJsQuery89() throws Exception {
		String query = loadSparqlJsQuery("sparql-4-2a");
		testSingleQuery("sparql-4-2a", query);
	}

	@Test
	public void testSparqlJsQuery90() throws Exception {
		String query = loadSparqlJsQuery("sparql-4-2b");
		testSingleQuery("sparql-4-2b", query);
	}

	@Test
	public void testSparqlJsQuery91() throws Exception {
		String query = loadSparqlJsQuery("sparql-4-2c");
		testSingleQuery("sparql-4-2c", query);
	}

	@Test
	public void testSparqlJsQuery92() throws Exception {
		String query = loadSparqlJsQuery("sparql-5-2-1");
		testSingleQuery("sparql-5-2-1", query);
	}

	@Test
	public void testSparqlJsQuery93() throws Exception {
		String query = loadSparqlJsQuery("sparql-5-2-2a");
		testSingleQuery("sparql-5-2-2a", query);
	}

	@Test
	public void testSparqlJsQuery94() throws Exception {
		String query = loadSparqlJsQuery("sparql-5-2-2b");
		testSingleQuery("sparql-5-2-2b", query);
	}

	@Test
	public void testSparqlJsQuery95() throws Exception {
		String query = loadSparqlJsQuery("sparql-5-2-2c");
		testSingleQuery("sparql-5-2-2c", query);
	}

	@Test
	public void testSparqlJsQuery96() throws Exception {
		String query = loadSparqlJsQuery("sparql-5-2-3a");
		testSingleQuery("sparql-5-2-3a", query);
	}

	@Test
	public void testSparqlJsQuery97() throws Exception {
		String query = loadSparqlJsQuery("sparql-5-2-3b");
		testSingleQuery("sparql-5-2-3b", query);
	}

	@Test
	public void testSparqlJsQuery98() throws Exception {
		String query = loadSparqlJsQuery("sparql-5-2-3c");
		testSingleQuery("sparql-5-2-3c", query);
	}

	@Test
	public void testSparqlJsQuery99() throws Exception {
		String query = loadSparqlJsQuery("sparql-5-2a");
		testSingleQuery("sparql-5-2a", query);
	}

	@Test
	public void testSparqlJsQuery100() throws Exception {
		String query = loadSparqlJsQuery("sparql-5-2b");
		testSingleQuery("sparql-5-2b", query);
	}

	@Test
	public void testSparqlJsQuery101() throws Exception {
		String query = loadSparqlJsQuery("sparql-6-1");
		testSingleQuery("sparql-6-1", query);
	}

	@Test
	public void testSparqlJsQuery102() throws Exception {
		String query = loadSparqlJsQuery("sparql-6-2");
		testSingleQuery("sparql-6-2", query);
	}

	@Test
	public void testSparqlJsQuery103() throws Exception {
		String query = loadSparqlJsQuery("sparql-6-3");
		testSingleQuery("sparql-6-3", query);
	}

	@Test
	public void testSparqlJsQuery104() throws Exception {
		String query = loadSparqlJsQuery("sparql-7a");
		testSingleQuery("sparql-7a", query);
	}

	@Test
	public void testSparqlJsQuery105() throws Exception {
		String query = loadSparqlJsQuery("sparql-7b");
		testSingleQuery("sparql-7b", query);
	}

	@Test
	public void testSparqlJsQuery106() throws Exception {
		String query = loadSparqlJsQuery("sparql-7c");
		testSingleQuery("sparql-7c", query);
	}

	@Test
	public void testSparqlJsQuery107() throws Exception {
		String query = loadSparqlJsQuery("sparql-8-1-1");
		testSingleQuery("sparql-8-1-1", query);
	}

	@Test
	public void testSparqlJsQuery108() throws Exception {
		String query = loadSparqlJsQuery("sparql-8-1-2");
		testSingleQuery("sparql-8-1-2", query);
	}

	@Test
	public void testSparqlJsQuery109() throws Exception {
		String query = loadSparqlJsQuery("sparql-8-2");
		testSingleQuery("sparql-8-2", query);
	}

	@Test
	public void testSparqlJsQuery110() throws Exception {
		String query = loadSparqlJsQuery("sparql-8-3-1a");
		testSingleQuery("sparql-8-3-1a", query);
	}

	@Test
	public void testSparqlJsQuery111() throws Exception {
		String query = loadSparqlJsQuery("sparql-8-3-1b");
		testSingleQuery("sparql-8-3-1b", query);
	}

	@Test
	public void testSparqlJsQuery112() throws Exception {
		String query = loadSparqlJsQuery("sparql-8-3-2a");
		testSingleQuery("sparql-8-3-2a", query);
	}

	@Test
	public void testSparqlJsQuery113() throws Exception {
		String query = loadSparqlJsQuery("sparql-8-3-2b");
		testSingleQuery("sparql-8-3-2b", query);
	}

	@Test
	public void testSparqlJsQuery114() throws Exception {
		String query = loadSparqlJsQuery("sparql-8-3-3a");
		testSingleQuery("sparql-8-3-3a", query);
	}

	@Test
	public void testSparqlJsQuery115() throws Exception {
		String query = loadSparqlJsQuery("sparql-8-3-3b");
		testSingleQuery("sparql-8-3-3b", query);
	}

	@Test
	public void testSparqlJsQuery116() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2a");
		testSingleQuery("sparql-9-2a", query);
	}

	@Test
	public void testSparqlJsQuery117() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2b");
		testSingleQuery("sparql-9-2b", query);
	}

	@Test
	public void testSparqlJsQuery118() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2c");
		testSingleQuery("sparql-9-2c", query);
	}

	@Test
	public void testSparqlJsQuery119() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2d");
		testSingleQuery("sparql-9-2d", query);
	}

	@Test
	public void testSparqlJsQuery120() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2e");
		testSingleQuery("sparql-9-2e", query);
	}

	@Test
	public void testSparqlJsQuery121() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2f");
		testSingleQuery("sparql-9-2f", query);
	}

	@Test
	public void testSparqlJsQuery122() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2g");
		testSingleQuery("sparql-9-2g", query);
	}

	@Test
	public void testSparqlJsQuery123() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2h");
		testSingleQuery("sparql-9-2h", query);
	}

	@Test
	public void testSparqlJsQuery124() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2i");
		testSingleQuery("sparql-9-2i", query);
	}

	@Test
	public void testSparqlJsQuery125() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2j");
		testSingleQuery("sparql-9-2j", query);
	}

	@Test
	public void testSparqlJsQuery126() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2k");
		testSingleQuery("sparql-9-2k", query);
	}

	@Test
	public void testSparqlJsQuery127() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2l");
		testSingleQuery("sparql-9-2l", query);
	}

	@Test
	public void testSparqlJsQuery128() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2m");
		testSingleQuery("sparql-9-2m", query);
	}

	@Test
	public void testSparqlJsQuery129() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2n");
		testSingleQuery("sparql-9-2n", query);
	}

	@Test
	public void testSparqlJsQuery130() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2o");
		testSingleQuery("sparql-9-2o", query);
	}

	@Test
	public void testSparqlJsQuery131() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2p");
		testSingleQuery("sparql-9-2p", query);
	}

	@Test
	@Ignore("The rendered query correctly reflects the original one. "
			+ "The tuple expression, however, is impossible to render properly: "
			+ "it contains anonymous vars as operands in comparisons and filters")
	public void testSparqlJsQuery132() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2q");
		testSingleQuery("sparql-9-2q", query);
	}

	@Test
	public void testSparqlJsQuery133() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-2r");
		testSingleQuery("sparql-9-2r", query);
	}

	@Test
	public void testSparqlJsQuery134() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-3a");
		testSingleQuery("sparql-9-3a", query);
	}

	@Test
	public void testSparqlJsQuery135() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-3b");
		testSingleQuery("sparql-9-3b", query);
	}

	@Test
	public void testSparqlJsQuery136() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-3c");
		testSingleQuery("sparql-9-3c", query);
	}

	@Test
	public void testSparqlJsQuery137() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-4a");
		testSingleQuery("sparql-9-4a", query);
	}

	@Test
	public void testSparqlJsQuery138() throws Exception {
		String query = loadSparqlJsQuery("sparql-9-4b");
		testSingleQuery("sparql-9-4b", query);
	}

	@Test
	public void testSparqlJsQuery139() throws Exception {
		String query = loadSparqlJsQuery("sparql-fed-2-1");
		testSingleQuery("sparql-fed-2-1", query);
	}

	@Test
	public void testSparqlJsQuery140() throws Exception {
		String query = loadSparqlJsQuery("sparql-fed-2-2");
		testSingleQuery("sparql-fed-2-2", query);
	}

	@Test
	public void testSparqlJsQuery141() throws Exception {
		String query = loadSparqlJsQuery("sparql-fed-2-3");
		testSingleQuery("sparql-fed-2-3", query);
	}

	@Test
	public void testSparqlJsQuery142() throws Exception {
		String query = loadSparqlJsQuery("sparql-fed-2-4a");
		testSingleQuery("sparql-fed-2-4a", query);
	}

	@Test
	public void testSparqlJsQuery143() throws Exception {
		String query = loadSparqlJsQuery("sparql-fed-2-4b");
		testSingleQuery("sparql-fed-2-4b", query);
	}

	@Test
	public void testSparqlJsQuery144() throws Exception {
		String query = loadSparqlJsQuery("sparql-fed-2-4c");
		testSingleQuery("sparql-fed-2-4c", query);
	}

	@Test
	public void testSparqlJsQuery145() throws Exception {
		String query = loadSparqlJsQuery("sparql-fed-2-4d");
		testSingleQuery("sparql-fed-2-4d", query);
	}

	@Test
	public void testSparqlJsQuery146() throws Exception {
		String query = loadSparqlJsQuery("sparql-fed-4");
		testSingleQuery("sparql-fed-4", query);
	}

	@Test
	public void testSparqlJsQuery147() throws Exception {
		String query = loadSparqlJsQuery("sparql-modifiers-order");
		testSingleQuery("sparql-modifiers-order", query);
	}

	@Test
	public void testSparqlJsQuery148() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-1-1-1");
		testSingleQuery("sparql-update-1-1-1", query);
	}

	@Test
	public void testSparqlJsQuery149() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-1-1a");
		testSingleQuery("sparql-update-3-1-1a", query);
	}

	@Test
	public void testSparqlJsQuery150() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-1-1b");
		testSingleQuery("sparql-update-3-1-1b", query);
	}

	@Test
	public void testSparqlJsQuery151() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-1-2a");
		testSingleQuery("sparql-update-3-1-2a", query);
	}

	@Test
	public void testSparqlJsQuery152() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-1-2b");
		testSingleQuery("sparql-update-3-1-2b", query);
	}

	@Test
	public void testSparqlJsQuery153() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-1-3-1a");
		testSingleQuery("sparql-update-3-1-3-1a", query);
	}

	@Test
	public void testSparqlJsQuery154() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-1-3-1b");
		testSingleQuery("sparql-update-3-1-3-1b", query);
	}

	@Test
	public void testSparqlJsQuery155() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-1-3-2a");
		testSingleQuery("sparql-update-3-1-3-2a", query);
	}

	@Test
	public void testSparqlJsQuery156() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-1-3-2b");
		testSingleQuery("sparql-update-3-1-3-2b", query);
	}

	@Test
	public void testSparqlJsQuery157() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-1-3-2c");
		testSingleQuery("sparql-update-3-1-3-2c", query);
	}

	@Test
	public void testSparqlJsQuery158() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-1-3-3a");
		testSingleQuery("sparql-update-3-1-3-3a", query);
	}

	@Test
	public void testSparqlJsQuery159() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-1-3-3b");
		testSingleQuery("sparql-update-3-1-3-3b", query);
	}

	@Test
	public void testSparqlJsQuery160() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-1-3");
		testSingleQuery("sparql-update-3-1-3", query);
	}

	@Test
	public void testSparqlJsQuery161() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-2-3");
		testSingleQuery("sparql-update-3-2-3", query);
	}

	@Test
	public void testSparqlJsQuery162() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-2-4");
		testSingleQuery("sparql-update-3-2-4", query);
	}

	@Test
	public void testSparqlJsQuery163() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-3-2-5");
		testSingleQuery("sparql-update-3-2-5", query);
	}

	@Test
	public void testSparqlJsQuery164() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-4-2-4");
		testSingleQuery("sparql-update-4-2-4", query);
	}

	@Test
	public void testSparqlJsQuery165() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-multiple-prefixes");
		testSingleQuery("sparql-update-multiple-prefixes", query);
	}

	@Test
	public void testSparqlJsQuery166() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-only-prefix");
		testSingleQuery("sparql-update-only-prefix", query);
	}

	@Test
	public void testSparqlJsQuery167() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-trailing-prologue");
		testSingleQuery("sparql-update-trailing-prologue", query);
	}

	@Test
	public void testSparqlJsQuery168() throws Exception {
		String query = loadSparqlJsQuery("sparql-update-trailing-semicolon");
		testSingleQuery("sparql-update-trailing-semicolon", query);
	}

	@Test
	public void testSparqlJsQuery169() throws Exception {
		String query = loadSparqlJsQuery("sparql-values-clause");
		testSingleQuery("sparql-values-clause", query);
	}

	@Test
	public void testSparqlJsQuery170() throws Exception {
		String query = loadSparqlJsQuery("strlen");
		testSingleQuery("strlen", query);
	}

	@Test
	public void testSparqlJsQuery171() throws Exception {
		String query = loadSparqlJsQuery("sub-values");
		testSingleQuery("sub-values", query);
	}

	@Test
	public void testSparqlJsQuery172() throws Exception {
		String query = loadSparqlJsQuery("sum-count");
		testSingleQuery("sum-count", query);
	}

	@Test
	public void testSparqlJsQuery173() throws Exception {
		String query = loadSparqlJsQuery("union-complex");
		testSingleQuery("union-complex", query);
	}

	@Test
	public void testSparqlJsQuery174() throws Exception {
		String query = loadSparqlJsQuery("union");
		testSingleQuery("union", query);
	}

	@Test
	public void testQueryWithValues() throws Exception {
		String query = loadQuery("queryWithValuesClause");
		testSingleQuery("queryWithValuesClause", query);
	}

	@Test
	public void testQueryWithValuesExternal() throws Exception {
		String query = loadQuery("queryWithValuesClauseExternal");
		testSingleQuery("queryWithValuesClauseExternal", query);
	}

	@Test
	public void testAskQueryWithValues() throws Exception {
		String query = loadQuery("askQueryWithValuesClause");
		testSingleQuery("askQueryWithValuesClause", query);
	}

	@Test
	public void testConstructQueryWithValues() throws Exception {
		String query = loadQuery("constructQueryWithValuesClause");
		testSingleQuery("constructQueryWithValuesClause", query);
	}

	@Test
	public void testSimpleRegex() throws Exception {
		String query = loadQuery("simpleRegexQuery");
		testSingleQuery("simpleRegexQuery", query);
	}

	@Test
	public void testQueryWithLiterals() throws Exception {
		String query = loadQuery("queryWithLiterals");
		testSingleQuery("queryWithLiterals", query);
	}

	@Test
	public void testWikiUniprotExample() throws Exception {
		String query = loadQuery("wikiUniprotExample");
		testSingleQuery("wikiUniprotExample", query);
	}

	@Test
	public void testQueryWithGroupByOrderLimitAndAggFilter() throws Exception {
		String query = loadQuery("queryWithGroupByOrderLimitAndAggFilter");
		testSingleQuery("queryWithGroupByOrderLimitAndAggFilter", query);
	}

	@Test
	public void testQueryWithASubqueryAndGroupBy() throws Exception {
		String query = loadQuery("queryWithASubqueryAndGroupBy");
		testSingleQuery("queryWithASubqueryAndGroupBy", query);
	}

	@Test
	public void testQueryWithGroupByAndBind() throws Exception {
		String query = loadQuery("queryWithGroupByAndBind");
		testSingleQuery("queryWithGroupByAndBind", query);
	}

	@Test
	public void testAskQueryWithASubqueryAndGroupBy() throws Exception {
		String query = loadQuery("askQueryWithASubqueryAndGroupBy");
		testSingleQuery("askQueryWithASubqueryAndGroupBy", query);
	}

	@Test
	public void testConstructQueryWithASubqueryAndGroupBy() throws Exception {
		String query = loadQuery("constructQueryWithASubqueryAndGroupBy");
		testSingleQuery("constructQueryWithASubqueryAndGroupBy", query);
	}

	@Test
	public void testQueryWithPropertyPathStar() throws Exception {
		String query = loadQuery("queryWithPropertyPathStar");
		testSingleQuery("queryWithPropertyPathStar", query);
	}

	@Test
	public void testQueryWithNonIriFunctions() throws Exception {
		String query = loadQuery("queryWithNonIriFunctions");
		testSingleQuery("queryWithNonIriFunctions", query);
	}

	@Test
	public void testQueryWithSameTerm() throws Exception {
		String query = loadQuery("queryWithSameTerm");
		testSingleQuery("queryWithSameTerm", query);
	}

	@Test
	public void testQueryWithLangMatches() throws Exception {
		String query = loadQuery("queryWithLangMatches");
		testSingleQuery("queryWithLangMatches", query);
	}

	@Test
	public void testQueryWithProjectionBind() throws Exception {
		String query = loadQuery("queryWithProjectionBind");
		testSingleQuery("queryWithProjectionBind", query);
	}

	@Test
	public void testQueryEmptySetBind() throws Exception {
		String query = loadQuery("queryEmptySetBind");
		testSingleQuery("queryEmptySetBind", query);
	}

	@Test
	public void testDeleteQueryWithASubqueryAndGroupBy() throws Exception {
		String query = loadQuery("deleteQueryWithASubqueryAndGroupBy");
		testSingleQuery("deleteQueryWithASubqueryAndGroupBy", query);
	}

	private void testSingleQuery(String id, String strQuery) {
		SparqlQueryRenderer mpQueryRenderer = new SparqlQueryRenderer();
		ParsedOperation query = (ParsedOperation) QueryParserUtil.parseOperation(
				QueryLanguage.SPARQL,
				strQuery, null);
		String renderedQuery = "";
		try {
			renderedQuery = mpQueryRenderer.render(query);
		} catch (Exception e) {
			throw new AssertionError("Could not render a parsed query "
					+ id
					+ ": "
					+ e.getMessage(), e);
		}
		try {
			ParsedOperation restoredQuery = (ParsedOperation) QueryParserUtil.parseOperation(
					QueryLanguage.SPARQL,
					renderedQuery, null);
			query = getCanonicalForm(query);
			restoredQuery = getCanonicalForm(restoredQuery);
			String strippedOriginal = stripScopeChange(query.toString());
			String stripppedRestored = stripScopeChange(restoredQuery.toString());
			Assert.assertEquals(
					"Error in the query "
							+ id
							+ " defined as:\n"
							+ strQuery
							+ "\nThe rendered query was:\n"
							+ renderedQuery,
					replaceAnonymous(strippedOriginal),
					replaceAnonymous(stripppedRestored));
		} catch (Exception e) {
			throw new AssertionError("Could not parse the rendered query: ["
					+ renderedQuery + "]. Reason: " + e.getMessage(), e);
		}
	}

	private ParsedOperation getCanonicalForm(ParsedOperation parsedOp) {
		ParsedOperation res = removeReduced(parsedOp);

		TestTupleExprCanonicalizer canonicalizer = new TestTupleExprCanonicalizer();
		TupleExpr expr;
		if (parsedOp instanceof ParsedGraphQuery) {
			expr = ((ParsedGraphQuery) parsedOp).getTupleExpr();
			canonicalizer.optimize(expr, null, null);
			((ParsedGraphQuery) parsedOp).setTupleExpr(canonicalizer.getOptimized());
		} else if (parsedOp instanceof ParsedBooleanQuery) {
			expr = ((ParsedBooleanQuery) parsedOp).getTupleExpr();
			canonicalizer.optimize(expr, null, null);
			((ParsedBooleanQuery) parsedOp).setTupleExpr(canonicalizer.getOptimized());
		} else if (parsedOp instanceof ParsedTupleQuery) {
			expr = ((ParsedTupleQuery) parsedOp).getTupleExpr();
			canonicalizer.optimize(expr, null, null);
			((ParsedTupleQuery) parsedOp).setTupleExpr(canonicalizer.getOptimized());
		} else if (parsedOp instanceof ParsedUpdate) {
			List<UpdateExpr> exprs = ((ParsedUpdate) parsedOp).getUpdateExprs();

			for (UpdateExpr update : exprs) {
				if (update instanceof Modify) {
					canonicalizer.optimize(((Modify) update).getWhereExpr(), null, null);
					((Modify) update).setWhereExpr(canonicalizer.getOptimized());
				}
			}
		}

		return res;
	}

	private ParsedOperation removeReduced(ParsedOperation parsedOp) {
		if (parsedOp instanceof ParsedGraphQuery) {
			TupleExpr expr = ((ParsedGraphQuery) parsedOp).getTupleExpr();
			if (expr instanceof QueryRoot) {
				expr = ((QueryRoot) expr).getArg();
			}
			if (expr instanceof Reduced) {
				((ParsedGraphQuery) parsedOp).setTupleExpr(new QueryRoot(((Reduced) expr).getArg()));
			}
		}
		return parsedOp;

	}

	/*
	 * FIXME: RDF4J now provides scope change info in the algebra tree string representation, and the renderer component
	 * does not always produce exactly the same scopes (due to differences in curly braces etc.). In almost all cases
	 * this makes no difference for the semantics of the query (just performance).
	 * 
	 * see https://metaphacts.atlassian.net/browse/ID-1860
	 */
	private String stripScopeChange(String input) {
		return input.replace(" (new scope)", "");
	}

	private String replaceAnonymous(String input) {
		String tmp = replaceVarPattern(input, patternAnonymousVar, "_anon_");
		tmp = replaceVarPattern(tmp, patternDescribeId, "_describe_");
		return tmp;
	}

	private String replaceVarPattern(String input, Pattern pattern, String prefix) {
		Matcher matcher = pattern.matcher(input);
		LinkedHashSet<String> anonymousIds = Sets.newLinkedHashSet();
		int i = 0;
		String tmp = input;
		while (matcher.find()) {
			anonymousIds.add(matcher.group(1));
		}

		for (String anonymousId : anonymousIds) {
			i++;
			tmp = tmp.replace(anonymousId, prefix + i);
		}
		return tmp;
	}
}