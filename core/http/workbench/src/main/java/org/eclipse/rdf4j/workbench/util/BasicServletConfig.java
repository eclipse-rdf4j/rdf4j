/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class BasicServletConfig implements ServletConfig {
	private String name;
	private ServletContext context;
	private Hashtable<String, String> params;

	public BasicServletConfig(String name, ServletContext context) {
		this.name = name;
		this.context = context;
		params = new Hashtable<String, String>();
	}

	@SuppressWarnings("unchecked")
	public BasicServletConfig(String name, ServletConfig config) {
		this(name, config.getServletContext());
		Enumeration<String> e = config.getInitParameterNames();
		while (e.hasMoreElements()) {
			String param = e.nextElement();
			params.put(param, config.getInitParameter(param));
		}
	}

	public BasicServletConfig(String name, ServletConfig config,
			Map<String, String> params) {
		this(name, config);
		this.params.putAll(params);
	}

	public BasicServletConfig(String name, ServletContext context,
			Map<String, String> params) {
		this.name = name;
		this.context = context;
		this.params = new Hashtable<String, String>(params);
	}

	public String getServletName() {
		return name;
	}

	public ServletContext getServletContext() {
		return context;
	}

	public Enumeration<String> getInitParameterNames() {
		return params.keys();
	}

	public String getInitParameter(String name) {
		return params.get(name);
	}

}
