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

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.junit.Before;
import org.junit.Test;

public class ElasticsearchStoreConfigTest {

	private ElasticsearchStoreConfig subject;

	private BNode implNode;

	private ModelBuilder mb;

	@Before
	public void setUp() throws Exception {
		subject = new ElasticsearchStoreConfig();
		implNode = SimpleValueFactory.getInstance().createBNode();
		mb = new ModelBuilder().subject(implNode);
	}

	@Test
	public void defaultsCorrectlySet() {
		assertThat(subject.getClusterName()).isNull();
		assertThat(subject.getHostname()).isNull();
		assertThat(subject.getIndex()).isNull();
		assertThat(subject.getPort()).isEqualTo(-1);

	}

	@Test
	public void parseFromModelSetValuesCorrectly() {

		// FIXME we need to set formatting guidelines for this kind of thing
		// @formatter:off
		mb
			.add(ElasticsearchStoreSchema.hostname, "host1")
			.add(ElasticsearchStoreSchema.clusterName, "cluster1")
			.add(ElasticsearchStoreSchema.index, "index1")
			.add(ElasticsearchStoreSchema.port, 9300);
		// @formatter:on

		subject.parse(mb.build(), implNode);

		assertThat(subject.getHostname()).isEqualTo("host1");
		assertThat(subject.getClusterName()).isEqualTo("cluster1");
		assertThat(subject.getIndex()).isEqualTo("index1");
		assertThat(subject.getPort()).isEqualTo(9300);

	}

	@Test
	public void parseFromPartialModelSetValuesCorrectly() {
		mb
				.add(ElasticsearchStoreSchema.hostname, "host1")
				.add(ElasticsearchStoreSchema.port, 9300);

		subject.parse(mb.build(), implNode);

		assertThat(subject.getHostname()).isEqualTo("host1");
		assertThat(subject.getPort()).isEqualTo(9300);
	}

	@Test(expected = SailConfigException.class)
	public void parseInvalidModelGivesCorrectException() {

		mb
				.add(ElasticsearchStoreSchema.port, "port1");

		subject.parse(mb.build(), implNode);

	}

	@Test
	public void exportAddsAllConfigData() {

		mb
				.add(ElasticsearchStoreSchema.hostname, "host1")
				.add(ElasticsearchStoreSchema.clusterName, "cluster1")
				.add(ElasticsearchStoreSchema.index, "index1")
				.add(ElasticsearchStoreSchema.port, 9300);
		// @formatter:on

		subject.parse(mb.build(), implNode);

		Model m = new TreeModel();
		Resource node = subject.export(m);

		assertThat(m.contains(node, ElasticsearchStoreSchema.hostname, null)).isTrue();
		assertThat(m.contains(node, ElasticsearchStoreSchema.clusterName, null)).isTrue();
		assertThat(m.contains(node, ElasticsearchStoreSchema.index, null)).isTrue();
		assertThat(m.contains(node, ElasticsearchStoreSchema.port, null)).isTrue();

	}

}
