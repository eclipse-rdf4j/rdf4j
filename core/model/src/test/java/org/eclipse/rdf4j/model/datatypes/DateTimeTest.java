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
package org.eclipse.rdf4j.model.datatypes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * This class provides JUnit test cases for class {@link XMLDateTime}.
 */
public class DateTimeTest extends TestCase {

	private static final String[] VALID_DATES = { "0001-01-01T00:00:00", "0001-01-01T00:00:00.0",
			"0001-01-01T00:00:00Z", "0001-01-01T00:00:00.0Z", "0001-01-01T00:00:00+00:00",
			"0001-01-01T00:00:00.0+00:00", "0001-01-01T00:00:00.0-00:00", "0001-01-01T00:00:00.0+14:00",
			"0001-01-01T00:00:00.0-14:00", "0001-05-31T00:00:00.00", "0001-07-31T00:00:00.00", "0001-08-31T00:00:00.00",
			"0001-10-31T00:00:00.00", "0001-12-31T00:00:00.00", "-0001-01-01T00:00:00", "1234-12-31T23:59:59",
			"1234-12-31T24:00:00", "12345-12-31T24:00:00",
			// "5000000000-12-31T24:00:00", // year > Integer.MAX_VALUE
			// "-5000000000-12-31T24:00:00", // year < Integer.MIN_VALUE
			"1234-12-31T24:00:00.1234567890", "2004-02-29T00:00:00" // leap year
	};

	private static final String[] INVALID_DATES = { "foo", "Mon, 11 Jul 2005 09:22:29 +0200", "0001-01-01T00:00",
			"0001-01-01T00:00.00", "0001-13-01T00:00:00.00", "0001-01-32T00:00:00.00", "0001-02-30T00:00:00.00",
			"2005-02-29T00:00:00", // not a leap year
			"0001-04-31T00:00:00.00", "0001-01-01T25:00:00.00", "0001-01-01T00:61:00.00", "0001-01-01T00:00:61.00",
			"0001-01-01T00:00.00+15:00", "0001-01-01T00:00.00-15:00", "001-01-01T00:00:00.0", "0001-1-01T00:00:00.0",
			"0001-01-1T00:00:00.0", "0001-01-01T0:00:00.0", "0001-01-01T00:0:00.0", "0001-01-01T00:00:0.0",
			"0001/01-01T00:00:00.0", "0001-01/01T00:00:00.0", "0001-01-01t00:00:00.0", "0001-01-01T00.00:00.0",
			"0001-01-01T00:00.00.0", "0001-01-01T00:00:00:0", "0001-01-01T00:00.00+0:00", "0001-01-01T00:00.00+00:0",
			"0001-jan-01T00:00:00", "0001-01-01T00:00:00+00:00Z", "0001-01-01T24:01:00", "0001-01-01T24:00:01",
			"00001-01-01T00:00:00", "0001-001-01T00:00:00", "0001-01-001T00:00:00", "0001-01-01T000:00:00",
			"0001-01-01T00:000:00", "0001-01-01T00:00:000", "0001-01-01T00:00:000", "0001-01-01T00:00:00z",
			"0001-01-01T00:00:00+05", "0001-01-01T00:00:00+0500", "0001-01-01T00:00:00GMT", "0001-01-01T00:00:00PST",
			"0001-01-01T00:00:00GMT+05", "0000-01-01T00:00:00", "0000-01-01T00:00:00", "-0000-01-01T00:00:00",
			"+0001-01-01T00:00:00" };

	private static final String[][] NORMALIZED_DATES = { { "0001-01-01T00:00:00", "0001-01-01T00:00:00" },
			{ "0001-01-01T00:00:00.0", "0001-01-01T00:00:00" }, { "0001-01-01T00:00:00Z", "0001-01-01T00:00:00Z" },
			{ "0001-01-01T00:00:00.0Z", "0001-01-01T00:00:00Z" },
			{ "0001-01-01T00:00:00+00:00", "0001-01-01T00:00:00Z" },
			{ "0001-01-01T00:00:00-00:00", "0001-01-01T00:00:00Z" },
			{ "0001-01-01T00:00:00.0+00:00", "0001-01-01T00:00:00Z" },
			{ "0001-01-01T00:00:00.0-00:00", "0001-01-01T00:00:00Z" },
			{ "0001-01-01T00:00:00-14:00", "0001-01-01T14:00:00Z" },
			{ "0001-01-01T00:00:00+14:00", "-0001-12-31T10:00:00Z" },
			{ "1234-12-31T24:00:00", "1235-01-01T00:00:00" } };

	private static final String[][] EQUAL_DATES = { { "2005-01-29T00:00:00", "2005-01-29T00:00:00" },
			{ "2005-01-29T00:00:00", "2005-01-28T24:00:00" },
			{ "2005-05-01T00:00:00.2+02:00", "2005-05-01T00:00:00.2+02:00" } };

