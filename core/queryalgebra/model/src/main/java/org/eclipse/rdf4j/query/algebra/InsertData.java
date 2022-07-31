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

/**
 * @author jeen
 */
public class InsertData extends AbstractQueryModelNode implements UpdateExpr {

	private final String dataBlock;

	private int lineNumberCorrection;

	public InsertData(String dataBlock) {
		this.dataBlock = dataBlock;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		// no-op
	}

	public String getDataBlock() {
		return dataBlock;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof InsertData) {
			InsertData o = (InsertData) other;
			return dataBlock.equals(o.dataBlock);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return dataBlock.hashCode();
	}

	@Override
	public InsertData clone() {
		return new InsertData(dataBlock);
	}

	@Override
	public boolean isSilent() {
		return false;
	}

	/**
	 * @return the lineNumberCorrection
	 */
	public int getLineNumberOffset() {
		return lineNumberCorrection;
	}

	public void setLineNumberOffset(int lineNumberOffset) {
		this.lineNumberCorrection = lineNumberOffset;
	}

}
