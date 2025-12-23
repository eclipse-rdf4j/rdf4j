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

import org.apache.solr.client.solrj.SolrClient;

/**
 * @deprecated since 5.3.0. Solr integration is deprecated for removal; use alternative Lucene-backed search
 *             implementations instead.
 */
@Deprecated(since = "5.3.0", forRemoval = true)
public interface SolrClientFactory {

	SolrClient create(String spec);
}
