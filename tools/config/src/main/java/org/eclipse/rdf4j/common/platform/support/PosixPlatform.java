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

package org.eclipse.rdf4j.common.platform.support;

import java.io.File;

import org.eclipse.rdf4j.common.platform.AbstractPlatform;

/**
 * Platform implementation for *nix platforms.
 */
public class PosixPlatform extends AbstractPlatform {

	@Override
	public String getName() {
		return "POSIX-compatible";
	}

	@Override
	public File getOSApplicationDataDir() {
		return new File(System.getProperty("user.home"), ".RDF4J");
	}

	@Override
	public boolean dataDirPreserveCase() {
		return false;
	}

	@Override
	public boolean dataDirReplaceWhitespace() {
		return true;
	}

	@Override
	public boolean dataDirReplaceColon() {
		return false;
	}
}
