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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.io.ResourceUtil;

/**
 * NavigationModel represents the navigation structure of a web application. A model consists of groups and views.
 *
 * @author Herko ter Horst
 */
public class NavigationModel extends Group {

	public static final String NAVIGATION_MODEL_KEY = "navigation-model";

	public static final String DEFAULT_PATH_PREFIX = "/";

	public static final String DEFAULT_PATH_SEPARATOR = "/";

	public static final String DEFAULT_VIEW_SUFFIX = ".view";

	public static final String DEFAULT_ICON_PREFIX = "/images/icons/";

	public static final String DEFAULT_ICON_SEPARATOR = "_";

	public static final String DEFAULT_ICON_SUFFIX = ".png";

	public static final String DEFAULT_I18N_PREFIX = "";

	public static final String DEFAULT_I18N_SEPARATOR = ".";

	public static final String DEFAULT_I18N_SUFFIX = ".title";

	private List<String> navigationModelLocations = new ArrayList<>();

	private String pathPrefix;

	private String pathSeparator;

	private String iconPrefix;

	private String iconSeparator;

	private String iconSuffix;

	private String i18nPrefix;

	private String i18nSeparator;

	private String i18nSuffix;

	/**
	 * Construct a new, anonymous, empty NavigationModel
	 */
	public NavigationModel() {
		super(null);
	}

	/**
	 * Construct a new emtpy NavigationModel with the specified ID.
	 *
	 * @param id the ID of the NavigationModel
	 */
	public NavigationModel(String id) {
		super(id);
	}

	@Override
	public String getId() {
		return "";
	}

	@Override
	public String getPathPrefix() {
		if (pathPrefix == null) {
			setPathPrefix(DEFAULT_PATH_PREFIX);
		}
		return pathPrefix;
	}

	public void setPathPrefix(String pathPrefix) {
		this.pathPrefix = pathPrefix;
	}

	@Override
	public String getPathSeparator() {
		if (pathSeparator == null) {
			setPathSeparator(DEFAULT_PATH_SEPARATOR);
		}
		return pathSeparator;
	}

	public void setPathSeparator(String pathSeparator) {
		this.pathSeparator = pathSeparator;
	}

	@Override
	public String getIconPrefix() {
		if (iconPrefix == null) {
			setIconPrefix(DEFAULT_ICON_PREFIX);
		}
		return iconPrefix;
	}

	public void setIconPrefix(String iconPrefix) {
		this.iconPrefix = iconPrefix;
	}

	@Override
	public String getIconSeparator() {
		if (iconSeparator == null) {
			setIconSeparator(DEFAULT_ICON_SEPARATOR);
		}
		return iconSeparator;
	}

	public void setIconSeparator(String iconSeparator) {
		this.iconSeparator = iconSeparator;
	}

	@Override
	public String getIconSuffix() {
		if (iconSuffix == null) {
			setIconSuffix(DEFAULT_ICON_SUFFIX);
		}
		return iconSuffix;
	}

	public void setIconSuffix(String iconSuffix) {
		this.iconSuffix = iconSuffix;
	}

	@Override
	public String getI18nPrefix() {
		if (i18nPrefix == null) {
			setI18nPrefix(DEFAULT_I18N_PREFIX);
		}
		return i18nPrefix;
	}

	public void setI18nPrefix(String i18nPrefix) {
		this.i18nPrefix = i18nPrefix;
	}

	@Override
	public String getI18nSeparator() {
		if (i18nSeparator == null) {
			setI18nSeparator(DEFAULT_I18N_SEPARATOR);
		}
		return i18nSeparator;
	}

	public void setI18nSeparator(String i18nSeparator) {
		this.i18nSeparator = i18nSeparator;
	}

	@Override
	public String getI18nSuffix() {
		if (i18nSuffix == null) {
			setI18nSuffix(DEFAULT_I18N_SUFFIX);
		}
		return i18nSuffix;
	}

	public void setI18nSuffix(String i18nSuffix) {
		this.i18nSuffix = i18nSuffix;
	}

	@Override
	public String getViewSuffix() {
		if (viewSuffix == null) {
			setViewSuffix(DEFAULT_VIEW_SUFFIX);
		}
		return viewSuffix;
	}

	/**
	 * Find the view with the specified name in the NavigationModel.
	 *
	 * @param viewName the name of the view, specified as a /-separated hierarchy of groups, where the part after the
	 *                 last / is interpreted as the name of the view itself.
	 * @return the view, or null if no view matching the specified name could be found
	 */
	public View findView(String viewName) {
		View result;

		int prefixLength = getPathPrefix().length();
		viewName = viewName.substring(prefixLength);
		result = findViewInternal(viewName);

		return result;
	}

	/**
	 * Add another NavigationModel to this one. This is done by adding all groups and view from the other model to this
	 * one.
	 *
	 * @param other the model to add to this one.
	 */
	public void addModel(NavigationModel other) {
		for (Group group : other.getGroups()) {
			addGroup(group);
		}
		for (View view : other.getViews()) {
			addView(view);
		}
	}

	/**
	 * Set the locations of the navigation model resources to be used in the construction of this model. Calling this
	 * method will cause this NavigationModel to be initialized.
	 *
	 * @param navigationModelLocations a list of resource names
	 */
	public void setNavigationModels(List<String> navigationModelLocations) {
		this.navigationModelLocations = navigationModelLocations;
		createNavigationModel();
	}

	private void createNavigationModel() {
		boolean first = true;
		for (String navigationModelLocation : navigationModelLocations) {
			NavigationXmlParser parser = new NavigationXmlParser();
			if (first) {
				parser.parseInto(this, ResourceUtil.getURL(navigationModelLocation));
				first = false;
			} else {
				addModel(parser.parse(ResourceUtil.getURL(navigationModelLocation)));
			}
		}
	}

	@Override
	public Object clone() {
		NavigationModel result = new NavigationModel(getId());
		copyCommonAttributes(result);
		copyGroupsAndViews(result);
		return result;
	}
}
