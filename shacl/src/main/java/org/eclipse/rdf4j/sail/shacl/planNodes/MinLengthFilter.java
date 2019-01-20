/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;


import org.eclipse.rdf4j.model.Value;

/**
 * @author Håvard Ottestad
 */
public class MinLengthFilter extends FilterPlanNode {

	private final long minLength;

	public MinLengthFilter(PlanNode parent, PushBasedPlanNode trueNode, PushBasedPlanNode falseNode, long minLength) {
		super(parent, trueNode, falseNode);
		this.minLength = minLength;
	}

	@Override
	boolean checkTuple(Tuple t) {
		Value literal = t.line.get(1);

		return literal.stringValue().length() >= minLength;
	}


	@Override
	public String toString() {
		return "MinLengthFilter{" +
			"minLength=" + minLength +
			'}';
	}
}
