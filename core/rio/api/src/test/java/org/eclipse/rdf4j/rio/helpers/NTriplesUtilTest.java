/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.function.Function;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link NTriplesUtil}
 *
 * @author Jeen Broekstra
 *
 */
public class NTriplesUtilTest {

	private StringBuilder appendable;
	private final ValueFactory f = SimpleValueFactory.getInstance();

	@Before
	public void setUp() throws Exception {
		appendable = new StringBuilder();
	}

	@Test
	public void testAppendWithoutEncoding() throws Exception {
		Literal l = f.createLiteral("Äbc");
		NTriplesUtil.append(l, appendable, true, false);
		assertThat(appendable.toString()).isEqualTo("\"Äbc\"");
	}

	@Test
	public void testAppendWithEncoding() throws Exception {
		Literal l = f.createLiteral("Äbc");
		NTriplesUtil.append(l, appendable, true, true);
		assertThat(appendable.toString()).isEqualTo("\"\\u00C4bc\"");
	}

	@Test
	public void testSerializeTriple() throws IOException {
		Object[] triples = new Object[] {
				f.createTriple(f.createIRI("urn:a"), f.createIRI("urn:b"), f.createIRI("urn:c")),
				"<<<urn:a> <urn:b> <urn:c>>>",
				//
				f.createTriple(f.createTriple(f.createIRI("urn:a"), f.createIRI("urn:b"), f.createIRI("urn:c")),
						DC.SOURCE, f.createLiteral("news")),
				"<<<<<urn:a> <urn:b> <urn:c>>> <http://purl.org/dc/elements/1.1/source> \"news\">>",
				//
				f.createTriple(f.createBNode("bnode1"), f.createIRI("urn:x"),
						f.createTriple(f.createIRI("urn:a"), f.createIRI("urn:b"), f.createIRI("urn:c"))),
				"<<_:bnode1 <urn:x> <<<urn:a> <urn:b> <urn:c>>>>>"
		};

		for (int i = 0; i < triples.length; i += 2) {
			assertEquals(triples[i + 1], NTriplesUtil.toNTriplesString((Triple) triples[i]));
			assertEquals(triples[i + 1], NTriplesUtil.toNTriplesString((Resource) triples[i]));
			assertEquals(triples[i + 1], NTriplesUtil.toNTriplesString((Value) triples[i]));
			NTriplesUtil.append((Triple) triples[i], appendable);
			assertEquals(triples[i + 1], appendable.toString());
			appendable = new StringBuilder();
			NTriplesUtil.append((Resource) triples[i], appendable);
			assertEquals(triples[i + 1], appendable.toString());
			appendable = new StringBuilder();
			NTriplesUtil.append((Value) triples[i], appendable);
			assertEquals(triples[i + 1], appendable.toString());
			appendable = new StringBuilder();
		}
	}

	@Test
	public void testParseTriple() {
		String[] triples = new String[] {
				"<<<http://foo.com/bar#baz%20><http://example.com/test><<<urn:foo><urn:\\u0440>\"täst\"@de-DE>>>>",
				"<<http://foo.com/bar#baz%20 http://example.com/test <<urn:foo urn:р \"täst\"@de-DE>>>>",
				//
				"<< <http://foo.com/bar#baz%20>  <http://example.com/test>  <<  <urn:foo>  <urn:\\u0440> \"täst\"@de-DE  >>  >>",
				"<<http://foo.com/bar#baz%20 http://example.com/test <<urn:foo urn:р \"täst\"@de-DE>>>>",
				//
				"<<<<_:bnode1foobar<urn:täst>\"literál за проба\"^^<urn:test\\u0444\\U00000444>>><http://test/baz>\"test\\\\\\\"lit\">>",
				"<<<<_:bnode1foobar urn:täst \"literál за проба\"^^<urn:testфф>>> http://test/baz \"test\\\"lit\">>",
				//
				"<<  <<_:bnode1foobar<urn:täst> \"literál за проба\"^^<urn:test\\u0444\\U00000444>  >>  <http://test/baz> \"test\\\\\\\"lit\" >>",
				"<<<<_:bnode1foobar urn:täst \"literál за проба\"^^<urn:testфф>>> http://test/baz \"test\\\"lit\">>",
				// test surrogate pair range in bnode
				"<<_:test_\uD800\uDC00_\uD840\uDC00_bnode <urn:x> <urn:y>>>",
				"<<_:test_\uD800\uDC00_\uD840\uDC00_bnode urn:x urn:y>>",
				// invalid: missing closing >> for inner triple
				"<<<<_:bnode1foobar<urn:täst>\"literál за проба\"^^<urn:test\\u0444\\U00000444><http://test/baz>\"test\\\\\\\"lit\">>",
				null,
				// invalid: missing closing >> for outer triple
				"<<<<_:bnode1foobar<urn:täst>\"literál за проба\"^^<urn:test\\u0444\\U00000444>>><http://test/baz>\"test\\\\\\\"lit\"",
				null,
				// invalid: literal subject
				"<<\"test\" <urn:test> \"test\">>",
				null,
				// invalid: bnode predicate
				"<<<urn:test> _:test \"test\">>",
				null,
				// invalid: triple predicate
				"<<<urn:a> <<<urn:1> <urn:2> <urn:3>>> <urn:b>>>",
				null,
				// tests with empty literal,
				"<<<urn:1> <urn:2> \"\">>",
				"<<urn:1 urn:2 \"\">>",
				"<<<urn:1> <urn:2> \"\"@bg>>",
				"<<urn:1 urn:2 \"\"@bg>>",
				"<<<urn:1> <urn:2> \"\"^^<urn:type>>>",
				"<<urn:1 urn:2 \"\"^^<urn:type>>>"
		};

		for (int i = 0; i < triples.length; i += 2) {
			parseTriple(triples[i], triples[i + 1], (t) -> NTriplesUtil.parseTriple(t, f));
			parseTriple(triples[i], triples[i + 1], (t) -> (Triple) NTriplesUtil.parseValue(t, f));
			parseTriple(triples[i], triples[i + 1], (t) -> (Triple) NTriplesUtil.parseResource(t, f));
		}
	}

