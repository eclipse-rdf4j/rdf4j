/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.http.helpers;

import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.RioSettingImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class encapsulates configuration settings specific for {@link HTTPRepository}.
 *
 * @author Jacek Grzebyta
 */
public class HTTPRepositorySettings {

	private static final Logger log = LoggerFactory.getLogger(HTTPRepositorySettings.class);

	/**
	 * Maximum size (in number of statements) allowed for statement buffers before they are forcibly flushed.
	 * <p>
	 * By default inner buffers within {@link org.eclipse.rdf4j.repository.http.HTTPRepositoryConnection} keep in memory
	 * up to 200000 statement before they are flushed to the remote repository.
	 */
	public static final RioSetting<Integer> MAX_STATEMENT_BUFFER_SIZE = new RioSettingImpl<>(
			"org.eclipse.rdf4j.http.maxstatementbuffersize", "Maximum number of statement buffered in memory", 200000);

}
