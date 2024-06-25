/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.rio.helpers.AbstractRioSetting;
import org.eclipse.rdf4j.rio.helpers.BooleanRioSetting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RioConfigTest {

	private RioConfig config;

	private final String key = "org.eclipse.rdf4j.rio.rioconfig.test";

	private final BooleanRioSetting testSetting = new BooleanRioSetting(key, "test setting", true);

	@BeforeEach
	public void setUp() {
		config = new RioConfig();
	}

	@AfterEach
	public void cleanup() {
		System.clearProperty(key);
	}

	@Test
	public void testIsSetDefault() {
		assertThat(config.isSet(testSetting)).isFalse();
	}

	@Test
	public void testIsSetWithSystemPropertyOverride() {
		System.setProperty(key, "false");
		assertThat(config.isSet(testSetting)).isTrue();
	}

	@Test
	public void testIsSetWithExplicitSet() {
		config.set(testSetting, false);
		assertThat(config.isSet(testSetting)).isTrue();
	}

	@Test
	public void testUseDefaultsNoOverride() {
		config.set(testSetting, false);
		config.useDefaults();
		assertThat(config.isSet(testSetting)).isFalse();
	}

	@Test
	public void testUseDefaultsWithOverride() {
		System.setProperty(key, "false");
		config.useDefaults();
		assertThat(config.isSet(testSetting)).isTrue();
	}

	@Test
	public void testGetWithSystemPropertyOverride() {
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
	public void testGetWithUnsupportedConversionType() {
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
