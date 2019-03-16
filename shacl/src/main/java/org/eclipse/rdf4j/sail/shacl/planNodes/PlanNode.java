/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

/**
 * @author Heshan Jayasinghe
 */
public interface PlanNode {

	CloseableIteration<Tuple, SailException> iterator();

	int depth();

	void getPlanAsGraphvizDot(StringBuilder stringBuilder);

	String getId();

	IteratorData getIteratorDataType();

}
