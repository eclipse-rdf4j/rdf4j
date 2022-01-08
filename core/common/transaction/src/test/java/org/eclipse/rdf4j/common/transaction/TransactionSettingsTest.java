/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class TransactionSettingsTest {
	@Test
	public void testGetCompatibleIsolationLevel() {

		List<IsolationLevel> supportedLevels = new ArrayList<>();
		supportedLevels.add(IsolationLevel.NONE);
		supportedLevels.add(IsolationLevel.SERIALIZABLE);

		IsolationLevel compatibleLevel = TransactionSettings.getCompatibleIsolationLevel(IsolationLevel.READ_COMMITTED,
				supportedLevels);
		assertNotNull(compatibleLevel);
		assertEquals(IsolationLevel.SERIALIZABLE, compatibleLevel);
	}

	@Test
	public void testGetCompatibleIsolationLevelNoneFound() {

		List<IsolationLevel> supportedLevels = new ArrayList<>();
		supportedLevels.add(IsolationLevel.NONE);
		supportedLevels.add(IsolationLevel.READ_UNCOMMITTED);
		supportedLevels.add(IsolationLevel.READ_COMMITTED);

		IsolationLevel compatibleLevel = TransactionSettings.getCompatibleIsolationLevel(IsolationLevel.SERIALIZABLE,
				supportedLevels);
		assertNull(compatibleLevel);
	}

	@Test
	public void testGetCompatibleIsolationLevelNullSupported() {
		assertThrows(IllegalArgumentException.class,
				() -> TransactionSettings.getCompatibleIsolationLevel(IsolationLevel.SNAPSHOT,
						null));
	}

	@Test
	public void testGetCompatibleIsolationLevelNullLevel() {
		List<IsolationLevel> supportedLevels = new ArrayList<>();
		supportedLevels.add(IsolationLevel.NONE);
		supportedLevels.add(IsolationLevel.SNAPSHOT);
		supportedLevels.add(IsolationLevel.SERIALIZABLE);

		assertThrows(IllegalArgumentException.class,
				() -> TransactionSettings.getCompatibleIsolationLevel(null, supportedLevels));
	}

}
