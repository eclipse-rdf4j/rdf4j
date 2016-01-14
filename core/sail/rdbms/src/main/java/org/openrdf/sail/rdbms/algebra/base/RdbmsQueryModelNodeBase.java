/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.openrdf.sail.rdbms.algebra.base;

import org.openrdf.query.algebra.AbstractQueryModelNode;
import org.openrdf.query.algebra.QueryModelVisitor;

/**
 * An extension to {@link AbstractQueryModelNode} for SQL query algebra.
 * 
 * @author James Leigh
 * 
 */
public abstract class RdbmsQueryModelNodeBase extends AbstractQueryModelNode {

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
		throws X
	{
		if (visitor instanceof RdbmsQueryModelVisitorBase) {
			visit((RdbmsQueryModelVisitorBase<X>)visitor);
		}
		else {
			visitor.meetOther(this);
		}
	}

	public abstract <X extends Exception> void visit(RdbmsQueryModelVisitorBase<X> visitor)
		throws X;
}
