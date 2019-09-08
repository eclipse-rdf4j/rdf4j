/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.endpoint.provider;

import java.util.Properties;

import com.fluidops.fedx.endpoint.EndpointConfiguration;
import com.fluidops.fedx.endpoint.EndpointType;

public class RepositoryInformation {

	protected Properties props = new Properties();
	private EndpointType type = EndpointType.Other;	// the endpoint type, default Other
	private EndpointConfiguration endpointConfiguration;	// optional configuration settings for the endpoint
	
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
}
