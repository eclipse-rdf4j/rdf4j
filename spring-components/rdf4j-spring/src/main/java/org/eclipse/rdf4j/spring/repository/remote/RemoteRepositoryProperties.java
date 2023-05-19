/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.repository.remote;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Gabriel Pickl
 * @author Florian Kleedorfer
 * @since 4.0.0
 */
@ConfigurationProperties(prefix = "rdf4j.spring.repository.remote")
public class RemoteRepositoryProperties {

	/**
	 * URL of the SPARQL endpoint
	 */
	@NotBlank
	@Pattern(regexp = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
	private String managerUrl = null;

	/**
	 * Optional username of the SPARQL endpoint
	 */
	private String username = null;

	/**
	 * Optional password of the SPARQL endpoint
	 */
	private String password = null;

	/**
	 * Name of the repository
	 */
	@NotBlank
	@Length(min = 1)
	private String name = null;

	public String getManagerUrl() {
		return managerUrl;
	}

	public void setManagerUrl(String managerUrl) {
		this.managerUrl = managerUrl;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isUsernamePasswordConfigured() {
		return username != null && password != null;
	}

	@Override
	public String toString() {
		return "RemoteRepositoryConfig{"
				+ "managerUrl='" + managerUrl + "'"
				+ (username != null ? ", username='" + username + "'" : "")
				+ (password != null ? ", password='****'" : "")
				+ ", name='" + name + "' }";
	}
}