	private void parseTriple(String triple, String expected, Function<String, Triple> parser) {
		try {
			Triple t = parser.apply(triple);
			assertEquals(expected, t.stringValue());
		} catch (IllegalArgumentException e) {
			if (expected != null) {
				fail("Unexpected exception for valid triple: " + triple);
			}
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseIRIvsTriple() {
		NTriplesUtil.parseURI("<<<urn:a><urn:b><urn:c>>>", f);
	}

	@Test
	public void testGH3323Stackoverflow() {
		String badLiteral = "<<<urn:1> <urn:2> \"Ko te kamupene  S.L., he kamupene i whakapumautia i te tau 2009 a i whakatapua ki nga kaupapa korero mo etahi atu kamupene, mai i te 2012 i tiimata te hanga i ona ake hangarau a kua poipoihia e te mahi tahi me nga hinonga a iwi. Ina koa, ko te mahi tahi me te Manatū o te Roto o Spain te mea nui, i runga i te ngana ki te whakawhanake i nga otinga hangarau motuhake e haangai ana ki nga hiahia o nga Hoia Haumaru o te Kawanatanga o Paniora i nga waahi o nga drones me nga anti-drones. te hononga i waenga i te trga me te Manatu o roto i te mana i roto i nga tau e 8 kua hipa kua kitea i roto i nga waahanga nui e rua: i tetahi taha, ko te tautoko a te Minita ki te maha o nga kaupapa R&D i hangaia e AEORUM me te tahua tahua me te iwi whanui. moni; a, i tetahi atu, ko te mahi tahi nui a te Hekeretari o te Whenua mo te Haumarutanga i te wa e whanakehia ana te punaha  me . He kamupene hangarau-hangarau e whakarato ana i nga otinga aunoa mo nga pokapu whakahaere me nga waka kaore he tangata, he nui te utu ki nga waahi e hiahia ana kia mamao te whakahaere me te mohio. Mo tenei, ka whakamahia e ia tana ake R&D i roto i nga waahanga o te tirohanga matakite (kitenga / mohio / taatari i nga taiao me nga tauira), te whakahaere waka (whakahaere aunoa me te mohio o nga waka rererangi robotic me nga tauira) me te mohio mohio. Ko te kaupapa uara o te , SL, e whaaia ana mo te hunga e ahuru ana i te ahuru, ko te whakarato i nga taputapu tirotiro hou kia aukati, kia tere, kia tika hoki te whakatau i nga ahuatanga uaua, kia pai ake ai te whakahaere i te ahuru me te karo i nga ahuatanga ohorere. Ko nga punaha i whakawhanakehia i roto i nga whare taiwhanga o , S.L., ka taea te whakamahi i enei waahanga e whai ake nei: · Te Tiakitanga me te Whakahaumaru i nga Hanganga Hanga. · Haumarutanga: he rongoa mo te whakahaere, tirotiro me te tirotiro aunoa i nga whakahaerenga ture, hei awhina i te whanuitanga o nga ahuatanga. · Whakahaerenga ohorere, mo nga rongoa taangata me nga hoia: nga taputapu hei tirotiro mo te waa-tuuturu o nga rohe e pa ana me nga punaha mohio hei awhina i nga mahi whakatau. · Nga Taone Tino me etahi atu whakauru: otinga aunoa hei whakapai ake i te whakahaerenga o te taiao i nga taone, nga tuawhenua me nga rohe takutai (te whakahaere waka, te taiao, nga whare, te whakahaere ururua, te tiaki ...) SL, he ake hangarau hei whirihora i te maha o nga rongoa, ka taea te urutau ki nga hiahia motuhake o ona kaihoko: te rapu me te waahi o nga taangata, te tiaki me nga hanganga whakahirahira, te aukati ahi, te tirotiro i nga taiao taone, te waahi o nga mahi koretake i nga whare / umanga, me era atu. Ko nga hangarau e whai ake nei me whakanui: · : Te Whakawhanake i tetahi Punaha Korero Maatauranga mo te Whakahaere me te Whakahaumaru i nga Hanganga Critical. · : Te Whakawhanake i te punaha Paanui mo te Kimi me te Whakakore i nga tuma me nga . · :   , me te mahi tahi me te tari Irirangi o te Awhina. · : Te Whakawhanake i te Punaha Whakatutukitanga Arā Atu Anō mo te Whakaaetanga i nga Whakanui Taone me nga Hanga Tino, me te mahi tahi me nga pirihimana pirihimana e ono. I te taumata o te ao (kei waho o te fg), kei te tuu a   S.L. ki etahi kaupapa rereke me nga hoa hangarau, whakaaweawe i nga roopu me nga roopu whanui i nga whenua penei i te ,  ko nga \">>";
		NTriplesUtil.parseTriple(badLiteral, SimpleValueFactory.getInstance());
	}
}
