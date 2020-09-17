/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import java.text.ParseException;
import java.util.*;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.LatLonShape;
import org.apache.lucene.geo.Line;
import org.apache.lucene.geo.Polygon;
import org.apache.lucene.geo.SimpleWKTShapeParser;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.spatial4j.Geo3dSpatialContextFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.io.GeoJSONWriter;
import org.locationtech.spatial4j.io.ShapeReader;
import org.locationtech.spatial4j.io.WKTReader;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;

import com.google.common.base.Function;

public class LuceneDocument implements SearchDocument {

	private final Document doc;

	private final Function<? super String, ? extends SpatialStrategy> geoStrategyMapper;

	/**
	 * To be removed, no longer used.
	 */
	@Deprecated
	public LuceneDocument() {
		this(null);
	}

	public LuceneDocument(Function<? super String, ? extends SpatialStrategy> geoStrategyMapper) {
		this(new Document(), geoStrategyMapper);
	}

	public LuceneDocument(Document doc, Function<? super String, ? extends SpatialStrategy> geoStrategyMapper) {
		this.doc = doc;
		this.geoStrategyMapper = geoStrategyMapper;
	}

	public LuceneDocument(String id, String resourceId, String context,
			Function<? super String, ? extends SpatialStrategy> geoStrategyMapper) {
		this(geoStrategyMapper);
		setId(id);
		setResource(resourceId);
		setContext(context);
	}

	private void setId(String id) {
		LuceneIndex.addIDField(id, doc);
	}

	private void setContext(String context) {
		LuceneIndex.addContextField(context, doc);
	}

	private void setResource(String resourceId) {
		LuceneIndex.addResourceField(resourceId, doc);
	}

	public Document getDocument() {
		return doc;
	}

	@Override
	public String getId() {
		return doc.get(SearchFields.ID_FIELD_NAME);
	}

	@Override
	public String getResource() {
		return doc.get(SearchFields.URI_FIELD_NAME);
	}

	@Override
	public String getContext() {
		return doc.get(SearchFields.CONTEXT_FIELD_NAME);
	}

	@Override
	public Set<String> getPropertyNames() {
		List<IndexableField> fields = doc.getFields();
		Set<String> names = new HashSet<>();
		for (IndexableField field : fields) {
			String name = field.name();
			if (SearchFields.isPropertyField(name)) {
				names.add(name);
			}
		}
		return names;
	}

	@Override
	public void addProperty(String name) {
		// don't need to do anything
	}

	/**
	 * Stores and indexes a property in a Document. We don't have to recalculate the concatenated text: just add another
	 * TEXT field and Lucene will take care of this. Additional advantage: Lucene may be able to handle the invididual
	 * strings in a way that may affect e.g. phrase and proximity searches (concatenation basically means loss of
	 * information). NOTE: The TEXT_FIELD_NAME has to be stored, see in LuceneSail
	 *
	 * @see LuceneSail
	 */
	@Override
	public void addProperty(String name, String text) {
		LuceneIndex.addPredicateField(name, text, doc);
		LuceneIndex.addTextField(text, doc);
	}

	/**
	 * Checks whether a field occurs with a specified value in a Document.
	 */
	@Override
	public boolean hasProperty(String fieldName, String value) {
		String[] fields = doc.getValues(fieldName);
		if (fields != null) {
			for (String field : fields) {
				if (value.equals(field)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public List<String> getProperty(String name) {
		return Arrays.asList(doc.getValues(name));
	}

	@Override
	public void addGeoProperty(String field, String value) {
		LuceneIndex.addStoredOnlyPredicateField(field, value, doc);
		try {
			String wkt = value;
			// wkt = wkt.replace("\"", "").replace("^^<http://www.opengis.net/ont/geosparql#wktLiteral>", "");
			// System.out.println(wkt);
			Object shape = SimpleWKTShapeParser.parse(wkt);
			if (shape instanceof Polygon[]) {
				for (Polygon p : (Polygon[]) shape) {
					for (Field f : LatLonShape.createIndexableFields(field, p)) {
						doc.add(f);
					}
				}
			} else if (shape instanceof Polygon) {
				for (Field f : LatLonShape.createIndexableFields(field, (Polygon) shape)) {
					doc.add(f);
				}
			} else if (shape instanceof Line) {
				for (Field f : LatLonShape.createIndexableFields(field, (Line) shape)) {
					doc.add(f);
				}
			} else if (shape instanceof double[]) {
				double point[] = (double[]) shape;
				// System.out.println(point[0]+" "+point[1]);
				doc.add(new LatLonPoint(field, point[1], point[0]));
			} else {
				throw new IllegalArgumentException("Geometry is not supported");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
