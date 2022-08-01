/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.Assert;
import org.junit.Test;

public class PrefixDeclarationsTest {
	@Test
	public void testReplaceInQuery_nothing() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		Assert.assertEquals("nothing to replace",
				pd.replacePrefixesInQuery("nothing to replace"));
	}

	@Test
	public void testReplaceInQuery_justTheNamespace() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		Assert.assertEquals("label: to replace",
				pd.replacePrefixesInQuery("<http://example.org/ns#> to replace"));
	}

	@Test
	public void testReplaceInQuery_atStart() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		Assert.assertEquals("label:local to replace",
				pd.replacePrefixesInQuery("<http://example.org/ns#local> to replace"));
	}

	@Test
	public void testReplaceInQuery_atEnd() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		Assert.assertEquals("replace label:local",
				pd.replacePrefixesInQuery("replace <http://example.org/ns#local>"));
	}

	@Test
	public void testReplaceInQuery_middle() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		Assert.assertEquals("replace label:local in the middle",
				pd.replacePrefixesInQuery("replace <http://example.org/ns#local> in the middle"));
	}

	@Test
	public void testReplaceInQuery_middle_with_angled_brackets() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		Assert.assertEquals("<replace <> <label:local> <in> the middle",
				pd.replacePrefixesInQuery("<replace <> <<http://example.org/ns#local>> <in> the middle"));
	}

	@Test
	public void testReplaceInQuery_multiple_long_shared_substring() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		pd.addPrefix(new Prefix("label2", Rdf.iri("http://example.org/ns2#")));
		Assert.assertEquals("<replace <> <label:local> <in> the middle followed by label2:local",
				pd.replacePrefixesInQuery(
						"<replace <> <<http://example.org/ns#local>> <in> the middle followed by <http://example.org/ns2#local>"));
	}

	@Test
	public void testReplaceInQuery_do_not_replace_in_string() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		Assert.assertEquals(
				"Do not replace \"<http://example.org/ns#local>\" because it's in a string, but replace label:local here.",
				pd.replacePrefixesInQuery(
						"Do not replace \"<http://example.org/ns#local>\" because it's in a string, but replace <http://example.org/ns#local> here."));
	}

	@Test
	public void testReplaceInQuery_do_not_replace_in_string_enclosed_with_multiline_quotes() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		Assert.assertEquals(
				"Do not replace \"in String even if enclosed in quotes:'''<http://example.org/ns#local>'''\", but replace label:local here.",
				pd.replacePrefixesInQuery(
						"Do not replace \"in String even if enclosed in quotes:'''<http://example.org/ns#local>'''\", but replace <http://example.org/ns#local> here."));
	}

	@Test
	public void testReplaceInQuery_do_not_replace_in_string_enclosed_with_escaped_double_quotes() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		Assert.assertEquals(
				"Do not replace \"in String even if enclosed in quotes:\\\"<http://example.org/ns#local>\\\"\", but replace label:local here.",
				pd.replacePrefixesInQuery(
						"Do not replace \"in String even if enclosed in quotes:\\\"<http://example.org/ns#local>\\\"\", but replace <http://example.org/ns#local> here."));
	}

	@Test
	public void testReplaceInQuery_do_not_replace_in_multiline_string() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		Assert.assertEquals(
				"Do not replace '''<http://example.org/ns#local>\n\\n''' because it's in a string, but replace label:local here.",
				pd.replacePrefixesInQuery(
						"Do not replace '''<http://example.org/ns#local>\n\\n''' because it's in a string, but replace <http://example.org/ns#local> here."));
	}

	@Test
	public void testReplaceInQuery_do_not_replace_in_multiline_string_with_nested_quotes() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		Assert.assertEquals(
				"Do not replace '''<http://example.org/ns#local>\nEven with a nested '\"'\n''' because it's in a string, but replace label:local here.",
				pd.replacePrefixesInQuery(
						"Do not replace '''<http://example.org/ns#local>\nEven with a nested '\"'\n''' because it's in a string, but replace <http://example.org/ns#local> here."));
	}

	@Test
	public void testReplaceInQuery_do_not_replace_in_multiline_string_enclosed_with_double_quotes() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		Assert.assertEquals(
				"Do not replace '''in String even if enlosed in quotes: \"<http://example.org/ns#local>\"''', but replace label:local here.",
				pd.replacePrefixesInQuery(
						"Do not replace '''in String even if enlosed in quotes: \"<http://example.org/ns#local>\"''', but replace <http://example.org/ns#local> here."));
	}

	@Test
	public void testReplaceInQuery_do_not_replace_in_multiline_string_enclosed_with_escaped_double_quotes() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		Assert.assertEquals(
				"Do not replace '''in String even if enlosed in quotes: \\\"<http://example.org/ns#local>\\\"''', but replace label:local here.",
				pd.replacePrefixesInQuery(
						"Do not replace '''in String even if enlosed in quotes: \\\"<http://example.org/ns#local>\\\"''', but replace <http://example.org/ns#local> here."));
	}

	@Test
	public void testReplaceInQuery_do_not_replace_if_continuation_is_not_localname() {
		PrefixDeclarations pd = new PrefixDeclarations();
		pd.addPrefix(new Prefix("label", Rdf.iri("http://example.org/ns#")));
		Assert.assertEquals(
				"Do not replace <http://example.org/ns#not/a/localname> because a prefix must be followed by a localname",
				pd.replacePrefixesInQuery(
						"Do not replace <http://example.org/ns#not/a/localname> because a prefix must be followed by a localname"));
	}
}
