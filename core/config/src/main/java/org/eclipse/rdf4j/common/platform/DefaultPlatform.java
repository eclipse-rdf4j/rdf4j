/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
 
package org.eclipse.rdf4j.common.platform;

import java.io.File;

public class DefaultPlatform extends AbstractPlatform {

	public String getName() {
		return "Default";
	}

	public File getOSApplicationDataDir() {
		return new File("Aduna");
	}

	public boolean dataDirPreserveCase() {
		return false;
	}

	public boolean dataDirReplaceWhitespace() {
		return false;
	}

	public boolean dataDirReplaceColon() {
		return false;
	}
}
