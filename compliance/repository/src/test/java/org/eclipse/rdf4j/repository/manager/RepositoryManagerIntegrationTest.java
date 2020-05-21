/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.manager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.junit.Before;

public class RepositoryManagerIntegrationTest {

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
			public Collection<RepositoryInfo> getAllRepositoryInfos(boolean skipSystemRepo) throws RepositoryException {
				return null;
			}

			@Override
			protected Repository createRepository(String id) throws RepositoryConfigException, RepositoryException {
				return null;
			}
		};
	}

}
