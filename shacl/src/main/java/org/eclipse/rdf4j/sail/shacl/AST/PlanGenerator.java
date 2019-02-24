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

import java.util.List;

/**
 * @author Heshan Jayasinghe
 */
public interface PlanGenerator {

	PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, PlanNode overrideTargetNode);

	PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape);

	PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape);

	List<Path> getPaths();

}
