/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
/*
 * LanguageTag.java
 *
 * Created on July 24, 2001, 11:45 PM
 */

package org.eclipse.rdf4j.model.util.language;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.rdf4j.model.util.Literals;

/**
 * RFC 3066, "Tags for the Identification of Languages".
 * 
 * @author jjc
 * 
 * @deprecated Use {@link Literals#normalizeLanguageTag(String) instead}
 */
@Deprecated
public class LanguageTag implements LanguageTagCodes {

	String subtags[];

	private int tagType;

	/**
	 * Creates new RFC3066 LanguageTag.
	 * 
	 * @param tag The tag to parse and analyse.
	 * @throws LanguageTagSyntaxException If the syntactic rules of RFC3066 section 2.1 are broken.
	 */
	public LanguageTag(String tag) throws LanguageTagSyntaxException {
		String lc = tag.toLowerCase();
		List<String> subtagList = new ArrayList<>();
		int subT;
		while (true) {
			subT = lc.indexOf('-');
			if (subT != -1) {
				subtagList.add(lc.substring(0, subT));
				lc = lc.substring(subT + 1);
			} else
				break;
		}
		subtagList.add(lc);
		subtags = new String[subtagList.size()];
		subtagList.toArray(subtags);
		int lg = subtags[0].length();
		if (lg == 0 || lg > 8) {
			throw new LanguageTagSyntaxException("Primary subtag must be between 1 and 8 alpha characters: " + tag);
		}
		for (int j = 0; j < lg; j++) {
			int ch = subtags[0].charAt(j);
			if (!('a' <= ch && ch <= 'z')) {
				throw new LanguageTagSyntaxException("Primary subtag must be between 1 and 8 alpha characters: " + tag);
			}
		}
		for (int i = 1; i < subtags.length; i++) {
			lg = subtags[i].length();
			if (lg == 0 || lg > 8) {
				throw new LanguageTagSyntaxException(
						"Subtag " + (i + 1) + " must be between 1 and 8 alphanumeric characters: " + tag);
			}
			for (int j = 0; j < lg; j++) {
				int ch = subtags[i].charAt(j);
				if (!(('a' <= ch && ch <= 'z') || ('0' <= ch && ch <= '9'))) {
					throw new LanguageTagSyntaxException(
							"Subtag " + (i + 1) + " must be between 1 and 8 alphanumeric characters: " + tag);
				}
			}
		}

		tagType = getTagType();
		if (tagType == LT_ILLEGAL) {
			throw new LanguageTagSyntaxException(getErrorMessage());
		}
	}

	// Primary tag.

	// Second tag.
	// Additional tags (either second or third).
	// Special cases.
	// Overall properties
	/**
	 * The properties of this LanguageTag, expressed as a bitwise or of fields from {@link LanguageTagCodes}. If the tag
	 * is illegal only <CODE>LT_ILLEGAL</CODE> is reported. Examples include:
	 * <dl>
	 * <dt><CODE>LT_ISO639</CODE></dt>
	 * <dd>en <I>English.</I></dd>
	 * <dt><CODE>LT_ISO639|LT_ISO3166</CODE></dt>
	 * <dd>en-GB <I>British English</I></dd>
	 * <dt><CODE>LT_ILLEGAL</CODE></dt>
	 * <dd>en-ENGLAND <I>No such country.</I> Never returned in combination with other values.</dd>
	 * <dt><CODE>LT_PRIVATE_USE</CODE></dt>
	 * <dd>x-en-ENGLAND <I>Private tag with private semantics.</I></dd>
	 * <dt><CODE>LT_IANA|LT_EXTRA</CODE></dt>
	 * <dd>i-klingon-trekkie <I>Klingon + "trekkie"</I></dd>
	 * <dt><CODE>LT_IANA_DEPRECATED</CODE></dt>
	 * <dd>
	 * <dt><CODE>LT_MULTIPLE|LT_ISO3166|LT_EXTRA</CODE></dt>
	 * <dd>mul-CH-dialects</dd>
	 * <dt><CODE>LT_ISO639|LT_ISO3166|LT_IANA|LT_EXTRA</CODE></dt>
	 * <dd>sgn-US-MA <I>Martha's Vineyard Sign Language</I></dd>
	 * </dl>
	 * 
	 * @return A bitwise or of all LT_xxx values that apply.
	 */
	private int getTagType() {
		IanaLanguageTag iana = null;
		if (this instanceof IanaLanguageTag) {
			iana = (IanaLanguageTag) this;
		} else {
			iana = IanaLanguageTag.find(this);
		}
		Iso639 lang = Iso639.find(subtags[0]);
		int rslt = iana == null ? 0 : iana.classification;
		if (iana != null) {
			if (iana.subtags.length < subtags.length) {
				rslt |= LT_EXTRA;
			}
		}
		switch (subtags[0].length()) {
		case 1:
			switch (subtags[0].charAt(0)) {
			case 'x':
				return LT_PRIVATE_USE; // reserved for private use.
			case 'i':
				return iana != null ? rslt : LT_ILLEGAL;
			default:
				return LT_ILLEGAL;
			}
		case 2:
			if (lang == null) {
				return LT_ILLEGAL;
			}
			rslt |= lang.classification; // all special case tags etc.
			break;
		case 3:
			if (lang == null) {
				return LT_ILLEGAL;
			}
			if (lang.twoCharCode != null) {
				return LT_ILLEGAL; // Section 2.3 Para 2
			}
			if (!lang.terminologyCode.equals(subtags[0])) {
				return LT_ILLEGAL;
				// Section 2.3 Para 3
			}
			rslt |= lang.classification; // all special case tags etc.
			// Section 2.3 para 4,5,6 in a separate function.
			break;
		default:
			return LT_ILLEGAL;
		}
		if (subtags.length == 1) {
			return rslt;
		}
		switch (subtags[1].length()) {
		case 1:
			return LT_ILLEGAL;
		case 2:
			if (Iso3166.find(subtags[1]) == null) {
				return LT_ILLEGAL;
			} else {
				rslt |= LT_ISO3166;
			}
			break;
		default:
			if (iana == null)
				rslt |= LT_EXTRA;
		}
		if (subtags.length > 2 && iana == null) {
			rslt |= LT_EXTRA;
		}
		return rslt;
	}

