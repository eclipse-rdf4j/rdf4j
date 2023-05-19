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

import static org.eclipse.rdf4j.model.vocabulary.RDF.TYPE;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.BOOST;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.INDEXID;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.LUCENE_QUERY;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.MATCHES;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.PROPERTY;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.QUERY;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SCORE;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SNIPPET;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.TupleFunctionCall;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A QueryInterpreter creates a set of QuerySpecs based on Lucene-related StatementPatterns that it finds in a
 * TupleExpr.
 * <p>
 * QuerySpecs will only be created when the set of StatementPatterns is complete (i.e. contains at least a matches and a
 * query statement connected properly) and correct (query pattern has a literal object, matches a resource subject,
 * etc.).
 */
public class QuerySpecBuilder implements SearchQueryInterpreter {

	private final static Logger logger = LoggerFactory.getLogger(QuerySpecBuilder.class);

	private final boolean incompleteQueryFails;

	private final IRI indexId;

	/**
	 * Initialize a new QuerySpecBuilder
	 *
	 * @param incompleteQueryFails see {@link LuceneSail#isIncompleteQueryFails()}
	 */
	public QuerySpecBuilder(boolean incompleteQueryFails) {
		this(incompleteQueryFails, null);
	}

	/**
	 * Initialize a new QuerySpecBuilder
	 *
	 * @param incompleteQueryFails see {@link LuceneSail#isIncompleteQueryFails()}
	 * @param indexId              the id of the index, null to do not filter by index id, see
	 *                             {@link LuceneSail#INDEX_ID}
	 */
	public QuerySpecBuilder(boolean incompleteQueryFails, IRI indexId) {
		this.incompleteQueryFails = incompleteQueryFails;
		this.indexId = indexId;
	}

