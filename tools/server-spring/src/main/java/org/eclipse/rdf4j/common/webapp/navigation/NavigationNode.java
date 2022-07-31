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
 * NavigationNode represents a node in a NavigationModel.
 *
 * @author Herko ter Horst
 */
public interface NavigationNode extends Cloneable {

	/**
	 * Get the ID of the node.
	 *
	 * @return the ID of the node
	 */
	String getId();

	/**
	 * Is the node hidden?
	 *
	 * @return true if the node is hidden, false otherwise
	 */
	boolean isHidden();

	/**
	 * Set the hidden status of the node.
	 *
	 * @param hidden the new hidden status of the node
	 */
	void setHidden(boolean hidden);

	/**
	 * Is the node enabled/active?
	 *
	 * @return true if the node is enabled, false otherwise
	 */
	boolean isEnabled();

	/**
	 * Set the enabled status of the node.
	 *
	 * @param enabled the new enabled status of the node
	 */
	void setEnabled(boolean enabled);

	/**
	 * Get the parent node of this node.
	 *
	 * @return the parent node of this node, or null if this node is the root NavigationModel
	 */
	NavigationNode getParent();

	/**
	 * Set the parent of this node.
	 *
	 * @param parent the new parent of this node
	 */
	void setParent(NavigationNode parent);

	/**
	 * Is this node a parent of the specified node?
	 *
	 * @param node the node to check
	 * @return true if this node is a direct or indirect parent of the specified node, false otherwise
	 */
	boolean isParent(NavigationNode node);

	/**
	 * Get the depth of this node in the hierarchy. The root NavigationModel has depth 0, all other nodes have a depth
	 * equal to the depth of their parent + 1.
	 *
	 * @return the depth of the node in the hierarhcy
	 */
	int getDepth();

	String getPathPrefix();

	String getPathSeparator();

	String getPath();

	void setPath(String path);

	String getIconPrefix();

	String getIconSeparator();

	String getIconSuffix();

	String getIcon();

	void setIcon(String icon);

	String getI18nPrefix();

	String getI18nSeparator();

	String getI18nSuffix();

	String getI18n();

	void setI18n(String i18n);

	String getViewSuffix();

	void setViewSuffix(String suffix);
}
