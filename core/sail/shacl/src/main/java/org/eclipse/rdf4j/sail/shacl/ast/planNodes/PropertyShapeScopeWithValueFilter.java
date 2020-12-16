/*******************************************************************************
 * .Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;

/**
 * @author Håvard Ottestad
 */
public class PropertyShapeScopeWithValueFilter extends FilterPlanNode {

	public PropertyShapeScopeWithValueFilter(PlanNode parent) {
		super(parent);
	}

	@Override
	boolean checkTuple(ValidationTuple t) {
		return t.getScope() == ConstraintComponent.Scope.propertyShape && t.hasValue();
	}

	@Override
	public String toString() {
		return "PropertyShapeScopeWithValueFilter{}";
	}
}
