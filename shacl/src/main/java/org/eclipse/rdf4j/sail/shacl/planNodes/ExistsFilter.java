/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;


/**
 * @author Håvard Ottestad
 */
public class ExistsFilter extends FilterPlanNode {

	public ExistsFilter(PlanNode parent, PushBasedPlanNode trueNode, PushBasedPlanNode falseNode) {
		super(parent, trueNode, falseNode);
	}

	@Override
	boolean checkTuple(Tuple t) {
		return false;
	}

}
