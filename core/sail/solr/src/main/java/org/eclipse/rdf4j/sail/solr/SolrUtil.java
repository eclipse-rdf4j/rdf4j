/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.solr;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

/**
 * Utility for Solr handling
 */
public class SolrUtil {

	/**
	 * Converts a {@link SolrDocument} to a {@link SolrInputDocument}
	 *
	 * @param solrDocument
	 * @return input document
	 */
	public static SolrInputDocument toSolrInputDocument(SolrDocument solrDocument) {

		/*
		 * Note: ClientUtils.toSolrInputDocument was removed in solr 6 Replacement found on
		 * https://stackoverflow.com/questions/38266684/
		 * substitute-of-org-apache-solr-client-solrj-util-clientutils-tosolrinputdocument
		 */

		SolrInputDocument solrInputDocument = new SolrInputDocument();

		for (String name : solrDocument.getFieldNames()) {
			solrInputDocument.addField(name, solrDocument.getFieldValue(name));
		}

		// Don't forget children documents
		if (solrDocument.getChildDocuments() != null) {
			for (SolrDocument childDocument : solrDocument.getChildDocuments()) {
				// You can add paranoic check against infinite loop childDocument == solrDocument
				solrInputDocument.addChildDocument(toSolrInputDocument(childDocument));
			}
		}
		return solrInputDocument;
	}
}
