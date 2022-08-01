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
package org.eclipse.rdf4j.sail.elasticsearchstore.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;
import org.junit.Before;
import org.junit.Test;

public class ElasticsearchStoreFactoryTest {

	private ElasticsearchStoreFactory subject;

	@Before
	public void setUp() throws Exception {
		subject = new ElasticsearchStoreFactory();
	}

	@Test
	public void getSailTypeReturnsCorrectValue() {
		assertThat(subject.getSailType()).isEqualTo(ElasticsearchStoreFactory.SAIL_TYPE);
	}

	/**
	 * Verify that the created sail is configured according to the supplied default configuration.
	 */
	@Test(expected = SailConfigException.class)
	public void getSailWithDefaultConfigFails() {
		ElasticsearchStoreConfig config = new ElasticsearchStoreConfig();
		ElasticsearchStore sail = (ElasticsearchStore) subject.getSail(config);
	}

	/**
	 * Verify that the created sail is configured according to the supplied customized configuration.
	 */
	@Test
	public void getSailWithCustomConfigSetsConfigurationCorrectly() {
		ElasticsearchStoreConfig config = new ElasticsearchStoreConfig();

		// set everything to the opposite of its default
		config.setHostname("localhost");
		config.setClusterName("elasticsearch");
		config.setIndex("index1");
		config.setPort(9300);

		ElasticsearchStore sail = (ElasticsearchStore) subject.getSail(config);
		assertMatchesConfig(sail, config);
	}

	private void assertMatchesConfig(ElasticsearchStore sail, ElasticsearchStoreConfig config) {
		assertThat(sail.getClusterName()).isEqualTo(config.getClusterName());
		assertThat(sail.getHostname()).isEqualTo(config.getHostname());
		assertThat(sail.getIndex()).isEqualTo(config.getIndex());
		assertThat(sail.getPort()).isEqualTo(config.getPort());

	}

}
