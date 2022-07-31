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
package org.eclipse.rdf4j.sail.solr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.eclipse.rdf4j.sail.lucene.SearchDocument;
import org.eclipse.rdf4j.sail.lucene.SearchFields;

public class SolrSearchDocument implements SearchDocument {

	private final SolrDocument doc;

	public SolrSearchDocument() {
		this(new SolrDocument());
	}

	public SolrSearchDocument(SolrDocument doc) {
		this.doc = doc;
	}

	public SolrSearchDocument(String id, String resourceId, String context) {
		this();
		doc.put(SearchFields.ID_FIELD_NAME, id);
		doc.put(SearchFields.URI_FIELD_NAME, resourceId);
		if (context != null) {
			doc.put(SearchFields.CONTEXT_FIELD_NAME, context);
		}
	}

	public SolrDocument getDocument() {
		return doc;
	}

	@Override
	public String getId() {
		return (String) doc.get(SearchFields.ID_FIELD_NAME);
	}

	@Override
	public String getResource() {
		return (String) doc.get(SearchFields.URI_FIELD_NAME);
	}

	@Override
	public String getContext() {
		return (String) doc.get(SearchFields.CONTEXT_FIELD_NAME);
	}

	@Override
	public Set<String> getPropertyNames() {
		return SolrIndex.getPropertyFields(doc.keySet());
	}

	@Override
	public void addProperty(String name) {
		// don't need to do anything
	}

	@Override
	public void addProperty(String name, String text) {
		addField(name, text, doc);
		addField(SearchFields.TEXT_FIELD_NAME, text, doc);
	}

	@Override
	public void addGeoProperty(String name, String text) {
		addField(name, text, doc);
	}

	@Override
	public boolean hasProperty(String name, String value) {
		List<String> fieldValues = asStringList(doc.get(name));
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
		return asStringList(doc.get(name));
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
