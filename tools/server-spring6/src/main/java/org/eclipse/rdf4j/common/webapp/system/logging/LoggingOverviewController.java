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
package org.eclipse.rdf4j.common.webapp.system.logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.app.AppConfiguration;
import org.eclipse.rdf4j.common.logging.LogLevel;
import org.eclipse.rdf4j.common.logging.LogReader;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

public class LoggingOverviewController implements Controller {

	private AppConfiguration config;

	String viewName = "system/logging/overview";

	String appenderName = null;

	String[] loglevels = { "All", LogLevel.ERROR.toString(), LogLevel.WARN.toString(), LogLevel.INFO.toString(),
			LogLevel.DEBUG.toString() };

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		int offset = getOffset(request);
		int count = getCount(request);
		Map<String, Object> model = new HashMap<>();
		LogReader logReader = getLogReader(offset, count, request);
		model.put("logreader", logReader);
		model.put("offset", offset);
		model.put("count", count);
		model.put("countsAvailable", Arrays.asList(50, 100, 200, 500));
		if (logReader.supportsLevelFilter()) {
			LogLevel level = logReader.getLevel();
			model.put("level", (level == null) ? "ALL" : level.toString());
			model.put("loglevels", Arrays.asList(this.loglevels));
		}
		if (logReader.supportsThreadFilter()) {
			String thread = logReader.getThread();
			model.put("thread", (thread == null) ? "ALL" : thread);
			List<String> l = new ArrayList<>();
			l.add("All");
			l.addAll(logReader.getThreadNames());
			model.put("threadnames", l);
		}
		if (logReader.supportsDateRanges()) {
			Calendar cal = Calendar.getInstance();
			if (logReader.getStartDate() != null) {
				cal.setTime(logReader.getStartDate());
				model.put("startDate", Boolean.TRUE);
			} else {
				cal.setTime(logReader.getMinDate());
				model.put("startDate", Boolean.FALSE);
			}
			model.put("s_year", cal.get(Calendar.YEAR));
			model.put("s_month", cal.get(Calendar.MONTH));
			model.put("s_day", cal.get(Calendar.DAY_OF_MONTH));
			model.put("s_hour", cal.get(Calendar.HOUR_OF_DAY));
			model.put("s_min", cal.get(Calendar.MINUTE));
			cal = Calendar.getInstance();
			if (logReader.getEndDate() != null) {
				cal.setTime(logReader.getEndDate());
				model.put("endDate", Boolean.TRUE);
			} else {
				cal.setTime(logReader.getMaxDate());
				model.put("endDate", Boolean.FALSE);
			}
			model.put("e_year", cal.get(Calendar.YEAR));
			model.put("e_month", cal.get(Calendar.MONTH));
			model.put("e_day", cal.get(Calendar.DAY_OF_MONTH));
			model.put("e_hour", cal.get(Calendar.HOUR_OF_DAY));
			model.put("e_min", cal.get(Calendar.MINUTE));
		}
		return new ModelAndView(this.viewName, model);
	}

	public LogReader getLogReader(int offset, int count, HttpServletRequest request) {
		LogReader logReader = (LogReader) request.getSession()
				.getAttribute("logreader" + (appenderName != null ? "+" + appenderName : ""));
		if (logReader == null) {
			if (appenderName == null) {
				logReader = config.getLogConfiguration().getDefaultLogReader();
			} else {
				logReader = config.getLogConfiguration().getLogReader(appenderName);
			}
			request.getSession()
					.setAttribute("logreader" + (appenderName != null ? "+" + appenderName : ""), logReader);
		}
		logReader.setOffset(offset);
		logReader.setLimit(count);
		if (logReader.supportsLevelFilter() && (request.getParameter("level") != null)) {
			if (request.getParameter("level").equalsIgnoreCase("ALL")) {
				logReader.setLevel(null);
			} else {
				logReader.setLevel(LogLevel.valueOf(request.getParameter("level")));
			}
		}
		if (logReader.supportsThreadFilter() && (request.getParameter("thread") != null)) {
			if (request.getParameter("thread").equalsIgnoreCase("ALL")) {
				logReader.setThread(null);
			} else {
				logReader.setThread(request.getParameter("thread"));
			}
		}
		if (logReader.supportsDateRanges() && (request.getParameter("filterapplied") != null)) {
			if (request.getParameter("applystartdate") != null) {
				Calendar cal = Calendar.getInstance();
				cal.set(Integer.parseInt(request.getParameter("s_year")),
						Integer.parseInt(request.getParameter("s_month")),
						Integer.parseInt(request.getParameter("s_day")),
						Integer.parseInt(request.getParameter("s_hour")),
						Integer.parseInt(request.getParameter("s_min")), 0);
				logReader.setStartDate(cal.getTime());
			} else if (logReader.getStartDate() != null) {
				logReader.setStartDate(null);
			}
			if (request.getParameter("applyenddate") != null) {
				Calendar cal = Calendar.getInstance();
				cal.set(Integer.parseInt(request.getParameter("e_year")),
						Integer.parseInt(request.getParameter("e_month")),
						Integer.parseInt(request.getParameter("e_day")),
						Integer.parseInt(request.getParameter("e_hour")),
						Integer.parseInt(request.getParameter("e_min")), 59);
				logReader.setEndDate(cal.getTime());
			} else if (logReader.getEndDate() != null) {
				logReader.setEndDate(null);
			}
		}
		try {
			logReader.init();
		} catch (Exception e) {
			throw new RuntimeException("Unable to initialize log reader.", e);
		}
		return logReader;
	}

	public AppConfiguration getConfig() {
		return config;
	}

	public void setConfig(AppConfiguration config) {
		this.config = config;
	}

	private int getOffset(HttpServletRequest request) {
		int result = 0;

		String offsetString = request.getParameter("offset");
		if (offsetString != null && !offsetString.isEmpty()) {
			try {
				result = Integer.parseInt(offsetString);
			} catch (NumberFormatException nfe) {
				// ignore, result stays 0
			}
		}

		return (result > 0) ? result : 0;
	}

	private int getCount(HttpServletRequest request) {
		int result = 50; // Default entries count

		String countString = request.getParameter("count");
		if (countString != null && !countString.isEmpty()) {
			try {
				result = Integer.parseInt(countString);
			} catch (NumberFormatException nfe) {
				// ignore, result stays 50
			}
		}

		return result;
	}

	/**
	 * @return Returns the appenderName.
	 */
	public String getAppenderName() {
		return appenderName;
	}

	/**
	 * @param appenderName The appenderName to set.
	 */
	public void setAppenderName(String appenderName) {
		this.appenderName = appenderName;
	}

	public void setViewName(String viewName) {
		this.viewName = viewName;
	}
}
