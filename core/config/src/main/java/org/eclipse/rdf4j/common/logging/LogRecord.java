/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public interface LogRecord {
	
	public static final SimpleDateFormat ISO8601_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
	
	public LogLevel getLevel();
	public Date getTime();
	public String getThreadName();
	public String getMessage();
	public List<String> getStackTrace();
}
