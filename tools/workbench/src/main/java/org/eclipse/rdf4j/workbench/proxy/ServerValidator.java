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
package org.eclipse.rdf4j.workbench.proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates a server
 */
class ServerValidator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerValidator.class);

	private static final String ACCEPTED_SERVER = "accepted-server-prefixes";

	private final String prefixes;

	protected ServerValidator(final ServletConfig config) {
		this.prefixes = config.getInitParameter(ACCEPTED_SERVER);
	}

	private boolean isDirectory(final String server) {
		boolean isDir = false;
		try {
			final URL url = new URL(server);
			isDir = asLocalFile(url).isDirectory();
		} catch (MalformedURLException e) {
			LOGGER.warn(e.toString(), e);
		} catch (IOException e) {
			LOGGER.warn(e.toString(), e);
		}
		return isDir;
	}

	/**
	 * Returns whether the given server can be connected to.
	 *
	 * @param server   the server path
	 * @param password the optional password
	 * @param user     the optional username
	 * @return true, if the given server can be connected to
	 */
	protected boolean isValidServer(final String server) {
		boolean isValid = checkServerPrefixes(server);
		if (isValid) {
			if (server.startsWith("http")) {
				isValid = canConnect(server);
			} else if (server.startsWith("file:")) {
				isValid = isDirectory(server);
			}
		}
		return isValid;
	}

	/**
	 * Returns whether the server prefix is in the list of acceptable prefixes, as given by the space-separated
	 * configuration parameter value for 'accepted-server-prefixes'.
	 *
	 * @param server the server for which to check the prefix
	 * @return whether the server prefix is in the list of acceptable prefixes
	 */
	private boolean checkServerPrefixes(final String server) {
		boolean accept = false;
		if (prefixes == null) {
			accept = true;
		} else {
			for (String prefix : prefixes.split(" ")) {
				if (server.startsWith(prefix)) {
					accept = true;
					break;
				}
			}
		}
		if (!accept) {
			LOGGER.warn("server URL {} does not have a prefix {}", server, prefixes);
		}
		return accept;
	}

	/**
	 * Assumption: server won't require credentials to access the protocol path.
	 */
	private boolean canConnect(final String server) {
		boolean success = false;
		try {
			final URL url = new URL(server + "/protocol");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
				Integer.parseInt(reader.readLine());
				success = true;
			}
		} catch (NumberFormatException | IOException e) {
			LOGGER.warn(e.toString(), e);
		}
		return success;
	}

	private File asLocalFile(final URL rdf) throws UnsupportedEncodingException {
		return new File(URLDecoder.decode(rdf.getFile(), StandardCharsets.UTF_8));
	}
}
