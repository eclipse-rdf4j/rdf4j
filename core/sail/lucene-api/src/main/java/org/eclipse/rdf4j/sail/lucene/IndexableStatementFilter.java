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

import org.eclipse.rdf4j.model.Statement;

/**
 * Specifies a filter, which determines whether a statement should be included in the keyword index when performing
 * complete reindexing. See {@link LuceneSail#registerStatementFilter(IndexableStatementFilter)}.}
 *
 * @author andriy.nikolov
 */
public interface IndexableStatementFilter {

	boolean accept(Statement statement);

}
