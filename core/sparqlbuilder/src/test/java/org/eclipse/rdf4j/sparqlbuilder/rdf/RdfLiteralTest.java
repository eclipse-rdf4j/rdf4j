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
package org.eclipse.rdf4j.sparqlbuilder.rdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;

import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfLiteral.StringLiteral;
import org.junit.Test;

public class RdfLiteralTest {

	@Test
	public void emptyStringLiteralIsNotPadded() {
		StringLiteral literal = new StringLiteral("");
		assertThat(literal.getQueryString()).isEqualTo("\"\"");
	}

	@Test
	public void simpleStringLiteralIsNotPadded() {
		StringLiteral literal = new StringLiteral("foo");
		assertThat(literal.getQueryString()).isEqualTo("\"foo\"");
	}

	@Test
	public void testNewline() {
		StringLiteral literal = new StringLiteral("foo\nbar");
		assertThat(literal.getQueryString()).isEqualTo("\"foo\\nbar\"");
		literal = new StringLiteral("foo\nbar\n");
		assertThat(literal.getQueryString()).isEqualTo("\"foo\\nbar\\n\"");
		literal = new StringLiteral("\nfoo\nbar\n");
		assertThat(literal.getQueryString()).isEqualTo("\"\\nfoo\\nbar\\n\"");
		literal = new StringLiteral("foobar\n");
		assertThat(literal.getQueryString()).isEqualTo("\"foobar\\n\"");
		literal = new StringLiteral("\nfoobar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\nfoobar\"");
		literal = new StringLiteral("\n\n\nfoobar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\n\\n\\nfoobar\"");
		literal = new StringLiteral("\nfoobar\n\n\n");
		assertThat(literal.getQueryString()).isEqualTo("\"\\nfoobar\\n\\n\\n\"");
	}

	@Test
	public void testCarriageReturn() {
		StringLiteral literal = new StringLiteral("foo\rbar");
		assertThat(literal.getQueryString()).isEqualTo("\"foo\\rbar\"");
		literal = new StringLiteral("foobar\r");
		assertThat(literal.getQueryString()).isEqualTo("\"foobar\\r\"");
		literal = new StringLiteral("\rfoobar\r");
		assertThat(literal.getQueryString()).isEqualTo("\"\\rfoobar\\r\"");
		literal = new StringLiteral("\rfoo\rbar\r");
		assertThat(literal.getQueryString()).isEqualTo("\"\\rfoo\\rbar\\r\"");
		literal = new StringLiteral("\rfoobar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\rfoobar\"");
	}

	@Test
	public void testTab() {
		StringLiteral literal = new StringLiteral("foo\tbar");
		assertThat(literal.getQueryString()).isEqualTo("\"foo\\tbar\"");
		literal = new StringLiteral("foobar\t");
		assertThat(literal.getQueryString()).isEqualTo("\"foobar\\t\"");
		literal = new StringLiteral("\tfoobar\t");
		assertThat(literal.getQueryString()).isEqualTo("\"\\tfoobar\\t\"");
		literal = new StringLiteral("\tfoo\tbar\t");
		assertThat(literal.getQueryString()).isEqualTo("\"\\tfoo\\tbar\\t\"");
		literal = new StringLiteral("\tfoobar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\tfoobar\"");
	}

	@Test
	public void testBackspace() {
		StringLiteral literal = new StringLiteral("foo\bbar");
		assertThat(literal.getQueryString()).isEqualTo("\"foo\\bbar\"");
		literal = new StringLiteral("foobar\b");
		assertThat(literal.getQueryString()).isEqualTo("\"foobar\\b\"");
		literal = new StringLiteral("\bfoobar\b");
		assertThat(literal.getQueryString()).isEqualTo("\"\\bfoobar\\b\"");
		literal = new StringLiteral("\bfoo\bbar\b");
		assertThat(literal.getQueryString()).isEqualTo("\"\\bfoo\\bbar\\b\"");
		literal = new StringLiteral("\bfoobar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\bfoobar\"");
	}

