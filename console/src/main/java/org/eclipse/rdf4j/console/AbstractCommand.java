/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eclipse.rdf4j.console;


/**
 *
 * @author Bart Hanssens
 */
public abstract class AbstractCommand implements Command {	
	@Override
	public String getHelpShort() {
		return "No help available";
	}

	@Override
	public String getHelpLong() {
		return "No additional help available";
	}
}
