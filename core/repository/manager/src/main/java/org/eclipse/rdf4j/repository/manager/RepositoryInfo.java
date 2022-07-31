/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.manager;

import java.net.URL;
import java.text.Collator;

/**
 * Repository meta-information such as its id, location, description.
 *
 * @author Jeen Broekstra
 */
public class RepositoryInfo implements Comparable<RepositoryInfo> {

	private String id;

	private URL location;

	private String description;

	private boolean readable;

	private boolean writable;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public URL getLocation() {
		return location;
	}

	public void setLocation(URL location) {
		this.location = location;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isReadable() {
		return readable;
	}

	public void setReadable(boolean readable) {
		this.readable = readable;
	}

	public boolean isWritable() {
		return writable;
	}

	public void setWritable(boolean writable) {
		this.writable = writable;
	}

	@Override
	public int compareTo(RepositoryInfo o) {
		int result = Collator.getInstance().compare(getDescription(), o.getDescription());
		if (result == 0) {
			result = getId().compareTo(o.getId());
		}
		return result;
	}
}
