/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.iteration;

import java.util.Collections;

public class FilterIterationTest extends CloseableIterationTest {

	@Override
	protected CloseableIteration<? extends String, Exception> createTestIteration() {
		return new FilterIteration<>(createStringList1Iteration()) {

			@Override
			protected boolean accept(String object) {
				return "3".equals(object);
			}

		};
	}

	@Override
	protected int getTestIterationSize() {
		return Collections.frequency(stringList1, "3");
	}
}
