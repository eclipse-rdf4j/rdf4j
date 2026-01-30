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
package org.eclipse.rdf4j.query.algebra;

import java.util.List;

public class StrLangDir extends NAryValueOperator {

	public StrLangDir() {
	}

	public StrLangDir(ValueExpr lexicalForm, ValueExpr lang, ValueExpr dir) {
		super(List.of(lexicalForm, lang, dir));
	}

	public ValueExpr getLexicalFormArg() {
		return getArguments().get(0);
	}

	public ValueExpr getLangArg() {
		return getArguments().get(1);
	}

	public ValueExpr getDirArg() {
		return getArguments().get(2);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "StrLangDir".hashCode();
	}

	@Override
	public StrLangDir clone() {
		return (StrLangDir) super.clone();
	}
}
