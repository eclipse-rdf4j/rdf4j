/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;

/**
 * @author Heshan Jayasinghe
 */
public interface PlanGenerator {

	PlanNode getPlan(ShaclSailConnection shaclSailConnection, Shape shape);

	PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection, Shape shape);

	PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection, Shape shape);


}
