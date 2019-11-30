/**
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.sail.lucene.fn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryContext;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.sail.lucene.LuceneSailSchema;
import org.eclipse.rdf4j.sail.lucene.QuerySpec;
import org.eclipse.rdf4j.sail.lucene.SearchIndex;
import org.eclipse.rdf4j.sail.lucene.SearchIndexQueryContextInitializer;
import org.eclipse.rdf4j.sail.lucene.SearchQueryEvaluator;

/**
 * Arguments:
 * <ol>
 * <li>query is the query string.</li>
 * <li>subject is the query subject or the constant search:allMatches.</li>
 * <li>propertyPredicate is the constant search:property or not present.</li>
 * <li>property is present if only propertyPredicate is present and is the property to query or the constant
 * search:allProperties.</li>
 * <li>scorePredicate is the constant search:score or not present.</li>
 * <li>snippetPredicate is the constant search:snippet or not present.</li>
 * </ol>
 * Results:
 * <ol>
 * <li>subject is included if the subject parameter is search:allMatches else omitted.</li>
 * <li>property included if the propertyPredicate parameter is present and the property parameter is
 * search:allProperties else omitted.</li>
 * <li>score is included if the scorePredicate is search:score else omitted.</li>
 * <li>snippet is included if the snippetPredicate is search:snippet else omitted.</li>
 * </ol>
 * 
 * @deprecated since 3.0. The LucenSpinSail is to be removed in the next major release.
 */
@Deprecated
public class QueryTupleFunction implements TupleFunction {

	@Override
	public String getURI() {
		return LuceneSailSchema.SEARCH.toString();
	}

	@Override
	public CloseableIteration<? extends List<? extends Value>, QueryEvaluationException> evaluate(
			ValueFactory valueFactory, Value... args) throws QueryEvaluationException {
		int i = 0;

		String queryString = ((Literal) args[i++]).getLabel();

		Value nextArg = args[i++];
		String matchesVarName = null;
		IRI subject = null;
		if (LuceneSailSchema.ALL_MATCHES.equals(nextArg)) {
			matchesVarName = "matches";
		} else {
			subject = (IRI) nextArg;
		}

		String propertyVarName = null;
		IRI propertyURI = null;
		String scoreVarName = null;
		String snippetVarName = null;

		while (i < args.length) {
			nextArg = args[i++];
			if (LuceneSailSchema.PROPERTY.equals(nextArg)) {
				nextArg = args[i++];
				if (LuceneSailSchema.ALL_PROPERTIES.equals(nextArg)) {
					propertyVarName = "property";
				} else {
					propertyURI = (IRI) nextArg;
				}
			} else if (LuceneSailSchema.SCORE.equals(nextArg)) {
				scoreVarName = "score";
			} else if (LuceneSailSchema.SNIPPET.equals(nextArg)) {
				snippetVarName = "score";
			}
		}

		final QuerySpec query = new QuerySpec(matchesVarName, propertyVarName, scoreVarName, snippetVarName, subject,
				queryString, propertyURI);
		SearchIndex luceneIndex = SearchIndexQueryContextInitializer.getSearchIndex(QueryContext.getQueryContext());
		Collection<BindingSet> results = luceneIndex.evaluate((SearchQueryEvaluator) query);
		return new ConvertingIteration<BindingSet, List<Value>, QueryEvaluationException>(
				new CloseableIteratorIteration<>(results.iterator())) {

			@Override
			protected List<Value> convert(BindingSet bindings) throws QueryEvaluationException {
				List<Value> results = new ArrayList<>(4);
				if (query.getMatchesVariableName() != null) {
					results.add(bindings.getValue(query.getMatchesVariableName()));
				}
				if (query.getPropertyVariableName() != null) {
					results.add(bindings.getValue(query.getPropertyVariableName()));
				}
				if (query.getScoreVariableName() != null) {
					results.add(bindings.getValue(query.getScoreVariableName()));
				}
				if (query.getSnippetVariableName() != null) {
					results.add(bindings.getValue(query.getSnippetVariableName()));
				}
				return results;
			}
		};
	}
}
