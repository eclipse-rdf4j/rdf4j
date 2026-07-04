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

import java.util.Objects;

/**
 * Compares the string representation of a value expression to a pattern.
 */
public class Regex extends BinaryValueOperator {

	/*-----------*
	 * Variables *
	 *-----------*/

	private ValueExpr flagsArg;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public Regex() {
	}

	public Regex(ValueExpr expr, ValueExpr pattern, ValueExpr flags) {
		super(expr, pattern);
		if (flags != null) {
			setFlagsArg(flags);
		}
	}

	/*---------*
	 * Methods *
	 *---------*/

	public ValueExpr getArg() {
		return super.getLeftArg();
	}

	public void setArg(ValueExpr leftArg) {
		super.setLeftArg(leftArg);
	}

	public ValueExpr getPatternArg() {
		return super.getRightArg();
	}

	public void setPatternArg(ValueExpr rightArg) {
		super.setRightArg(rightArg);
	}

	public ValueExpr getFlagsArg() {
		return flagsArg;
	}

	public void setFlagsArg(ValueExpr flags) {
		this.flagsArg = flags;
		flags.setParentNode(this);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		super.visitChildren(visitor);
		if (flagsArg != null) {
			flagsArg.visit(visitor);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Regex && super.equals(other)) {
			Regex o = (Regex) other;
			return Objects.equals(flagsArg, o.getFlagsArg());
		}
		return false;
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (current == flagsArg) {
			setFlagsArg((ValueExpr) replacement);
		}
		super.replaceChildNode(current, replacement);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode() ^ "Regex".hashCode();
		if (flagsArg != null) {
			result ^= flagsArg.hashCode();
		}
		return result;
	}

	@Override
	public Regex clone() {
		Regex clone = (Regex) super.clone();
		if (flagsArg != null) {
			clone.setFlagsArg(flagsArg.clone());
		}
		return clone;
	}
}
