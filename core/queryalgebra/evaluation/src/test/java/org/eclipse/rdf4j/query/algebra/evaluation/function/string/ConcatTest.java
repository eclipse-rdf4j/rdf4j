/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.string;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.Before;
import org.junit.Test;

public class ConcatTest {

	private Concat concatFunc;

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	private static final Literal foo = vf.createLiteral("foo");

	private static final Literal bar = vf.createLiteral("bar");

	private static final Literal foo_en = vf.createLiteral("foo", "en");

	private static final Literal bar_en = vf.createLiteral("bar", "en");

	private static final Literal foo_nl = vf.createLiteral("foo", "nl");

	@Before
	public void setUp() throws Exception {
		concatFunc = new Concat();
	}

	@Test
	public void stringLiteralHandling() {
		Literal result = concatFunc.evaluate(vf, foo, bar);

		assertThat(result.stringValue()).isEqualTo("foobar");
		assertThat(result.getDatatype()).isEqualTo(XSD.STRING);
		assertThat(result.getLanguage().isPresent()).isFalse();
	}

	@Test
	public void commonLanguageLiteralHandling() {
		Literal result = concatFunc.evaluate(vf, foo_en, bar_en);

		assertThat(result.stringValue()).isEqualTo("foobar");
		assertThat(result.getDatatype()).isEqualTo(RDF.LANGSTRING);
		assertThat(result.getLanguage().get()).isEqualTo("en");

	}

	@Test
	public void mixedLanguageLiteralHandling() {
		Literal result = concatFunc.evaluate(vf, foo_nl, bar_en);

		assertThat(result.stringValue()).isEqualTo("foobar");
		assertThat(result.getDatatype()).isEqualTo(XSD.STRING);
		assertThat(result.getLanguage().isPresent()).isFalse();
	}

	@Test
	public void mixedLiteralHandling() {
		Literal result = concatFunc.evaluate(vf, foo, bar_en);

		assertThat(result.stringValue()).isEqualTo("foobar");
		assertThat(result.getDatatype()).isEqualTo(XSD.STRING);
		assertThat(result.getLanguage().isPresent()).isFalse();
	}

	@Test
	public void nonStringLiteralHandling() {
		try {
			concatFunc.evaluate(vf, RDF.TYPE, BooleanLiteral.TRUE);
			fail("CONCAT expected to fail on non-stringliteral argument");
		} catch (ValueExprEvaluationException e) {
			// ignore, expected
		}
	}

	@Test
	public void nonLiteralHandling() {
		try {
			concatFunc.evaluate(vf, RDF.TYPE, bar_en);
			fail("CONCAT expected to fail on non-literal argument");
		} catch (ValueExprEvaluationException e) {
			// ignore, expected
		}
	}

}
