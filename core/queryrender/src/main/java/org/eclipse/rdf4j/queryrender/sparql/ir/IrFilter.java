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

import java.util.function.UnaryOperator;

/**
 * Textual IR node for a FILTER line.
 *
 * Two forms are supported: - Plain condition text: {@code FILTER (<text>)} where text is already rendered by the
 * renderer. - Structured bodies: {@link IrExists} and {@link IrNot}({@link IrExists}) to support EXISTS/NOT EXISTS
 * blocks with a nested {@link IrBGP}. Unknown structured bodies are emitted as a comment to avoid silent misrendering.
 */
public class IrFilter extends IrNode {
	private final String conditionText;
	// Optional structured body (e.g., EXISTS { ... } or NOT EXISTS { ... })
	private final IrNode body;

	public IrFilter(String conditionText) {
		this.conditionText = conditionText;
		this.body = null;
	}

	public IrFilter(IrNode body) {
		this.conditionText = null;
		this.body = body;
	}

	public String getConditionText() {
		return conditionText;
	}

	public IrNode getBody() {
		return body;
	}

	@Override
	public void print(IrPrinter p) {
		if (body == null) {
			p.line("FILTER (" + conditionText + ")");
			return;
		}

		// Structured bodies: EXISTS { ... } and NOT EXISTS { ... }
		if (body instanceof IrExists) {
			IrExists ex = (IrExists) body;
			printExists(p, false, ex.getWhere(), ex.isNewScope());
			return;
		}
		if (body instanceof IrNot) {
			IrNot n = (IrNot) body;
			IrNode inner = n.getInner();
			if (inner instanceof IrExists) {
				IrExists ex = (IrExists) inner;
				printExists(p, true, ex.getWhere(), ex.isNewScope());
				return;
			}
		}

		// Fallback: print the inner as raw text if it is IrText
		if (body instanceof IrText) {
			p.line("FILTER (" + ((IrText) body).getText() + ")");
			return;
		}
		// Unknown body type: just print a comment
		p.line("# unsupported FILTER body: " + body.getClass().getSimpleName());
	}

	private void printExists(IrPrinter p, boolean negated, IrBGP where, boolean wrapByScope) {
		// Match expected style: no extra parentheses around EXISTS/NOT EXISTS
		String head = negated ? "FILTER NOT EXISTS {" : "FILTER EXISTS {";
		p.line(head);
		p.pushIndent();
		if (where != null) {
			final boolean wrapInner = wrapByScope || where.isNewScope();
			if (wrapInner) {
				p.openBlock();
			}
			p.printLines(where.getLines());
			if (wrapInner) {
				p.closeBlock();
			}
		}
		p.popIndent();
		p.line("}");
	}

	@Override
	public IrNode transformChildren(UnaryOperator<IrNode> op) {
		if (body == null) {
			return this;
		}
		// Transform nested BGP inside EXISTS (possibly under NOT)
		if (body instanceof IrExists) {
			IrExists ex = (IrExists) body;
			IrBGP inner = ex.getWhere();
			if (inner != null) {
				IrNode t = op.apply(inner);
				t = t.transformChildren(op);
				if (t instanceof IrBGP) {
					inner = (IrBGP) t;
				}
			}
			IrExists ex2 = new IrExists(inner, ex.isNewScope());
			ex2.setNewScope(ex.isNewScope());
			return new IrFilter(ex2);
		}
		if (body instanceof IrNot) {
			IrNot n = (IrNot) body;
			IrNode innerNode = n.getInner();
			if (innerNode instanceof IrExists) {
				IrExists ex = (IrExists) innerNode;
				IrBGP inner = ex.getWhere();
				if (inner != null) {
					IrNode t = op.apply(inner);
					t = t.transformChildren(op);
					if (t instanceof IrBGP) {
						inner = (IrBGP) t;
					}
				}
				IrExists ex2 = new IrExists(inner, ex.isNewScope());
				ex2.setNewScope(ex.isNewScope());
				return new IrFilter(new IrNot(ex2));
			}
			// Unknown NOT inner: keep as-is
			return new IrFilter(new IrNot(innerNode));
		}
		return this;
	}
}
