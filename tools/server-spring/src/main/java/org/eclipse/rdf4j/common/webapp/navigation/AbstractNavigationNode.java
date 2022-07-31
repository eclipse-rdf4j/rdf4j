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
 * Base implementation of the NavigationNode interface.
 *
 * @author Herko ter Horst
 */
public abstract class AbstractNavigationNode implements NavigationNode {

	private String id;

	private boolean hidden;

	private boolean enabled;

	private NavigationNode parent;

	protected String path;

	protected String icon;

	protected String i18n;

	protected String viewSuffix;

	public AbstractNavigationNode(String id) {
		setId(id);
		setEnabled(true);
	}

	@Override
	public String getId() {
		return id;
	}

	void setId(String id) {
		this.id = id;
	}

	@Override
	public boolean isHidden() {
		return hidden;
	}

	@Override
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public NavigationNode getParent() {
		return parent;
	}

	@Override
	public void setParent(NavigationNode parent) {
		this.parent = parent;
	}

	@Override
	public boolean isParent(NavigationNode node) {
		boolean result = false;

		if (node != null && node != this) {
			if (node.getParent() == this) {
				result = true;
			} else if (node.getParent() != null) {
				result = isParent(node.getParent());
			}
		}

		return result;
	}

	@Override
	public String getPathPrefix() {
		StringBuilder result = new StringBuilder();
		if (getParent() != null) {
			if (getParent().getPathPrefix() != null) {
				result.append(getParent().getPathPrefix());
			}
			if (getParent().getId().length() > 0) {
				result.append(getParent().getId());
				result.append(getPathSeparator());
			}
		}
		return result.toString();
	}

	@Override
	public String getPathSeparator() {
		String result = null;
		if (getParent() != null) {
			result = getParent().getPathSeparator();
		}
		return result;
	}

	@Override
	public String getPath() {
		if (path == null) {
			StringBuilder result = new StringBuilder();
			result.append(getPathPrefix());
			result.append(getId());
			setPath(result.toString());
		}
		return path;
	}

	@Override
	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String getIconPrefix() {
		StringBuilder result = new StringBuilder();
		if (getParent() != null) {
			if (getParent().getIconPrefix() != null) {
				result.append(getParent().getIconPrefix());
			}
			if (getParent().getId().length() > 0) {
				result.append(getParent().getId());
				result.append(getIconSeparator());
			}
		}
		return result.toString();
	}

	@Override
	public String getIconSeparator() {
		String result = null;
		if (getParent() != null) {
			result = getParent().getIconSeparator();
		}
		return result;
	}

	@Override
	public String getIconSuffix() {
		String result = null;
		if (getParent() != null) {
			result = getParent().getIconSuffix();
		}
		return result;
	}

	@Override
	public String getIcon() {
		if (icon == null) {
			StringBuilder result = new StringBuilder();
			result.append(getIconPrefix());
			result.append(getId());
			result.append(getIconSuffix());
			setIcon(result.toString());
		}

		return icon;
	}

	@Override
	public void setIcon(String icon) {
		this.icon = icon;
	}

	@Override
	public String getI18nPrefix() {
		StringBuilder result = new StringBuilder();
		if (getParent() != null) {
			if (getParent().getI18nPrefix() != null) {
				result.append(getParent().getI18nPrefix());
			}
			if (getParent().getId().length() > 0) {
				result.append(getParent().getId());
				result.append(getI18nSeparator());
			}
		}
		return result.toString();
	}

	@Override
	public String getI18nSeparator() {
		String result = null;
		if (getParent() != null) {
			result = getParent().getI18nSeparator();
		}
		return result;
	}

	@Override
	public String getI18nSuffix() {
		String result = null;
		if (getParent() != null) {
			result = getParent().getI18nSuffix();
		}
		return result;
	}

	@Override
	public String getI18n() {
		if (i18n == null) {
			StringBuilder result = new StringBuilder();
			result.append(getI18nPrefix());
			result.append(getId());
			result.append(getI18nSuffix());
			setI18n(result.toString());
		}
		return i18n;
	}

	@Override
	public void setI18n(String i18n) {
		this.i18n = i18n;
	}

	@Override
	public String getViewSuffix() {
		if (viewSuffix == null) {
			if (getParent() != null) {
				setViewSuffix(getParent().getViewSuffix());
			}
		}
		return viewSuffix;
	}

	@Override
	public void setViewSuffix(String viewSuffix) {
		this.viewSuffix = viewSuffix;
	}

	@Override
	public int getDepth() {
		int result = 0;

		if (getParent() != null) {
			result = getParent().getDepth() + 1;
		}

		return result;
	}

	@Override
	public boolean equals(Object other) {
		boolean result = this == other;
		if (!result && other instanceof NavigationNode && getClass().equals(other.getClass())) {
			NavigationNode otherNode = (NavigationNode) other;
			result = getId().equals(otherNode.getId());
			if (result && !(getParent() == null && otherNode.getParent() == null)) {
				if (getParent() != null && otherNode.getParent() != null) {
					result = getParent().equals(otherNode.getParent());
				} else {
					result = false;
				}
			}
		}
		return result;
	}

	@Override
	public int hashCode() {
		int result = getId().hashCode();
		if (getParent() != null) {
			result += 31 * getParent().hashCode();
		}
		return result;
	}

	protected void copyCommonAttributes(NavigationNode node) {
		node.setEnabled(isEnabled());
		node.setHidden(isHidden());
		node.setI18n(getI18n());
		node.setIcon(getIcon());
		node.setPath(getPath());
		node.setViewSuffix(getViewSuffix());
	}
}
