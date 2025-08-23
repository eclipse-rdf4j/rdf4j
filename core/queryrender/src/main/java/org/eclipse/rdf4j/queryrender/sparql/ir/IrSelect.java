/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.ir;

import java.util.ArrayList;
import java.util.List;

/**
 * Textual IR for a SELECT query.
 */
public class IrSelect extends IrNode {
	private boolean distinct;
	private boolean reduced;
	private final List<IrProjectionItem> projection = new ArrayList<>();
	private IrWhere where;
	private final List<IrGroupByElem> groupBy = new ArrayList<>();
	private final List<String> having = new ArrayList<>();
	private final List<IrOrderSpec> orderBy = new ArrayList<>();
	private long limit = -1;
	private long offset = -1;

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public boolean isReduced() {
		return reduced;
	}

	public void setReduced(boolean reduced) {
		this.reduced = reduced;
	}

	public List<IrProjectionItem> getProjection() {
		return projection;
	}

	public IrWhere getWhere() {
		return where;
	}

	public void setWhere(IrWhere where) {
		this.where = where;
	}

	public List<IrGroupByElem> getGroupBy() {
		return groupBy;
	}

	public List<String> getHaving() {
		return having;
	}

	public List<IrOrderSpec> getOrderBy() {
		return orderBy;
	}

	public long getLimit() {
		return limit;
	}

	public void setLimit(long limit) {
		this.limit = limit;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}
}
