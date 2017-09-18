/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

/**
 * @author dale
 */
public interface ConsoleParameters {

	int getWidth();

	void setWidth(int width);

	boolean isShowPrefix();

	void setShowPrefix(boolean value);

	boolean isQueryPrefix();

	void setQueryPrefix(boolean value);
}