	/**
	 * Returns a set of QuerySpecs embodying all necessary information to perform the Lucene query embedded in a
	 * TupleExpr. To be removed, prefer {@link #process(TupleExpr, BindingSet, Collection<SearchQueryEvaluator>)}.
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	public Set<QuerySpec> process(TupleExpr tupleExpr, BindingSet bindings) throws SailException {
		HashSet<QuerySpec> result = new HashSet<>();
		process(tupleExpr, bindings, (Collection<SearchQueryEvaluator>) (Collection<?>) result);
		return result;
	}

	/**
	 * Appends a set of QuerySpecs embodying all necessary information to perform the Lucene query embedded in a
	 * TupleExpr.
	 */
	@Override
	public void process(TupleExpr tupleExpr, BindingSet bindings, Collection<SearchQueryEvaluator> result)
			throws SailException {
		// find Lucene-related StatementPatterns
		PatternFilter filter = new PatternFilter();
		tupleExpr.visit(filter);

		// loop over all matches statements
		for (StatementPattern matchesPattern : filter.matchesPatterns) {
			// the subject of the matches statements should be a variable or a
			// Resource
			Var subjectVar = matchesPattern.getSubjectVar();
			Value subjectValue = subjectVar.hasValue() ? subjectVar.getValue()
					: bindings.getValue(subjectVar.getName());

			if (subjectValue != null && !(subjectValue instanceof Resource)) {
				failOrWarn(MATCHES + " properties should have Resource subjects: " + subjectVar.getValue());
				continue;
			}

			Resource subject = (Resource) subjectValue;

			// the matches var should have no value
			Var matchesVar = matchesPattern.getObjectVar();
			if (matchesVar.hasValue()) {
				failOrWarn(MATCHES + " properties should have variable objects: " + matchesVar.getValue());
				continue;
			}

			// do we need to filter by id?
			StatementPattern idPattern;

			if (indexId != null) {
				try {
					idPattern = getPattern(matchesVar, filter.idPatterns);
				} catch (IllegalArgumentException e) {
					failOrWarn(e);
					continue;
				}

				if (idPattern == null) {
					continue;
				}

				Var indexIdVar = idPattern.getObjectVar();
				Value indexIdValue = indexIdVar.hasValue() ? indexIdVar.getValue()
						: bindings.getValue(indexIdVar.getName());

				if (!(indexIdValue instanceof IRI && indexIdVar.getValue().equals(indexId))) {
					continue; // this match isn't for this index, continue for the next one
				}
			} else {
				idPattern = null;
			}

			// find the relevant outgoing patterns
			StatementPattern typePattern, propertyPattern, scorePattern, snippetPattern;
			List<StatementPattern> queryPatterns;

			try {
				typePattern = getPattern(matchesVar, filter.typePatterns);
				queryPatterns = getQueryVar(matchesVar, filter.queryPatterns);
				propertyPattern = getPattern(matchesVar, filter.propertyPatterns);
				scorePattern = getPattern(matchesVar, filter.scorePatterns);
				snippetPattern = getPattern(matchesVar, filter.snippetPatterns);
			} catch (IllegalArgumentException e) {
				failOrWarn(e);
				continue;
			}

			// fetch the query String
			String queryString = null;
			List<QuerySpec.QueryParam> queries = new ArrayList<>();
			StatementPattern litQueryPattern = null;
			boolean multiFieldQuery = false;

			if (!queryPatterns.isEmpty()) {
				Var queryVar = queryPatterns.get(0).getObjectVar();
				Value firstQueryValue = queryVar.hasValue() ? queryVar.getValue()
						: bindings.getValue(queryVar.getName());
				multiFieldQuery = firstQueryValue == null || !firstQueryValue.isLiteral();

				if (multiFieldQuery) {
					// multiple queries
					for (StatementPattern queryPattern : queryPatterns) {
						Var queryPatternVar = queryPattern.getObjectVar();
						StatementPattern fieldQueryQueryPattern = getPattern(queryPatternVar, filter.queryPatterns);
						StatementPattern fieldQueryBoostPattern = getPattern(queryPatternVar, filter.boostPatterns);
						StatementPattern fieldQueryPropertyPattern = getPattern(queryPatternVar,
								filter.propertyPatterns);
						StatementPattern fieldQuerySnippetPattern = getPattern(queryPatternVar, filter.snippetPatterns);
						StatementPattern fieldTypePattern = getPattern(queryPatternVar, filter.typePatterns);

						String query = null;
						IRI property = null;
						Float boost = null;
						Var snippetVar = fieldQuerySnippetPattern == null ? null
								: fieldQuerySnippetPattern.getObjectVar();

						if (fieldQueryQueryPattern != null) {
							Var fieldQueryQueryVar = fieldQueryQueryPattern.getObjectVar();
							Value queryValue = fieldQueryQueryVar.hasValue() ? fieldQueryQueryVar.getValue()
									: bindings.getValue(fieldQueryQueryVar.getName());

							if (queryValue instanceof Literal) {
								query = ((Literal) queryValue).getLabel();
							}
						}

						if (fieldQueryBoostPattern != null) {
							Var fieldQueryBoostVar = fieldQueryBoostPattern.getObjectVar();
							Value boostValue = fieldQueryBoostVar.hasValue() ? fieldQueryBoostVar.getValue()
									: bindings.getValue(fieldQueryBoostVar.getName());

							if (boostValue instanceof Literal) {
								boost = ((Literal) boostValue).floatValue();
							}
						}

						if (fieldQueryPropertyPattern != null) {
							Var propertyVar = fieldQueryPropertyPattern.getObjectVar();
							Value propertyValue = propertyVar.hasValue() ? propertyVar.getValue()
									: bindings.getValue(propertyVar.getName());

							// if property is a restriction, it should be an URI
							if (propertyValue instanceof IRI) {
								property = (IRI) propertyValue;
							}
							// otherwise, it should be a variable
							else if (propertyValue != null) {
								failOrWarn(PROPERTY + " should have a property URI or a variable as object: "
										+ propertyVar.getValue());
								continue;
							}
						}

						// check the snippet variable, if any
						if (snippetVar != null && snippetVar.hasValue()) {
							failOrWarn(SNIPPET + " should have a variable as object: " + snippetVar.getValue());
							continue;
						}

						// check type pattern
						if (fieldTypePattern == null) {
							logger.debug("Query variable '{}' has not rdf:type, assuming {}", fieldTypePattern,
									LUCENE_QUERY);
						}

						queries.add(new QuerySpec.QueryParam(queryPattern, fieldQueryQueryPattern,
								fieldQueryPropertyPattern, fieldQuerySnippetPattern, fieldQueryBoostPattern,
								fieldTypePattern, query, property, boost));
					}
				} else {
					// using literal query
					queryString = ((Literal) firstQueryValue).getLabel();
					litQueryPattern = queryPatterns.get(0);
				}
			}

			// check property restriction or variable
			IRI propertyURI = null;
			if (propertyPattern != null) {
				if (multiFieldQuery) {
					failOrWarn(PROPERTY + " can't be used with " + MATCHES + " for non literal query");
					continue;
				}
				Var propertyVar = propertyPattern.getObjectVar();
				Value propertyValue = propertyVar.hasValue() ? propertyVar.getValue()
						: bindings.getValue(propertyVar.getName());

				// if property is a restriction, it should be an URI
				if (propertyValue instanceof IRI) {
					propertyURI = (IRI) propertyValue;
				}
				// otherwise, it should be a variable
				else if (propertyValue != null) {
					failOrWarn(PROPERTY + " should have a property URI or a variable as object: "
							+ propertyVar.getValue());
					continue;
				}
			}

			// check the score variable, if any
			Var scoreVar = scorePattern == null ? null : scorePattern.getObjectVar();
			if (scoreVar != null && scoreVar.hasValue()) {
				failOrWarn(SCORE + " should have a variable as object: " + scoreVar.getValue());
				continue;
			}

			// check the snippet variable, if any
			Var snippetVar = snippetPattern == null ? null : snippetPattern.getObjectVar();
			if (snippetVar != null && snippetVar.hasValue()) {
				failOrWarn(SNIPPET + " should have a variable as object: " + snippetVar.getValue());
				continue;
			}

			// check type pattern
			if (typePattern == null) {
				logger.debug("Query variable '{}' has not rdf:type, assuming {}", subject, LUCENE_QUERY);
			}

			if (!multiFieldQuery) {
				queries.add(new QuerySpec.QueryParam(litQueryPattern, propertyPattern, snippetPattern, null,
						queryString, propertyURI, null));
			}

			QuerySpec querySpec = new QuerySpec(matchesPattern, queries, scorePattern, typePattern, idPattern, subject);

			if (querySpec.isEvaluable()) {
				// constant optimizer
				result.add(querySpec);
			} else {
				// evaluate later
				TupleFunctionCall funcCall = new TupleFunctionCall();
				funcCall.setURI(LuceneSailSchema.SEARCH.toString());
				if (multiFieldQuery) {
					funcCall.addArg(new ValueConstant(QUERY));
					funcCall.addArg(new ValueConstant(Values.literal(queryPatterns.size())));
					queryPatterns.stream().map(StatementPattern::getObjectVar).forEach(funcCall::addArg);
				} else {
					funcCall.addArg(queryPatterns.get(0).getObjectVar());
				}
				if (subject != null) {
					funcCall.addArg(matchesPattern.getSubjectVar());
				} else {
					funcCall.addArg(new ValueConstant(LuceneSailSchema.ALL_MATCHES));
					funcCall.addResultVar(matchesPattern.getSubjectVar());
				}
				if (propertyPattern != null) {
					funcCall.addArg(new ValueConstant(LuceneSailSchema.PROPERTY));
					if (propertyURI != null) {
						funcCall.addArg(propertyPattern.getObjectVar());
					} else {
						funcCall.addArg(new ValueConstant(LuceneSailSchema.ALL_PROPERTIES));
						funcCall.addResultVar(propertyPattern.getObjectVar());
					}
				}
				if (scoreVar != null) {
					funcCall.addArg(new ValueConstant(LuceneSailSchema.SCORE));
					funcCall.addResultVar(scoreVar);
				}
				if (snippetVar != null) {
					funcCall.addArg(new ValueConstant(LuceneSailSchema.SNIPPET));
					funcCall.addResultVar(snippetVar);
				}

				Join join = new Join();
				matchesPattern.replaceWith(join);
				join.setLeftArg(matchesPattern);
				join.setRightArg(funcCall);

				querySpec.removeQueryPatterns();
			}
		}

		// fail on superflous typePattern, query, score, or snippet patterns.
	}

