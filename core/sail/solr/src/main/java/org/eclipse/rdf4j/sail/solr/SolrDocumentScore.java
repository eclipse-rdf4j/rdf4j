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

import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.sail.lucene.DocumentScore;

public class SolrDocumentScore extends SolrDocumentResult implements DocumentScore {

	private final Map<String, List<String>> highlighting;

	public SolrDocumentScore(SolrSearchDocument doc, Map<String, List<String>> highlighting) {
		super(doc);
		this.highlighting = highlighting;
	}

	@Override
	public float getScore() {
		Number s = ((Number) doc.getDocument().get("score"));
		return (s != null) ? s.floatValue() : 0.0f;
	}

	@Override
	public boolean isHighlighted() {
		return (highlighting != null);
	}

	@Override
	public Iterable<String> getSnippets(String field) {
		return highlighting.get(field);
	}
}
