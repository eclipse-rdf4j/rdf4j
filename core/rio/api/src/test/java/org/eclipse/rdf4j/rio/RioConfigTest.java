/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.rio.helpers.AbstractRioSetting;
import org.eclipse.rdf4j.rio.helpers.BooleanRioSetting;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RioConfigTest {

	private RioConfig config;

	private final String key = "org.eclipse.rdf4j.rio.rioconfig.test";

	private final BooleanRioSetting testSetting = new BooleanRioSetting(key, "test setting", true);

	@Before
	public void setUp() throws Exception {
		config = new RioConfig();
	}

	@After
	public void cleanup() {
		System.clearProperty(key);
	}

	@Test
	public void testIsSetDefault() throws Exception {
		assertThat(config.isSet(testSetting)).isFalse();
	}

	@Test
	public void testIsSetWithSystemPropertyOverride() throws Exception {
		System.setProperty(key, "false");
		assertThat(config.isSet(testSetting)).isTrue();
	}

	@Test
	public void testIsSetWithExplicitSet() throws Exception {
		config.set(testSetting, false);
		assertThat(config.isSet(testSetting)).isTrue();
	}

	@Test
	public void testUseDefaultsNoOverride() throws Exception {
		config.set(testSetting, false);
		config.useDefaults();
		assertThat(config.isSet(testSetting)).isFalse();
	}

	@Test
	public void testUseDefaultsWithOverride() throws Exception {
		System.setProperty(key, "false");
		config.useDefaults();
		assertThat(config.isSet(testSetting)).isTrue();
	}

	@Test
	public void testGetWithSystemPropertyOverride() throws Exception {
		System.setProperty(key, "false");
		assertThat(config.get(testSetting)).as("default setting overridden by system prop").isFalse();

		config.set(testSetting, true);
		assertThat(config.get(testSetting)).as("explicit user-configured setting overriding system prop").isTrue();

		config.useDefaults();
		assertThat(config.get(testSetting)).as("default setting overridden by sytem prop").isFalse();

		System.clearProperty(key);
		assertThat(config.get(testSetting)).as("default setting overridden by system prop").isFalse();

		config.useDefaults();
		assertThat(config.get(testSetting)).as("default setting").isTrue();
	}

	@Test
	public void testGetWithUnsupportedConversionType() throws Exception {
		// we deliberately do not use StringRioSetting as that supports conversion of system property values
		AbstractRioSetting<String> nonConvertableSetting = new AbstractRioSetting<>(key, "test setting",
				"default value") {
			private static final long serialVersionUID = 1L;
		};

		assertThat(config.get(nonConvertableSetting)).isEqualTo("default value");

		System.setProperty(key, "system property value");

		// system property should be ignored
		assertThat(config.get(nonConvertableSetting)).isEqualTo("default value");

	}

}
