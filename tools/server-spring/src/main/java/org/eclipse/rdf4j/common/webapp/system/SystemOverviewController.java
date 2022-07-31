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
package org.eclipse.rdf4j.common.webapp.system;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.app.AppConfiguration;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class SystemOverviewController implements Controller {

	private String view;

	private AppConfiguration config;

	private final ServerInfo server;

	public SystemOverviewController() {
		server = new ServerInfo();
	}

	public String getView() {
		return view;
	}

	public void setView(String view) {
		this.view = view;
	}

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ModelAndView result = new ModelAndView();
		result.setViewName(view);

		Map<String, Object> model = new HashMap<>();
		model.put("appConfig", config);
		model.put("server", server);
		model.put("memory", new MemoryInfo());
		result.addAllObjects(model);

		return result;
	}

	public AppConfiguration getConfig() {
		return config;
	}

	public void setConfig(AppConfiguration config) {
		this.config = config;
	}

	public static class ServerInfo {

		private final String os;

		private final String java;

		private final String user;

		public ServerInfo() {
			os = System.getProperty("os.name") + " " + System.getProperty("os.version") + " ("
					+ System.getProperty("os.arch") + ")";
			java = System.getProperty("java.vendor") + " " + System.getProperty("java.vm.name") + " "
					+ System.getProperty("java.version");
			user = System.getProperty("user.name");
		}

		public String getOs() {
			return os;
		}

		public String getJava() {
			return java;
		}

		public String getUser() {
			return user;
		}
	}

	public static class MemoryInfo {

		private final int maximum;

		private final int used;

		private final float percentageInUse;

		public MemoryInfo() {
			Runtime runtime = Runtime.getRuntime();
			long usedMemory = runtime.totalMemory() - runtime.freeMemory();
			long maxMemory = runtime.maxMemory();

			// Memory usage (percentage)
			percentageInUse = (float) ((float) usedMemory / (float) maxMemory);

			// Memory usage in MB
			used = (int) (usedMemory / 1024 / 1024);
			maximum = (int) (maxMemory / 1024 / 1024);
		}

		public int getMaximum() {
			return maximum;
		}

		public int getUsed() {
			return used;
		}

		public float getPercentageInUse() {
			return percentageInUse;
		}
	}
}
