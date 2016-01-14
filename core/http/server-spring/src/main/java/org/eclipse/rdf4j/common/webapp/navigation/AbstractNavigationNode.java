/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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

	public String getId() {
		return id;
	}

	void setId(String id) {
		this.id = id;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public NavigationNode getParent() {
		return parent;
	}

	public void setParent(NavigationNode parent) {
		this.parent = parent;
	}

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

	public String getPathSeparator() {
		String result = null;
		if (getParent() != null) {
			result = getParent().getPathSeparator();
		}
		return result;
	}

	public String getPath() {
		if (path == null) {
			StringBuilder result = new StringBuilder();
			result.append(getPathPrefix());
			result.append(getId());
			setPath(result.toString());
		}
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

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

	public String getIconSeparator() {
		String result = null;
		if (getParent() != null) {
			result = getParent().getIconSeparator();
		}
		return result;
	}

	public String getIconSuffix() {
		String result = null;
		if (getParent() != null) {
			result = getParent().getIconSuffix();
		}
		return result;
	}

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

	public void setIcon(String icon) {
		this.icon = icon;
	}

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

	public String getI18nSeparator() {
		String result = null;
		if (getParent() != null) {
			result = getParent().getI18nSeparator();
		}
		return result;
	}

	public String getI18nSuffix() {
		String result = null;
		if (getParent() != null) {
			result = getParent().getI18nSuffix();
		}
		return result;
	}

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

	public void setI18n(String i18n) {
		this.i18n = i18n;
	}

	public String getViewSuffix() {
		if (viewSuffix == null) {
			if (getParent() != null) {
				setViewSuffix(getParent().getViewSuffix());
			}
		}
		return viewSuffix;
	}

	public void setViewSuffix(String viewSuffix) {
		this.viewSuffix = viewSuffix;
	}

	public int getDepth() {
		int result = 0;

		if (getParent() != null) {
			result = getParent().getDepth() + 1;
		}

		return result;
	}

	public boolean equals(Object other) {
		boolean result = this == other;
		if (!result && other instanceof NavigationNode
				&& getClass().equals(other.getClass())) {
			NavigationNode otherNode = (NavigationNode) other;
			result = getId().equals(otherNode.getId());
			if (result
					&& !(getParent() == null && otherNode.getParent() == null)) {
				if (getParent() != null && otherNode.getParent() != null) {
					result = getParent().equals(otherNode.getParent());
				} else {
					result = false;
				}
			}
		}
		return result;
	}

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
