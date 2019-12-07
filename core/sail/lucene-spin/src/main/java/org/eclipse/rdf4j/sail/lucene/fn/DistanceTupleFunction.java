/**
 * Copyright (c) 2015 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryContext;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;
import org.eclipse.rdf4j.sail.lucene.DistanceQuerySpec;
import org.eclipse.rdf4j.sail.lucene.LuceneSailSchema;
import org.eclipse.rdf4j.sail.lucene.SearchIndex;
import org.eclipse.rdf4j.sail.lucene.SearchIndexQueryContextInitializer;
import org.eclipse.rdf4j.sail.lucene.SearchQueryEvaluator;

/**
 * Arguments:
 * <ol>
 * <li>from is the WKT point to measure from.</li>
 * <li>maxDistance is the maximum distance to consider.</li>
 * <li>units are the measurement units.</li>
 * <li>geoProperty is the predicate to use.</li>
 * <li>search:distance else omitted.</li>
 * <li>search:context else omitted.</li>
 * </ol>
 * Results:
 * <ol>
 * <li>subject is the subject whose geometry is within the given max distance.</li>
 * <li>to is the WKT of the shape measured to.</li>
 * <li>distance is the distance to the shape if search:distance is present.</li>
 * <li>context is the context if search:context is present.</li>
 * </ol>
 * 
 * @deprecated since 3.0. The LuceneSpinSail is to removed in the next major release.
 */
@Deprecated
public class DistanceTupleFunction implements TupleFunction {

	@Override
	public String getURI() {
		return LuceneSailSchema.WITHIN_DISTANCE.toString();
	}

	@Override
	public CloseableIteration<? extends List<? extends Value>, QueryEvaluationException> evaluate(
			ValueFactory valueFactory, Value... args) throws QueryEvaluationException {
		int i = 0;

		Literal from = (Literal) args[i++];
		Literal maxDist = (Literal) args[i++];
		IRI units = (IRI) args[i++];
		IRI geoProperty = (IRI) args[i++];
		String geoVar = "geometry";
		String subjectVar = "subject";
		String distanceVar = null;
		if (args.length - i > 0 && LuceneSailSchema.DISTANCE.equals(args[i])) {
			distanceVar = "distance";
			i++;
		}
		Var contextVar = null;
		if (args.length - i > 0) {
			contextVar = new Var("context");
			Resource context = (Resource) args[i];
			if (!LuceneSailSchema.CONTEXT.equals(context)) {
				contextVar.setValue(context);
			}
		}

		final DistanceQuerySpec query = new DistanceQuerySpec(from, units, maxDist.doubleValue(), distanceVar,
				geoProperty, geoVar, subjectVar, contextVar);

		SearchIndex luceneIndex = SearchIndexQueryContextInitializer.getSearchIndex(QueryContext.getQueryContext());
		Collection<BindingSet> results = luceneIndex.evaluate((SearchQueryEvaluator) query);
		return new ConvertingIteration<BindingSet, List<Value>, QueryEvaluationException>(
				new CloseableIteratorIteration<>(results.iterator())) {

			@Override
			protected List<Value> convert(BindingSet bindings) throws QueryEvaluationException {
				List<Value> results = new ArrayList<>(3);
				if (query.getSubjectVar() != null) {
					results.add(bindings.getValue(query.getSubjectVar()));
				}
				if (query.getGeoVar() != null) {
					results.add(bindings.getValue(query.getGeoVar()));
				}
				if (query.getDistanceVar() != null) {
					results.add(bindings.getValue(query.getDistanceVar()));
				}
				if (query.getContextVar() != null) {
					results.add(bindings.getValue(query.getContextVar().getName()));
				}
				return results;
			}
		};
	}
}
