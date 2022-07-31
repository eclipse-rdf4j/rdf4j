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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;

import org.junit.Test;

/**
 *
 */
public abstract class CloseableIterationTest extends IterationTest {

	@Override
	protected abstract CloseableIteration<String, Exception> createTestIteration() throws Exception;

	@Test
	public void testClosedIteration() throws Exception {
		for (int n = 0; n < getTestIterationSize(); n++) {
			CloseableIteration<String, Exception> iter = createTestIteration();

			// Close after n iterations
			for (int i = 0; i < n; i++) {
				iter.next();
			}

			iter.close();

			assertFalse("closed iteration should not contain any more elements", iter.hasNext());

			try {
				iter.next();
				fail("next() called on a closed iteration should throw a NoSuchElementException");
			} catch (NoSuchElementException e) {
				// expected exception
			} catch (Exception e) {
				fail("next() called on a closed iteration should throw a NoSuchElementException");
			}
		}
	}
}
