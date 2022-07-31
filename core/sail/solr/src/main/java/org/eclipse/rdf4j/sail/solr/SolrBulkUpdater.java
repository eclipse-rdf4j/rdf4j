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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.eclipse.rdf4j.sail.lucene.BulkUpdater;
import org.eclipse.rdf4j.sail.lucene.SearchDocument;

public class SolrBulkUpdater implements BulkUpdater {

	private final SolrClient client;

	private final List<SolrInputDocument> addOrUpdateList = new ArrayList<>();

	private final List<String> deleteList = new ArrayList<>();

	public SolrBulkUpdater(SolrClient client) {
		this.client = client;
	}

	@Override
	public void add(SearchDocument doc) throws IOException {
		SolrDocument document = ((SolrSearchDocument) doc).getDocument();
		addOrUpdateList.add(SolrUtil.toSolrInputDocument(document));
	}

	@Override
	public void update(SearchDocument doc) throws IOException {
		add(doc);
	}

	@Override
	public void delete(SearchDocument doc) throws IOException {
		deleteList.add(doc.getId());
	}

	@Override
	public void end() throws IOException {
		try {
			if (!deleteList.isEmpty()) {
				client.deleteById(deleteList);
			}
			if (!addOrUpdateList.isEmpty()) {
				client.add(addOrUpdateList);
			}
		} catch (SolrServerException e) {
			throw new IOException(e);
		}
	}
}
