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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class NavigationTest {

	private NavigationModel model = null;

	@Before
	public void setUp() {
		model = new NavigationModel();
		List<String> navigationModelLocations = new ArrayList<>();
		navigationModelLocations.add("/navigation.xml");
		model.setNavigationModels(navigationModelLocations);
	}

	@Test
	public void testParse() {
		assertNotNull("Parsed model is null", model);
		assertEquals("Model should have one group", 1, model.getGroups().size());
		Group systemGroup = model.getGroups().get(0);
		assertEquals("system group should have 1 subgroup", 1, systemGroup.getGroups().size());
		assertEquals("system group should have 2 views", 2, systemGroup.getViews().size());
		View loggingView = systemGroup.getViews().get(1);
		assertFalse("logging view should not be hidden", loggingView.isHidden());
		assertTrue("logging view should be enabled", loggingView.isEnabled());
		assertEquals("Path for logging is not correct", "/system/logging.view", loggingView.getPath());
		assertEquals("Icon for logging is not correct", "/images/icons/system_logging.png", loggingView.getIcon());
		assertEquals("I18N for logging is not correct", "system.logging.title", loggingView.getI18n());
		Group loggingGroup = systemGroup.getGroups().get(0);
		assertEquals("logging subgroup should have 1 views", 1, loggingGroup.getViews().size());
		assertTrue("logging subgroup should be hidden", loggingGroup.isHidden());
		assertTrue("logging subgroup should be enabled", loggingGroup.isEnabled());
		View loggingOverview = loggingGroup.getViews().get(0);
		assertFalse("logging overview should be disabled", loggingOverview.isEnabled());
	}

	@Test
	public void testFind() {
		assertNotNull("Find should have succeeded", model.findView("/system/logging/overview.view"));
		assertNull("Find should not have succeeded", model.findView("/system/logging/bogus.view"));
	}
}
