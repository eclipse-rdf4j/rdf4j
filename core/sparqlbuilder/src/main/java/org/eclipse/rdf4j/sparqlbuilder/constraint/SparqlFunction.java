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

package org.eclipse.rdf4j.sparqlbuilder.constraint;

/**
 * The built-in SPARQL Functions. Keeping this public until {@link Expressions} is completed.
 *
 * @see <a href= "http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#SparqlOps"> SPARQL Function Definitions</a>
 */
@SuppressWarnings("javadoc") // acceptable, as this won't be public for long
public enum SparqlFunction implements SparqlOperator {
	ABS("ABS"),
	BNODE("BNODE"),
	BOUND("BOUND"),
	CEIL("CEIL"),
	COALESCE("COALESCE"),
	CONCAT("CONCAT"),
	CONTAINS("CONTAINS"),
	DATATYPE("DATATYPE"),
	DAY("DAY"),
	ENCODE_FOR_URI("ENCODE_FOR_URI"),
	FLOOR("FLOOR"),
	HOURS("HOURS"),
	IF("IF"),
	IN("IN", true),
	NOT_IN("NOT IN", true),
	IRI("IRI"),
	IS_BLANK("isBLANK"),
	IS_IRI("isIRI"),
	IS_LITERAL("isLITERAL"),
	IS_NUMERIC("isNUMERIC"),
	IS_URI("isURI"),
	LANG("LANG"),
	LANGMATCHES("LANGMATCHES"),
	LCASE("LCASE"),
	MD5("MD5"),
	MINUTES("MINUTES"),
	MONTH("MONTH"),
	NOW("NOW"),
	RAND("RAND"),
	REGEX("REGEX"),
	REPLACE("REPLACE"),
	ROUND("ROUND"),
	SAME_TERM("sameTerm"),
	SECONDS("SECONDS"),
	SHA1("SHA1"),
	SHA256("SHA256"),
	SHA384("SHA384"),
	SHA512("SHA512"),
	STR("STR"),
	STRAFTER("STRAFTER"),
	STRBEFORE("STRBEFORE"),
	STRDT("STRDT"),
	STRENDS("STRENDS"),
	STRING("STR"),
	STRLANG("STRLANG"),
	STRLEN("STRLEN"),
	STRSTARTS("STRSTARTS"),
	STRUUID("STRUUID"),
	SUBSTR("SUBSTR"),
	TIMEZONE("TIMEZONE"),
	TZ("TZ"),
	UCASE("UCASE"),
	URI("URI"),
	UUID("UUID"),
	YEAR("YEAR");

	private final String function;
	private final boolean pad;

	SparqlFunction(String function) {
		this(function, false);
	}

	SparqlFunction(String function, boolean pad) {
		this.function = function;
		this.pad = pad;
	}

	boolean pad() {
		return this.pad;
	}

	public String getQueryString() {
		return function + (pad ? " " : "");
	}
}
