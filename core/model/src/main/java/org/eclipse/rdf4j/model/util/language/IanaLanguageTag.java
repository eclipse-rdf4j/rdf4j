/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
/*
 * IanaLanguageTags.java
 *
 * Created on July 24, 2001, 11:47 PM
 */

package org.eclipse.rdf4j.model.util.language;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.util.Literals;

/**
 * Language codes registered by IANA. An encapsulation of the IANA language registry found at <a href=
 * " http://www.iana.org/assignments/language-tags"> http://www.iana.org/assignments/language-tags</a>. The
 * values were updated on 8th July 2002 from a file dated 7th July 2002.
 * 
 * @author jjc
 * @deprecated Use {@link Literals#normalizeLanguageTag(String) instead}
 */
@Deprecated
public class IanaLanguageTag extends LanguageTag {

	static final Map<String, IanaLanguageTag[]> all = new HashMap<String, IanaLanguageTag[]>();
	static {
		try {
			all.put("lojban", new IanaLanguageTag[] { new IanaLanguageTag("art-lojban") });
			all.put("gaulish", new IanaLanguageTag[] { new IanaLanguageTag("cel-gaulish") });
			all.put("scouse", new IanaLanguageTag[] { new IanaLanguageTag("en-scouse") });
			all.put("ami", new IanaLanguageTag[] { new IanaLanguageTag("i-ami") });
			all.put("bnn", new IanaLanguageTag[] { new IanaLanguageTag("i-bnn") });
			all.put("default", new IanaLanguageTag[] { new IanaLanguageTag("i-default", LT_DEFAULT) });
			all.put("enochian", new IanaLanguageTag[] { new IanaLanguageTag("i-enochian") });
			all.put("hak", new IanaLanguageTag[] { new IanaLanguageTag("i-hak", LT_IANA_DEPRECATED) });
			all.put("klingon", new IanaLanguageTag[] { new IanaLanguageTag("i-klingon") });
			all.put("lux", new IanaLanguageTag[] { new IanaLanguageTag("i-lux", LT_IANA_DEPRECATED) });
			all.put("mingo", new IanaLanguageTag[] { new IanaLanguageTag("i-mingo") });
			all.put("navajo", new IanaLanguageTag[] { new IanaLanguageTag("i-navajo", LT_IANA_DEPRECATED) });
			all.put("pwn", new IanaLanguageTag[] { new IanaLanguageTag("i-pwn") });
			all.put("tao", new IanaLanguageTag[] { new IanaLanguageTag("i-tao") });
			all.put("tay", new IanaLanguageTag[] { new IanaLanguageTag("i-tay") });
			all.put("tsu", new IanaLanguageTag[] { new IanaLanguageTag("i-tsu") });
			all.put("bok", new IanaLanguageTag[] { new IanaLanguageTag("no-bok", LT_IANA_DEPRECATED) });
			all.put("nyn", new IanaLanguageTag[] { new IanaLanguageTag("no-nyn", LT_IANA_DEPRECATED) });
			all.put("be", new IanaLanguageTag[] {
					new IanaLanguageTag("sgn-BE-fr"),
					new IanaLanguageTag("sgn-BE-nl") });
			all.put("br", new IanaLanguageTag[] { new IanaLanguageTag("sgn-BR") });
			all.put("ch", new IanaLanguageTag[] { new IanaLanguageTag("sgn-CH-de") });
			all.put("co", new IanaLanguageTag[] { new IanaLanguageTag("sgn-CO") });
			all.put("de", new IanaLanguageTag[] { new IanaLanguageTag("sgn-DE") });
			all.put("dk", new IanaLanguageTag[] { new IanaLanguageTag("sgn-DK") });
			all.put("es", new IanaLanguageTag[] { new IanaLanguageTag("sgn-ES") });
			all.put("fr", new IanaLanguageTag[] { new IanaLanguageTag("sgn-FR") });
			all.put("gb", new IanaLanguageTag[] { new IanaLanguageTag("sgn-GB") });
			all.put("gr", new IanaLanguageTag[] { new IanaLanguageTag("sgn-GR") });
			all.put("ie", new IanaLanguageTag[] { new IanaLanguageTag("sgn-IE") });
			all.put("it", new IanaLanguageTag[] { new IanaLanguageTag("sgn-IT") });
			all.put("jp", new IanaLanguageTag[] { new IanaLanguageTag("sgn-JP") });
			all.put("mx", new IanaLanguageTag[] { new IanaLanguageTag("sgn-MX") });
			all.put("ni", new IanaLanguageTag[] { new IanaLanguageTag("sgn-NI") });
			all.put("nl", new IanaLanguageTag[] { new IanaLanguageTag("sgn-NL") });
			all.put("no", new IanaLanguageTag[] { new IanaLanguageTag("sgn-NO") });
			all.put("pt", new IanaLanguageTag[] { new IanaLanguageTag("sgn-PT") });
			all.put("se", new IanaLanguageTag[] { new IanaLanguageTag("sgn-SE") });
			all.put("us", new IanaLanguageTag[] { new IanaLanguageTag("sgn-US") });
			all.put("za", new IanaLanguageTag[] { new IanaLanguageTag("sgn-ZA") });
			all.put("gan", new IanaLanguageTag[] { new IanaLanguageTag("zh-gan") });
			all.put("guoyu", new IanaLanguageTag[] { new IanaLanguageTag("zh-guoyu") });
			all.put("hakka", new IanaLanguageTag[] { new IanaLanguageTag("zh-hakka") });
			all.put("min", new IanaLanguageTag[] {
					new IanaLanguageTag("zh-min"),
					new IanaLanguageTag("zh-min-nan") });
			all.put("wuu", new IanaLanguageTag[] { new IanaLanguageTag("zh-wuu") });
			all.put("xiang", new IanaLanguageTag[] { new IanaLanguageTag("zh-xiang") });
			all.put("yue", new IanaLanguageTag[] { new IanaLanguageTag("zh-yue") });
		}
		catch (LanguageTagSyntaxException ee) {
			System.err.println("Internal Error in static initializer of IanaLanguageTag:" + ee.getMessage());
		}

	}

