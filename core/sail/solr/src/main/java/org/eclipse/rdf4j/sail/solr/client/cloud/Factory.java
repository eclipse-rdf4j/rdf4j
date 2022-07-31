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
package org.eclipse.rdf4j.sail.solr.client.cloud;

import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.eclipse.rdf4j.sail.solr.SolrClientFactory;

import com.google.common.collect.Lists;

public class Factory implements SolrClientFactory {

	@Override
	public SolrClient create(String spec) {
		List<String> zkHosts = Lists.newArrayList(spec.substring("cloud:".length()));
		return new CloudSolrClient.Builder().withZkHost(zkHosts).build();
	}
}