	/**
	 * An error message describing the reason the tag is illegal.
	 * 
	 * @return null if legal, or an error message if not.
	 */
	private String getErrorMessage() {
		switch (subtags[0].length()) {
		case 1:
			switch (subtags[0].charAt(0)) {
			case 'x':
				return null; // reserved for private use.
			case 'i':
				if (this instanceof IanaLanguageTag || IanaLanguageTag.find(this) != null) {
					return null;
				}
				return toString() + " not found in IANA langauge registry.";
			default:
				return "Only 'x' and 'i' single character primary language subtags are defined in RFC3066.";
			}
		case 2:
			if (Iso639.find(subtags[0]) == null) {
				return "ISO-639 does not define language: '" + subtags[0] + "'.";
			}
			break;
		case 3:
			Iso639 lang = Iso639.find(subtags[0]);
			if (lang == null) {
				return "ISO-639 does not define language: '" + subtags[0] + "'.";
			}
			if (lang.twoCharCode != null) {
				return "RFC 3066 section 2.3 mandates the use of '" + lang.twoCharCode + "' instead of '" + subtags[0]
						+ "'.";
				// Section 2.3 Para 2
			}
			if (!lang.terminologyCode.equals(subtags[0])) {
				return "RFC 3066 section 2.3 mandates the use of '" + lang.terminologyCode + "' instead of '"
						+ subtags[0] + "'.";
				// Section 2.3 Para 3
			}
			// Section 2.3 para 4,5,6 in a separate function.
			break;
		default:
			return "No primary language subtags of length greater than 3 are currently defined.";
		}
		if (subtags.length == 1) {
			return null;
		}
		switch (subtags[1].length()) {
		case 1:
			return "Second language subtags of length 1 are prohibited by RFC3066.";
		case 2:
			if (Iso3166.find(subtags[1]) == null) {
				return "Country code, '" + subtags[1] + "', not found in ISO 3166.";
			}
			break;
		}
		return null;
	}

	public boolean hasLanguage() {
		boolean result = (tagType & LT_ISO639) == LT_ISO639 && (tagType & LT_MULTIPLE) != LT_MULTIPLE
				&& (tagType & LT_UNDETERMINED) != LT_UNDETERMINED && (tagType & LT_LOCAL_USE) != LT_LOCAL_USE;
		return result;
	}

	public Iso639 getLanguage() {
		Iso639 result = null;
		if (hasLanguage()) {
			result = Iso639.find(subtags[0]);
		}
		return result;
	}

	public boolean hasCountry() {
		boolean result = (tagType & LT_ISO3166) == LT_ISO3166;
		return result;
	}

	public Iso3166 getCountry() {
		Iso3166 result = null;
		if (hasCountry()) {
			result = Iso3166.find(subtags[1]);
		}
		return result;
	}

	public boolean hasVariant() {
		boolean result = (tagType & LT_EXTRA) == LT_EXTRA;
		return result;
	}

	public String getVariant() {
		String result = null;
		if (hasVariant()) {
			if (hasCountry()) {
				result = subtags[2];
			} else {
				result = subtags[1];
			}
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		for (String subtag : subtags) {
			result.append(subtag);
			result.append('-');
		}
		result.setLength(result.length() - 1);
		return result.toString();
	}

	public Locale toLocale() {
		Locale result = null;

		Iso639 iso639Language = getLanguage();
		Iso3166 iso3166Country = getCountry();
		String variant = getVariant();

		if (iso639Language != null) {
			String language = iso639Language.twoCharCode.toLowerCase();
			if (iso3166Country != null) {
				String country = iso3166Country.code.toUpperCase();
				if (variant != null) {
					result = new Locale(language, country, variant);
				} else {
					result = new Locale(language, country);
				}
			} else {
				result = new Locale(language);
			}
		}

		return result;
	}
}
