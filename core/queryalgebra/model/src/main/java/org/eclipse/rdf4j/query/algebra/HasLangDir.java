/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

public class HasLangDir extends UnaryValueOperator {
	public HasLangDir() {
	}

	public HasLangDir(ValueExpr arg) {
		super(arg);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "HasLangDir".hashCode();
	}

	@Override
	public HasLangDir clone() {
		return (HasLangDir) super.clone();
	}
}
