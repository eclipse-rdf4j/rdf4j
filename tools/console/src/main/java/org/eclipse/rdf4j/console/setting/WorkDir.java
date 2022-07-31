/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console.setting;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Working directory setting
 *
 * @author Bart Hanssens
 */
public class WorkDir extends ConsoleSetting<Path> {
	public final static String NAME = "workdir";

	@Override
	public String getHelpLong() {
		return "set workDir=<dir>              Set the working directory\n";
	}

	/**
	 * Constructor
	 *
	 * Default dir is system property user.dir (= current directory)
	 */
	public WorkDir() {
		super(Paths.get(System.getProperty("user.dir")));
	}

	/**
	 * Constructor
	 *
	 * @param initValue
	 */
	public WorkDir(Path initValue) {
		super(initValue);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void set(Path value) throws IllegalArgumentException {
		if (value != null && value.toFile().isDirectory() && value.toFile().canWrite()) {
			super.set(value);
		} else {
			throw new IllegalArgumentException("Path is not a writable directory");
		}
	}

	@Override
	public void setFromString(String value) throws IllegalArgumentException {
		set(Paths.get(value));
	}
}
