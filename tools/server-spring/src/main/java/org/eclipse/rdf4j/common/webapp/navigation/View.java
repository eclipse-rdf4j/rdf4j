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
package org.eclipse.rdf4j.common.webapp.navigation;

/**
 * View represents a "leaf" in the navigation model, for example a page in a website.
 *
 * @author Herko ter Horst
 */
public class View extends AbstractNavigationNode {

	/**
	 * Construct a new view with the specified ID.
	 *
	 * @param id the ID of the view
	 */
	public View(String id) {
		super(id);
	}

	@Override
	public String getPath() {
		if (path == null) {
			StringBuilder result = new StringBuilder();
			result.append(super.getPath());
			result.append(getViewSuffix());
			setPath(result.toString());
		}
		return path;
	}

	@Override
	public Object clone() {
		View result = new View(getId());
		copyCommonAttributes(result);
		return result;
	}
}
