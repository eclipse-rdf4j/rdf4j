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
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import static org.eclipse.rdf4j.query.algebra.Compare.CompareOp.EQ;
import static org.eclipse.rdf4j.query.algebra.Compare.CompareOp.LT;
import static org.eclipse.rdf4j.query.algebra.Compare.CompareOp.NE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Jeen Broekstra
 */
public class QueryEvaluationUtilTest {

	private final ValueFactory f = SimpleValueFactory.getInstance();

	private Literal arg1simple;

	private Literal arg2simple;

	private Literal arg1en;

	private Literal arg2en;

	private Literal arg1cy;

	private Literal arg2cy;

	private Literal arg1string;

	private Literal arg2string;

	private Literal arg1int;

	private Literal arg2int;

	private Literal arg1year;

	private Literal arg2year;

	private Literal arg1dateTime;

	private Literal arg2dateTime;

	private Literal arg1duration;

	private Literal arg2duration;

	private Literal arg1yearMonthDuration;

	private Literal arg2yearMonthDuration;

	private Literal arg1unknown;

	private Literal arg2unknown;

	@Before
	public void setUp() {
		arg1simple = f.createLiteral("abc");
		arg2simple = f.createLiteral("b");

		arg1en = f.createLiteral("abc", "en");
		arg2en = f.createLiteral("b", "en");

		arg1cy = f.createLiteral("abc", "cy");
		arg2cy = f.createLiteral("b", "cy");

		arg1string = f.createLiteral("abc", XSD.STRING);
		arg2string = f.createLiteral("b", XSD.STRING);

		arg1year = f.createLiteral("2007", XSD.GYEAR);
		arg2year = f.createLiteral("2009", XSD.GYEAR);

		arg1dateTime = f.createLiteral("2009-01-01T20:20:20Z", XSD.DATETIME);
		arg2dateTime = f.createLiteral("2007-01-01T20:20:20+02:00", XSD.DATETIME);

		arg1int = f.createLiteral(10);
		arg2int = f.createLiteral(1);

		arg1duration = f.createLiteral("P1Y30DT1H1M1S", XSD.DURATION);
		arg2duration = f.createLiteral("P1Y31DT1H1M1S", XSD.DURATION);

		arg1yearMonthDuration = f.createLiteral("P1M", XSD.YEARMONTHDURATION);
		arg2yearMonthDuration = f.createLiteral("P1Y1M", XSD.YEARMONTHDURATION);

		arg1unknown = f.createLiteral("foo", f.createIRI("http://example.com/datatype"));
		arg2unknown = f.createLiteral("bar", f.createIRI("http://example.com/datatype"));
	}