	/**
	 * The bitwise OR of all applicable values from {@link LanguageTagCodes}. The possibilities are:
	 * <ul>
	 * <li><CODE>LT_IANA</CODE></li>
	 * <li><CODE>LT_IANA|LT_IANA_DEPRECATED</CODE></li>
	 * <li><CODE>LT_IANA|LT_DEFAULT</CODE> i.e. <CODE>i-default</CODE></li>
	 * </ul>
	 */
	final public int classification;

	private IanaLanguageTag(String s, int classification)
		throws LanguageTagSyntaxException
	{
		super(s);
		this.classification = LT_IANA | classification;
	}

	private IanaLanguageTag(String s)
		throws LanguageTagSyntaxException
	{
		this(s, 0);
	}

	/**
	 * Look up a language identifier in the IANA list. Trailing additional subtags are ignored.
	 * 
	 * @param t
	 *        The LanguageTag corresponding to the item being looked up.
	 * @return The IanaLanguageTag if found, or null if it is not in the list.
	 */
	static public IanaLanguageTag find(LanguageTag t) {
		if (t.subtags.length < 2) {
			return null;
		}
		IanaLanguageTag matches[] = (IanaLanguageTag[])all.get(t.subtags[1]);
		if (matches == null) {
			return null;
		}
		nextMatch: for (int i = 0; i < matches.length; i++) {
			if (t.subtags.length >= matches[i].subtags.length) {
				for (int j = 0; j < matches[i].subtags.length; j++) {
					if (!t.subtags[j].equals(matches[i].subtags[j]))
						continue nextMatch;
				}
				return matches[i];
			}
		}
		return null;
	}

}
