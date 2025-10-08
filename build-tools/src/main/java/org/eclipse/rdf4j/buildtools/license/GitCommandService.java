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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GitCommandService implements GitService {

	private final Path repositoryRoot;
	private final String baseReference;

	public GitCommandService(Path repositoryRoot, String baseReference) {
		this.repositoryRoot = repositoryRoot.toAbsolutePath().normalize();
		this.baseReference = baseReference;
	}

	@Override
	public Map<Path, Integer> findNewFilesWithCreationYear() throws IOException {
		Optional<String> mergeBase = determineMergeBase();
		List<String> newFiles = listNewFiles(mergeBase);
		Map<Path, Integer> result = new LinkedHashMap<>();

		for (String file : newFiles) {
			if (file.isBlank()) {
				continue;
			}
			Path relative = Path.of(file.trim());
			int year = findCreationYear(relative);
			result.put(relative, year);
		}

		return result;
	}

	private Optional<String> determineMergeBase() throws IOException {
		Optional<String> reference = resolveBaseReference();
		if (reference.isPresent()) {
			GitResult mergeBase = runGit(false, "merge-base", "HEAD", reference.get());
			if (mergeBase.isSuccess() && !mergeBase.lines().isEmpty()) {
				return Optional.of(mergeBase.lines().get(0));
			}
		}

		GitResult parent = runGit(false, "rev-parse", "HEAD^");
		if (parent.isSuccess() && !parent.lines().isEmpty()) {
			return Optional.of(parent.lines().get(0));
		}

		return Optional.empty();
	}

	private Optional<String> resolveBaseReference() throws IOException {
		if (baseReference != null && !baseReference.isBlank()) {
			return Optional.of(baseReference);
		}

		GitResult upstream = runGit(false, "rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}");
		if (upstream.isSuccess() && !upstream.lines().isEmpty()) {
			return Optional.of(upstream.lines().get(0));
		}

		for (String candidate : List.of("origin/main", "origin/master", "main", "master")) {
			GitResult verify = runGit(false, "rev-parse", "--verify", candidate);
			if (verify.isSuccess()) {
				return Optional.of(candidate);
			}
		}

		return Optional.empty();
	}

	private List<String> listNewFiles(Optional<String> mergeBase) throws IOException {
		GitResult diff;
		if (mergeBase.isPresent()) {
			diff = runGit(false, "diff", "--name-only", "--diff-filter=A", mergeBase.get() + "..HEAD");
		} else {
			diff = runGit(false, "show", "--name-only", "--diff-filter=A", "--pretty=format:", "HEAD");
		}

		if (!diff.isSuccess()) {
			return List.of();
		}

		return diff.lines();
	}

	private int findCreationYear(Path relativePath) throws IOException {
		String gitPath = relativePath.toString().replace('\\', '/');
		GitResult log = runGit(false, "log", "--date=format:%Y", "--diff-filter=A", "--follow", "--format=%ad", "--",
				gitPath);
		if (log.isSuccess() && !log.lines().isEmpty()) {
			return parseYear(log.lines().get(0));
		}
		return Year.now().getValue();
	}

	private GitResult runGit(boolean failOnError, String... arguments) throws IOException {
		List<String> command = new ArrayList<>(arguments.length + 1);
		command.add("git");
		command.addAll(List.of(arguments));

		ProcessBuilder builder = new ProcessBuilder(command);
		builder.directory(repositoryRoot.toFile());
		builder.redirectErrorStream(true);
		Process process = builder.start();

		String output;
		try (InputStream inputStream = process.getInputStream()) {
			output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			process.destroyForcibly();
			throw e;
		}

		try {
			int exitCode = process.waitFor();
			if (exitCode != 0 && failOnError) {
				throw new IOException(
						"Git command failed: " + String.join(" ", command) + System.lineSeparator() + output);
			}
			return new GitResult(exitCode, output);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while executing git command", e);
		}
	}

	private int parseYear(String value) {
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return Year.now().getValue();
		}
	}

	private static final class GitResult {
		private final int exitCode;
		private final String output;

		private GitResult(int exitCode, String output) {
			this.exitCode = exitCode;
			this.output = output;
		}

		boolean isSuccess() {
			return exitCode == 0;
		}

		List<String> lines() {
			return output.lines().map(String::trim).filter(line -> !line.isEmpty()).collect(Collectors.toList());
		}
	}
}
