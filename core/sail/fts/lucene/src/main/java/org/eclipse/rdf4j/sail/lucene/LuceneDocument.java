/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.spatial.SpatialStrategy;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.SearchDocument;
import org.eclipse.rdf4j.sail.lucene.SearchFields;

import com.google.common.base.Function;
import com.spatial4j.core.shape.Shape;

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

	public LuceneDocument(String id, String resourceId, String context, Function<? super String, ? extends SpatialStrategy> geoStrategyMapper)
	{
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
		Set<String> names = new HashSet<String>();
		for (IndexableField field : fields) {
			String name = field.name();
			if (SearchFields.isPropertyField(name))
				names.add(name);
		}
		return names;
	}

	@Override
	public void addProperty(String name) {
		// don't need to do anything
	}

	/**
	 * Stores and indexes a property in a Document. We don't have to recalculate
	 * the concatenated text: just add another TEXT field and Lucene will take
	 * care of this. Additional advantage: Lucene may be able to handle the
	 * invididual strings in a way that may affect e.g. phrase and proximity
	 * searches (concatenation basically means loss of information). NOTE: The
	 * TEXT_FIELD_NAME has to be stored, see in LuceneSail
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
			SpatialStrategy geoStrategy = geoStrategyMapper.apply(field);
			Shape shape = geoStrategy.getSpatialContext().readShapeFromWkt(value);
			for (IndexableField f : geoStrategy.createIndexableFields(shape)) {
				doc.add(f);
			}
		}
		catch (ParseException e) {
			// ignore
		}
	}
}
