/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerTest;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.SameTermFilterOptimizer;

/**
 * @author jeen
 *
 */
public class SameTermFilterOptimizerTest extends QueryOptimizerTest {

	@Override
	public SameTermFilterOptimizer getOptimizer() {
		return new SameTermFilterOptimizer();
	}

}