	@Test
	public void testCompatibleArguments() {

		assertTrue(QueryEvaluationUtil.compatibleArguments(arg1simple, arg2simple));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1simple, arg2en));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1simple, arg2cy));
		assertTrue(QueryEvaluationUtil.compatibleArguments(arg1simple, arg2string));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1simple, arg2int));

		assertTrue(QueryEvaluationUtil.compatibleArguments(arg1en, arg2simple));
		assertTrue(QueryEvaluationUtil.compatibleArguments(arg1en, arg2en));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg2en, arg2cy));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1en, arg2cy));
		assertTrue(QueryEvaluationUtil.compatibleArguments(arg1en, arg2string));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1en, arg2int));

		assertTrue(QueryEvaluationUtil.compatibleArguments(arg1cy, arg2simple));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1cy, arg2en));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg2cy, arg2en));
		assertTrue(QueryEvaluationUtil.compatibleArguments(arg1cy, arg2cy));
		assertTrue(QueryEvaluationUtil.compatibleArguments(arg1cy, arg2string));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1cy, arg2int));

		assertTrue(QueryEvaluationUtil.compatibleArguments(arg1string, arg2simple));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1string, arg2en));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1string, arg2cy));
		assertTrue(QueryEvaluationUtil.compatibleArguments(arg1string, arg2string));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1string, arg2int));

		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1int, arg2simple));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1int, arg2en));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1int, arg2cy));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1int, arg2string));
		assertFalse(QueryEvaluationUtil.compatibleArguments(arg1int, arg2int));

	}

	@Test
	public void testCompareEQ() {
		assertCompareTrue(arg1simple, arg1simple, EQ);
		assertCompareTrue(arg1en, arg1en, EQ);
		assertCompareTrue(arg2cy, arg2cy, EQ);
		assertCompareTrue(arg1string, arg1string, EQ);
		assertCompareTrue(arg1int, arg1int, EQ);
		assertCompareTrue(arg1year, arg1year, EQ);
		assertCompareTrue(arg1dateTime, arg1dateTime, EQ);
		assertCompareTrue(arg1duration, arg1duration, EQ);
		assertCompareTrue(arg1yearMonthDuration, arg1yearMonthDuration, EQ);
		assertCompareException(arg1unknown, arg2unknown, EQ);

		assertCompareFalse(arg1simple, arg2simple, EQ);
		assertCompareFalse(arg1simple, arg2en, EQ);
		assertCompareFalse(arg1simple, arg2cy, EQ);
		assertCompareFalse(arg1simple, arg2string, EQ);
		assertCompareException(arg1simple, arg2int, EQ);
		assertCompareException(arg1simple, arg2year, EQ);
		assertCompareException(arg1simple, arg2unknown, EQ);

		assertCompareFalse(arg1en, arg2simple, EQ);
		assertCompareFalse(arg1en, arg2en, EQ);
		assertCompareFalse(arg2en, arg2cy, EQ);
		assertCompareFalse(arg1en, arg2cy, EQ);
		assertCompareFalse(arg1en, arg2string, EQ);
		assertCompareFalse(arg1en, arg2int, EQ);
		assertCompareFalse(arg1en, arg2unknown, EQ);

		assertCompareFalse(arg1cy, arg2simple, EQ);
		assertCompareFalse(arg1cy, arg2en, EQ);
		assertCompareFalse(arg2cy, arg2en, EQ);
		assertCompareFalse(arg1cy, arg2cy, EQ);
		assertCompareFalse(arg1cy, arg2string, EQ);
		assertCompareFalse(arg1cy, arg2int, EQ);
		assertCompareFalse(arg1cy, arg2unknown, EQ);

		assertCompareFalse(arg1string, arg2simple, EQ);
		assertCompareFalse(arg1string, arg2en, EQ);
		assertCompareFalse(arg1string, arg2cy, EQ);
		assertCompareFalse(arg1string, arg2string, EQ);
		assertCompareException(arg1string, arg2int, EQ);
		assertCompareException(arg1string, arg2year, EQ);
		assertCompareException(arg1string, arg2unknown, EQ);

		assertCompareException(arg1int, arg2simple, EQ);
		assertCompareFalse(arg1int, arg2en, EQ);
		assertCompareFalse(arg1int, arg2cy, EQ);
		assertCompareException(arg1int, arg2string, EQ);
		assertCompareFalse(arg1int, arg2int, EQ);
		assertCompareException(arg1int, arg2year, EQ);
		assertCompareException(arg1int, arg2unknown, EQ);

		assertCompareException(arg1year, arg2simple, EQ);
		assertCompareFalse(arg1year, arg2en, EQ);
		assertCompareException(arg1year, arg2string, EQ);
		assertCompareException(arg1year, arg2int, EQ);
		assertCompareFalse(arg1year, arg2year, EQ);
		assertCompareFalse(arg1year, arg2dateTime, EQ);
		assertCompareException(arg1year, arg2unknown, EQ);

		assertCompareException(arg1dateTime, arg2simple, EQ);
		assertCompareFalse(arg1dateTime, arg2en, EQ);
		assertCompareException(arg1dateTime, arg2string, EQ);
		assertCompareException(arg1dateTime, arg2int, EQ);
		assertCompareFalse(arg1dateTime, arg2year, EQ);
		assertCompareFalse(arg1dateTime, arg2dateTime, EQ);
		assertCompareException(arg1dateTime, arg2unknown, EQ);

		assertCompareException(arg1duration, arg2simple, EQ);
		assertCompareFalse(arg1duration, arg2en, EQ);
		assertCompareException(arg1duration, arg2string, EQ);
		assertCompareException(arg1duration, arg2int, EQ);
		assertCompareException(arg1duration, arg2year, EQ);
		assertCompareException(arg1duration, arg2dateTime, EQ);
		assertCompareException(arg1duration, arg2duration, EQ);
		assertCompareFalse(arg1duration, arg2duration, EQ, false);
		assertCompareException(arg1duration, arg2yearMonthDuration, EQ);
		assertCompareException(arg1duration, arg2yearMonthDuration, EQ, false);
		assertCompareException(arg1duration, arg2unknown, EQ);
	}

	@Test
	public void testCompareNE() {
		assertCompareFalse(arg1simple, arg1simple, NE);
		assertCompareFalse(arg1en, arg1en, NE);
		assertCompareFalse(arg1cy, arg1cy, NE);
		assertCompareFalse(arg1string, arg1string, NE);
		assertCompareFalse(arg1int, arg1int, NE);
		assertCompareFalse(arg1year, arg1year, NE);
		assertCompareFalse(arg1dateTime, arg1dateTime, NE);
		assertCompareException(arg1unknown, arg2unknown, NE);

		assertCompareTrue(arg1simple, arg2simple, NE);
		assertCompareTrue(arg1simple, arg2en, NE);
		assertCompareTrue(arg1simple, arg2cy, NE);
		assertCompareTrue(arg1simple, arg2string, NE);
		assertCompareException(arg1simple, arg2int, NE);
		assertCompareException(arg1simple, arg2year, NE);
		assertCompareException(arg1unknown, arg2unknown, NE);

		assertCompareTrue(arg1en, arg2simple, NE);
		assertCompareTrue(arg1en, arg2en, NE);
		assertCompareTrue(arg2en, arg2cy, NE);
		assertCompareTrue(arg1en, arg2cy, NE);
		assertCompareTrue(arg1en, arg2string, NE);
		assertCompareTrue(arg1en, arg2int, NE);
		assertCompareTrue(arg1en, arg2unknown, NE);

		assertCompareTrue(arg1cy, arg2simple, NE);
		assertCompareTrue(arg1cy, arg2en, NE);
		assertCompareTrue(arg2cy, arg2en, NE);
		assertCompareTrue(arg1cy, arg2cy, NE);
		assertCompareTrue(arg1cy, arg2string, NE);
		assertCompareTrue(arg1cy, arg2int, NE);
		assertCompareTrue(arg1cy, arg2unknown, NE);

		assertCompareTrue(arg1string, arg2simple, NE);
		assertCompareTrue(arg1string, arg2en, NE);
		assertCompareTrue(arg1string, arg2cy, NE);
		assertCompareTrue(arg1string, arg2string, NE);
		assertCompareException(arg1string, arg2int, NE);
		assertCompareException(arg1string, arg2year, NE);
		assertCompareException(arg1string, arg2unknown, NE);

		assertCompareException(arg1int, arg2simple, NE);
		assertCompareTrue(arg1int, arg2en, NE);
		assertCompareTrue(arg1int, arg2cy, NE);
		assertCompareException(arg1int, arg2string, NE);
		assertCompareTrue(arg1int, arg2int, NE);
		assertCompareException(arg1int, arg2year, NE);
		assertCompareException(arg1int, arg2unknown, NE);

		assertCompareException(arg1year, arg2simple, NE);
		assertCompareTrue(arg1year, arg2en, NE);
		assertCompareException(arg1year, arg2string, NE);
		assertCompareException(arg1year, arg2int, NE);
		assertCompareTrue(arg1year, arg2year, NE);
		assertCompareTrue(arg1year, arg2dateTime, NE);
		assertCompareException(arg1year, arg2unknown, NE);

		assertCompareException(arg1dateTime, arg2simple, NE);
		assertCompareTrue(arg1dateTime, arg2en, NE);
		assertCompareException(arg1dateTime, arg2string, NE);
		assertCompareException(arg1dateTime, arg2int, NE);
		assertCompareTrue(arg1dateTime, arg2year, NE);
		assertCompareTrue(arg1dateTime, arg2dateTime, NE);
		assertCompareException(arg1dateTime, arg2unknown, NE);

		assertCompareException(arg1duration, arg2simple, NE);
		assertCompareTrue(arg1duration, arg2en, NE);
		assertCompareException(arg1duration, arg2string, NE);
		assertCompareException(arg1duration, arg2int, NE);
		assertCompareException(arg1duration, arg2year, NE);
		assertCompareException(arg1duration, arg2dateTime, NE);
		assertCompareException(arg1duration, arg2duration, NE);
		assertCompareTrue(arg1duration, arg2duration, NE, false);
		assertCompareException(arg1duration, arg2yearMonthDuration, NE);
		assertCompareException(arg1duration, arg2yearMonthDuration, NE, false);
		assertCompareException(arg1duration, arg2unknown, NE);
	}

	@Test
	public void testCompareLT() {
		assertCompareFalse(arg1simple, arg1simple, LT);
		assertCompareException(arg1en, arg1en, LT);
		assertCompareFalse(arg1string, arg1string, LT);
		assertCompareFalse(arg1int, arg1int, LT);
		assertCompareFalse(arg1year, arg1year, LT);
		assertCompareFalse(arg1dateTime, arg1dateTime, LT);
		assertCompareException(arg1unknown, arg2unknown, LT);

		assertCompareTrue(arg1simple, arg2simple, LT);
		assertCompareException(arg1simple, arg2en, LT);
		assertCompareTrue(arg1simple, arg2string, LT);
		assertCompareException(arg1simple, arg2int, LT);
		assertCompareException(arg1simple, arg2year, LT);
		assertCompareException(arg1unknown, arg2unknown, LT);

		assertCompareException(arg1en, arg2simple, LT);
		assertCompareException(arg1en, arg2en, LT);
		assertCompareException(arg1en, arg2string, LT);
		assertCompareException(arg1en, arg2int, LT);
		assertCompareException(arg1en, arg2unknown, LT);

		assertCompareTrue(arg1string, arg2simple, LT);
		assertCompareException(arg1string, arg2en, LT);
		assertCompareTrue(arg1string, arg2string, LT);
		assertCompareException(arg1string, arg2int, LT);
		assertCompareException(arg1string, arg2year, LT);
		assertCompareException(arg1string, arg2unknown, LT);

		assertCompareException(arg1int, arg2simple, LT);
		assertCompareException(arg1int, arg2en, LT);
		assertCompareException(arg1int, arg2string, LT);
		assertCompareFalse(arg1int, arg2int, LT);
		assertCompareException(arg1int, arg2year, LT);
		assertCompareException(arg1int, arg2unknown, LT);

		assertCompareException(arg1year, arg2simple, LT);
		assertCompareException(arg1year, arg2en, LT);
		assertCompareException(arg1year, arg2string, LT);
		assertCompareException(arg1year, arg2int, LT);
		assertCompareTrue(arg1year, arg2year, LT);

		// comparison between xsd:gYear and xsd:dateTime should raise type error in strict mode
		assertCompareException(arg1year, arg1dateTime, LT);

		// ... but should succeed in extended mode.
		assertCompareTrue(arg1year, arg1dateTime, LT, false);

		assertCompareException(arg1year, arg2dateTime, LT);
		assertCompareException(arg1year, arg2unknown, LT);

		assertCompareException(arg1dateTime, arg2simple, LT);
		assertCompareException(arg1dateTime, arg2en, LT);
		assertCompareException(arg1dateTime, arg2string, LT);
		assertCompareException(arg1dateTime, arg2int, LT);
		assertCompareFalse(arg1dateTime, arg1year, LT, false);
		assertCompareException(arg1dateTime, arg2year, LT);
		assertCompareFalse(arg1dateTime, arg2dateTime, LT);
		assertCompareException(arg1dateTime, arg2unknown, LT);

		assertCompareException(arg1duration, arg2simple, LT);
		assertCompareException(arg1duration, arg2en, LT);
		assertCompareException(arg1duration, arg2string, LT);
		assertCompareException(arg1duration, arg2int, LT);
		assertCompareException(arg1duration, arg2year, LT);
		assertCompareException(arg1duration, arg2dateTime, LT);
		assertCompareException(arg1duration, arg2duration, LT);
		assertCompareTrue(arg1duration, arg2duration, LT, false);
		assertCompareException(arg1duration, arg2yearMonthDuration, LT);
		assertCompareException(arg1duration, arg2yearMonthDuration, LT, false);
		assertCompareException(arg1duration, arg2unknown, LT);

		assertCompareException(arg1yearMonthDuration, arg2simple, LT);
		assertCompareException(arg1yearMonthDuration, arg2en, LT);
		assertCompareException(arg1yearMonthDuration, arg2string, LT);
		assertCompareException(arg1yearMonthDuration, arg2int, LT);
		assertCompareException(arg1yearMonthDuration, arg2year, LT);
		assertCompareException(arg1yearMonthDuration, arg2dateTime, LT);
		assertCompareException(arg1yearMonthDuration, arg2duration, LT);
		assertCompareTrue(arg1yearMonthDuration, arg2duration, LT, false);
		assertCompareException(arg1yearMonthDuration, arg2yearMonthDuration, LT);
		assertCompareTrue(arg1yearMonthDuration, arg2yearMonthDuration, LT, false);
		assertCompareException(arg1yearMonthDuration, arg2unknown, LT);
	}

	/**
	 * Assert that there is an exception as a result of comparing the two literals with the given operator.
	 *
	 * @param lit1 The left literal
	 * @param lit2 The right literal
	 * @param op   The operator for the comparison
	 */
	private void assertCompareException(Literal lit1, Literal lit2, CompareOp op) {
		assertCompareException(lit1, lit2, op, true);
	}

	/**
	 * Assert that there is an exception as a result of comparing the two literals with the given operator.
	 *
	 * @param lit1 The left literal
	 * @param lit2 The right literal
	 * @param op   The operator for the comparison
	 */
	private void assertCompareException(Literal lit1, Literal lit2, CompareOp op, boolean strict) {
		try {
			boolean returnValue = QueryEvaluationUtil.compareLiterals(lit1, lit2, op, strict);
			fail("Did not receive expected ValueExprEvaluationException (return value was " + returnValue + ") for "
					+ lit1.toString() + op.getSymbol() + lit2.toString());
		} catch (ValueExprEvaluationException e) {
			// Expected exception
		}
	}

	private void assertCompareFalse(Literal lit1, Literal lit2, CompareOp op) {
		assertCompareFalse(lit1, lit2, op, true);
	}

	/**
	 * Assert that there is no exception as a result of comparing the two literals with the given operator and it
	 * returns false.
	 *
	 * @param lit1 The left literal
	 * @param lit2 The right literal
	 * @param op   The operator for the comparison
	 */
	private void assertCompareFalse(Literal lit1, Literal lit2, CompareOp op, boolean strict) {
		assertFalse("Compare did not return false for " + lit1.toString() + op.getSymbol() + lit2.toString(),
				QueryEvaluationUtil.compareLiterals(lit1, lit2, op, strict));
	}

	private void assertCompareTrue(Literal lit1, Literal lit2, CompareOp op) {
		assertCompareTrue(lit1, lit2, op, true);
	}

	/**
	 * Assert that there is no exception as a result of comparing the two literals with the given operator and it
	 * returns true.
	 *
	 * @param lit1   The left literal
	 * @param lit2   The right literal
	 * @param op     The operator for the comparison
	 * @param strict boolean switch between strict and extended comparison
	 */
	private void assertCompareTrue(Literal lit1, Literal lit2, CompareOp op, boolean strict) {
		assertTrue("Compare did not return true for " + lit1.toString() + op.getSymbol() + lit2.toString(),
				QueryEvaluationUtil.compareLiterals(lit1, lit2, op, strict));
	}

	/**
	 * Reporoduces GH-2760: an NPE has been thrown when comparing custom literal implementations
	 */
	@Test
	public void testCompareWithCustomLiterals() {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		Literal left = vf.createLiteral(5);

		Literal right = getCustomLiteral(vf.createLiteral(6));
		// GH-2760: should not throw an NPE, simply try all avaliable comparator operators
		for (CompareOp op : Compare.CompareOp.values()) {
			QueryEvaluationUtil.compareLiterals(left, right, op, true);
			QueryEvaluationUtil.compareLiterals(right, left, op, true);
		}

	}

	/**
	 * Reporoduces GH-2760: an NPE has been thrown when comparing custom literal implementations
	 */
	@Test
	public void testCompareWithCustomLiteralsIncompatible() {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		Literal left = vf.createLiteral("abc");

		Literal right = getCustomLiteral(vf.createLiteral(6));
		// GH-2760: should not throw an NPE, simply try all avaliable comparator operators
		for (CompareOp op : Compare.CompareOp.values()) {
			try {
				QueryEvaluationUtil.compareLiterals(left, right, op, true);
			} catch (ValueExprEvaluationException e) {
				assertEquals("Unable to compare strings with other supported types", e.getMessage());
			}
			try {
				QueryEvaluationUtil.compareLiterals(right, left, op, true);
			} catch (ValueExprEvaluationException e) {
				assertEquals("Unable to compare strings with other supported types", e.getMessage());
			}
		}
	}

	@Test
	public void testCompareWithCustomLiteralsInditerminate() {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		Literal left = vf.createLiteral("2000-01-01T12:00:00", XSD.DATETIME);

		Literal right = getCustomLiteral(vf.createLiteral("1999-12-31T23:00:00Z", XSD.DATETIME));
		// GH-2760: should not throw an NPE, simply try all avaliable comparator operators
		for (CompareOp op : Compare.CompareOp.values()) {
			try {
				QueryEvaluationUtil.compareLiterals(left, right, op, true);
			} catch (ValueExprEvaluationException e) {
				assertEquals("Indeterminate result for date/time comparison", e.getMessage());
			}
			try {
				QueryEvaluationUtil.compareLiterals(right, left, op, true);
			} catch (ValueExprEvaluationException e) {
				assertEquals("Indeterminate result for date/time comparison", e.getMessage());
			}
		}
	}

	@Test
	public void testCompareCustomDatatypes() {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		Literal left = vf.createLiteral(1);

		Literal right = vf.createLiteral("I", vf.createIRI("http://example.org/romanNumeral"));
		// GH-2760: should not throw an NPE, simply try all avaliable comparator operators
		for (CompareOp op : Compare.CompareOp.values()) {
			try {
				QueryEvaluationUtil.compareLiterals(left, right, op, true);
			} catch (ValueExprEvaluationException e) {
				assertEquals("Unable to compare literals with unsupported types", e.getMessage());
			}
			try {
				QueryEvaluationUtil.compareLiterals(right, left, op, true);
			} catch (ValueExprEvaluationException e) {
				assertEquals("Unable to compare literals with unsupported types", e.getMessage());
			}
		}
	}

	/**
	 * Reporoduces GH-2760: an NPE has been thrown when comparing custom literal implementations
	 */
	@Test
	public void testCompareWithOnlyCustomLiterals() {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		Literal left = getCustomLiteral(vf.createLiteral(1));

		Literal right = getCustomLiteral(vf.createLiteral(6));
		// GH-2760: should not throw an NPE, simply try all avaliable comparator operators
		for (CompareOp op : Compare.CompareOp.values()) {
			QueryEvaluationUtil.compareLiterals(left, right, op, true);
		}
	}

	private Literal getCustomLiteral(Literal nested) {
		Literal right = new Literal() {

			@Override
			public String stringValue() {
				return nested.stringValue();
			}

			@Override
			public short shortValue() {
				return nested.shortValue();
			}

			@Override
			public long longValue() {
				return nested.longValue();
			}

			@Override
			public BigInteger integerValue() {
				return nested.integerValue();
			}

			@Override
			public int intValue() {
				return nested.intValue();
			}

			@Override
			public Optional<String> getLanguage() {
				return nested.getLanguage();
			}

			@Override
			public String getLabel() {
				return nested.getLabel();
			}

			@Override
			public IRI getDatatype() {
				return nested.getDatatype();
			}

			@Override
			public float floatValue() {
				return nested.floatValue();
			}

			@Override
			public double doubleValue() {
				return nested.doubleValue();
			}

			@Override
			public BigDecimal decimalValue() {
				return nested.decimalValue();
			}

			@Override
			public XMLGregorianCalendar calendarValue() {
				return nested.calendarValue();
			}

			@Override
			public CoreDatatype getCoreDatatype() {
				return nested.getCoreDatatype();
			}

			@Override
			public byte byteValue() {
				return nested.byteValue();
			}

			@Override
			public boolean booleanValue() {
				return nested.booleanValue();
			}
		};
		return right;
	}
}
