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

package org.eclipse.rdf4j.common.platform;

import java.io.File;

public class DefaultPlatform extends AbstractPlatform {

	@Override
	public String getName() {
		return "Default";
	}

	@Override
	public File getOSApplicationDataDir() {
		return new File("RDF4J");
	}

	@Override
	public boolean dataDirPreserveCase() {
		return false;
	}

	@Override
	public boolean dataDirReplaceWhitespace() {
		return false;
	}

	@Override
	public boolean dataDirReplaceColon() {
		return false;
	}
}
