/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.manager;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link RepositoryManager}. May be extended by specific {@link RepositoryManager} implementations.
 *
 * @author Jeen Broekstra
 *
 */
public class RepositoryManagerTest {

	protected RepositoryManager subject;

	@Before
	public void setUp() throws Exception {
		subject = new RepositoryManager() {

			@Override
			public void setHttpClient(HttpClient httpClient) {
				// TODO Auto-generated method stub

			}

			@Override
			public URL getLocation() throws MalformedURLException {
				return null;
			}

			@Override
			public HttpClient getHttpClient() {
				return null;
			}

			@Override
			public Collection<RepositoryInfo> getAllRepositoryInfos() throws RepositoryException {
				return null;
			}

			@Override
			protected Repository createRepository(String id) throws RepositoryConfigException, RepositoryException {
				return null;
			}

			@Override
			public RepositoryConfig getRepositoryConfig(String repositoryID)
					throws RepositoryConfigException, RepositoryException {
				return null;
			}

			@Override
			public void addRepositoryConfig(RepositoryConfig config)
					throws RepositoryException, RepositoryConfigException {
			}
		};
	}

	@Test
	public void testSetModelFactory() {
		ModelFactory f = new LinkedHashModelFactory();
		subject.setModelFactory(f);
		assertThat(subject.getModelFactory()).isEqualTo(f);
	}

	@Test(expected = NullPointerException.class)
	public void testSetModelFactoryWithNull() {
		subject.setModelFactory(null);
	}
}
