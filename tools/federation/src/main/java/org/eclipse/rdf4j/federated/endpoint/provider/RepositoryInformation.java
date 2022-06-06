/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.endpoint.provider;

import java.util.Properties;

import org.eclipse.rdf4j.federated.endpoint.EndpointConfiguration;
import org.eclipse.rdf4j.federated.endpoint.EndpointType;

public class RepositoryInformation {

	protected Properties props = new Properties();
	private EndpointType type;
	private EndpointConfiguration endpointConfiguration; // optional configuration settings for the endpoint

	private boolean writable;

	public RepositoryInformation(String id, String name, String location, EndpointType type) {
		props.setProperty("id", id);
		props.setProperty("name", name);
		props.setProperty("location", location);
		this.type = type;
	}

	protected RepositoryInformation(EndpointType type) {
		this.type = type;
	}

	public String getId() {
		return props.getProperty("id");
	}

	public String getName() {
		return props.getProperty("name");
	}

	public String getLocation() {
		return props.getProperty("location");
	}

	public EndpointType getType() {
		return type;
	}

	/**
	 * @return the optional {@link EndpointConfiguration} or <code>null</code>
	 */
	public EndpointConfiguration getEndpointConfiguration() {
		return endpointConfiguration;
	}

	public void setEndpointConfiguration(EndpointConfiguration endpointConfiguration) {
		this.endpointConfiguration = endpointConfiguration;
	}

	public String get(String key) {
		return props.getProperty(key);
	}

	public String get(String key, String def) {
		return props.getProperty(key, def);
	}

	public void setProperty(String key, String value) {
		props.setProperty(key, value);
	}

	public void setType(EndpointType type) {
		this.type = type;
	}

	/**
	 * @return the writable
	 */
	public boolean isWritable() {
		return writable;
	}

	/**
	 * @param writable the writable to set
	 */
	public void setWritable(boolean writable) {
		this.writable = writable;
	}
}
