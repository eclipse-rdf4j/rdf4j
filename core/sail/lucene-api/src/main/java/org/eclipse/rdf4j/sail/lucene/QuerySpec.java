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

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

	/**
	 * Replace the given pattern with a new instance of the given replacement type.
	 *
	 * @param pattern     the pattern to remove
	 * @param replacement the replacement type
	 */
	private static void replace(QueryModelNode pattern, Supplier<? extends QueryModelNode> replacement) {
		if (pattern != null) {
			pattern.replaceWith(replacement.get());
		}
	}

	private static void append(StatementPattern pattern, StringBuilder buffer) {
		if (pattern == null) {
			return;
		}

		buffer.append("   ");
		buffer.append("StatementPattern\n");
		append(pattern.getSubjectVar(), buffer);
		append(pattern.getPredicateVar(), buffer);
		append(pattern.getObjectVar(), buffer);
	}

	private static void append(Var var, StringBuilder buffer) {
		buffer.append("      ");
		buffer.append(var.toString());
	}

	private final StatementPattern matchesPattern;

	private final Collection<QueryParam> queryPatterns;

	private final StatementPattern scorePattern;

	private final StatementPattern typePattern;

	private final StatementPattern idPattern;

	private final Resource subject;

	private final String matchesVarName;

	private final String scoreVarName;

	public QuerySpec(StatementPattern matchesPattern, Collection<QueryParam> queryPatterns,
			StatementPattern scorePattern, StatementPattern typePattern,
			StatementPattern idPattern, Resource subject) {
		this.matchesPattern = matchesPattern;
		this.queryPatterns = queryPatterns;
		this.scorePattern = scorePattern;
		this.typePattern = typePattern;
		this.idPattern = idPattern;
		this.subject = subject;
		if (matchesPattern != null) {
			this.matchesVarName = matchesPattern.getSubjectVar().getName();
		} else {
			this.matchesVarName = null;
		}
		if (scorePattern != null) {
			this.scoreVarName = scorePattern.getObjectVar().getName();
		} else {
			this.scoreVarName = null;
		}
	}

	public QuerySpec(String matchesVarName, String propertyVarName, String scoreVarName, String snippetVarName,
			Resource subject, String queryString, IRI propertyURI) {
		this.matchesVarName = matchesVarName;
		this.scoreVarName = scoreVarName;
		this.matchesPattern = null;
		this.scorePattern = null;
		this.typePattern = null;
		this.queryPatterns = Set.of();
		this.idPattern = null;
		this.subject = subject;
	}

	@Override
	public QueryModelNode getParentQueryModelNode() {
		return getMatchesPattern();
	}

	@Override
	public QueryModelNode removeQueryPatterns() {
		final Supplier<? extends QueryModelNode> replacement = SingletonSet::new;

		for (QueryParam param : getQueryPatterns()) {
			param.removeQueryPatterns();
		}
		replace(getScorePattern(), replacement);
		replace(getTypePattern(), replacement);
		replace(getIdPattern(), replacement);

		final QueryModelNode placeholder = new SingletonSet();

		getMatchesPattern().replaceWith(placeholder);

		return placeholder;
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

	public Collection<QueryParam> getQueryPatterns() {
		return queryPatterns;
	}

	public StatementPattern getIdPattern() {
		return idPattern;
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

	public boolean isEvaluable() {
		return queryPatterns.stream().allMatch(QueryParam::isEvaluable);
	}

	public boolean isHighlight() {
		return queryPatterns.stream().anyMatch(QueryParam::isHighlight);
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("QuerySpec\n");
		buffer.append("   subject=" + subject + "\n");
		append(matchesPattern, buffer);
		buffer.append("   queryPatterns=").append(queryPatterns);
		append(scorePattern, buffer);
		append(typePattern, buffer);
		return buffer.toString();
	}

	public String getCatQuery() {
		return getQueryPatterns().stream()
				.map(q -> {
					StringBuilder buffer = new StringBuilder();
					buffer
							.append('"')
							.append(q.getQuery())
							.append('"');

					if (q.getProperty() != null) {
						buffer.append("@<").append(q.getProperty()).append(">");
					}

					if (q.getBoost() != null) {
						buffer.append('^').append(q.getBoost());
					}
					return buffer.toString();
				})
				.collect(Collectors.joining(" "));
	}

	/**
	 * Param in a Lucene query extracted in {@link org.eclipse.rdf4j.sail.lucene.QuerySpec}
	 */
	public static class QueryParam {
		private final StatementPattern fieldPattern;
		private final StatementPattern queryPattern;
		private final StatementPattern propertyPattern;
		private final StatementPattern snippetPattern;
		private final StatementPattern boostPattern;
		private final StatementPattern typePattern;

		private final String query;
		private final Float boost;
		private final IRI property;
		private final String snippetVarName;
		private final String propertyVarName;

		public QueryParam(StatementPattern queryPattern, StatementPattern propertyPattern,
				StatementPattern snippetPattern, StatementPattern typePattern, String query, IRI property,
				Float boost) {
			this(null, queryPattern, propertyPattern, snippetPattern, null, typePattern, query, property, boost);
		}

		public QueryParam(StatementPattern fieldPattern, StatementPattern queryPattern,
				StatementPattern propertyPattern, StatementPattern snippetPattern, StatementPattern boostPattern,
				StatementPattern typePattern, String query, IRI property, Float boost) {
			this.fieldPattern = fieldPattern;
			this.queryPattern = queryPattern;
			this.propertyPattern = propertyPattern;
			this.snippetPattern = snippetPattern;
			this.boostPattern = boostPattern;
			this.typePattern = typePattern;
			this.query = query;
			this.property = property;
			this.boost = boost;

			snippetVarName = (snippetPattern != null) ? snippetPattern.getObjectVar().getName() : null;
			propertyVarName = (propertyPattern != null) ? propertyPattern.getObjectVar().getName() : null;
		}

		/**
		 * replace all the query patterns by a singleton
		 */
		public void removeQueryPatterns() {
			final Supplier<? extends QueryModelNode> replacement = SingletonSet::new;
			replace(getTypePattern(), replacement);
			replace(getQueryPattern(), replacement);
			replace(getPropertyPattern(), replacement);
			replace(getSnippetPattern(), replacement);
			replace(getBoostPattern(), replacement);
			replace(getFieldPattern(), replacement);
		}

		public StatementPattern getTypePattern() {
			return typePattern;
		}

		public StatementPattern getFieldPattern() {
			return fieldPattern;
		}

		public StatementPattern getQueryPattern() {
			return queryPattern;
		}

		public StatementPattern getPropertyPattern() {
			return propertyPattern;
		}

		public StatementPattern getSnippetPattern() {
			return snippetPattern;
		}

		public StatementPattern getBoostPattern() {
			return boostPattern;
		}

		public String getQuery() {
			return query;
		}

		public IRI getProperty() {
			return property;
		}

		public String getPropertyVarName() {
			return propertyVarName;
		}

		public String getSnippetVarName() {
			return snippetVarName;
		}

		public Float getBoost() {
			return boost;
		}

		/**
		 * @return is this param is highlighted or not
		 */
		public boolean isHighlight() {
			return getSnippetVarName() != null || getPropertyVarName() != null;
		}

		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append("QueryParam");
			append(fieldPattern, buffer);
			append(queryPattern, buffer);
			append(propertyPattern, buffer);
			append(snippetPattern, buffer);
			append(boostPattern, buffer);
			return buffer.toString();
		}

		/**
		 * @return is this param is evaluable
		 */
		public boolean isEvaluable() {
			return query != null;
		}
	}
}
