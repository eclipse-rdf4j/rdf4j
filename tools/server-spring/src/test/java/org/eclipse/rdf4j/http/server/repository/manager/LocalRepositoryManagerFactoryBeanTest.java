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
package org.eclipse.rdf4j.http.server.repository.manager;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.eclipse.rdf4j.opentelemetry.repository.manager.OpenTelemetrySettings;
import org.eclipse.rdf4j.opentelemetry.repository.manager.TracingLocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalRepositoryManagerFactoryBeanTest {

	@AfterEach
	void tearDown() {
		System.clearProperty(OpenTelemetrySettings.ENABLED_PROPERTY);
	}

	@Test
	void disabledByDefault_returnsPlainManager(@TempDir File baseDir) throws Exception {
		LocalRepositoryManagerFactoryBean factory = new LocalRepositoryManagerFactoryBean();
		factory.setBaseDir(baseDir);
		factory.afterPropertiesSet();

		LocalRepositoryManager manager = factory.getObject();

		assertThat(manager).isExactlyInstanceOf(LocalRepositoryManager.class);

		factory.destroy();
	}

	@Test
	void enabled_returnsTracingManager(@TempDir File baseDir) throws Exception {
		System.setProperty(OpenTelemetrySettings.ENABLED_PROPERTY, "true");

		LocalRepositoryManagerFactoryBean factory = new LocalRepositoryManagerFactoryBean();
		factory.setBaseDir(baseDir);
		factory.afterPropertiesSet();

		LocalRepositoryManager manager = factory.getObject();

		assertThat(manager).isInstanceOf(TracingLocalRepositoryManager.class);

		factory.destroy();
	}

	@Test
	void factoryBeanContract() {
		LocalRepositoryManagerFactoryBean factory = new LocalRepositoryManagerFactoryBean();

		assertThat(factory.getObjectType()).isEqualTo(LocalRepositoryManager.class);
		assertThat(factory.isSingleton()).isTrue();
	}
}
