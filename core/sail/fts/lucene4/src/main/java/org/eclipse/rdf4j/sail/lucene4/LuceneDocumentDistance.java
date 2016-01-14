/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene4;

import java.text.ParseException;
import java.util.List;
import java.util.Set;

import org.apache.lucene.search.ScoreDoc;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.sail.lucene.DocumentDistance;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.eclipse.rdf4j.sail.lucene.util.GeoUnits;

import com.google.common.collect.Sets;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Shape;

public class LuceneDocumentDistance extends LuceneDocumentResult implements DocumentDistance {

	private final String geoProperty;

	private final URI units;

	private final Point origin;

	private static Set<String> requiredFields(String geoProperty, boolean includeContext) {
		Set<String> fields = Sets.newHashSet(SearchFields.URI_FIELD_NAME, geoProperty);
		if(includeContext) {
			fields.add(SearchFields.CONTEXT_FIELD_NAME);
		}
		return fields;
	}

	public LuceneDocumentDistance(ScoreDoc doc, String geoProperty, URI units, Point origin, boolean includeContext, LuceneIndex index)
	{
		super(doc, index, requiredFields(geoProperty, includeContext));
		this.geoProperty = geoProperty;
		this.units = units;
		this.origin = origin;
	}

	@Override
	public double getDistance() {
		List<String> wkts = getDocument().getProperty(geoProperty);
		double min = Double.POSITIVE_INFINITY;
		for (String wkt : wkts) {
			Shape shape;
			try {
				shape = index.getSpatialContext(geoProperty).readShapeFromWkt(wkt);
				double dist = index.getSpatialContext(geoProperty).calcDistance(shape.getCenter(), origin);
				min = Math.min(dist, min);
			}
			catch (ParseException e) {
				// ignore
			}
		}
		return GeoUnits.fromDegrees(min, units);
	}
}
