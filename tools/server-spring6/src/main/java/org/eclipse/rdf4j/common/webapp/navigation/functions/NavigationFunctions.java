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
package org.eclipse.rdf4j.common.webapp.navigation.functions;

import org.eclipse.rdf4j.common.webapp.navigation.NavigationNode;

/**
 * JSTL functions for navigation.
 *
 * @author Herko ter Horst
 */
public class NavigationFunctions {

	/**
	 * Is the specified potential parent indeed a parent of the specified node.
	 *
	 * @param potentialParent the potential parent
	 * @param node            the node
	 * @return true if the potential parent is part of the hierarchical string of parents for the specified node
	 */
	public static boolean isParent(NavigationNode potentialParent, NavigationNode node) {
		return potentialParent.isParent(node);
	}
}
