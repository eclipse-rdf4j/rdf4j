/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.iteration;

public class DelayedIterationTest extends CloseableIterationTest {

	@Override
	protected CloseableIteration<String, Exception> createTestIteration() {
		return new DelayedIteration<String, Exception>() {

			@Override
			protected Iteration<? extends String, Exception> createIteration() {
				return createStringList1Iteration();
			}
		};
	}

	@Override
	protected int getTestIterationSize() {
		return stringList1.size();
	}
}
