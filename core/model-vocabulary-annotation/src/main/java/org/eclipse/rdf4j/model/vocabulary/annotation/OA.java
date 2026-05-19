/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.model.vocabulary.annotation;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.Vocabularies;

/**
 * Constants for the Web Annotation Ontology.
 *
 * @see <a href="https://www.w3.org/TR/annotation-vocab/">Web Annotation Ontology</a>
 *
 * @author Bart Hanssens
 */
public class OA {
	/**
	 * The OA namespace: http://www.w3.org/ns/oa#
	 */
	public static final String NAMESPACE = "http://www.w3.org/ns/oa#";

	/**
	 * Recommended prefix for the namespace: "oa"
	 */
	public static final String PREFIX = "oa";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** oa:Annotation */
	public static final IRI Annotation;

	/** oa:Choice */
	public static final IRI Choice;

	/** oa:CssSelector */
	public static final IRI CssSelector;

	/** oa:CssStyle */
	public static final IRI CssStyle;

	/** oa:DataPositionSelector */
	public static final IRI DataPositionSelector;

	/** oa:Direction */
	public static final IRI Direction;

	/** oa:FragmentSelector */
	public static final IRI FragmentSelector;

	/** oa:HttpRequestState */
	public static final IRI HttpRequestState;

	/** oa:Motivation */
	public static final IRI Motivation;

	/** oa:RangeSelector */
	public static final IRI RangeSelector;

	/** oa:ResourceSelection */
	public static final IRI ResourceSelection;

	/** oa:Selector */
	public static final IRI Selector;

	/** oa:SpecificResource */
	public static final IRI SpecificResource;

	/** oa:State */
	public static final IRI State;

	/** oa:Style */
	public static final IRI Style;

	/** oa:SvgSelector */
	public static final IRI SvgSelector;

	/** oa:TextPositionSelector */
	public static final IRI TextPositionSelector;

	/** oa:TextQuoteSelector */
	public static final IRI TextQuoteSelector;

	/** oa:TextualBody */
	public static final IRI TextualBody;

	/** oa:TimeState */
	public static final IRI TimeState;

	/** oa:XPathSelector */
	public static final IRI XpathSelector;

	// Properties
	/** oa:annotationService */
	public static final IRI annotationService;

	/** oa:bodyValue */
	public static final IRI bodyValue;

	/** oa:cachedSource */
	public static final IRI cachedSource;

	/** oa:canonical */
	public static final IRI canonical;

	/** oa:end */
	public static final IRI end;

	/** oa:exact */
	public static final IRI exact;

	/** oa:hasBody */
	public static final IRI hasBody;

	/** oa:hasEndSelector */
	public static final IRI hasEndSelector;

	/** oa:hasPurpose */
	public static final IRI hasPurpose;

	/** oa:hasScope */
	public static final IRI hasScope;

	/** oa:hasSelector */
	public static final IRI hasSelector;

	/** oa:hasSource */
	public static final IRI hasSource;

	/** oa:hasStartSelector */
	public static final IRI hasStartSelector;

	/** oa:hasState */
	public static final IRI hasState;

	/** oa:hasTarget */
	public static final IRI hasTarget;

	/** oa:motivatedBy */
	public static final IRI motivatedBy;

	/** oa:prefix */
	public static final IRI prefix;

	/** oa:processingLanguage */
	public static final IRI processingLanguage;

	/** oa:refinedBy */
	public static final IRI refinedBy;

	/** oa:renderedVia */
	public static final IRI renderedVia;

	/** oa:sourceDate */
	public static final IRI sourceDate;

	/** oa:sourceDateEnd */
	public static final IRI sourceDateEnd;

	/** oa:sourceDateStart */
	public static final IRI sourceDateStart;

	/** oa:start */
	public static final IRI start;

	/** oa:styleClass */
	public static final IRI styleClass;

	/** oa:styledBy */
	public static final IRI styledBy;

	/** oa:suffix */
	public static final IRI suffix;

	/** oa:textDirection */
	public static final IRI textDirection;

	/** oa:via */
	public static final IRI via;

	// Individuals
	/** oa:assessing */
	public static final IRI assessing;

	/** oa:bookmarking */
	public static final IRI bookmarking;

	/** oa:classifying */
	public static final IRI classifying;

	/** oa:commenting */
	public static final IRI commenting;

	/** oa:describing */
	public static final IRI describing;

	/** oa:editing */
	public static final IRI editing;

	/** oa:highlighting */
	public static final IRI highlighting;

	/** oa:identifying */
	public static final IRI identifying;

	/** oa:linking */
	public static final IRI linking;

	/** oa:ltrDirection */
	public static final IRI ltrDirection;

	/** oa:moderating */
	public static final IRI moderating;

	/** oa:questioning */
	public static final IRI questioning;

	/** oa:replying */
	public static final IRI replying;

	/** oa:rtlDirection */
	public static final IRI rtlDirection;

	/** oa:tagging */
	public static final IRI tagging;

