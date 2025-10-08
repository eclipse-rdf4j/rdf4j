/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.buildtools.license;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "check-new-files", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class CheckNewFilesCopyrightMojo extends AbstractMojo {

	private static final List<String> DEFAULT_INCLUDES = List.of("**/*.java", "**/*.kt", "**/*.kts", "**/*.scala",
			"**/*.groovy", "**/*.xml", "**/*.xsd", "**/*.xsl", "**/*.properties", "**/*.sh");

	@Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
	private File baseDirectory;

	@Parameter
	private List<String> includes;

	@Parameter
	private List<String> excludes;

	@Parameter(property = "copyrightCheck.baseRef")
	private String baseReference;

	@Override
	public void execute() throws MojoExecutionException {
		Path root = baseDirectory.toPath();
		GitService gitService = new GitCommandService(root, baseReference);
		List<String> effectiveIncludes = includes == null || includes.isEmpty() ? DEFAULT_INCLUDES
				: new ArrayList<>(includes);
		List<String> effectiveExcludes = excludes == null ? List.of() : new ArrayList<>(excludes);

		NewFileCopyrightChecker checker = new NewFileCopyrightChecker(root, gitService, effectiveIncludes,
				effectiveExcludes);
		try {
			checker.check();
		} catch (CopyrightCheckException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to execute git copyright validation", e);
		}
	}
}
