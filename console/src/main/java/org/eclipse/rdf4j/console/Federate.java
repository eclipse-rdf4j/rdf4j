/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.runtime.RepositoryManagerFederator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the 'federate' command for the RDF4J Console.
 *
 * @author Dale Visser
 */
public class Federate implements Command {

	private static final Logger LOGGER = LoggerFactory.getLogger(Federate.class);

	private final ConsoleIO cio;
	private final ConsoleState state;

	/**
	 * Constructor
	 * 
	 * @param cio
	 * @param state 
	 */
	protected Federate(ConsoleIO cio, ConsoleState state) {
		this.cio = cio;
		this.state = state;
	}

	/**
	 * Executes a 'federate' command for the Sesame Console.
	 *
	 * @param parameters the expectations for the tokens in this array are fully documented in
	 * {@link PrintHelp#FEDERATE} .
	 * @throws java.io.IOException
	 */
	@Override
	public void execute(String... parameters) throws IOException {
		if (parameters.length < 4) {
			cio.writeln(PrintHelp.FEDERATE);
		} else {
			LinkedList<String> plist = new LinkedList<>(Arrays.asList(parameters));
			plist.remove(); // "federate"
			boolean distinct = getOptionalParamValue(plist, "distinct", false);
			boolean readonly = getOptionalParamValue(plist, "readonly", true);
			
			if (distinctValues(plist)) {
				String fedID = plist.pop();
				federate(distinct, readonly, fedID, plist);
			} else {
				cio.writeError("Duplicate repository id's specified.");
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
	 * @param readonly true when all 
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
				cio.writeError(fedID + " already exists.");
			} else if (validateMembers(manager, readonly, memberIDs)) {
				String description = cio.readln("Federation Description (optional): ");
				RepositoryManagerFederator rmf = new RepositoryManagerFederator(manager);
				rmf.addFed(fedID, description, memberIDs, readonly, distinct);
				cio.writeln("Federation created.");
			}
		} catch (RepositoryConfigException rce) {
			cio.writeError(rce.getMessage());
		} catch (RepositoryException re) {
			cio.writeError(re.getMessage());
		} catch (MalformedURLException mue) {
			cio.writeError(mue.getMessage());
		} catch (RDF4JException ore) {
			cio.writeError(ore.getMessage());
		} catch (IOException ioe) {
			cio.writeError(ioe.getMessage());
		}
	}

	/**
	 * Validate members of a federation
	 * 
	 * @param manager repository manager
	 * @param readonly set to true if read-only repositories are OK
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
							cio.writeError(memberID + " is read-only.");
						}
					}
				} else {
					result = false;
					cio.writeError(memberID + " does not exist.");
				}
			}
		} catch (RepositoryException re) {
			cio.writeError(re.getMessage());
		} catch (RepositoryConfigException rce) {
			cio.writeError(rce.getMessage());
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
		builder.append(".\n  Distinct set to ").append(distinct).append(", and readonly set to ").append(
				readonly).append(".\n");
		LOGGER.debug(builder.toString());
	}

	/**
	 * Get the value of an optional boolean parameter or the default
	 * 
	 * @param parameters set of parameters
	 * @param name name of the parameter
	 * @param defaultValue default value
	 * @return value or default
	 */
	private boolean getOptionalParamValue(Deque<String> parameters, String name, boolean defaultValue) {
		return Boolean.parseBoolean(getOptionalParamValue(parameters, name, Boolean.toString(defaultValue)));
	}

	/**
	 * Get the value of an optional string parameter or the default
	 * 
	 * @param parameters set of parameters
	 * @param name parameter name
	 * @param defaultValue default string value
	 * @return value or default
	 */
	private String getOptionalParamValue(Deque<String> parameters, String name, String defaultValue) {
		String result = defaultValue;
		
		for (String parameter : parameters) {
			if (parameter.length() >= name.length()
					&& parameter.substring(0, name.length()).equalsIgnoreCase(name)) {
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
