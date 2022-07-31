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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.Test;

/**
 * Unit tests on {@link org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil}
 *
 * @author Jeen Broekstra
 */
public class XMLDatatypeUtilTest {

	private static final String[] VALID_FLOATS = { "1", "1.0", "1.0E6", "-1.0E6", "15.00001E2", "1500000000000",
			"1E104", "0.1E105", "INF", "-INF", "NaN" };

	private static final String[] INVALID_FLOATS = { "A1", "1,0", "1E106", "100E104", "1.0e1.2", "1.0E 5", "-NaN",
			"+NaN", "+INF", "NAN" };

	/** valid xsd:date values */
	private static final String[] VALID_DATES = { "2001-01-01", "2001-01-01Z", "2001-12-12+10:00", "-1800-06-06Z",
			"2004-02-29" // leap year
	};

	/** invalid xsd:date values */
	private static final String[] INVALID_DATES = { "foo", "Mon, 11 Jul 2005 +0200", "2001", "01", "2001-01",
			"2001-13-01", "2001-01-32", "2001-12-12+16:00", "2003-02-29" // not a leap year
	};

	/** valid xsd:time values */
	private static final String[] VALID_TIMES = { "13:00:00", "09:15:10", "09:15:10.01", "09:15:10.12345", "11:11:11Z",
			"10:00:01+06:00", "10:01:58-06:00" };

	/** invalid xsd:time values */
	private static final String[] INVALID_TIMES = { "foo", "21:32", "9:15:16", "09:15:10.", "09:15:10.x",
			"2001-10-10:10:10:10", "-10:00:00", "25:25:25" };

	/** valid xsd:dateTimeStamp values */
	private static final String[] VALID_DATETIMESTAMPS = { "2001-01-01T11:11:11Z", "2001-01-01T10:00:01+06:00",
			"2001-01-01T10:01:58-06:00" };

	/** valid xsd:dateTimeStamp values */
	private static final String[] INVALID_DATETIMESTAMPS = { "2001-01-01T13:00:00", "2001-01-01T09:15:10",
			"2001-01-01T09:15:10.01", "2001-01-01T09:15:10.12345" };

	/** valid xsd:gYear values */
	private static final String[] VALID_GYEAR = { "2001", "2001+02:00", "2001Z", "-2001", "-20000", "20000" };

	/** invalid xsd:gYear values */
	private static final String[] INVALID_GYEAR = { "foo", "01", "2001-01", "2001-01-01" };

	/** valid xsd:gDay values */
	private static final String[] VALID_GDAY = { "---01", "---26Z", "---12-06:00", "---13+10:00" };

	/** invalid xsd:gDay values */
	private static final String[] INVALID_GDAY = { "01", "--01-", "2001-01", "foo", "---1", "---01+16:00", "---32" };

	/** valid xsd:gMonth values */
	private static final String[] VALID_GMONTH = { "--05", "--11Z", "--11+02:00", "--11-04:00", "--02" };

	/** invalid xsd:gMonth values */
	private static final String[] INVALID_GMONTH = { "foo", "-05-", "--13", "--1", "01", "2001-01" };

	/** valid xsd:gMonthDay values */
	private static final String[] VALID_GMONTHDAY = { "--05-01", "--11-01Z", "--11-01+02:00", "--11-13-04:00",
			"--11-15" };

	/** invalid xsd:gMonthDay values */
	private static final String[] INVALID_GMONTHDAY = { "foo", "-01-30-", "--01-35", "--1-5", "01-15", "--13-01" };

	/** valid xsd:gYearMonth values */
	private static final String[] VALID_GYEARMONTH = { "2001-10", "2001-10Z", "2001-10+02:00", "2001-10-04:00", };

	/** invalid xsd:gYearMonth values */
	private static final String[] INVALID_GYEARMONTH = { "foo", "2001", "2001-15", "2001-13-26+02:00",
			"2001-11-11+02:00", "01-10" };

	/** valid xsd:duration values */
	private static final String[] VALID_DURATION = { "PT1004199059S", "PT130S", "PT2M10S", "P1DT2S", "P1M2D", "P2Y2M1D",
			"-P1Y", "P60D", "P1Y2M3DT5H20M30.123S", "PT15.5S" };

	/** invalid xsd:duration values */
	private static final String[] INVALID_DURATION = { "1Y", "P1S", "P-1Y", "P1M2Y", "P2YT", "P", "" };

	/** valid xsd:dayTimeDuration values */
	private static final String[] VALID_DAYTIMEDURATION = { "P1DT2H", "PT20M", "PT120M", "P3DT5H20M30.123S", "-P6D",
			"PT15.5S" };

	/** invalid xsd:dayTimeDuration values */
	private static final String[] INVALID_DAYTIMEDURATION = { "P1Y2M3DT5H20M30.123S", "P1Y", "P-20D", "P20DT", "P15.5D",
			"P1D2H", "P", "", "PT15.S" };

	/** valid xsd:QName values */
	private static final String[] VALID_QNAMES = { "foo:bar", "foo:_bar", "foo:_123f", ":bar", ":_1bar", "foo:",
			"föö:bar", "foo:băr", "жоо:вар", // cyrillic chars
			"⻐:⻘⻨" // random chinese chars, if this sequence happens to mean something, this is unintended
	};

