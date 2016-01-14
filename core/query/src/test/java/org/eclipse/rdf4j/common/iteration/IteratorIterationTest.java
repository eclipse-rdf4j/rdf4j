/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.iteration;

import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.IteratorIteration;

public class IteratorIterationTest extends IterationTest {

	@Override
	protected Iteration<String, Exception> createTestIteration()
	{
		return new IteratorIteration<String, Exception>(stringList1.iterator());
	}

	@Override
	protected int getTestIterationSize()
	{
		return stringList1.size();
	}
}