	private void failOrWarn(Exception exception) throws SailException {
		if (incompleteQueryFails) {
			throw exception instanceof SailException ? (SailException) exception : new SailException(exception);
		} else {
			logger.warn(exception.getMessage(), exception);
		}
	}

	private void failOrWarn(String message) throws SailException {
		if (incompleteQueryFails) {
			throw new SailException("Invalid Text Query: " + message);
		} else {
			logger.warn(message);
		}
	}

	/**
	 * Returns the StatementPattern, if any, from the specified Collection that has the specified subject var. If
	 * multiple StatementPatterns exist with this subject var, an IllegalArgumentException is thrown. It also removes
	 * the patter from the arraylist, to be able to check if some patterns are added without a MATCHES property.
	 */
	private StatementPattern getPattern(Var subjectVar, ArrayList<StatementPattern> patterns)
			throws IllegalArgumentException {
		StatementPattern result = null;

		for (StatementPattern pattern : patterns) {
			if (pattern.getSubjectVar().equals(subjectVar)) {
				if (result == null) {
					result = pattern;
				} else {
					throw new IllegalArgumentException(
							"multiple StatementPatterns with the same subject: " + result + ", " + pattern);
				}
			}
		}
		// remove the result from the list, to filter out superflous patterns
		if (result != null) {
			patterns.remove(result);
		}
		return result;
	}

