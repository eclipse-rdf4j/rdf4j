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
package org.eclipse.rdf4j.sail.elasticsearchstore;

import java.util.Map;

class BuilderAndSha {
	private final String sha256;
	private final Map<String, Object> map;

	BuilderAndSha(String sha256, Map<String, Object> map) {
		this.sha256 = sha256;
		this.map = map;
	}

	String getSha256() {
		return sha256;
	}

	Map<String, Object> getMap() {
		return map;
	}
}
