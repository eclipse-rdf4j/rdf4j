/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core.query;

import org.eclipse.rdf4j.sparqlbuilder.core.QueryElement;

abstract class GraphManagementQuery<T extends GraphManagementQuery<T>> implements QueryElement {
	private static final String SILENT = "SILENT";

	private boolean silent = false;

	GraphManagementQuery() {
	}

	/**
	 * Set the <code>SILENT</code> option to true on this query
	 *
	 * @return this query instance
	 */
	public T silent() {
		return silent(true);
	}

	/**
	 * Specify if the <code>SILENT</code> option should be on for this query
	 *
	 * @param isSilent if this should be a SILENT operation or not
	 *
	 * @return this query instance
	 */
	@SuppressWarnings("unchecked")
	public T silent(boolean isSilent) {
		this.silent = isSilent;

		return (T) this;
	}

	protected void appendSilent(StringBuilder builder) {
		if (silent) {
			builder.append(SILENT).append(" ");
		}
	}
}
