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
package org.eclipse.rdf4j.common.app.logging.logback;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.rdf4j.common.logging.LogReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.OptionHelper;

/**
 * @author alex
 */
public class LogConfigurator extends JoranConfigurator {

	Map<String, String> logReaderClassNames = new HashMap<>();

	Map<String, Appender<?>> appenders = new HashMap<>();
	String defaultAppender = null;
	private static final String CLASS_ATTRIBUTE = Action.CLASS_ATTRIBUTE;

	/**
	 * Configure logback using the provided file and collect any custom {@code <logreader>} declarations found inside
	 * appender definitions.
	 */
	public void configure(File configFile) throws JoranException {
		logReaderClassNames.clear();
		appenders.clear();
		defaultAppender = null;

		super.doConfigure(configFile);
		collectLogReaders(configFile);
	}

	/**
	 * Get default log reader
	 *
	 * @return log reader
	 */
	public LogReader getDefaultLogReader() {
		if (defaultAppender == null) {
			if (appenders.keySet().iterator().hasNext()) {
				defaultAppender = appenders.keySet().iterator().next();
			}
		}
		return this.getLogReader(defaultAppender);
	}

	/**
	 *
	 * @param appenderName
	 * @return log reader
	 */
	public LogReader getLogReader(String appenderName) {
		if (appenderName != null) {
			String className = logReaderClassNames.get(appenderName);
			if (className != null) {
				try {
					LogReader logReader = (LogReader) OptionHelper.instantiateByClassName(className,
							org.eclipse.rdf4j.common.logging.LogReader.class, context);
					logReader.setAppender(appenders.get(appenderName));
					return logReader;
				} catch (Exception ex) {
					System.err.println("Could not create logreader of type " + className + " !");
					ex.printStackTrace();
				}
			} else {
				System.err.println("Could not find logreader for appender " + appenderName + " !");
			}
		}
		return null;
	}

	private void collectLogReaders(File configFile) {
		List<LogReaderDefinition> definitions = parseDefinitions(configFile);
		if (!(context instanceof LoggerContext)) {
			return;
		}
		Map<String, Appender<?>> availableAppenders = collectAppenders((LoggerContext) context);

		for (LogReaderDefinition definition : definitions) {
			Appender<?> appender = availableAppenders.get(definition.appenderName);
			if (appender == null) {
				addWarn("No appender found for logreader on appender " + definition.appenderName);
				continue;
			}
			logReaderClassNames.put(definition.appenderName, definition.className);
			appenders.put(definition.appenderName, appender);
			if (definition.isDefault) {
				defaultAppender = definition.appenderName;
			}
		}
	}

	private Map<String, Appender<?>> collectAppenders(LoggerContext loggerContext) {
		Map<String, Appender<?>> mapped = new HashMap<>();
		for (Logger logger : loggerContext.getLoggerList()) {
			for (Iterator<Appender<ch.qos.logback.classic.spi.ILoggingEvent>> it = logger.iteratorForAppenders(); it
					.hasNext();) {
				Appender<?> appender = it.next();
				if (appender.getName() != null) {
					mapped.putIfAbsent(appender.getName(), appender);
				}
			}
		}
		return mapped;
	}

	private List<LogReaderDefinition> parseDefinitions(File configFile) {
		List<LogReaderDefinition> definitions = new ArrayList<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(configFile);
			NodeList appenderNodes = document.getElementsByTagName("appender");
			for (int i = 0; i < appenderNodes.getLength(); i++) {
				Element appender = (Element) appenderNodes.item(i);
				String appenderName = appender.getAttribute("name");
				NodeList logReaders = appender.getElementsByTagName("logreader");
				for (int j = 0; j < logReaders.getLength(); j++) {
					Element logreader = (Element) logReaders.item(j);
					String className = logreader.getAttribute(CLASS_ATTRIBUTE);
					boolean isDefault = "true".equalsIgnoreCase(logreader.getAttribute("default"));
					if (!appenderName.isEmpty() && !className.isEmpty()) {
						definitions.add(new LogReaderDefinition(appenderName, className, isDefault));
					}
				}
			}
		} catch (Exception e) {
			addWarn("Failed to parse logreader elements from " + configFile + ": " + e.getMessage());
		}
		return definitions;
	}

	private static final class LogReaderDefinition {
		private final String appenderName;
		private final String className;
		private final boolean isDefault;

		LogReaderDefinition(String appenderName, String className, boolean isDefault) {
			this.appenderName = appenderName;
			this.className = className;
			this.isDefault = isDefault;
		}
	}
}
