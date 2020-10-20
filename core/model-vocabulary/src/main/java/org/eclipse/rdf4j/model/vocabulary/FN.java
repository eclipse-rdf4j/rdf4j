/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import static org.eclipse.rdf4j.model.base.AbstractIRI.createIRI;
import static org.eclipse.rdf4j.model.base.AbstractNamespace.createNamespace;

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
	 * The XPath functions namespace ( <tt>	http://www.w3.org/2005/xpath-functions#</tt>).
	 */
	public static final String NAMESPACE = "http://www.w3.org/2005/xpath-functions#";

	/**
	 * Recommended prefix for the XPath Functions namespace: "fn"
	 */
	public static final String PREFIX = "fn";

	/**
	 * An immutable {@link Namespace} constant that represents the XPath Functions namespace.
	 */
	public static final Namespace NS = createNamespace(PREFIX, NAMESPACE);

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

		CONCAT = createIRI(NAMESPACE, "concat");

		CONTAINS = createIRI(NAMESPACE, "contains");

		DAY_FROM_DATETIME = createIRI(NAMESPACE, "day-from-dateTime");

		ENCODE_FOR_URI = createIRI(NAMESPACE, "encode-for-uri");

		ENDS_WITH = createIRI(NAMESPACE, "ends-with");

		HOURS_FROM_DATETIME = createIRI(NAMESPACE, "hours-from-dateTime");

		LOWER_CASE = createIRI(NAMESPACE, "lower-case");

		MINUTES_FROM_DATETIME = createIRI(NAMESPACE, "minutes-from-dateTime");

		MONTH_FROM_DATETIME = createIRI(NAMESPACE, "month-from-dateTime");

		NUMERIC_ABS = createIRI(NAMESPACE, "numeric-abs");

		NUMERIC_CEIL = createIRI(NAMESPACE, "numeric-ceil");

		NUMERIC_FLOOR = createIRI(NAMESPACE, "numeric-floor");

		NUMERIC_ROUND = createIRI(NAMESPACE, "numeric-round");

		REPLACE = createIRI(NAMESPACE, "replace");

		SECONDS_FROM_DATETIME = createIRI(NAMESPACE, "seconds-from-dateTime");

		STARTS_WITH = createIRI(NAMESPACE, "starts-with");

		STRING_LENGTH = createIRI(NAMESPACE, "string-length");

		SUBSTRING = createIRI(NAMESPACE, "substring");

		SUBSTRING_BEFORE = createIRI(NAMESPACE, "substring-before");

		SUBSTRING_AFTER = createIRI(NAMESPACE, "substring-after");

		TIMEZONE_FROM_DATETIME = createIRI(NAMESPACE, "timezone-from-dateTime");

		UPPER_CASE = createIRI(NAMESPACE, "upper-case");

		YEAR_FROM_DATETIME = createIRI(NAMESPACE, "year-from-dateTime");
	}
}
