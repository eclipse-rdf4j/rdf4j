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

	private static final Pattern SERQL_NAMESPACE = Pattern.compile("\\busing namespace\\b", FLAGS);

	private final String modifiedQuery;

	private final boolean inlineLimitAndOffset;

	private int limitSubstitute, offsetSubstitute;

	/***
	 * <p>
	 * Creates an object that adds or modifies the limit and offset clauses of the query to be executed so
	 * that only those results to be displayed are requested from the query engine.
	 * </p>
	 * <p>
	 * Implementation note: The new object contains the user's query with appended or modified LIMIT and
	 * OFFSET clauses.
	 * </p>
	 * 
	 * @param query
	 *        as it was specified by the user
	 * @param language
	 *        SPARQL or SeRQL, as specified by the user
	 * @param requestLimit
	 *        maximum number of results to return, as specified by the URL query parameters or cookies
	 * @param requestOffset
	 *        which result to start at when populating the result set
	 */
	public PagedQuery(final String query, final QueryLanguage language, final int requestLimit,
			final int requestOffset)
	{
		LOGGER.debug(
				"Query Language: {}, requestLimit: " + requestLimit + ", requestOffset: " + requestOffset,
				language);
		LOGGER.debug("Query: {}", query);

		String rval = query;

		/*
		 * the matcher on the pattern will have a group for "limit l#" as well as a group for l#, similarly
		 * for "offset o#" and o#. If either exists, disable paging.
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
		if (QueryLanguage.SPARQL == language) {
			if (offset > 0) {
				rval = ensureNewlineAndAppend(rval, newOffsetClause);
			}
		}
		else {
			/*
			 * SeRQL, add the clause before before the namespace section
			 */
			rval = insertAtMatchOnOwnLine(SERQL_NAMESPACE, rval, newOffsetClause);
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

	private static String modifyLimit(final QueryLanguage language, final String query,
			final int limitSubstitute)
	{
		String rval = query;

		/*
		 * In SPARQL, LIMIT and/or OFFSET can occur at the end, in either order. In SeRQL, LIMIT and/or OFFSET
		 * must be immediately prior to the *optional* namespace declaration section (which is itself last),
		 * and LIMIT must precede OFFSET. This code makes no attempt to correct if the user places them out of
		 * order in the query.
		 */
		if (QueryLanguage.SPARQL == language) {
			rval = ensureNewlineAndAppend(rval, "limit " + limitSubstitute);
		}
		else {
			rval = insertAtMatchOnOwnLine(SERQL_NAMESPACE, rval, "limit " + limitSubstitute);
		}
		return rval;
	}

	/**
	 * Insert a given string into another string at the point at which the given matcher matches, making sure
	 * to place the insertion string on its own line. If there is no match, appends to end on own line.
	 * 
	 * @param pattern
	 *        pattern to search for insertion location
	 * @param orig
	 *        string to perform insertion on
	 * @param insert
	 *        string to insert on own line
	 * @returns result of inserting text
	 */
	private static String insertAtMatchOnOwnLine(final Pattern pattern, final String orig,
			final String insert)
	{
		final Matcher matcher = pattern.matcher(orig);
		final boolean found = matcher.find();
		final int location = found ? matcher.start() : orig.length();
		final StringBuilder builder = new StringBuilder(orig.length() + insert.length() + 2);
		builder.append(orig.substring(0, location));
		if (builder.charAt(builder.length() - 1) != '\n') {
			builder.append('\n');
		}

		builder.append(insert);
		final String end = orig.substring(location);
		if (!end.startsWith("\n")) {
			builder.append('\n');
		}

		builder.append(end);
		return builder.toString();
	}
}
