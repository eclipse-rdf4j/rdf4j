/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene3;

import java.util.Set;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.spatial.tier.DistanceFilter;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.sail.lucene.DocumentDistance;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.eclipse.rdf4j.sail.lucene.util.GeoUnits;

import com.google.common.collect.Sets;

public class LuceneDocumentDistance extends LuceneDocumentResult implements DocumentDistance {

	private final URI units;

	private final DistanceFilter distanceFilter;

	private static Set<String> requiredFields(String geoProperty, boolean includeContext) {
		Set<String> fields = Sets.newHashSet(SearchFields.URI_FIELD_NAME, geoProperty);
		if(includeContext) {
			fields.add(SearchFields.CONTEXT_FIELD_NAME);
		}
		return fields;
	}

	public LuceneDocumentDistance(ScoreDoc doc, String geoProperty, URI units, DistanceFilter df,
			boolean includeContext, LuceneIndex index)
	{
		super(doc, index, requiredFields(geoProperty, includeContext));
		this.units = units;
		this.distanceFilter = df;
	}

	@Override
	public double getDistance() {
		double miles = distanceFilter.getDistance(scoreDoc.doc);
		return GeoUnits.fromMiles(miles, units);
	}
}
