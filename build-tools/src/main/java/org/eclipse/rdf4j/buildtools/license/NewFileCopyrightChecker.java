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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NewFileCopyrightChecker {

	private static final Pattern COPYRIGHT_PATTERN = Pattern.compile("Copyright \\(c\\) (\\d{4})");
	private static final int HEADER_SCAN_LIMIT = 80;

	private final Path repositoryRoot;
	private final GitService gitService;
	private final List<PathMatcher> includeMatchers;
	private final List<PathMatcher> excludeMatchers;

	public NewFileCopyrightChecker(Path repositoryRoot, GitService gitService, List<String> includes,
			List<String> excludes) {
		this.repositoryRoot = repositoryRoot.toAbsolutePath().normalize();
		this.gitService = gitService;
		this.includeMatchers = compileMatchers(includes);
		this.excludeMatchers = compileMatchers(excludes);
	}

	public void check() throws IOException, CopyrightCheckException {
		Map<Path, Integer> newFiles = gitService.findNewFilesWithCreationYear();
		List<String> violations = new ArrayList<>();

		for (Map.Entry<Path, Integer> entry : newFiles.entrySet()) {
			Path relativePath = toRelative(entry.getKey());
			if (!shouldCheck(relativePath)) {
				continue;
			}

			Path file = resolve(relativePath);
			if (!Files.exists(file) || !Files.isRegularFile(file)) {
				continue;
			}

			Integer detectedYear = findCopyrightYear(file);
			if (detectedYear == null) {
				violations.add(String.format("Missing copyright header in %s (expected %d)",
						relativePath, entry.getValue()));
				continue;
			}

			if (!detectedYear.equals(entry.getValue())) {
				violations.add(String.format("File %s has copyright year %d but expected %d",
						relativePath, detectedYear, entry.getValue()));
			}
		}

		if (!violations.isEmpty()) {
			String message = violations.stream().collect(Collectors.joining(System.lineSeparator()));
			throw new CopyrightCheckException(message);
		}
	}

	private Path resolve(Path path) {
		if (path.isAbsolute()) {
			return path.normalize();
		}
		return repositoryRoot.resolve(path).normalize();
	}

	private Path toRelative(Path path) {
		Path normalized = path.normalize();
		if (normalized.isAbsolute()) {
			if (normalized.startsWith(repositoryRoot)) {
				return repositoryRoot.relativize(normalized);
			}
			return normalized;
		}
		return normalized;
	}

	private boolean shouldCheck(Path path) {
		if (!excludeMatchers.isEmpty()) {
			for (PathMatcher matcher : excludeMatchers) {
				if (matcher.matches(path)) {
					return false;
				}
			}
		}

		if (includeMatchers.isEmpty()) {
			return true;
		}

		for (PathMatcher matcher : includeMatchers) {
			if (matcher.matches(path)) {
				return true;
			}
		}

		return false;
	}

	private static List<PathMatcher> compileMatchers(List<String> patterns) {
		if (patterns == null || patterns.isEmpty()) {
			return List.of();
		}
		return patterns.stream()
				.filter(pattern -> pattern != null && !pattern.isEmpty())
				.map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern))
				.collect(Collectors.toList());
	}

	private Integer findCopyrightYear(Path file) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(file)) {
			String line;
			int lines = 0;
			while ((line = reader.readLine()) != null && lines++ < HEADER_SCAN_LIMIT) {
				Matcher matcher = COPYRIGHT_PATTERN.matcher(line);
				if (matcher.find()) {
					return Integer.parseInt(matcher.group(1));
				}
			}
		}
		return null;
	}
}
