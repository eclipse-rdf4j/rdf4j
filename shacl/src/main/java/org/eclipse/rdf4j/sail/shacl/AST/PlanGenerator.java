/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;

import java.util.List;

/**
 * @author Heshan Jayasinghe
 */
public interface PlanGenerator {

	PlanNode getPlan(ShaclSailConnection shaclSailConnection, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans);

	PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection,
			PlaneNodeWrapper planeNodeWrapper);

	PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection,
			PlaneNodeWrapper planeNodeWrapper);

	PlanNode getAllTargetsPlan(ShaclSailConnection shaclSailConnection, boolean negated);

	List<Path> getPaths();

}
