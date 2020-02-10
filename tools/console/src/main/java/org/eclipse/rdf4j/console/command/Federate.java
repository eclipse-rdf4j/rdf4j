/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.runtime.RepositoryManagerFederator;

/**
 * Implements the 'federate' command for the RDF4J Console.
 *
 * @author Dale Visser
 */
public class Federate extends ConsoleCommand {
	@Override
	public String getName() {
		return "federate";
	}

	@Override
	public String getHelpShort() {
		return "Federate existing repositories";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE
				+ "federate [distinct=<true|false>] [readonly=<true|false>] <fedID> <repoID_1> <repoID_2> [<repoID_n>]*\n"
				+ "  [distinct=<true|false>]  If true, uses a DISTINCT filter that suppresses duplicate results for identical quads\n"
				+ "                           from different federation members. Default is false.\n"
				+ "  [readonly=<true|false>]  If true, sets the fedearated repository as read-only. If any member is read-only, then\n"
				+ "                           this may only be set to true. Default is true. \n"
				+ "  <fedId>                  The id to assign the federated repository.\n"
				+ "  <repoID1> <repoID2>      The id's of at least 2 repositories to federate.\n"
				+ "  [<repoID_n>]*            The id's of 0 or mare additional repositories to federate.\n\n"
				+ "You will be prompted to enter a description for the federated repository as well.";
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 */
	public Federate(ConsoleIO consoleIO, ConsoleState state) {
		super(consoleIO, state);
	}

	/**
	 * Executes a 'federate' command for the RDF4J Console.
	 *
	 * @param parameters the expectations for the tokens in this array are fully documented in {@link PrintHelp} .
	 * @throws java.io.IOException
	 */
	@Override
	public void execute(String... parameters) throws IOException {
		if (parameters.length < 4) {
			writeln(getHelpLong());
		} else {
			LinkedList<String> plist = new LinkedList<>(Arrays.asList(parameters));
			plist.remove(); // "federate"
			boolean distinct = getOptionalParamValue(plist, "distinct", false);
			boolean readonly = getOptionalParamValue(plist, "readonly", true);

			if (distinctValues(plist)) {
				String fedID = plist.pop();
				federate(distinct, readonly, fedID, plist);
			} else {
				writeError("Duplicate repository id's specified.");
			}
		}
	}

	/**
	 * Check if all values are distinct
	 * 
	 * @param plist
	 * @return true if all values are distinct
	 */
	private boolean distinctValues(Deque<String> plist) {
		return plist.size() == new HashSet<>(plist).size();
	}

	/**
	 * Add one or more repositories to a repository federation
	 * 
	 * @param distinct
	 * @param readonly  true when all
	 * @param fedID
	 * @param memberIDs list of member
	 */
	private void federate(boolean distinct, boolean readonly, String fedID, Deque<String> memberIDs) {
		if (LOGGER.isDebugEnabled()) {
			logCallDetails(distinct, readonly, fedID, memberIDs);
		}
		RepositoryManager manager = state.getManager();
		try {
			if (manager.hasRepositoryConfig(fedID)) {
				writeError(fedID + " already exists.");
			} else if (validateMembers(manager, readonly, memberIDs)) {
				String description = consoleIO.readln("Federation Description (optional): ");
				RepositoryManagerFederator rmf = new RepositoryManagerFederator(manager);
				rmf.addFed(fedID, description, memberIDs, readonly, distinct);
				writeln("Federation created.");
			}
		} catch (RepositoryConfigException | RepositoryException | MalformedURLException rce) {
			writeError("Federation failed", rce);
		} catch (RDF4JException | IOException rce) {
			writeError("I/O exception on federation", rce);
		}
	}

	/**
	 * Validate members of a federation
	 * 
	 * @param manager   repository manager
	 * @param readonly  set to true if read-only repositories are OK
	 * @param memberIDs IDs of the federated repositories
	 * @return true when all members are present
	 */
	private boolean validateMembers(RepositoryManager manager, boolean readonly, Deque<String> memberIDs) {
		boolean result = true;
		try {
			for (String memberID : memberIDs) {
				if (manager.hasRepositoryConfig(memberID)) {
					if (!readonly) {
						if (!manager.getRepository(memberID).isWritable()) {
							result = false;
							writeError(memberID + " is read-only.");
						}
					}
				} else {
					result = false;
					writeError(memberID + " does not exist.");
				}
			}
		} catch (RepositoryException | RepositoryConfigException re) {
			writeError(re.getMessage());
		}
		return result;
	}

	/**
	 * Log basic details about calls to federated repositories
	 * 
	 * @param distinct
	 * @param readonly
	 * @param fedID
	 * @param memberIDs
	 */
	private void logCallDetails(boolean distinct, boolean readonly, String fedID, Deque<String> memberIDs) {
		StringBuilder builder = new StringBuilder();
		builder.append("Federate called with federation ID = " + fedID + ", and member ID's = ");

		for (String member : memberIDs) {
			builder.append("[").append(member).append("]");
		}
		builder.append(".\n  Distinct set to ")
				.append(distinct)
				.append(", and readonly set to ")
				.append(readonly)
				.append(".\n");
		LOGGER.debug(builder.toString());
	}

	/**
	 * Get the value of an optional boolean parameter or the default
	 * 
	 * @param parameters   set of parameters
	 * @param name         name of the parameter
	 * @param defaultValue default value
	 * @return value or default
	 */
	private boolean getOptionalParamValue(Deque<String> parameters, String name, boolean defaultValue) {
		return Boolean.parseBoolean(getOptionalParamValue(parameters, name, Boolean.toString(defaultValue)));
	}

	/**
	 * Get the value of an optional string parameter or the default
	 * 
	 * @param parameters   set of parameters
	 * @param name         parameter name
	 * @param defaultValue default string value
	 * @return value or default
	 */
	private String getOptionalParamValue(Deque<String> parameters, String name, String defaultValue) {
		String result = defaultValue;

		for (String parameter : parameters) {
			if (parameter.length() >= name.length() && parameter.substring(0, name.length()).equalsIgnoreCase(name)) {
				String[] parsed = parameter.split("=");

				if (parsed.length == 2 && parsed[0].equalsIgnoreCase(name)) {
					result = parsed[1].toLowerCase();
					parameters.remove(parameter);
					break;
				}
			}
		}
		return result;
	}
}
