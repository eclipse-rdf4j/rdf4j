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
package org.eclipse.rdf4j.sail.solr.client.embedded;

import java.nio.file.Path;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.core.SolrXmlConfig;
import org.eclipse.rdf4j.sail.solr.SolrClientFactory;

public class Factory implements SolrClientFactory {

	@Override
	public SolrClient create(String spec) {
		// FIXME: We need a new way of obtaining Solr home directory.
		// The following method is deprecated in version 8 and removed in version 9.
		Path solrHome = SolrResourceLoader.locateSolrHome();
		Path configFile = solrHome.resolve(SolrXmlConfig.SOLR_XML_FILE);
		return new EmbeddedSolrServer(CoreContainer.createAndLoad(solrHome, configFile), "embedded");
	}
}
