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
package org.eclipse.rdf4j.sail.helpers;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.StackableSail;

/**
 * Defines utility methods for working with Sails.
 */
public class SailUtil {

	/**
	 * Searches a stack of Sails from top to bottom for a Sail that is an instance of the suppied class or interface.
	 * The first Sail that matches (i.e. the one closest to the top) is returned.
	 *
	 * @param topSail   The top of the Sail stack.
	 * @param sailClass A class or interface.
	 * @return A Sail that is an instance of sailClass, or null if no such Sail was found.
	 */
	@SuppressWarnings("unchecked")
	public static <C extends Sail> C findSailInStack(Sail topSail, Class<C> sailClass) {
		if (sailClass == null) {
			return null;
		}

		// Check Sails on the stack one by one, starting with the top-most.
		Sail currentSail = topSail;

		while (currentSail != null) {
			// Check current Sail
			if (sailClass.isInstance(currentSail)) {
				break;
			}

			// Current Sail is not an instance of sailClass, check the
			// rest of the stack
			if (currentSail instanceof StackableSail) {
				currentSail = ((StackableSail) currentSail).getBaseSail();
			} else {
				// We've reached the bottom of the stack
				currentSail = null;
			}
		}

		return (C) currentSail;
	}
}
