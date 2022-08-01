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
package org.eclipse.rdf4j.console.command;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.http.client.HttpClientSessionManager;
import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.http.client.SharedHttpClientSessionManager;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

/**
 * Connect command
 *
 * @author dale
 */
public class Connect extends ConsoleCommand {
	private final Disconnect disconnect;

	@Override
	public String getName() {
		return "connect";
	}

	@Override
	public String getHelpShort() {
		return "Connects to a (local or remote) set of repositories";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE
				+ "connect default                         Opens the default repository set for this console\n"
				+ "connect <dataDirectory>                 Opens the repository set in the specified data dir\n"
				+ "connect <serverURL> [user [password]]   Connects to an RDF4J server with optional credentials\n";

	}

	/**
	 * Constructor
	 *
	 * @param consoleIO
	 * @param state
	 * @param disconnect
	 */
	public Connect(ConsoleIO consoleIO, ConsoleState state, Disconnect disconnect) {
		super(consoleIO, state);
		this.disconnect = disconnect;
	}

	@Override
	public void execute(String... tokens) {
		if (tokens.length < 2) {
			writeln(getHelpLong());
			return;
		}

		final String target = tokens[1];
		if ("default".equalsIgnoreCase(target)) {
			connectDefault();
		} else {
			try {
				new URL(target);
				// target is a valid URL
				final String username = (tokens.length > 2) ? tokens[2] : null; // NOPMD
				final String password = (tokens.length > 3) ? tokens[3] : null; // NOPMD
				connectRemote(target, username, password);
			} catch (MalformedURLException e) {
				// assume target is a directory path
				connectLocal(target);
			}
		}
	}

	/**
	 * Connect to default repository
	 *
	 * @return true if connected
	 */
	public boolean connectDefault() {
		return installNewManager(new LocalRepositoryManager(this.state.getDataDirectory()), "default data directory");
	}

	/**
	 * Connect to remote repository
	 *
	 * @param url    URL of remote repository
	 * @param user   username
	 * @param passwd password
	 * @return true on success
	 */
	private boolean connectRemote(final String url, final String user, final String passwd) {
		final String pass = (passwd == null) ? "" : passwd;
		boolean result = false;
		try {
			// Ping server
			final HttpClientSessionManager client = new SharedHttpClientSessionManager();
			try {
				try (RDF4JProtocolSession httpClient = client.createRDF4JProtocolSession(url)) {
					if (user != null) {
						httpClient.setUsernameAndPassword(user, pass);
					}
					// Ping the server
					httpClient.getServerProtocol();
				}
			} finally {
				client.shutDown();
			}

			final RemoteRepositoryManager manager = new RemoteRepositoryManager(url);
			manager.setUsernameAndPassword(user, pass);
			result = installNewManager(manager, url);
		} catch (UnauthorizedException e) {
			if (user != null && pass.length() > 0) {
				writeError("Authentication for user '" + user + "' failed");
			} else {
				// Ask user for credentials
				try {
					writeln("Authentication required");
					final String username = consoleIO.readln("Username: ");
					final String password = consoleIO.readPassword("Password: ");
					connectRemote(url, username, password);
				} catch (IOException ioe) {
					writeError("Failed to read user credentials", ioe);
				}
			}
		} catch (IOException | RepositoryException e) {
			writeError("Failed to access the server", e);
		}

		return result;
	}

	/**
	 * Connect to local repository
	 *
	 * @param path directory of the local repository
	 * @return true on success
	 */
	public boolean connectLocal(final String path) {
		final File dir = new File(path);
		boolean result = false;
		if (dir.exists() && dir.isDirectory()) {
			result = installNewManager(new LocalRepositoryManager(dir), dir.toString());
		} else {
			writeError("Specified path is not an (existing) directory: " + path);
		}
		return result;
	}

	/**
	 * Install and initialize new repository manager
	 *
	 * @param newManager   repository manager
	 * @param newManagerID repository manager ID
	 * @return true on success
	 */
	private boolean installNewManager(final RepositoryManager newManager, final String newManagerID) {
		boolean installed = false;
		final String managerID = this.state.getManagerID();

		if (newManagerID.equals(managerID)) {
			writeln("Already connected to " + managerID);
			installed = true;
		} else {
			try {
				newManager.init();
				disconnect.execute(false);

				this.state.setManager(newManager);
				this.state.setManagerID(newManagerID);
				writeln("Connected to " + newManagerID);

				installed = true;
			} catch (RepositoryException e) {
				writeError("Failed to install new manager", e);
			}
		}
		return installed;
	}

	/**
	 * Connect to remote repository without username of password
	 *
	 * @param url URL of the repository
	 * @return true on success
	 */
	public boolean connectRemote(final String url) {
		return connectRemote(url, null, null);
	}
}
