/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.rio.helpers.BooleanRioSetting;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RioConfigTest {

	private RioConfig config;

	private String key = "org.eclipse.rdf4j.rio.rioconfig.test";
	
	private BooleanRioSetting testSetting = new BooleanRioSetting(key, "test setting", true);
	
	@Before
	public void setUp()
		throws Exception
	{
		config = new RioConfig();
	}
	
	@After
	public void cleanup() {
		System.clearProperty(key);
	}

	@Test
	public void testIsSetDefault()
		throws Exception
	{
		assertThat(config.isSet(testSetting)).isFalse();
	}

	@Test
	public void testIsSetWithSystemPropertyOverride()
		throws Exception
	{
		System.setProperty(key, "false");
		assertThat(config.isSet(testSetting)).isTrue();
	}
	
	@Test
	public void testIsSetWithExplicitSet()
		throws Exception
	{
		config.set(testSetting, false);
		assertThat(config.isSet(testSetting)).isTrue();
	}
	
	@Test
	public void testUseDefaultsNoOverride() throws Exception
	{
		config.set(testSetting, false);
		config.useDefaults();
		assertThat(config.isSet(testSetting)).isFalse();
	}
	
	
	@Test
	public void testUseDefaultsWithOverride() throws Exception
	{
		System.setProperty(key, "false");
		config.useDefaults();
		assertThat(config.isSet(testSetting)).isTrue();
	}
}
