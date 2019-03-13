/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;


import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;

/**
 * @author Håvard Ottestad
 */
public class DatatypeFilter extends FilterPlanNode {

	private final Resource datatype;

	public DatatypeFilter(PlanNode parent, Resource datatype) {
		super(parent);
		this.datatype = datatype;
	}

	@Override
	boolean checkTuple(Tuple t) {
		if(! (t.line.get(1) instanceof Literal)) return false;

		Literal literal = (Literal) t.line.get(1);
		return literal.getDatatype() == datatype || literal.getDatatype().equals(datatype);
	}


	@Override
	public String toString() {
		return "DatatypeFilter{" +
			"datatype=" + datatype +
			'}';
	}
}
