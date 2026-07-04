/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.iteration;

import java.util.Iterator;

public class LookAheadIterationTest extends CloseableIterationTest {

	@Override
	protected CloseableIteration<String> createTestIteration() {
		final Iterator<String> iter = stringList1.iterator();

		return new LookAheadIteration<>() {

			@Override
			protected void handleClose() {

			}

			@Override
			protected String getNextElement() {
				if (iter.hasNext()) {
					return iter.next();
				} else {
					return null;
				}
			}

		};
	}

	@Override
	protected int getTestIterationSize() {
		return stringList1.size();
	}
}
