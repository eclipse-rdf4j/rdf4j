/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dale Visser
 */
public class PagedQuery {

	private static final Logger LOGGER = LoggerFactory.getLogger(PagedQuery.class);

	private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL;

	private static final Pattern LIMIT_OR_OFFSET = Pattern.compile("((limit)|(offset))\\s+\\d+", FLAGS);

	private final String modifiedQuery;

	private final boolean inlineLimitAndOffset;

	private final int limitSubstitute;
	private final int offsetSubstitute;

	/***
	 * <p>
	 * Creates an object that adds or modifies the limit and offset clauses of the query to be executed so that only
	 * those results to be displayed are requested from the query engine.
	 * </p>
	 * <p>
	 * Implementation note: The new object contains the user's query with appended or modified LIMIT and OFFSET clauses.
	 * </p>
	 *
	 * @param query         as it was specified by the user
	 * @param language      a {@link QueryLanguage} as specified by the user
	 * @param requestLimit  maximum number of results to return, as specified by the URL query parameters or cookies
	 * @param requestOffset which result to start at when populating the result set
	 */
	public PagedQuery(final String query, final QueryLanguage language, final int requestLimit,
			final int requestOffset) {
		LOGGER.debug("Query Language: {}, requestLimit: " + requestLimit + ", requestOffset: " + requestOffset,
				language);
		LOGGER.debug("Query: {}", query);

		String rval = query;

		/*
		 * the matcher on the pattern will have a group for "limit l#" as well as a group for l#, similarly for
		 * "offset o#" and o#. If either exists, disable paging.
		 */
		final Matcher matcher = LIMIT_OR_OFFSET.matcher(query);
		// requestLimit <= 0 actually means don't limit display
		inlineLimitAndOffset = requestLimit > 0 && !matcher.find();
		// gracefully handle malicious value
		offsetSubstitute = (requestOffset < 0) ? 0 : requestOffset;
		limitSubstitute = requestLimit;
		if (inlineLimitAndOffset) {
			rval = modifyLimit(language, rval, limitSubstitute);
			rval = modifyOffset(language, offsetSubstitute, rval);
			LOGGER.debug("Modified Query: {}", rval);
		}

		this.modifiedQuery = rval;
	}

	public boolean isPaged() {
		return this.inlineLimitAndOffset;
	}

	public int getLimit() {
		return this.limitSubstitute;
	}

	public int getOffset() {
		return this.offsetSubstitute;
	}

	@Override
	public String toString() {
		return this.modifiedQuery;
	}

	private String modifyOffset(final QueryLanguage language, final int offset, final String query) {
		String rval = query;
		final String newOffsetClause = "offset " + offset;
		if (offset > 0) {
			rval = ensureNewlineAndAppend(rval, newOffsetClause);
		}
		return rval;
	}

	private static String ensureNewlineAndAppend(final String original, final String append) {
		final StringBuffer buffer = new StringBuffer(original.length() + append.length() + 1);
		buffer.append(original);
		if (buffer.charAt(buffer.length() - 1) != '\n') {
			buffer.append('\n');
		}

		return buffer.append(append).toString();
	}

	private static String modifyLimit(final QueryLanguage language, final String query, final int limitSubstitute) {
		return ensureNewlineAndAppend(query, "limit " + limitSubstitute);
	}

}
