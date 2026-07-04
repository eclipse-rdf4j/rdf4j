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

import java.io.IOException;
import java.net.URL;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.rdf4j.common.xml.DocumentUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XPath-based parser for NavigationModel configuration files.
 *
 * @author Herko ter Horst
 */
public class NavigationXmlParser {

	private final XPath xpath = XPathFactory.newInstance().newXPath();

	public NavigationModel parse(URL navigationXml) {
		NavigationModel result = new NavigationModel();
		parseInto(result, navigationXml);
		return result;
	}

	public void parseInto(NavigationModel result, URL navigationXml) {
		try {
			Document document = DocumentUtil.getDocument(navigationXml);
			Node rootNode = (Node) xpath.evaluate("/navigation", document, XPathConstants.NODE);
			fillModel(result, rootNode);
		} catch (IOException | XPathExpressionException e) {
			e.printStackTrace();
		}
	}

	private void fillModel(NavigationModel result, Node modelNode) throws XPathExpressionException {
		String id = xpath.evaluate("@id", modelNode);
		result.setId(id);

		String pathPrefix = xpath.evaluate("path-prefix", modelNode);
		if (!"".equals(pathPrefix)) {
			result.setPathPrefix(pathPrefix);
		}
		String pathSeparator = xpath.evaluate("path-separator", modelNode);
		if (!"".equals(pathSeparator)) {
			result.setPathSeparator(pathSeparator);
		}

		String iconPrefix = xpath.evaluate("icon-prefix", modelNode);
		if (!"".equals(iconPrefix)) {
			result.setIconPrefix(iconPrefix);
		}
		String iconSeparator = xpath.evaluate("icon-separator", modelNode);
		if (!"".equals(iconSeparator)) {
			result.setIconSeparator(iconSeparator);
		}
		String iconSuffix = xpath.evaluate("icon-suffix", modelNode);
		if (!"".equals(iconSuffix)) {
			result.setIconSuffix(iconSuffix);
		}

		String i18nPrefix = xpath.evaluate("i18n-prefix", modelNode);
		if (!"".equals(i18nPrefix)) {
			result.setI18nPrefix(i18nPrefix);
		}
		String i18nSeparator = xpath.evaluate("i18n-separator", modelNode);
		if (!"".equals(i18nSeparator)) {
			result.setI18nSeparator(i18nSeparator);
		}
		String i18nSuffix = xpath.evaluate("i18n-suffix", modelNode);
		if (!"".equals(i18nSuffix)) {
			result.setI18nSuffix(i18nSuffix);
		}

		setAttributes(result, modelNode);

		setGroupsAndViews(result, modelNode);
	}

	private void setAttributes(NavigationNode navNode, Node xmlNode) throws XPathExpressionException {
		boolean hidden = getBooleanAttribute(xpath.evaluate("@hidden", xmlNode), false);
		navNode.setHidden(hidden);

		boolean enabled = getBooleanAttribute(xpath.evaluate("@enabled", xmlNode), true);
		navNode.setEnabled(enabled);

		String path = xpath.evaluate("path", xmlNode);
		if (!"".equals(path)) {
			navNode.setPath(path);
		}

		String icon = xpath.evaluate("icon", xmlNode);
		if (!"".equals(icon)) {
			navNode.setIcon(icon);
		}

		String i18n = xpath.evaluate("i18n", xmlNode);
		if (!"".equals(i18n)) {
			navNode.setI18n(i18n);
		}

		String viewSuffix = xpath.evaluate("view-suffix", xmlNode);
		if (!"".equals(viewSuffix)) {
			navNode.setViewSuffix(viewSuffix);
		}
	}

	private void setGroupsAndViews(Group parent, Node xmlNode) throws XPathExpressionException {
		NodeList groupList = (NodeList) xpath.evaluate("group", xmlNode, XPathConstants.NODESET);
		int groupCount = groupList.getLength();
		for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
			Node groupNode = groupList.item(groupIndex);

			Group group = new Group(xpath.evaluate("@id", groupNode));
			parent.addGroup(group);
			setAttributes(group, groupNode);
			setGroupsAndViews(group, groupNode);
		}

		NodeList viewList = (NodeList) xpath.evaluate("view", xmlNode, XPathConstants.NODESET);
		int viewCount = viewList.getLength();
		for (int viewIndex = 0; viewIndex < viewCount; viewIndex++) {
			Node viewNode = viewList.item(viewIndex);

			View view = new View(xpath.evaluate("@id", viewNode));
			parent.addView(view);
			setAttributes(view, viewNode);
		}
	}

	private boolean getBooleanAttribute(String attrValue, boolean defaultValue) {
		boolean result = defaultValue;
		if (attrValue != null && !attrValue.trim().isEmpty()) {
			result = attrValue.equalsIgnoreCase("true") || attrValue.equalsIgnoreCase("yes")
					|| attrValue.equalsIgnoreCase("on");
		}
		return result;
	}
}
