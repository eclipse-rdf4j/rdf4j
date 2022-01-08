/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.transaction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Jeen Broekstra
 */
@SuppressWarnings({ "removal" })
public class IsolationLevelTest {

	@Test
	public void testIsCompatibleWith() {
		assertTrue(IsolationLevel.SNAPSHOT.isCompatibleWith(IsolationLevels.READ_COMMITTED));
		assertTrue(IsolationLevel.SNAPSHOT.isCompatibleWith(IsolationLevel.READ_COMMITTED));
		assertTrue(IsolationLevel.SERIALIZABLE.isCompatibleWith(IsolationLevels.READ_COMMITTED));
		assertTrue(IsolationLevel.SERIALIZABLE.isCompatibleWith(IsolationLevel.READ_COMMITTED));
		assertTrue(IsolationLevel.SNAPSHOT.isCompatibleWith(IsolationLevels.READ_UNCOMMITTED));
		assertTrue(IsolationLevel.SNAPSHOT.isCompatibleWith(IsolationLevel.READ_UNCOMMITTED));
		assertFalse(IsolationLevel.READ_COMMITTED.isCompatibleWith(IsolationLevels.SNAPSHOT));
		assertFalse(IsolationLevel.READ_COMMITTED.isCompatibleWith(IsolationLevel.SNAPSHOT));
	}

}
