/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.io.IOException;

/**
 * Abstraction of console commands.
 *
 * @author Dale Visser
 */
public interface Command {	
	/**
	 * Get the name of the command
	 * 
	 * @return lowercase name 
	 */
	public String getName();
	
	/**
	 * Get short help, used in list of available commands 
	 * 
	 * @return string
	 */
	public String getHelpShort();
	
	/**
	 * Get extended help, can be multiple lines 
	 * 
	 * @return string
	 */
	public String getHelpLong();
	

	/**
	 * Execute the given parameters.
	 *
	 * @param parameters parameters typed by user
	 * @throws IOException if a problem occurs reading or writing
	 */
	void execute(String... parameters) throws IOException;
}
