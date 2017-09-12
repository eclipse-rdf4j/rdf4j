/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.AST;

import org.eclipse.rdf4j.plan.PlanNode;
import org.eclipse.rdf4j.validation.ShaclSailConnection;

/**
 * @author Heshan Jayasinghe
 */
public interface PlanGenerator {

	PlanNode getPlan(ShaclSailConnection shaclSailConnection, Shape shape);
}
