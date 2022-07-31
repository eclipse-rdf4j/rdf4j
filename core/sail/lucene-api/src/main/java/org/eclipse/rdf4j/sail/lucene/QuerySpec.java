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
package org.eclipse.rdf4j.sail.lucene;

import java.util.function.Supplier;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;

/**
 * A QuerySpec holds information extracted from a TupleExpr corresponding with a single Lucene query. Access the
 * patterns or use the get-methods to get the names of the variables to bind.
 */
public class QuerySpec extends AbstractSearchQueryEvaluator {

	private final StatementPattern matchesPattern;

	private final StatementPattern queryPattern;

	private final StatementPattern propertyPattern;

	private final StatementPattern scorePattern;

	private final StatementPattern snippetPattern;

	private final StatementPattern typePattern;

	private final StatementPattern idPattern;

	private final Resource subject;

	private final String queryString;

	private final IRI propertyURI;

	private final String matchesVarName;

	private final String propertyVarName;

	private final String scoreVarName;

	private final String snippetVarName;

	public QuerySpec(StatementPattern matchesPattern, StatementPattern queryPattern, StatementPattern propertyPattern,
			StatementPattern scorePattern, StatementPattern snippetPattern, StatementPattern typePattern,
			Resource subject, String queryString, IRI propertyURI) {
		this(matchesPattern, queryPattern, propertyPattern, scorePattern, snippetPattern, typePattern,
				null, subject, queryString, propertyURI);
	}

	public QuerySpec(StatementPattern matchesPattern, StatementPattern queryPattern, StatementPattern propertyPattern,
			StatementPattern scorePattern, StatementPattern snippetPattern, StatementPattern typePattern,
			StatementPattern idPattern, Resource subject, String queryString, IRI propertyURI) {
		this.matchesPattern = matchesPattern;
		this.queryPattern = queryPattern;
		this.propertyPattern = propertyPattern;
		this.scorePattern = scorePattern;
		this.snippetPattern = snippetPattern;
		this.typePattern = typePattern;
		this.idPattern = idPattern;
		this.subject = subject;
		this.queryString = queryString;
		this.propertyURI = propertyURI;
		if (matchesPattern != null) {
			this.matchesVarName = matchesPattern.getSubjectVar().getName();
		} else {
			this.matchesVarName = null;
		}
		if (propertyPattern != null) {
			this.propertyVarName = propertyPattern.getObjectVar().getName();
		} else {
			this.propertyVarName = null;
		}
		if (scorePattern != null) {
			this.scoreVarName = scorePattern.getObjectVar().getName();
		} else {
			this.scoreVarName = null;
		}
		if (snippetPattern != null) {
			this.snippetVarName = snippetPattern.getObjectVar().getName();
		} else {
			this.snippetVarName = null;
		}
	}

	public QuerySpec(String matchesVarName, String propertyVarName, String scoreVarName, String snippetVarName,
			Resource subject, String queryString, IRI propertyURI) {
		this.matchesVarName = matchesVarName;
		this.propertyVarName = propertyVarName;
		this.scoreVarName = scoreVarName;
		this.snippetVarName = snippetVarName;
		this.matchesPattern = null;
		this.propertyPattern = null;
		this.scorePattern = null;
		this.snippetPattern = null;
		this.typePattern = null;
		this.queryPattern = null;
		this.idPattern = null;
		this.subject = subject;
		this.queryString = queryString;
		this.propertyURI = propertyURI;
	}

	@Override
	public QueryModelNode getParentQueryModelNode() {
		return getMatchesPattern();
	}

	@Override
	public QueryModelNode removeQueryPatterns() {
		final Supplier<? extends QueryModelNode> replacement = SingletonSet::new;

		replace(getQueryPattern(), replacement);
		replace(getScorePattern(), replacement);
		replace(getPropertyPattern(), replacement);
		replace(getSnippetPattern(), replacement);
		replace(getTypePattern(), replacement);
		replace(getIdPattern(), replacement);

		final QueryModelNode placeholder = new SingletonSet();

		getMatchesPattern().replaceWith(placeholder);

		return placeholder;
	}

	/**
	 * Replace the given pattern with a new instance of the given replacement type.
	 *
	 * @param pattern     the pattern to remove
	 * @param replacement the replacement type
	 */
	private void replace(QueryModelNode pattern, Supplier<? extends QueryModelNode> replacement) {
		if (pattern != null) {
			pattern.replaceWith(replacement.get());
		}
	}

	public StatementPattern getMatchesPattern() {
		return matchesPattern;
	}

	/**
	 * return the name of the bound variable that should match the query
	 *
	 * @return the name of the variable or null, if no name set
	 */
	public String getMatchesVariableName() {
		return matchesVarName;
	}

	public StatementPattern getQueryPattern() {
		return queryPattern;
	}

	public StatementPattern getPropertyPattern() {
		return propertyPattern;
	}

	public StatementPattern getIdPattern() {
		return idPattern;
	}

	public String getPropertyVariableName() {
		return propertyVarName;
	}

	public StatementPattern getScorePattern() {
		return scorePattern;
	}

	/**
	 * The variable name associated with the query score
	 *
	 * @return the name or null, if no score is queried in the pattern
	 */
	public String getScoreVariableName() {
		return scoreVarName;
	}

	public StatementPattern getSnippetPattern() {
		return snippetPattern;
	}

	public String getSnippetVariableName() {
		return snippetVarName;
	}

	public StatementPattern getTypePattern() {
		return typePattern;
	}

	/**
	 * the type of query, must equal {@link LuceneSailSchema#LUCENE_QUERY}. A null type is possible, but not valid.
	 *
	 * @return the type of the Query or null, if no type assigned.
	 */
	public IRI getQueryType() {
		if (typePattern != null) {
			return (IRI) typePattern.getObjectVar().getValue();
		} else {
			return null;
		}
	}

	public Resource getSubject() {
		return subject;
	}

	/**
	 * return the literal expression of the query or null, if none set. (null values are possible, but not valid).
	 *
	 * @return the query or null
	 */
	public String getQueryString() {
		// this should be the same as ((Literal)
		// queryPattern.getObjectVar().getValue()).getLabel();
		return queryString;
	}

	/**
	 * @return The URI of the property who's literal values should be searched, or <code>null</code>
	 */
	public IRI getPropertyURI() {
		return propertyURI;
	}

	public boolean isEvaluable() {
		return queryString != null;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("QuerySpec\n");
		buffer.append("   queryString=\"" + queryString + "\"\n");
		buffer.append("   propertyURI=" + propertyURI + "\n");
		buffer.append("   subject=" + subject + "\n");
		append(matchesPattern, buffer);
		append(queryPattern, buffer);
		append(propertyPattern, buffer);
		append(scorePattern, buffer);
		append(snippetPattern, buffer);
		append(typePattern, buffer);
		return buffer.toString();
	}

	private void append(StatementPattern pattern, StringBuilder buffer) {
		if (pattern == null) {
			return;
		}

		buffer.append("   ");
		buffer.append("StatementPattern\n");
		append(pattern.getSubjectVar(), buffer);
		append(pattern.getPredicateVar(), buffer);
		append(pattern.getObjectVar(), buffer);
	}

	private void append(Var var, StringBuilder buffer) {
		buffer.append("      ");
		buffer.append(var.toString());
	}
}
