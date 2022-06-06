/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

/**
 * @author jeen
 */
public class Modify extends AbstractQueryModelNode implements UpdateExpr {

	private TupleExpr deleteExpr;

	private TupleExpr insertExpr;

	private TupleExpr whereExpr;

	public Modify(TupleExpr deleteExpr, TupleExpr insertExpr) {
		this(deleteExpr, insertExpr, null);
	}

	public Modify(TupleExpr deleteExpr, TupleExpr insertExpr, TupleExpr whereExpr) {
		setDeleteExpr(deleteExpr);
		setInsertExpr(insertExpr);
		setWhereExpr(whereExpr);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		if (deleteExpr != null) {
			deleteExpr.visit(visitor);
		}
		if (insertExpr != null) {
			insertExpr.visit(visitor);
		}
		if (whereExpr != null) {
			whereExpr.visit(visitor);
		}
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (deleteExpr == current) {
			setDeleteExpr((TupleExpr) replacement);
		} else if (insertExpr == current) {
			setInsertExpr((TupleExpr) replacement);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Modify) {
			Modify o = (Modify) other;
			return nullEquals(deleteExpr, o.deleteExpr) && nullEquals(insertExpr, o.insertExpr)
					&& nullEquals(whereExpr, o.whereExpr);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = 0;
		if (deleteExpr != null) {
			result ^= deleteExpr.hashCode();
		}
		if (insertExpr != null) {
			result ^= insertExpr.hashCode();
		}
		if (whereExpr != null) {
			result ^= whereExpr.hashCode();
		}
		return result;
	}

	@Override
	public Modify clone() {

		TupleExpr deleteClone = deleteExpr != null ? deleteExpr.clone() : null;
		TupleExpr insertClone = insertExpr != null ? insertExpr.clone() : null;
		TupleExpr whereClone = whereExpr != null ? whereExpr.clone() : null;
		return new Modify(deleteClone, insertClone, whereClone);
	}

	/**
	 * @param deleteExpr The deleteExpr to set.
	 */
	public void setDeleteExpr(TupleExpr deleteExpr) {
		this.deleteExpr = deleteExpr;
	}

	/**
	 * @return Returns the deleteExpr.
	 */
	public TupleExpr getDeleteExpr() {
		return deleteExpr;
	}

	/**
	 * @param insertExpr The insertExpr to set.
	 */
	public void setInsertExpr(TupleExpr insertExpr) {
		this.insertExpr = insertExpr;
	}

	/**
	 * @return Returns the insertExpr.
	 */
	public TupleExpr getInsertExpr() {
		return insertExpr;
	}

	/**
	 * @param whereExpr The whereExpr to set.
	 */
	public void setWhereExpr(TupleExpr whereExpr) {
		this.whereExpr = whereExpr;
	}

	/**
	 * @return Returns the whereExpr.
	 */
	public TupleExpr getWhereExpr() {
		return whereExpr;
	}

	@Override
	public boolean isSilent() {
		// TODO Auto-generated method stub
		return false;
	}

}
