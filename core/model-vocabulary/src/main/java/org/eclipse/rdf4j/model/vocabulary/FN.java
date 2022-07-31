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
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Defines constants for the standard <a href="http://www.w3.org/TR/xpath-functions/">XPath functions</a>.
 *
 * @see <a href="http://www.w3.org/TR/xpath-functions/">XPath functions</a>
 * @author Jeen Broekstra
 */
public class FN {

	/**
	 * The XPath functions namespace ( <var> http://www.w3.org/2005/xpath-functions#</var>).
	 */
	public static final String NAMESPACE = "http://www.w3.org/2005/xpath-functions#";

	/**
	 * Recommended prefix for the XPath Functions namespace: "fn"
	 */
	public static final String PREFIX = "fn";

	/**
	 * An immutable {@link Namespace} constant that represents the XPath Functions namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	/** fn:concat */
	public static final IRI CONCAT;

	/** fn:contains */
	public static final IRI CONTAINS;

	/** fn:day-from-dateTime */
	public static final IRI DAY_FROM_DATETIME;

	/** fn:encode-for-uri */
	public static final IRI ENCODE_FOR_URI;

	/** fn:ends-with */
	public static final IRI ENDS_WITH;

	/** fn:hours-from-dateTime */
	public static final IRI HOURS_FROM_DATETIME;

	/** fn:lower-case */
	public static final IRI LOWER_CASE;

	/** fn:minutes-from-dateTime */
	public static final IRI MINUTES_FROM_DATETIME;

	/** fn:month-from-dateTime */
	public static final IRI MONTH_FROM_DATETIME;

	/** fn:numeric-abs */
	public static final IRI NUMERIC_ABS;

	/** fn:numeric-ceil */
	public static final IRI NUMERIC_CEIL;

	/** fn:numeric-floor */
	public static final IRI NUMERIC_FLOOR;

	/** fn:numeric-round */
	public static final IRI NUMERIC_ROUND;

	/** fn:replace */
	public static final IRI REPLACE;

	/** fn:seconds-from-dateTime */
	public static final IRI SECONDS_FROM_DATETIME;

	/** fn:starts-with */
	public static final IRI STARTS_WITH;

	/** fn:string-length */
	public static final IRI STRING_LENGTH;

	/** fn:substring */
	public static final IRI SUBSTRING;

	/** fn:substring-before */
	public static final IRI SUBSTRING_BEFORE;

	/** fn:substring-after */
	public static final IRI SUBSTRING_AFTER;

	/** fn:timezone-from-dateTime */
	public static final IRI TIMEZONE_FROM_DATETIME;

	/** fn:upper-case */
	public static final IRI UPPER_CASE;

	/** fn:year-from-dateTime */
	public static final IRI YEAR_FROM_DATETIME;

	static {

		CONCAT = Vocabularies.createIRI(NAMESPACE, "concat");

		CONTAINS = Vocabularies.createIRI(NAMESPACE, "contains");

		DAY_FROM_DATETIME = Vocabularies.createIRI(NAMESPACE, "day-from-dateTime");

		ENCODE_FOR_URI = Vocabularies.createIRI(NAMESPACE, "encode-for-uri");

		ENDS_WITH = Vocabularies.createIRI(NAMESPACE, "ends-with");

		HOURS_FROM_DATETIME = Vocabularies.createIRI(NAMESPACE, "hours-from-dateTime");

		LOWER_CASE = Vocabularies.createIRI(NAMESPACE, "lower-case");

		MINUTES_FROM_DATETIME = Vocabularies.createIRI(NAMESPACE, "minutes-from-dateTime");

		MONTH_FROM_DATETIME = Vocabularies.createIRI(NAMESPACE, "month-from-dateTime");

		NUMERIC_ABS = Vocabularies.createIRI(NAMESPACE, "numeric-abs");

		NUMERIC_CEIL = Vocabularies.createIRI(NAMESPACE, "numeric-ceil");

		NUMERIC_FLOOR = Vocabularies.createIRI(NAMESPACE, "numeric-floor");

		NUMERIC_ROUND = Vocabularies.createIRI(NAMESPACE, "numeric-round");

		REPLACE = Vocabularies.createIRI(NAMESPACE, "replace");

		SECONDS_FROM_DATETIME = Vocabularies.createIRI(NAMESPACE, "seconds-from-dateTime");

		STARTS_WITH = Vocabularies.createIRI(NAMESPACE, "starts-with");

		STRING_LENGTH = Vocabularies.createIRI(NAMESPACE, "string-length");

		SUBSTRING = Vocabularies.createIRI(NAMESPACE, "substring");

		SUBSTRING_BEFORE = Vocabularies.createIRI(NAMESPACE, "substring-before");

		SUBSTRING_AFTER = Vocabularies.createIRI(NAMESPACE, "substring-after");

		TIMEZONE_FROM_DATETIME = Vocabularies.createIRI(NAMESPACE, "timezone-from-dateTime");

		UPPER_CASE = Vocabularies.createIRI(NAMESPACE, "upper-case");

		YEAR_FROM_DATETIME = Vocabularies.createIRI(NAMESPACE, "year-from-dateTime");
	}
}
