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
package org.eclipse.rdf4j.sail.lucene;

import java.io.IOException;

/**
 * A bulk updater that updates documents one-by-one.
 */
public class SimpleBulkUpdater implements BulkUpdater {

	private final AbstractSearchIndex index;

	public SimpleBulkUpdater(AbstractSearchIndex index) {
		this.index = index;
	}

	@Override
	public void add(SearchDocument doc) throws IOException {
		index.addDocument(doc);
	}

	@Override
	public void update(SearchDocument doc) throws IOException {
		index.updateDocument(doc);
	}

	@Override
	public void delete(SearchDocument doc) throws IOException {
		index.deleteDocument(doc);
	}

	@Override
	public void end() {
	}

}