	@Test
	public void testFormFeed() {
		StringLiteral literal = new StringLiteral("foo\fbar");
		assertThat(literal.getQueryString()).isEqualTo("\"foo\\fbar\"");
		literal = new StringLiteral("foobar\f");
		assertThat(literal.getQueryString()).isEqualTo("\"foobar\\f\"");
		literal = new StringLiteral("\ffoobar\f");
		assertThat(literal.getQueryString()).isEqualTo("\"\\ffoobar\\f\"");
		literal = new StringLiteral("\ffoo\fbar\f");
		assertThat(literal.getQueryString()).isEqualTo("\"\\ffoo\\fbar\\f\"");
		literal = new StringLiteral("\ffoobar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\ffoobar\"");
	}

	@Test
	public void testSingleQuote() {
		StringLiteral literal = new StringLiteral("'foobar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\'foobar\"");
		literal = new StringLiteral("foobar'");
		assertThat(literal.getQueryString()).isEqualTo("\"foobar\\'\"");
		literal = new StringLiteral("foo'bar");
		assertThat(literal.getQueryString()).isEqualTo("\"foo\\'bar\"");
		literal = new StringLiteral("'foo'bar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\'foo\\'bar\"");
		literal = new StringLiteral("'foobar'");
		assertThat(literal.getQueryString()).isEqualTo("\"\\'foobar\\'\"");
		literal = new StringLiteral("foo'bar'");
		assertThat(literal.getQueryString()).isEqualTo("\"foo\\'bar\\'\"");
		literal = new StringLiteral("'foo'bar'");
		assertThat(literal.getQueryString()).isEqualTo("\"\\'foo\\'bar\\'\"");
		literal = new StringLiteral("foobar'''");
		assertThat(literal.getQueryString()).isEqualTo("\"foobar\\'\\'\\'\"");
		literal = new StringLiteral("'''foobar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\'\\'\\'foobar\"");
	}

	@Test
	public void testDoubleQuote() {
		StringLiteral literal = new StringLiteral("\"foobar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\\"foobar\"");
		literal = new StringLiteral("foobar\"");
		assertThat(literal.getQueryString()).isEqualTo("\"foobar\\\"\"");
		literal = new StringLiteral("foo\"bar");
		assertThat(literal.getQueryString()).isEqualTo("\"foo\\\"bar\"");
		literal = new StringLiteral("\"foo\"bar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\\"foo\\\"bar\"");
		literal = new StringLiteral("\"foobar\"");
		assertThat(literal.getQueryString()).isEqualTo("\"\\\"foobar\\\"\"");
		literal = new StringLiteral("foo\"bar\"");
		assertThat(literal.getQueryString()).isEqualTo("\"foo\\\"bar\\\"\"");
		literal = new StringLiteral("\"foo\"bar\"");
		assertThat(literal.getQueryString()).isEqualTo("\"\\\"foo\\\"bar\\\"\"");
		literal = new StringLiteral("foobar\"\"\"");
		assertThat(literal.getQueryString()).isEqualTo("\"foobar\\\"\\\"\\\"\"");
		literal = new StringLiteral("\"\"\"foobar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\\"\\\"\\\"foobar\"");
	}

	@Test
	public void testCornerCases() {
		StringLiteral literal = new StringLiteral("\\nfoobar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\\\nfoobar\"");
		literal = new StringLiteral("\\\"foobar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\\\\\\"foobar\"");
		literal = new StringLiteral("\\'foobar");
		assertThat(literal.getQueryString()).isEqualTo("\"\\\\\\'foobar\"");
		literal = new StringLiteral("\n\n\t\t\tfoo\\bar\n");
		assertThat(literal.getQueryString()).isEqualTo("\"\\n\\n\\t\\t\\tfoo\\\\bar\\n\"");
	}

	@Test
	public void testBoolean() {
		RdfLiteral.BooleanLiteral literal = new RdfLiteral.BooleanLiteral(true);
		assertThat(literal.getQueryString()).isEqualTo("true");
	}

	@Test
	public void testNumeric() {
		RdfLiteral.NumericLiteral literal = new RdfLiteral.NumericLiteral(10);
		assertThat(literal.getQueryString()).isEqualTo("10");
		literal = new RdfLiteral.NumericLiteral(10.00001);
		assertThat(literal.getQueryString()).isEqualTo("10.00001");
		literal = new RdfLiteral.NumericLiteral(new BigInteger("999999999999999999999999999999999999999999999"));
		assertThat(literal.getQueryString()).isEqualTo("999999999999999999999999999999999999999999999");
	}

}
