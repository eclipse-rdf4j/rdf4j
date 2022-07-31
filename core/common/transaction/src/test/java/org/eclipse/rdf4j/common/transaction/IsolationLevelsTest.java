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
package org.eclipse.rdf4j.common.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * @author jeen
 */
public class IsolationLevelsTest {

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.common.transaction.IsolationLevels#isCompatibleWith(org.eclipse.rdf4j.common.transaction.IsolationLevel)}
	 * .
	 */
	@Test
	public void testIsCompatibleWith() {
		assertTrue(IsolationLevels.SNAPSHOT.isCompatibleWith(IsolationLevels.READ_COMMITTED));
		assertTrue(IsolationLevels.SERIALIZABLE.isCompatibleWith(IsolationLevels.READ_COMMITTED));
		assertTrue(IsolationLevels.SNAPSHOT.isCompatibleWith(IsolationLevels.READ_UNCOMMITTED));
		assertFalse(IsolationLevels.READ_COMMITTED.isCompatibleWith(IsolationLevels.SNAPSHOT));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.common.transaction.IsolationLevels#getCompatibleIsolationLevel(org.eclipse.rdf4j.common.transaction.IsolationLevel, java.util.List)}
	 * .
	 */
	@Test
	public void testGetCompatibleIsolationLevel() {

		List<IsolationLevels> supportedLevels = new ArrayList<>();
		supportedLevels.add(IsolationLevels.NONE);
		supportedLevels.add(IsolationLevels.SERIALIZABLE);

		IsolationLevel compatibleLevel = IsolationLevels.getCompatibleIsolationLevel(IsolationLevels.READ_COMMITTED,
				supportedLevels);
		assertNotNull(compatibleLevel);
		assertEquals(IsolationLevels.SERIALIZABLE, compatibleLevel);
	}

	@Test
	public void testGetCompatibleIsolationLevelNoneFound() {

		List<IsolationLevels> supportedLevels = new ArrayList<>();
		supportedLevels.add(IsolationLevels.NONE);
		supportedLevels.add(IsolationLevels.READ_UNCOMMITTED);
		supportedLevels.add(IsolationLevels.READ_COMMITTED);

		IsolationLevel compatibleLevel = IsolationLevels.getCompatibleIsolationLevel(IsolationLevels.SERIALIZABLE,
				supportedLevels);
		assertNull(compatibleLevel);

	}

	@Test
	public void testGetCompatibleIsolationLevelNullParams() {
		try {
			IsolationLevel compatibleLevel = IsolationLevels.getCompatibleIsolationLevel(IsolationLevels.SNAPSHOT,
					null);
			fail("should have resulted in an IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// do nothing, expected.
		}

		List<IsolationLevels> supportedLevels = new ArrayList<>();
		supportedLevels.add(IsolationLevels.NONE);
		supportedLevels.add(IsolationLevels.SNAPSHOT);
		supportedLevels.add(IsolationLevels.SERIALIZABLE);

		try {
			IsolationLevel compatibleLevel = IsolationLevels.getCompatibleIsolationLevel(null, supportedLevels);
			fail("should have resulted in an IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// do nothing, expected.
		}
	}

}