	/** invalid xsd:QName values */
	private static final String[] INVALID_QNAMES = { "1:bar", "foo:1bar", "foo:bar:baz", "foo", "_:bar" };

	/** http://www.datypic.com/sc/xsd/t-xsd_anyURI.html */
	private static final String[] VALID_URI = { "http://datypic.com", "mailto:info@datypic.com", "../%C3%A9dition.html",
			"../édition.html", "http://datypic.com/prod.html#shirt", " http://datypic.com/prod.html#shirt ",
			"../prod.html#shirt", "urn:example:org", "" };

	private static final String[] INVALID_URI = { "http://datypic.com#frag1#frag2", "http://datypic.com#f% rag" };

	@Test
	public void testNormalize() {
		assertEquals("-1.0E-1", XMLDatatypeUtil.normalize("-0.1", XSD.DOUBLE));
		assertEquals("1.0E-1", XMLDatatypeUtil.normalize("0.1", XSD.DOUBLE));
		assertEquals("1.001E2", XMLDatatypeUtil.normalize("100.1", XSD.DOUBLE));
		assertEquals("1.011E2", XMLDatatypeUtil.normalize("101.1", XSD.DOUBLE));
		assertEquals("-1.011E2", XMLDatatypeUtil.normalize("-101.1", XSD.DOUBLE));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil#isValidValue(java.lang.String, org.eclipse.rdf4j.model.IRI)}
	 * .
	 */
	@Test
	public void testIsValidValue() {

		testValidation(VALID_FLOATS, XSD.FLOAT, true);
		testValidation(INVALID_FLOATS, XSD.FLOAT, false);

		testValidation(VALID_DATES, XSD.DATE, true);
		testValidation(INVALID_DATES, XSD.DATE, false);

		testValidation(VALID_DATETIMESTAMPS, XSD.DATETIMESTAMP, true);
		testValidation(INVALID_DATETIMESTAMPS, XSD.DATETIMESTAMP, false);

		testValidation(VALID_TIMES, XSD.TIME, true);
		testValidation(INVALID_TIMES, XSD.TIME, false);

		testValidation(VALID_GDAY, XSD.GDAY, true);
		testValidation(INVALID_GDAY, XSD.GDAY, false);

		testValidation(VALID_GMONTH, XSD.GMONTH, true);
		testValidation(INVALID_GMONTH, XSD.GMONTH, false);

		testValidation(VALID_GMONTHDAY, XSD.GMONTHDAY, true);
		testValidation(INVALID_GMONTHDAY, XSD.GMONTHDAY, false);

		testValidation(VALID_GYEAR, XSD.GYEAR, true);
		testValidation(INVALID_GYEAR, XSD.GYEAR, false);

		testValidation(VALID_GYEARMONTH, XSD.GYEARMONTH, true);
		testValidation(INVALID_GYEARMONTH, XSD.GYEARMONTH, false);

		testValidation(VALID_DURATION, XSD.DURATION, true);
		testValidation(INVALID_DURATION, XSD.DURATION, false);

		testValidation(VALID_DAYTIMEDURATION, XSD.DAYTIMEDURATION, true);
		testValidation(INVALID_DAYTIMEDURATION, XSD.DAYTIMEDURATION, false);

		testValidation(VALID_QNAMES, XSD.QNAME, true);
		testValidation(INVALID_QNAMES, XSD.QNAME, false);

		testValidation(VALID_URI, XSD.ANYURI, true);
		testValidation(INVALID_URI, XSD.ANYURI, false);
	}

	private void testValidation(String[] values, IRI datatype, boolean validValues) {
		for (String value : values) {
			boolean result = XMLDatatypeUtil.isValidValue(value, datatype);
			boolean resultCoreDatatype = XMLDatatypeUtil.isValidValue(value, CoreDatatype.from(datatype));
			assertEquals(result, resultCoreDatatype);

			if (validValues) {
				if (!result) {
					fail("value " + value + " should have validated for type " + datatype);
				}
			} else {
				if (result) {
					fail("value " + value + " should not have validated for type " + datatype);
				}
			}
		}
	}

	@Test
	public void testParseDouble() {
		for (String value : VALID_FLOATS) {
			XMLDatatypeUtil.parseDouble(value);
		}
	}

	@Test
	public void testParseFloat() {
		for (String value : VALID_FLOATS) {
			XMLDatatypeUtil.parseFloat(value);
		}
	}

	@Test
	public void testCompareDateTimeStamp() {
		int sameOffset = XMLDatatypeUtil.compare("2019-12-06T00:00:00Z", "2019-12-06T00:00:00+00:00",
				XSD.DATETIMESTAMP);
		assertTrue("Not the same", sameOffset == 0);

		int offset1 = XMLDatatypeUtil.compare("2019-12-06T14:00:00+02:00", "2019-12-06T13:00:00+02:00",
				XSD.DATETIMESTAMP);
		assertTrue("Wrong order", offset1 > 0);

		int offset2 = XMLDatatypeUtil.compare("2019-12-06T12:00:00+02:00", "2019-12-06T13:00:00-04:00",
				XSD.DATETIMESTAMP);
		assertTrue("Wrong order", offset2 < 0);
	}

	@Test
	public void testToStringNaN() {
		Double d = Double.NaN;
		assertEquals(XMLDatatypeUtil.toString(d), XMLDatatypeUtil.NaN);
	}
}
