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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.eclipse.rdf4j.query.algebra.Var;

/**
 * Textual IR for a SELECT query (header + WHERE + trailing modifiers).
 *
 * The WHERE body is an {@link IrBGP}. Header sections keep rendered expressions as text to preserve the exact surface
 * form chosen by the renderer.
 */
public class IrSelect extends IrNode {
	private final List<IrProjectionItem> projection = new ArrayList<>();
	private final List<IrGroupByElem> groupBy = new ArrayList<>();
	private final List<String> having = new ArrayList<>();
	private final List<IrOrderSpec> orderBy = new ArrayList<>();
	private boolean distinct;
	private boolean reduced;
	private IrBGP where;
	private long limit = -1;
	private long offset = -1;

	public IrSelect(boolean newScope) {
		super(newScope);
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public void setReduced(boolean reduced) {
		this.reduced = reduced;
	}

	public List<IrProjectionItem> getProjection() {
		return projection;
	}

	public IrBGP getWhere() {
		return where;
	}

	public void setWhere(IrBGP bgp) {
		this.where = bgp;
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

	@Override
	public IrNode transformChildren(UnaryOperator<IrNode> op) {
		IrBGP newWhere = this.where;
		if (newWhere != null) {
			IrNode t = op.apply(newWhere);
			if (t instanceof IrBGP) {
				newWhere = (IrBGP) t;
			}
		}
		IrSelect copy = new IrSelect(this.isNewScope());
		copy.setDistinct(this.distinct);
		copy.setReduced(this.reduced);
		copy.getProjection().addAll(this.projection);
		copy.setWhere(newWhere);
		copy.getGroupBy().addAll(this.groupBy);
		copy.getHaving().addAll(this.having);
		copy.getOrderBy().addAll(this.orderBy);
		copy.setLimit(this.limit);
		copy.setOffset(this.offset);
		return copy;
	}

	@Override
	public void print(IrPrinter p) {
		// SELECT header (keep WHERE on the same line for canonical formatting)
		StringBuilder hdr = new StringBuilder(64);
		hdr.append("SELECT ");
		if (distinct) {
			hdr.append("DISTINCT ");
		} else if (reduced) {
			hdr.append("REDUCED ");
		}
		if (projection.isEmpty()) {
			hdr.append("*");
		} else {
			for (int i = 0; i < projection.size(); i++) {
				IrProjectionItem it = projection.get(i);
				if (it.getExprText() == null) {
					hdr.append('?').append(it.getVarName());
				} else {
					hdr.append('(').append(it.getExprText()).append(" AS ?").append(it.getVarName()).append(')');
				}
				if (i + 1 < projection.size()) {
					hdr.append(' ');
				}
			}
		}
		p.startLine();
		p.append(hdr.toString());
		p.append(" WHERE ");

		// WHERE
		if (where != null) {
			where.print(p);
		} else {
			p.openBlock();
			p.closeBlock();
		}

		// GROUP BY
		if (!groupBy.isEmpty()) {
			StringBuilder gb = new StringBuilder("GROUP BY");
			for (IrGroupByElem g : groupBy) {
				if (g.getExprText() == null) {
					gb.append(' ').append('?').append(g.getVarName());
				} else {
					gb.append(" (").append(g.getExprText()).append(" AS ?").append(g.getVarName()).append(")");
				}
			}
			p.line(gb.toString());
		}

		// HAVING
		if (!having.isEmpty()) {
			StringBuilder hv = new StringBuilder("HAVING");
			for (String cond : having) {
				String t = cond == null ? "" : cond.trim();
				// Add parentheses when not already a single wrapped expression
				if (!t.isEmpty() && !(t.startsWith("(") && t.endsWith(")"))) {
					t = "(" + t + ")";
				}
				hv.append(' ').append(t);
			}
			p.line(hv.toString());
		}

		// ORDER BY
		if (!orderBy.isEmpty()) {
			StringBuilder ob = new StringBuilder("ORDER BY");
			for (IrOrderSpec o : orderBy) {
				if (o.isAscending()) {
					ob.append(' ').append(o.getExprText());
				} else {
					ob.append(" DESC(").append(o.getExprText()).append(')');
				}
			}
			p.line(ob.toString());
		}

		// LIMIT / OFFSET
		if (limit >= 0) {
			p.line("LIMIT " + limit);
		}
		if (offset >= 0) {
			p.line("OFFSET " + offset);
		}
	}

	@Override
	public Set<Var> getVars() {
		if (where != null) {
			return where.getVars();
		}
		return Collections.emptySet();
	}

}
