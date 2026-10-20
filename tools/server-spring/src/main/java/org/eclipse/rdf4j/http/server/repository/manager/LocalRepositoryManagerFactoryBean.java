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

import java.io.File;
import java.util.Objects;

import org.eclipse.rdf4j.opentelemetry.repository.manager.OpenTelemetrySettings;
import org.eclipse.rdf4j.opentelemetry.repository.manager.TracingLocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * A Spring {@link FactoryBean} that constructs either a plain {@link LocalRepositoryManager} or a
 * {@link TracingLocalRepositoryManager}, depending on {@link OpenTelemetrySettings#isEnabled()} at the time this bean
 * is created.
 * <p>
 * This lets a single Spring bean definition (see {@code rdf4j-http-server-servlet.xml}) opt into OpenTelemetry tracing
 * for every repository the RDF4J Server serves, purely via a system property: no {@link TracingLocalRepositoryManager}
 * is instantiated unless tracing is enabled.
 * <p>
 * Spring applies {@code init-method}/{@code destroy-method} declared on a {@code FactoryBean}'s bean definition to the
 * factory itself, not to the object returned by {@link #getObject()}. This class therefore drives the produced
 * {@link LocalRepositoryManager}'s {@code init()}/{@code shutDown()} lifecycle directly, via {@link InitializingBean}
 * and {@link DisposableBean}.
 */
public class LocalRepositoryManagerFactoryBean
		implements FactoryBean<LocalRepositoryManager>, InitializingBean, DisposableBean {

	private File baseDir;

	private LocalRepositoryManager repositoryManager;

	public void setBaseDir(File baseDir) {
		this.baseDir = baseDir;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Objects.requireNonNull(baseDir, "baseDir must be set");
		LocalRepositoryManager manager = OpenTelemetrySettings.isEnabled() ? new TracingLocalRepositoryManager(baseDir)
				: new LocalRepositoryManager(baseDir);
		manager.init();
		// only assign once fully initialized, so a failed init() never exposes a partially-initialized manager
		this.repositoryManager = manager;
	}

	@Override
	public LocalRepositoryManager getObject() {
		return repositoryManager;
	}

	@Override
	public Class<?> getObjectType() {
		return LocalRepositoryManager.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void destroy() {
		if (repositoryManager != null) {
			repositoryManager.shutDown();
		}
	}
}
