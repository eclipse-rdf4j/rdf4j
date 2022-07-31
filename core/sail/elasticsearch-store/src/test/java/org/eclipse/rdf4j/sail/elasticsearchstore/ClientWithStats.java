/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.elasticsearchstore;

import java.util.Map;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.explain.ExplainRequest;
import org.elasticsearch.action.explain.ExplainRequestBuilder;
import org.elasticsearch.action.explain.ExplainResponse;
import org.elasticsearch.action.fieldcaps.FieldCapabilitiesRequest;
import org.elasticsearch.action.fieldcaps.FieldCapabilitiesRequestBuilder;
import org.elasticsearch.action.fieldcaps.FieldCapabilitiesResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollRequestBuilder;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.termvectors.MultiTermVectorsRequest;
import org.elasticsearch.action.termvectors.MultiTermVectorsRequestBuilder;
import org.elasticsearch.action.termvectors.MultiTermVectorsResponse;
import org.elasticsearch.action.termvectors.TermVectorsRequest;
import org.elasticsearch.action.termvectors.TermVectorsRequestBuilder;
import org.elasticsearch.action.termvectors.TermVectorsResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;

public class ClientWithStats implements Client {

	Client wrapped;

	public ClientWithStats(Client wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public AdminClient admin() {
		return wrapped.admin();
	}

	@Override
	public ActionFuture<IndexResponse> index(IndexRequest request) {
		return wrapped.index(request);
	}

	@Override
	public void index(IndexRequest request, ActionListener<IndexResponse> listener) {
		wrapped.index(request, listener);
	}

	@Override
	public IndexRequestBuilder prepareIndex() {
		return wrapped.prepareIndex();
	}

	@Override
	public ActionFuture<UpdateResponse> update(UpdateRequest request) {
		return wrapped.update(request);
	}

	@Override
	public void update(UpdateRequest request, ActionListener<UpdateResponse> listener) {
		wrapped.update(request, listener);
	}

	@Override
	public UpdateRequestBuilder prepareUpdate() {
		return wrapped.prepareUpdate();
	}

	@Override
	public UpdateRequestBuilder prepareUpdate(String index, String type, String id) {
		return wrapped.prepareUpdate(index, type, id);
	}

	@Override
	public IndexRequestBuilder prepareIndex(String index, String type) {
		return wrapped.prepareIndex(index, type);
	}

	@Override
	public IndexRequestBuilder prepareIndex(String index, String type, String id) {
		return wrapped.prepareIndex(index, type, id);
	}

	@Override
	public ActionFuture<DeleteResponse> delete(DeleteRequest request) {
		return wrapped.delete(request);
	}

	@Override
	public void delete(DeleteRequest request, ActionListener<DeleteResponse> listener) {
		wrapped.delete(request, listener);
	}

	@Override
	public DeleteRequestBuilder prepareDelete() {
		return wrapped.prepareDelete();
	}

	@Override
	public DeleteRequestBuilder prepareDelete(String index, String type, String id) {
		return wrapped.prepareDelete(index, type, id);
	}

	long bulkCalls;

	@Override
	public ActionFuture<BulkResponse> bulk(BulkRequest request) {
		bulkCalls++;
		return wrapped.bulk(request);
	}

	@Override
	public void bulk(BulkRequest request, ActionListener<BulkResponse> listener) {
		bulkCalls++;
		wrapped.bulk(request, listener);
	}

	@Override
	public BulkRequestBuilder prepareBulk() {
		bulkCalls++;
		return wrapped.prepareBulk();
	}

	@Override
	public BulkRequestBuilder prepareBulk(String globalIndex, String globalType) {
		bulkCalls++;
		return wrapped.prepareBulk(globalIndex, globalType);
	}

	@Override
	public ActionFuture<GetResponse> get(GetRequest request) {
		return wrapped.get(request);
	}

	@Override
	public void get(GetRequest request, ActionListener<GetResponse> listener) {
		wrapped.get(request, listener);
	}

	@Override
	public GetRequestBuilder prepareGet() {
		return wrapped.prepareGet();
	}

	@Override
	public GetRequestBuilder prepareGet(String index, String type, String id) {
		return wrapped.prepareGet(index, type, id);
	}

	@Override
	public ActionFuture<MultiGetResponse> multiGet(MultiGetRequest request) {
		return wrapped.multiGet(request);
	}

	@Override
	public void multiGet(MultiGetRequest request, ActionListener<MultiGetResponse> listener) {
		wrapped.multiGet(request, listener);
	}

	@Override
	public MultiGetRequestBuilder prepareMultiGet() {
		return wrapped.prepareMultiGet();
	}

	@Override
	public ActionFuture<SearchResponse> search(SearchRequest request) {
		return wrapped.search(request);
	}

	@Override
	public void search(SearchRequest request, ActionListener<SearchResponse> listener) {
		wrapped.search(request, listener);
	}

	@Override
	public SearchRequestBuilder prepareSearch(String... indices) {
		return wrapped.prepareSearch(indices);
	}

	@Override
	public ActionFuture<SearchResponse> searchScroll(SearchScrollRequest request) {
		return wrapped.searchScroll(request);
	}

	@Override
	public void searchScroll(SearchScrollRequest request, ActionListener<SearchResponse> listener) {
		wrapped.searchScroll(request, listener);
	}

	@Override
	public SearchScrollRequestBuilder prepareSearchScroll(String scrollId) {
		return wrapped.prepareSearchScroll(scrollId);
	}

	@Override
	public ActionFuture<MultiSearchResponse> multiSearch(MultiSearchRequest request) {
		return wrapped.multiSearch(request);
	}

	@Override
	public void multiSearch(MultiSearchRequest request, ActionListener<MultiSearchResponse> listener) {
		wrapped.multiSearch(request, listener);
	}

	@Override
	public MultiSearchRequestBuilder prepareMultiSearch() {
		return wrapped.prepareMultiSearch();
	}

	@Override
	public ActionFuture<TermVectorsResponse> termVectors(TermVectorsRequest request) {
		return wrapped.termVectors(request);
	}

	@Override
	public void termVectors(TermVectorsRequest request, ActionListener<TermVectorsResponse> listener) {
		wrapped.termVectors(request, listener);
	}

	@Override
	public TermVectorsRequestBuilder prepareTermVectors() {
		return wrapped.prepareTermVectors();
	}

	@Override
	public TermVectorsRequestBuilder prepareTermVectors(String index, String type, String id) {
		return wrapped.prepareTermVectors(index, type, id);
	}

	@Override
	public ActionFuture<MultiTermVectorsResponse> multiTermVectors(MultiTermVectorsRequest request) {
		return wrapped.multiTermVectors(request);
	}

	@Override
	public void multiTermVectors(MultiTermVectorsRequest request, ActionListener<MultiTermVectorsResponse> listener) {
		wrapped.multiTermVectors(request, listener);
	}

	@Override
	public MultiTermVectorsRequestBuilder prepareMultiTermVectors() {
		return wrapped.prepareMultiTermVectors();
	}

	@Override
	public ExplainRequestBuilder prepareExplain(String index, String type, String id) {
		return wrapped.prepareExplain(index, type, id);
	}

	@Override
	public ActionFuture<ExplainResponse> explain(ExplainRequest request) {
		return wrapped.explain(request);
	}

	@Override
	public void explain(ExplainRequest request, ActionListener<ExplainResponse> listener) {
		wrapped.explain(request, listener);
	}

	@Override
	public ClearScrollRequestBuilder prepareClearScroll() {
		return wrapped.prepareClearScroll();
	}

	@Override
	public ActionFuture<ClearScrollResponse> clearScroll(ClearScrollRequest request) {
		return wrapped.clearScroll(request);
	}

	@Override
	public void clearScroll(ClearScrollRequest request, ActionListener<ClearScrollResponse> listener) {
		wrapped.clearScroll(request, listener);
	}

	@Override
	public FieldCapabilitiesRequestBuilder prepareFieldCaps(String... indices) {
		return wrapped.prepareFieldCaps(indices);
	}

	@Override
	public ActionFuture<FieldCapabilitiesResponse> fieldCaps(FieldCapabilitiesRequest request) {
		return wrapped.fieldCaps(request);
	}

	@Override
	public void fieldCaps(FieldCapabilitiesRequest request, ActionListener<FieldCapabilitiesResponse> listener) {
		wrapped.fieldCaps(request, listener);
	}

	@Override
	public Settings settings() {
		return wrapped.settings();
	}

	@Override
	public Client filterWithHeader(Map<String, String> headers) {
		return wrapped.filterWithHeader(headers);
	}

	@Override
	public Client getRemoteClusterClient(String clusterAlias) {
		return wrapped.getRemoteClusterClient(clusterAlias);
	}

	@Override
	public <Request extends ActionRequest, Response extends ActionResponse> ActionFuture<Response> execute(
			ActionType<Response> action, Request request) {
		return wrapped.execute(action, request);
	}

	@Override
	public <Request extends ActionRequest, Response extends ActionResponse> void execute(ActionType<Response> action,
			Request request, ActionListener<Response> listener) {
		wrapped.execute(action, request, listener);
	}

	@Override
	public ThreadPool threadPool() {
		return wrapped.threadPool();
	}

	@Override
	public void close() {
		wrapped.close();
	}

	public long getBulkCalls() {
		return bulkCalls;
	}
}
