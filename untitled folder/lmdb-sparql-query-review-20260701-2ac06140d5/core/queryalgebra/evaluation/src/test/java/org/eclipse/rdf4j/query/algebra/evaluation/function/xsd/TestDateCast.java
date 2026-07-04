/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.xsd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil.parseCalendar;
import static org.eclipse.rdf4j.model.vocabulary.XSD.DATE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.jupiter.api.Test;

public class TestDateCast {

	private static final DateCast dateCast = new DateCast();
	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void testCastPlainLiteral_date() {
		testDateCast(vf.createLiteral("1999-09-09"), "1999-09-09");
	}

	@Test
	public void testCastPlainLiteral_date_withTimeZone_utc() {
		testDateCast(vf.createLiteral("1999-09-09Z"), "1999-09-09Z");
	}

	@Test
	public void testCastPlainLiteral_date_withTimeZone_offset() {
		testDateCast(vf.createLiteral("1999-09-09-06:00"), "1999-09-09-06:00");
	}

	@Test
	public void testCastPlainLiteral_date_invalid() {
		// Arrange
		Literal plainLit = vf.createLiteral("1999-09-xx");

		// Act & Assert
		assertThatExceptionOfType(ValueExprEvaluationException.class).isThrownBy(() -> dateCast.evaluate(vf, plainLit));
	}

	@Test
	public void testCastPlainLiteral_dateTime() {
		testDateCast(vf.createLiteral("1999-09-09T14:45:13"), "1999-09-09");
	}

	@Test
	public void testCastPlainLiteral_dateTime_withTimeZone_utc() {
		testDateCast(vf.createLiteral("1999-09-09T14:45:13Z"), "1999-09-09-00:00");
	}

	@Test
	public void testCastPlainLiteral_dateTime_withTimeZone_offset() {
		testDateCast(vf.createLiteral("1999-09-09T14:45:13-06:00"), "1999-09-09-06:00");
	}

	@Test
	public void testCastPlainLiteral_dateTime_invalid() {
		// Arrange
		Literal plainLit = vf.createLiteral("1999-09-09T14:45:xx");

		// Act & Assert
		assertThatExceptionOfType(ValueExprEvaluationException.class).isThrownBy(() -> dateCast.evaluate(vf, plainLit));
	}

	@Test
	public void testCastDateLiteral() {
		testDateCast(vf.createLiteral("2022-11-01Z", DATE), "2022-11-01Z");
	}

	@Test
	public void testCastDateTimeLiteral() {
		testDateCast(vf.createLiteral(parseCalendar("1999-09-09T14:45:13")), "1999-09-09");
	}

	@Test
	public void testCastDateTimeLiteral_withTimeZone_utc() {
		testDateCast(vf.createLiteral(parseCalendar("1999-09-09T14:45:13Z")), "1999-09-09-00:00");
	}

	@Test
	public void testCastDateTimeLiteral_withTimeZone_offset() {
		testDateCast(vf.createLiteral(parseCalendar("1999-09-09T14:45:13+03:00")), "1999-09-09+03:00");
	}

	private void testDateCast(Literal literal, String expected) {
		// Arrange
		Literal result = null;

		// Act
		try {
			result = dateCast.evaluate(vf, literal);
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}

		// Assert
		assertNotNull(result);
		assertThat(result.getLabel()).isEqualTo(expected);
		assertThat(result.getDatatype()).isEqualTo(DATE);
	}
}
