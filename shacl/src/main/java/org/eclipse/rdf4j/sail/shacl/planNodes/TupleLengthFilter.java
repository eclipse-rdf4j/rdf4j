/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

public class TupleLengthFilter extends FilterPlanNode{
	private final int length;
	private final boolean exact;


	public TupleLengthFilter(PlanNode parent, PushBasedPlanNode trueNode, PushBasedPlanNode falseNode, int length, boolean exact) {
		super(parent, trueNode, falseNode);
		this.length = length;
		this.exact = exact;
	}

	@Override
	boolean checkTuple(Tuple t) {
		if(t.line.size() >= length){
			if(exact){
				return t.line.size() == length;
			}else {
				return true;
			}
		}

		return false;
	}
}
