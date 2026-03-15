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
package org.eclipse.rdf4j.repository.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Arjohn Kampman
 */
public class ConfigTemplate {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{%.*?%\\}");

	private static final Pattern INLINE_HINT_PATTERN = Pattern.compile("^(.*?)(?:\\[([^\\]]+)\\])?$");

	private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("(\\w+)=(?:\"([^\"]*)\"|([^\\s,]+))");

	/*-----------*
	 * Variables *
	 *-----------*/

	private String template;

	private final Map<String, List<String>> variableMap = new LinkedHashMap<>();

	private final Map<String, String> multilineMap = new LinkedHashMap<>();

	/**
	 * Parsed template token with normalized variable name, inline hint attributes, and effective default values.
	 */
	public static final class Token {
		private final String rawText;
		private final String rawName;
		private final String name;
		private final List<String> values;
		private final Map<String, String> attributes;
		private final int start;
		private final int end;

		private Token(String rawText, String rawName, String name, List<String> values, Map<String, String> attributes,
				int start, int end) {
			this.rawText = rawText;
			this.rawName = rawName;
			this.name = name;
			this.values = List.copyOf(values);
			this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
			this.start = start;
			this.end = end;
		}

		public String getRawText() {
			return rawText;
		}

		public String getRawName() {
			return rawName;
		}

		public String getName() {
			return name;
		}

		public List<String> getValues() {
			return values;
		}

		public Map<String, String> getAttributes() {
			return attributes;
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}

		private static Token parse(String tokenText, int start, int end) {
			String[] tokensArray = tokenText.substring(2, tokenText.length() - 2).split("\\|");
			String rawName = tokensArray.length == 0 ? "" : tokensArray[0].trim();
			Matcher matcher = INLINE_HINT_PATTERN.matcher(rawName);
			String name = rawName;
			Map<String, String> attributes = Map.of();
			if (matcher.matches()) {
				String normalized = matcher.group(1).trim();
				name = normalized.isEmpty() ? rawName : normalized;
				attributes = parseAttributes(matcher.group(2));
			}
			return new Token(tokenText, rawName, name, defaultValues(Arrays.asList(tokensArray), attributes),
					attributes,
					start, end);
		}
	}

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ConfigTemplate(String template) {
		setTemplate(template);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getTemplate() {
		return template;
	}

	public static List<Token> parseTokens(String text) {
		if (text == null || text.isEmpty()) {
			return List.of();
		}

		List<Token> tokens = new ArrayList<>();
		Matcher matcher = TOKEN_PATTERN.matcher(text);
		while (matcher.find()) {
			tokens.add(Token.parse(matcher.group(), matcher.start(), matcher.end()));
		}
		return List.copyOf(tokens);
	}

	public static Map<String, String> parseAttributes(String text) {
		if (text == null || text.isBlank()) {
			return Map.of();
		}

		Map<String, String> attributes = new LinkedHashMap<>();
		Matcher matcher = ATTRIBUTE_PATTERN.matcher(text);
		while (matcher.find()) {
			String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
			attributes.put(matcher.group(1), value);
		}
		return attributes;
	}

	public final void setTemplate(String template) {
		if (template == null) {
			throw new IllegalArgumentException("template must not be null");
		}

		this.template = template;

		parseTemplate();
	}

	private void parseTemplate() {
		for (Token token : parseTokens(template)) {
			String var = token.getName();
			if (var.isEmpty()) {
				throw new IllegalArgumentException("Illegal template token: " + token.getRawText());
			}
			if (!variableMap.containsKey(var)) {
				variableMap.put(var, token.getValues());
				int start = token.getStart();
				String before = template.substring(Math.max(start - 3, 0), start);
				int end = token.getEnd();
				if (("'''".equals(before) || "\"\"\"".equals(before))
						&& before.equals(template.substring(end, end + 3))) {
					multilineMap.put(var, before);
				}
			}
		}
	}

	public Map<String, List<String>> getVariableMap() {
		return Collections.unmodifiableMap(variableMap);
	}

	public String render(Map<String, String> valueMap) {
		StringBuffer result = new StringBuffer(template.length());
		Matcher matcher = TOKEN_PATTERN.matcher(template);
		while (matcher.find()) {
			Token token = Token.parse(matcher.group(), matcher.start(), matcher.end());
			String var = token.getName();
			String value = valueMap.get(var);
			if (value == null) {
				List<String> values = variableMap.get(var);
				value = values.isEmpty() ? "" : values.get(0);
			}
			if (!value.isEmpty() && multilineMap.containsKey(var)) {
				value = escapeMultilineQuotes(multilineMap.get(var), value);
			}
			matcher.appendReplacement(result, Matcher.quoteReplacement(value));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private static List<String> defaultValues(List<String> tokens, Map<String, String> attributes) {
		List<String> values = tokens.size() > 1 ? tokens.subList(1, tokens.size()) : List.of();
		String hintedDefault = attributes.get("default");
		if (hintedDefault == null) {
			return values;
		}
		if (values.isEmpty()) {
			return List.of(hintedDefault);
		}
		if (values.get(0).isEmpty()) {
			java.util.ArrayList<String> updated = new java.util.ArrayList<>(values);
			updated.set(0, hintedDefault);
			return updated;
		}
		return values;
	}

	/**
	 * Escape Turtle multiline literal quote characters in the given value.
	 *
	 * @param quoteVariant either ''' or """
	 * @param value        the value to escape properly
	 * @return the value with any needed multiline quote sequences escaped
	 */
	protected static String escapeMultilineQuotes(String quoteVariant, String value) {
		if ("'''".equals(quoteVariant) || "\"\"\"".equals(quoteVariant)) {
			return value.replace(quoteVariant, new String(new char[3]).replace("\0", "\\" + quoteVariant.charAt(0)));
		} else {
			throw new IllegalArgumentException("Only a valid Turtle multi-line quote delmiter is allowed.");
		}
	}

	public Map<String, String> getMultilineMap() {
		return Collections.unmodifiableMap(multilineMap);
	}
}