	static {
		Annotation = Vocabularies.createIRI(NAMESPACE, "Annotation");
		Choice = Vocabularies.createIRI(NAMESPACE, "Choice");
		CssSelector = Vocabularies.createIRI(NAMESPACE, "CssSelector");
		CssStyle = Vocabularies.createIRI(NAMESPACE, "CssStyle");
		DataPositionSelector = Vocabularies.createIRI(NAMESPACE, "DataPositionSelector");
		Direction = Vocabularies.createIRI(NAMESPACE, "Direction");
		FragmentSelector = Vocabularies.createIRI(NAMESPACE, "FragmentSelector");
		HttpRequestState = Vocabularies.createIRI(NAMESPACE, "HttpRequestState");
		Motivation = Vocabularies.createIRI(NAMESPACE, "Motivation");
		RangeSelector = Vocabularies.createIRI(NAMESPACE, "RangeSelector");
		ResourceSelection = Vocabularies.createIRI(NAMESPACE, "ResourceSelection");
		Selector = Vocabularies.createIRI(NAMESPACE, "Selector");
		SpecificResource = Vocabularies.createIRI(NAMESPACE, "SpecificResource");
		State = Vocabularies.createIRI(NAMESPACE, "State");
		Style = Vocabularies.createIRI(NAMESPACE, "Style");
		SvgSelector = Vocabularies.createIRI(NAMESPACE, "SvgSelector");
		TextPositionSelector = Vocabularies.createIRI(NAMESPACE, "TextPositionSelector");
		TextQuoteSelector = Vocabularies.createIRI(NAMESPACE, "TextQuoteSelector");
		TextualBody = Vocabularies.createIRI(NAMESPACE, "TextualBody");
		TimeState = Vocabularies.createIRI(NAMESPACE, "TimeState");
		XpathSelector = Vocabularies.createIRI(NAMESPACE, "XPathSelector");

		annotationService = Vocabularies.createIRI(NAMESPACE, "annotationService");
		bodyValue = Vocabularies.createIRI(NAMESPACE, "bodyValue");
		cachedSource = Vocabularies.createIRI(NAMESPACE, "cachedSource");
		canonical = Vocabularies.createIRI(NAMESPACE, "canonical");
		end = Vocabularies.createIRI(NAMESPACE, "end");
		exact = Vocabularies.createIRI(NAMESPACE, "exact");
		hasBody = Vocabularies.createIRI(NAMESPACE, "hasBody");
		hasEndSelector = Vocabularies.createIRI(NAMESPACE, "hasEndSelector");
		hasPurpose = Vocabularies.createIRI(NAMESPACE, "hasPurpose");
		hasScope = Vocabularies.createIRI(NAMESPACE, "hasScope");
		hasSelector = Vocabularies.createIRI(NAMESPACE, "hasSelector");
		hasSource = Vocabularies.createIRI(NAMESPACE, "hasSource");
		hasStartSelector = Vocabularies.createIRI(NAMESPACE, "hasStartSelector");
		hasState = Vocabularies.createIRI(NAMESPACE, "hasState");
		hasTarget = Vocabularies.createIRI(NAMESPACE, "hasTarget");
		motivatedBy = Vocabularies.createIRI(NAMESPACE, "motivatedBy");
		prefix = Vocabularies.createIRI(NAMESPACE, "prefix");
		processingLanguage = Vocabularies.createIRI(NAMESPACE, "processingLanguage");
		refinedBy = Vocabularies.createIRI(NAMESPACE, "refinedBy");
		renderedVia = Vocabularies.createIRI(NAMESPACE, "renderedVia");
		sourceDate = Vocabularies.createIRI(NAMESPACE, "sourceDate");
		sourceDateEnd = Vocabularies.createIRI(NAMESPACE, "sourceDateEnd");
		sourceDateStart = Vocabularies.createIRI(NAMESPACE, "sourceDateStart");
		start = Vocabularies.createIRI(NAMESPACE, "start");
		styleClass = Vocabularies.createIRI(NAMESPACE, "styleClass");
		styledBy = Vocabularies.createIRI(NAMESPACE, "styledBy");
		suffix = Vocabularies.createIRI(NAMESPACE, "suffix");
		textDirection = Vocabularies.createIRI(NAMESPACE, "textDirection");
		via = Vocabularies.createIRI(NAMESPACE, "via");

		assessing = Vocabularies.createIRI(NAMESPACE, "assessing");
		bookmarking = Vocabularies.createIRI(NAMESPACE, "bookmarking");
		classifying = Vocabularies.createIRI(NAMESPACE, "classifying");
		commenting = Vocabularies.createIRI(NAMESPACE, "commenting");
		describing = Vocabularies.createIRI(NAMESPACE, "describing");
		editing = Vocabularies.createIRI(NAMESPACE, "editing");
		highlighting = Vocabularies.createIRI(NAMESPACE, "highlighting");
		identifying = Vocabularies.createIRI(NAMESPACE, "identifying");
		linking = Vocabularies.createIRI(NAMESPACE, "linking");
		ltrDirection = Vocabularies.createIRI(NAMESPACE, "ltrDirection");
		moderating = Vocabularies.createIRI(NAMESPACE, "moderating");
		questioning = Vocabularies.createIRI(NAMESPACE, "questioning");
		replying = Vocabularies.createIRI(NAMESPACE, "replying");
		rtlDirection = Vocabularies.createIRI(NAMESPACE, "rtlDirection");
		tagging = Vocabularies.createIRI(NAMESPACE, "tagging");
	}
}
