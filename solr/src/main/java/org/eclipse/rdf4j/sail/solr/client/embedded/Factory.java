/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.solr.client.embedded;

import java.io.File;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.core.SolrXmlConfig;
import org.eclipse.rdf4j.sail.solr.SolrClientFactory;

public class Factory implements SolrClientFactory {

	@Override
	public SolrClient create(String spec) {
		String solrHome = SolrResourceLoader.locateSolrHome();
		File configFile = new File(solrHome, SolrXmlConfig.SOLR_XML_FILE);
		return new EmbeddedSolrServer(CoreContainer.createAndLoad(solrHome, configFile), "embedded");
	}
}