	private static final String[][] COMPARISON_DATES = { { "-0005-02-27T00:00:00", "2005-02-28T24:00:00" },
			{ "2005-02-28T24:00:00", "2005-03-27T00:00:00" }, { "2005-01-28T24:00:00", "2005-02-28T00:00:00.4" },
			{ "2005-02-28T00:00:00.1", "2005-02-28T00:00:00.4" }, { "2005-02-28T00:00:00", "2005-02-28T00:00:00.1" },
			{ "2005-02-28T00:00:00", "2005-02-28T00:00:00.2" },
			{ "2005-05-01T00:00:00.2+02:00", "2005-05-01T00:00:00.2+01:00" },
			{ "2005-05-01T00:00:00.2-01:00", "2005-05-01T00:00:00.2-02:00" },
			{ "2005-05-01T00:00:00+02:00", "2005-05-01T00:00:00.2+01:00" },
			{ "2005-05-01T00:00:00-01:00", "2005-05-01T00:00:00.2-02:00" },
			{ "2005-05-01T00:00:00-01:00", "2005-05-01T00:00:00-02:00" },
			{ "2005-05-01T00:00:00-01:00", "2005-05-01T00:00:00-02:00" },
			{ "-2005-05-01T00:00:00.2+02:00", "2005-05-01T00:00:00.2+02:00" },
			{ "2005-05-01T00:00:00.2+02:00", "20005-05-01T00:00:00.2+02:00" } };

	public static Test suite() {
		TestSuite suite = new TestSuite();

		for (String validDate : VALID_DATES) {
			suite.addTest(new ValidDateTest(validDate));
		}

		for (String invalidDate : INVALID_DATES) {
			suite.addTest(new InvalidDateTest(invalidDate));
		}

		for (String[] normalizedDate : NORMALIZED_DATES) {
			suite.addTest(new NormalizedDateTest(normalizedDate[0], normalizedDate[1]));
		}

		for (String[] equalDate : EQUAL_DATES) {
			suite.addTest(new EqualDateTest(equalDate[0], equalDate[1]));
		}

		for (String[] comparisonDate : COMPARISON_DATES) {
			suite.addTest(new CompareDateTest(comparisonDate[0], comparisonDate[1]));
		}

		return suite;
	}

	private static void logError(String errMsg) {
		System.err.println("[ERROR]: " + errMsg);
	}

	/*
	 * run test
	 */
	public static void main(String[] args) {
		TestRunner.run(suite());
	}

	/*--------------------------------------------------*
	 * Inner classes for testing specific functionality *
	 *--------------------------------------------------*/

	private static class ValidDateTest extends TestCase {

		private final String dateString;

		public ValidDateTest(String dateString) {
			super("valid: " + dateString);
			this.dateString = dateString;
		}

		@Override
		protected void runTest() {
			if (!XMLDatatypeUtil.isValidDateTime(dateString)) {
				String errMsg = "string should be valid but is not: " + dateString;
				logError(errMsg);
				fail(errMsg);
			}
		}
	}

	private static class InvalidDateTest extends TestCase {

		private final String dateString;

		public InvalidDateTest(String dateString) {
			super("invalid: " + dateString);
			this.dateString = dateString;
		}

		@Override
		protected void runTest() {
			if (XMLDatatypeUtil.isValidDateTime(dateString)) {
				String errMsg = "string should be invalid but is not: " + dateString;
				logError(errMsg);
				fail(errMsg);
			}
		}
	}

	private static class NormalizedDateTest extends TestCase {

		private final String input;
		private final String expected;

		public NormalizedDateTest(String input, String expected) {
			super("normalize: " + input + " --> " + expected);
			this.input = input;
			this.expected = expected;
		}

		@Override
		protected void runTest() {
			String normalized = XMLDatatypeUtil.normalizeDateTime(input);
			if (!expected.equals(normalized)) {
				String errMsg = "normalizing " + input + " should produce " + expected + " but was " + normalized;
				logError(errMsg);
				fail(errMsg);
			}
		}
	}

	private static class EqualDateTest extends TestCase {

		private final String dateString1;
		private final String dateString2;

		public EqualDateTest(String dateString1, String dateString2) {
			super(dateString1 + " == " + dateString2);
			this.dateString1 = dateString1;
			this.dateString2 = dateString2;
		}

		@Override
		protected void runTest() {
			int result = XMLDatatypeUtil.compareDateTime(dateString1, dateString2);
			if (result != 0) {
				String errMsg = dateString1 + " and " + dateString2 + " should be equals but are not (result=" + result
						+ ")";
				logError(errMsg);
				fail(errMsg);
			}
		}
	}

	private static class CompareDateTest extends TestCase {

		private final String dateString1;
		private final String dateString2;

		public CompareDateTest(String dateString1, String dateString2) {
			super(dateString1 + " < " + dateString2);
			this.dateString1 = dateString1;
			this.dateString2 = dateString2;
		}

		@Override
		protected void runTest() {
			int result = XMLDatatypeUtil.compareDateTime(dateString1, dateString2);
			if (result >= 0) {
				String errMsg = dateString1 + " should be smaller than " + dateString2 + " but is not (result=" + result
						+ ")";
				logError(errMsg);
				fail(errMsg);
			}
		}
	}
}
