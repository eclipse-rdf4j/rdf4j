/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.opentelemetry.repository.manager;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpenTelemetrySettingsTest {

	@AfterEach
	void tearDown() {
		System.clearProperty(OpenTelemetrySettings.ENABLED_PROPERTY);
	}

	@Test
	void disabledByDefault() {
		System.clearProperty(OpenTelemetrySettings.ENABLED_PROPERTY);
		assertThat(OpenTelemetrySettings.isEnabled()).isFalse();
	}

	@Test
	void enabledWhenPropertySetTrue() {
		System.setProperty(OpenTelemetrySettings.ENABLED_PROPERTY, "true");
		assertThat(OpenTelemetrySettings.isEnabled()).isTrue();
	}

	@Test
	void disabledWhenPropertySetFalse() {
		System.setProperty(OpenTelemetrySettings.ENABLED_PROPERTY, "false");
		assertThat(OpenTelemetrySettings.isEnabled()).isFalse();
	}
}
