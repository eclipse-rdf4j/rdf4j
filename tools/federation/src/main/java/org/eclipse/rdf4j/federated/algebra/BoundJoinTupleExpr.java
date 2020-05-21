/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import org.eclipse.rdf4j.federated.evaluation.join.ControlledWorkerBoundJoin;

/**
 * Marker interface indicating that instances are applicable for bound join processing (see
 * {@link ControlledWorkerBoundJoin}
 *
 * @author Andreas Schwarte
 * @see ControlledWorkerBoundJoin
 */
public interface BoundJoinTupleExpr {

}
