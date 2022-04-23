/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.webapp.navigation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Group represents a logical group of views in a NavigationModel.
 *
 * @author Herko ter Horst
 */
public class Group extends AbstractNavigationNode {

	protected Map<String, Group> groups;

	protected Map<String, View> views;

	protected Map<String, View> viewNames;

	/**
	 * Construct a new group with the specified ID.
	 *
	 * @param id the ID of the group
	 */
	public Group(String id) {
		super(id);
		groups = new LinkedHashMap<>();
		views = new LinkedHashMap<>();
		viewNames = new LinkedHashMap<>();
	}

	/**
	 * Add a group to this group. The group becomes a sub-group of this group.
	 *
	 * @param group the group to add
	 */
	public void addGroup(Group group) {
		group.setParent(this);
		groups.put(group.getId(), group);
	}

	/**
	 * Get the sub-group with the specified ID.
	 *
	 * @param id the ID of the sub-group
	 * @return the sub-group with the specified ID, or null if this group doesn't contain a sub-group with that ID
	 */
	public Group getGroup(String id) {
		return groups.get(id);
	}

	/**
	 * Get the list of sub-groups
	 *
	 * @return the list of sub-groups
	 */
	public List<Group> getGroups() {
		return new ArrayList<>(groups.values());
	}

	/**
	 * Add a view to this group.
	 *
	 * @param view the view to add
	 */
	public void addView(View view) {
		view.setParent(this);
		views.put(view.getId(), view);
		viewNames.put(view.getId() + view.getViewSuffix(), view);
	}

	public View getView(String viewId) {
		return views.get(viewId);
	}

	/**
	 * Get the view with the specified name.
	 *
	 * @param viewName the name of the view (ID+suffix)
	 * @return the view with the specified name, or null if this group doesn't contain a view with that name
	 */
	public View getViewByName(String viewName) {
		return viewNames.get(viewName);
	}

	protected View findViewInternal(String viewName) {
		View result;

		int indexOfSeparator = viewName.indexOf(getPathSeparator());
		if (indexOfSeparator > 0) {
			String groupId = viewName.substring(0, indexOfSeparator);
			Group subGroup = getGroup(groupId);
			result = subGroup.findViewInternal(viewName.substring(indexOfSeparator + 1));
		} else {
			result = getViewByName(viewName);
		}

		return result;
	}

	/**
	 * Get the list of views.
	 *
	 * @return the list of views
	 */
	public List<View> getViews() {
		return new ArrayList<>(views.values());
	}

	@Override
	public Object clone() {
		Group result = new Group(getId());
		copyCommonAttributes(result);
		copyGroupsAndViews(result);
		return result;
	}

	protected void copyGroupsAndViews(Group group) {
		for (Group subGroup : getGroups()) {
			Group clonedGroup = (Group) subGroup.clone();
			group.addGroup(clonedGroup);
		}
		for (View view : getViews()) {
			View clonedView = (View) view.clone();
			group.addView(clonedView);
		}
	}
}
