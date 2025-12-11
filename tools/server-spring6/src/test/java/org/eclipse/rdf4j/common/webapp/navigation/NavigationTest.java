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
package org.eclipse.rdf4j.common.webapp.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NavigationTest {

	private NavigationModel model = null;

	@BeforeEach
	public void setUp() {
		model = new NavigationModel();
		List<String> navigationModelLocations = new ArrayList<>();
		navigationModelLocations.add("/navigation.xml");
		model.setNavigationModels(navigationModelLocations);
	}

	@Test
	public void testParse() {
		assertNotNull(model, "Parsed model is null");
		assertEquals(1, model.getGroups().size(), "Model should have one group");
		Group systemGroup = model.getGroups().get(0);
		assertEquals(1, systemGroup.getGroups().size(), "system group should have 1 subgroup");
		assertEquals(2, systemGroup.getViews().size(), "system group should have 2 views");
		View loggingView = systemGroup.getViews().get(1);
		assertFalse(loggingView.isHidden(), "logging view should not be hidden");
		assertTrue(loggingView.isEnabled(), "logging view should be enabled");
		assertEquals("/system/logging.view", loggingView.getPath(), "Path for logging is not correct");
		assertEquals("/images/icons/system_logging.png", loggingView.getIcon(), "Icon for logging is not correct");
		assertEquals("system.logging.title", loggingView.getI18n(), "I18N for logging is not correct");
		Group loggingGroup = systemGroup.getGroups().get(0);
		assertEquals(1, loggingGroup.getViews().size(), "logging subgroup should have 1 views");
		assertTrue(loggingGroup.isHidden(), "logging subgroup should be hidden");
		assertTrue(loggingGroup.isEnabled(), "logging subgroup should be enabled");
		View loggingOverview = loggingGroup.getViews().get(0);
		assertFalse(loggingOverview.isEnabled(), "logging overview should be disabled");
	}

	@Test
	public void testFind() {
		assertNotNull(model.findView("/system/logging/overview.view"), "Find should have succeeded");
		assertNull(model.findView("/system/logging/bogus.view"), "Find should not have succeeded");
	}
}