	/**
	 * Return all the var of the patterns with the subject subjectVar, if a pattern is a literal, it will return a
	 * singleton list, otherwise it will return an empty list or a list without any literal var
	 */
	private List<StatementPattern> getQueryVar(Var subjectVar, ArrayList<StatementPattern> patterns)
			throws IllegalArgumentException {
		StatementPattern litResult = null;
		List<StatementPattern> objectResult = null;

		for (StatementPattern pattern : patterns) {
			// ignore other subject
			if (!pattern.getSubjectVar().equals(subjectVar)) {
				continue;
			}

			Var queryPatternVar = pattern.getObjectVar();
			if (queryPatternVar.hasValue() && queryPatternVar.getValue().isLiteral()) {
				if (objectResult != null) {
					throw new IllegalArgumentException("query can't be done over both literal and resource!");
				}
				if (litResult != null) {
					throw new IllegalArgumentException(
							"multiple StatementPatterns with the same subject: " + litResult + ", " + pattern);
				} else {
					litResult = pattern;
				}
			} else {
				if (litResult != null) {
					throw new IllegalArgumentException("query can't be done over both literal and resource!");
				}
				if (objectResult == null) {
					objectResult = new ArrayList<>();
				}
				objectResult.add(pattern);
			}
		}
		// remove the result from the list, to filter out superflous patterns
		// we have one literal
		if (litResult != null) {
			patterns.remove(litResult);
			return List.of(litResult);
		}
		// we have resources
		if (objectResult != null) {
			patterns.removeAll(objectResult);
			return objectResult;
		}
		// no query
		return List.of();
	}

	private static class PatternFilter extends AbstractQueryModelVisitor<RuntimeException> {

		public ArrayList<StatementPattern> typePatterns = new ArrayList<>();

		public ArrayList<StatementPattern> matchesPatterns = new ArrayList<>();

		public ArrayList<StatementPattern> queryPatterns = new ArrayList<>();

		public ArrayList<StatementPattern> propertyPatterns = new ArrayList<>();

		public ArrayList<StatementPattern> scorePatterns = new ArrayList<>();

		public ArrayList<StatementPattern> snippetPatterns = new ArrayList<>();

		public ArrayList<StatementPattern> idPatterns = new ArrayList<>();

		public ArrayList<StatementPattern> boostPatterns = new ArrayList<>();

		/**
		 * Method implementing the visitor pattern that gathers all statements using a predicate from the LuceneSail's
		 * namespace.
		 */
		@Override
		public void meet(StatementPattern node) {
			Value predicate = node.getPredicateVar().getValue();

			if (MATCHES.equals(predicate)) {
				matchesPatterns.add(node);
			} else if (QUERY.equals(predicate)) {
				queryPatterns.add(node);
			} else if (PROPERTY.equals(predicate)) {
				propertyPatterns.add(node);
			} else if (SCORE.equals(predicate)) {
				scorePatterns.add(node);
			} else if (SNIPPET.equals(predicate)) {
				snippetPatterns.add(node);
			} else if (INDEXID.equals(predicate)) {
				idPatterns.add(node);
			} else if (BOOST.equals(predicate)) {
				boostPatterns.add(node);
			} else if (TYPE.equals(predicate)) {
				Value object = node.getObjectVar().getValue();
				if (LUCENE_QUERY.equals(object)) {
					typePatterns.add(node);
				}
			}
		}
	}
}
