/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene.util;

import static org.eclipse.rdf4j.sail.lucene.LuceneSail.DEFAULT_INDEX_CLASS;
import static org.eclipse.rdf4j.sail.lucene.LuceneSail.INDEX_CLASS_KEY;

import java.util.Properties;

import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.SearchIndex;

/**
 * This is utility class with tool useful for manipulation on the {@link SearchIndex}.
 *
 * @author jacek grzebyta
 * @version 2.3
 */
public class SearchIndexUtils {

	/**
	 * The method creates instance of {@link SearchIndex}. The type of instantiated class depends on the value of
	 * {@link LuceneSail#INDEX_CLASS_KEY} parameter. By default it is
	 * <code>org.eclipse.rdf4j.sail.lucene.LuceneIndex</code>.
	 *
	 * @param parameters
	 * @return search index
	 * @throws Exception
	 */
	public static SearchIndex createSearchIndex(Properties parameters) throws Exception {
		String indexClassName = parameters.getProperty(INDEX_CLASS_KEY, DEFAULT_INDEX_CLASS);
		SearchIndex index = (SearchIndex) Class.forName(indexClassName).newInstance();
		index.initialize(parameters);
		return index;
	}
}
