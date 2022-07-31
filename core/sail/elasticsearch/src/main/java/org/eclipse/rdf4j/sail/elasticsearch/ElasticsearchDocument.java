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
package org.eclipse.rdf4j.sail.elasticsearch;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.sail.lucene.SearchDocument;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.index.seqno.SequenceNumbers;
import org.elasticsearch.search.SearchHit;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;

import com.google.common.base.Function;

public class ElasticsearchDocument implements SearchDocument {

	private final String id;

	private final String type;

	@Deprecated
	private long version;

	private final long seqNo;
	private final long primaryTerm;

	private final String index;

	private final Map<String, Object> fields;

	private final Function<? super String, ? extends SpatialContext> geoContextMapper;

	@Deprecated
	public ElasticsearchDocument(SearchHit hit) {
		this(hit, null);
	}

	public ElasticsearchDocument(SearchHit hit, Function<? super String, ? extends SpatialContext> geoContextMapper) {
		this(hit.getId(), hit.getType(), hit.getIndex(), hit.getSeqNo(), hit.getPrimaryTerm(),
				hit.getSourceAsMap(), geoContextMapper);
	}

	public ElasticsearchDocument(String id, String type, String index, String resourceId, String context,
			Function<? super String, ? extends SpatialContext> geoContextMapper) {
		this(id, type, index, SequenceNumbers.UNASSIGNED_SEQ_NO, SequenceNumbers.UNASSIGNED_PRIMARY_TERM,
				new HashMap<>(), geoContextMapper);
		fields.put(SearchFields.URI_FIELD_NAME, resourceId);
		if (context != null) {
			fields.put(SearchFields.CONTEXT_FIELD_NAME, context);
		}
	}

	@Deprecated
	public ElasticsearchDocument(String id, String type, String index, long version, Map<String, Object> fields,
			Function<? super String, ? extends SpatialContext> geoContextMapper) {
		this(id, type, index, SequenceNumbers.UNASSIGNED_SEQ_NO, SequenceNumbers.UNASSIGNED_PRIMARY_TERM,
				new HashMap<>(), geoContextMapper);
		this.version = version;
	}

	public ElasticsearchDocument(String id, String type, String index, long seqNo, long primaryTerm,
			Map<String, Object> fields, Function<? super String, ? extends SpatialContext> geoContextMapper) {
		this.id = id;
		this.type = type;
		this.version = Versions.MATCH_ANY;
		this.seqNo = seqNo;
		this.primaryTerm = primaryTerm;
		this.index = index;
		this.fields = fields;
		this.geoContextMapper = geoContextMapper;
	}

	@Override
	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	@Deprecated
	public long getVersion() {
		return version;
	}

	public long getSeqNo() {
		return seqNo;
	}

	public long getPrimaryTerm() {
		return primaryTerm;
	}

	public String getIndex() {
		return index;
	}

	public Map<String, Object> getSource() {
		return fields;
	}

	@Override
	public String getResource() {
		return (String) fields.get(SearchFields.URI_FIELD_NAME);
	}

	@Override
	public String getContext() {
		return (String) fields.get(SearchFields.CONTEXT_FIELD_NAME);
	}

	@Override
	public Set<String> getPropertyNames() {
		Set<String> propertyFields = ElasticsearchIndex.getPropertyFields(fields.keySet());
		Set<String> propertyNames = new HashSet<>(propertyFields.size() + 1);
		for (String f : propertyFields) {
			propertyNames.add(ElasticsearchIndex.toPropertyName(f));
		}
		return propertyNames;
	}

	@Override
	public void addProperty(String name) {
		String fieldName = ElasticsearchIndex.toPropertyFieldName(name);
		// in elastic search, fields must have an explicit value
		if (fields.containsKey(fieldName)) {
			throw new IllegalStateException("Property already added: " + name);
		}
		fields.put(fieldName, null);
		if (!fields.containsKey(SearchFields.TEXT_FIELD_NAME)) {
			fields.put(SearchFields.TEXT_FIELD_NAME, null);
		}
	}

	@Override
	public void addProperty(String name, String text) {
		String fieldName = ElasticsearchIndex.toPropertyFieldName(name);
		addField(fieldName, text, fields);
		addField(SearchFields.TEXT_FIELD_NAME, text, fields);
	}

	@Override
	public void addGeoProperty(String name, String text) {
		String fieldName = ElasticsearchIndex.toPropertyFieldName(name);
		addField(fieldName, text, fields);
		try {
			Shape shape = geoContextMapper.apply(name).readShapeFromWkt(text);
			if (shape instanceof Point) {
				Point p = (Point) shape;
				fields.put(ElasticsearchIndex.toGeoPointFieldName(name), new GeoPoint(p.getY(), p.getX()).getGeohash());
			} else {
				fields.put(ElasticsearchIndex.toGeoShapeFieldName(name),
						ElasticsearchSpatialSupport.getSpatialSupport().toGeoJSON(shape));
			}
		} catch (ParseException e) {
			// ignore
		}
	}

	@Override
	public boolean hasProperty(String name, String value) {
		String fieldName = ElasticsearchIndex.toPropertyFieldName(name);
		List<String> fieldValues = asStringList(fields.get(fieldName));
		if (fieldValues != null) {
			for (String fieldValue : fieldValues) {
				if (value.equals(fieldValue)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public List<String> getProperty(String name) {
		String fieldName = ElasticsearchIndex.toPropertyFieldName(name);
		return asStringList(fields.get(fieldName));
	}

	private static void addField(String name, String value, Map<String, Object> document) {
		Object oldValue = document.get(name);
		Object newValue;
		if (oldValue != null) {
			List<String> newList = makeModifiable(asStringList(oldValue));
			newList.add(value);
			newValue = newList;
		} else {
			newValue = value;
		}
		document.put(name, newValue);
	}

	private static List<String> makeModifiable(List<String> l) {
		List<String> modList;
		if (!(l instanceof ArrayList<?>)) {
			modList = new ArrayList<>(l.size() + 1);
			modList.addAll(l);
		} else {
			modList = l;
		}
		return modList;
	}

	@SuppressWarnings("unchecked")
	private static List<String> asStringList(Object value) {
		List<String> l;
		if (value == null) {
			l = null;
		} else if (value instanceof List<?>) {
			l = (List<String>) value;
		} else {
			l = Collections.singletonList((String) value);
		}
		return l;
	}
}
