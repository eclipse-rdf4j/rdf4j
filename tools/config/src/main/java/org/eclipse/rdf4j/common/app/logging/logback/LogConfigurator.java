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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.common.logging.LogReader;
import org.xml.sax.Attributes;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.util.OptionHelper;

/**
 * @author alex
 */
public class LogConfigurator extends JoranConfigurator {

	Map<String, String> logReaderClassNames = new HashMap<>();

	Map<String, Appender<?>> appenders = new HashMap<>();
	String defaultAppender = null;

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

	@Override
	public void addInstanceRules(RuleStore rs) {
		// parent rules already added
		super.addInstanceRules(rs);
		rs.addRule(new ElementSelector("configuration/appender/logreader"), new LogReaderAction());
	}

	public class LogReaderAction extends Action {

		String className;

		boolean def = false;

		@Override
		public void begin(InterpretationContext ec, String name, Attributes attributes) {
			className = attributes.getValue(CLASS_ATTRIBUTE);
			def = (attributes.getValue("default") != null) && attributes.getValue("default").equalsIgnoreCase("true");
			ec.pushObject(className);
		}

		@Override
		public void end(InterpretationContext ec, String arg1) {
			Object o = ec.peekObject();
			if (o != className) {
				addWarn("The object on the top the of the stack is not the logreader classname pushed earlier.");
			} else {
				ec.popObject();
				Appender<?> appender = (Appender<?>) ec.peekObject();
				logReaderClassNames.put(appender.getName(), className);
				appenders.put(appender.getName(), appender);
				if (def) {
					defaultAppender = appender.getName();
				}
			}
		}

	}
}
