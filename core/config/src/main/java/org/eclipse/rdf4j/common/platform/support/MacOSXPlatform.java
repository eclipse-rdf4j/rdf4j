/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
 
package org.eclipse.rdf4j.common.platform.support;

import java.io.File;

/**
 * Platform implementation for Mac OS X platforms.
 */
public class MacOSXPlatform extends PosixPlatform {

	public static final String APPLICATION_DATA = "Library/Application Support/Aduna";
	
	public String getName() {
		return "Mac OS X";
	}
	
	@Override
	public File getOSApplicationDataDir() {
		return new File(System.getProperty("user.home"), APPLICATION_DATA);
	}

	public boolean dataDirPreserveCase() {
		return true;
	}

	public boolean dataDirReplaceWhitespace() {
		return false;
	}	
}
