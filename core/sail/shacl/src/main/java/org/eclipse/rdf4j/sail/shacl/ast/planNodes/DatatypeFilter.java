/*******************************************************************************
 * .Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;

/**
 * @author Håvard Ottestad
 */
public class DatatypeFilter extends FilterPlanNode {

	private final Resource datatype;
	private StackTraceElement[] stackTrace;

	public DatatypeFilter(PlanNode parent, Resource datatype) {
		super(parent);
		this.datatype = datatype;
//		stackTrace = Thread.currentThread().getStackTrace();
	}

	@Override
	boolean checkTuple(ValidationTuple t) {
		if (!(t.getValue().isLiteral())) {
			return false;
		}

		Literal literal = (Literal) t.getValue();
		return literal.getDatatype() == datatype || literal.getDatatype().equals(datatype);
	}

	@Override
	public String toString() {
		return "DatatypeFilter{" + "datatype=" + Formatter.prefix(datatype) + '}';
	}
}
