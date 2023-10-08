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

import static org.eclipse.rdf4j.model.util.Values.literal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Configurations;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.sail.base.config.BaseSailConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

/**
 * @author Håvard Mikkelsen Ottestad
 */
public class ElasticsearchStoreConfig extends BaseSailConfig {

	private String hostname;
	private int port = -1;
	private String clusterName;
	private String index;

	public ElasticsearchStoreConfig() {
		super(ElasticsearchStoreFactory.SAIL_TYPE);
	}

	@Override
	public Resource export(Model m) {
		if (Configurations.useLegacyConfig()) {
			return exportLegacy(m);
		}

		Resource implNode = super.export(m);

		if (hostname != null) {
			m.add(implNode, CONFIG.Ess.hostname, literal(hostname));
		}
		if (clusterName != null) {
			m.add(implNode, CONFIG.Ess.clusterName, literal(clusterName));
		}
		if (index != null) {
			m.add(implNode, CONFIG.Ess.index, literal(index));
		}
		if (port != -1) {
			m.add(implNode, CONFIG.Ess.port, literal(port));
		}

		return implNode;
	}

	private Resource exportLegacy(Model m) {
		Resource implNode = super.export(m);

		if (hostname != null) {
			m.add(implNode, ElasticsearchStoreSchema.hostname, literal(hostname));
		}
		if (clusterName != null) {
			m.add(implNode, ElasticsearchStoreSchema.clusterName, literal(clusterName));
		}
		if (index != null) {
			m.add(implNode, ElasticsearchStoreSchema.index, literal(index));
		}
		if (port != -1) {
			m.add(implNode, ElasticsearchStoreSchema.port, literal(port));
		}

		return implNode;
	}

	@Override
	public void parse(Model graph, Resource implNode) throws SailConfigException {
		super.parse(graph, implNode);

		try {

			Configurations.getLiteralValue(graph, implNode, CONFIG.Ess.hostname, ElasticsearchStoreSchema.hostname)
					.ifPresent(value -> {
						try {
							setHostname(value.stringValue());
						} catch (IllegalArgumentException e) {
							throw new SailConfigException(
									"String value required for " + CONFIG.Ess.hostname
											+ " property, found "
											+ value);
						}
					});

			Configurations.getLiteralValue(graph, implNode, CONFIG.Ess.index, ElasticsearchStoreSchema.index)
					.ifPresent(value -> {
						try {
							setIndex(value.stringValue());
						} catch (IllegalArgumentException e) {
							throw new SailConfigException(
									"String value required for " + CONFIG.Ess.index + " property, found "
											+ value);
						}
					});

			Configurations
					.getLiteralValue(graph, implNode, CONFIG.Ess.clusterName, ElasticsearchStoreSchema.clusterName)
					.ifPresent(value -> {
						try {
							setClusterName(value.stringValue());
						} catch (IllegalArgumentException e) {
							throw new SailConfigException(
									"String value required for " + CONFIG.Ess.clusterName
											+ " property, found " + value);
						}
					});

			Configurations.getLiteralValue(graph, implNode, CONFIG.Ess.port, ElasticsearchStoreSchema.port)
					.ifPresent(value -> {
						try {
							setPort(value.intValue());
						} catch (IllegalArgumentException e) {
							throw new SailConfigException(
									"Integer value required for " + CONFIG.Ess.port + " property, found "
											+ value);
						}
					});

		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public String getClusterName() {
		return clusterName;
	}

	public String getIndex() {
		return index;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public void assertRequiredValuesPresent() {

		List<String> missingFields = new ArrayList<>();

		if (hostname == null) {
			missingFields.add("hostname");
		}
		if (clusterName == null) {
			missingFields.add("clusterName");
		}
		if (index == null) {
			missingFields.add("index");
		}
		if (port == -1) {
			missingFields.add("port");
		}

		if (!missingFields.isEmpty()) {
			throw new SailConfigException(
					"Required config missing for: " + missingFields.stream().reduce((a, b) -> a + " and " + b));
		}

	}
}
