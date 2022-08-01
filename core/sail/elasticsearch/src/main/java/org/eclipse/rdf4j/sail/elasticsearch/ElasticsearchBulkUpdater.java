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

import java.io.IOException;

import org.eclipse.rdf4j.sail.lucene.BulkUpdater;
import org.eclipse.rdf4j.sail.lucene.SearchDocument;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;

public class ElasticsearchBulkUpdater implements BulkUpdater {

	private final Client client;

	private final BulkRequestBuilder bulkRequest;

	public ElasticsearchBulkUpdater(Client client) {
		this.client = client;
		this.bulkRequest = client.prepareBulk();
	}

	@Override
	public void add(SearchDocument doc) throws IOException {
		ElasticsearchDocument esDoc = (ElasticsearchDocument) doc;
		bulkRequest.add(
				client.prepareIndex(esDoc.getIndex(), esDoc.getType(), esDoc.getId()).setSource(esDoc.getSource()));
	}

	@Override
	public void update(SearchDocument doc) throws IOException {
		ElasticsearchDocument esDoc = (ElasticsearchDocument) doc;
		bulkRequest.add(client.prepareUpdate(esDoc.getIndex(), esDoc.getType(), esDoc.getId())
				.setIfSeqNo(esDoc.getSeqNo())
				.setIfPrimaryTerm(esDoc.getPrimaryTerm())
				.setDoc(esDoc.getSource()));
	}

	@Override
	public void delete(SearchDocument doc) throws IOException {
		ElasticsearchDocument esDoc = (ElasticsearchDocument) doc;
		bulkRequest.add(
				client.prepareDelete(esDoc.getIndex(), esDoc.getType(), esDoc.getId())
						.setIfSeqNo(esDoc.getSeqNo())
						.setIfPrimaryTerm(esDoc.getPrimaryTerm()));
	}

	@Override
	public void end() throws IOException {
		if (bulkRequest.numberOfActions() > 0) {
			BulkResponse response = bulkRequest.execute().actionGet();
			if (response.hasFailures()) {
				throw new IOException(response.buildFailureMessage());
			}
		}
	}
}
