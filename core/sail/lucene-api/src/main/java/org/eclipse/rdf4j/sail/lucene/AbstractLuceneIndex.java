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

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractLuceneIndex extends AbstractSearchIndex {

	/**
	 * keep a lit of old monitors that are still iterating but not closed (open iterators), will be all closed on
	 * shutdown items are removed from list by ReaderMnitor.endReading() when closing
	 */
	protected final Collection<AbstractReaderMonitor> oldmonitors = new ArrayList<>();

	protected abstract AbstractReaderMonitor getCurrentMonitor();

	public Collection<AbstractReaderMonitor> getOldMonitors() {
		return oldmonitors;
	}
}
