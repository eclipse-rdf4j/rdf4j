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

package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

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
	public static final IRI ANNOTATION;

	/** oa:Choice */
	public static final IRI CHOICE;

	/** oa:CssSelector */
	public static final IRI CSS_SELECTOR;

	/** oa:CssStyle */
	public static final IRI CSS_STYLE;

	/** oa:DataPositionSelector */
	public static final IRI DATA_POSITION_SELECTOR;

	/** oa:Direction */
	public static final IRI DIRECTION;

	/** oa:FragmentSelector */
	public static final IRI FRAGMENT_SELECTOR;

	/** oa:HttpRequestState */
	public static final IRI HTTP_REQUEST_STATE;

	/** oa:Motivation */
	public static final IRI MOTIVATION;

	/** oa:RangeSelector */
	public static final IRI RANGE_SELECTOR;

	/** oa:ResourceSelection */
	public static final IRI RESOURCE_SELECTION;

	/** oa:Selector */
	public static final IRI SELECTOR;

	/** oa:SpecificResource */
	public static final IRI SPECIFIC_RESOURCE;

	/** oa:State */
	public static final IRI STATE;

	/** oa:Style */
	public static final IRI STYLE;

	/** oa:SvgSelector */
	public static final IRI SVG_SELECTOR;

	/** oa:TextPositionSelector */
	public static final IRI TEXT_POSITION_SELECTOR;

	/** oa:TextQuoteSelector */
	public static final IRI TEXT_QUOTE_SELECTOR;

	/** oa:TextualBody */
	public static final IRI TEXTUAL_BODY;

	/** oa:TimeState */
	public static final IRI TIME_STATE;

	/** oa:XPathSelector */
	public static final IRI XPATH_SELECTOR;

	// Properties
	/** oa:annotationService */
	public static final IRI ANNOTATION_SERVICE;

	/** oa:bodyValue */
	public static final IRI BODY_VALUE;

	/** oa:cachedSource */
	public static final IRI CACHED_SOURCE;

	/** oa:canonical */
	public static final IRI CANONICAL;

	/** oa:end */
	public static final IRI END;

	/** oa:exact */
	public static final IRI EXACT;

	/** oa:hasBody */
	public static final IRI HAS_BODY;

	/** oa:hasEndSelector */
	public static final IRI HAS_END_SELECTOR;

	/** oa:hasPurpose */
	public static final IRI HAS_PURPOSE;

	/** oa:hasScope */
	public static final IRI HAS_SCOPE;

	/** oa:hasSelector */
	public static final IRI HAS_SELECTOR;

	/** oa:hasSource */
	public static final IRI HAS_SOURCE;

	/** oa:hasStartSelector */
	public static final IRI HAS_START_SELECTOR;

	/** oa:hasState */
	public static final IRI HAS_STATE;

	/** oa:hasTarget */
	public static final IRI HAS_TARGET;

	/** oa:motivatedBy */
	public static final IRI MOTIVATED_BY;

	/** oa:prefix */
	public static final IRI PREFIX_PROP;

	/** oa:processingLanguage */
	public static final IRI PROCESSING_LANGUAGE;

	/** oa:refinedBy */
	public static final IRI REFINED_BY;

	/** oa:renderedVia */
	public static final IRI RENDERED_VIA;

	/** oa:sourceDate */
	public static final IRI SOURCE_DATE;

	/** oa:sourceDateEnd */
	public static final IRI SOURCE_DATE_END;

	/** oa:sourceDateStart */
	public static final IRI SOURCE_DATE_START;

	/** oa:start */
	public static final IRI START;

	/** oa:styleClass */
	public static final IRI STYLE_CLASS;

	/** oa:styledBy */
	public static final IRI STYLED_BY;

	/** oa:suffix */
	public static final IRI SUFFIX;

	/** oa:textDirection */
	public static final IRI TEXT_DIRECTION;

	/** oa:via */
	public static final IRI VIA;

	// Individuals
	/** oa:assessing */
	public static final IRI ASSESSING;

	/** oa:bookmarking */
	public static final IRI BOOKMARKING;

	/** oa:classifying */
	public static final IRI CLASSIFYING;

	/** oa:commenting */
	public static final IRI COMMENTING;

	/** oa:describing */
	public static final IRI DESCRIBING;

	/** oa:editing */
	public static final IRI EDITING;

	/** oa:highlighting */
	public static final IRI HIGHLIGHTING;

	/** oa:identifying */
	public static final IRI IDENTIFYING;

	/** oa:linking */
	public static final IRI LINKING;

	/** oa:ltrDirection */
	public static final IRI LTR_DIRECTION;

	/** oa:moderating */
	public static final IRI MODERATING;

	/** oa:questioning */
	public static final IRI QUESTIONING;

	/** oa:replying */
	public static final IRI REPLYING;

	/** oa:rtlDirection */
	public static final IRI RTL_DIRECTION;

	/** oa:tagging */
	public static final IRI TAGGING;

	static {
		ANNOTATION = Vocabularies.createIRI(NAMESPACE, "Annotation");
		CHOICE = Vocabularies.createIRI(NAMESPACE, "Choice");
		CSS_SELECTOR = Vocabularies.createIRI(NAMESPACE, "CssSelector");
		CSS_STYLE = Vocabularies.createIRI(NAMESPACE, "CssStyle");
		DATA_POSITION_SELECTOR = Vocabularies.createIRI(NAMESPACE, "DataPositionSelector");
		DIRECTION = Vocabularies.createIRI(NAMESPACE, "Direction");
		FRAGMENT_SELECTOR = Vocabularies.createIRI(NAMESPACE, "FragmentSelector");
		HTTP_REQUEST_STATE = Vocabularies.createIRI(NAMESPACE, "HttpRequestState");
		MOTIVATION = Vocabularies.createIRI(NAMESPACE, "Motivation");
		RANGE_SELECTOR = Vocabularies.createIRI(NAMESPACE, "RangeSelector");
		RESOURCE_SELECTION = Vocabularies.createIRI(NAMESPACE, "ResourceSelection");
		SELECTOR = Vocabularies.createIRI(NAMESPACE, "Selector");
		SPECIFIC_RESOURCE = Vocabularies.createIRI(NAMESPACE, "SpecificResource");
		STATE = Vocabularies.createIRI(NAMESPACE, "State");
		STYLE = Vocabularies.createIRI(NAMESPACE, "Style");
		SVG_SELECTOR = Vocabularies.createIRI(NAMESPACE, "SvgSelector");
		TEXT_POSITION_SELECTOR = Vocabularies.createIRI(NAMESPACE, "TextPositionSelector");
		TEXT_QUOTE_SELECTOR = Vocabularies.createIRI(NAMESPACE, "TextQuoteSelector");
		TEXTUAL_BODY = Vocabularies.createIRI(NAMESPACE, "TextualBody");
		TIME_STATE = Vocabularies.createIRI(NAMESPACE, "TimeState");
		XPATH_SELECTOR = Vocabularies.createIRI(NAMESPACE, "XPathSelector");

		ANNOTATION_SERVICE = Vocabularies.createIRI(NAMESPACE, "annotationService");
		BODY_VALUE = Vocabularies.createIRI(NAMESPACE, "bodyValue");
		CACHED_SOURCE = Vocabularies.createIRI(NAMESPACE, "cachedSource");
		CANONICAL = Vocabularies.createIRI(NAMESPACE, "canonical");
		END = Vocabularies.createIRI(NAMESPACE, "end");
		EXACT = Vocabularies.createIRI(NAMESPACE, "exact");
		HAS_BODY = Vocabularies.createIRI(NAMESPACE, "hasBody");
		HAS_END_SELECTOR = Vocabularies.createIRI(NAMESPACE, "hasEndSelector");
		HAS_PURPOSE = Vocabularies.createIRI(NAMESPACE, "hasPurpose");
		HAS_SCOPE = Vocabularies.createIRI(NAMESPACE, "hasScope");
		HAS_SELECTOR = Vocabularies.createIRI(NAMESPACE, "hasSelector");
		HAS_SOURCE = Vocabularies.createIRI(NAMESPACE, "hasSource");
		HAS_START_SELECTOR = Vocabularies.createIRI(NAMESPACE, "hasStartSelector");
		HAS_STATE = Vocabularies.createIRI(NAMESPACE, "hasState");
		HAS_TARGET = Vocabularies.createIRI(NAMESPACE, "hasTarget");
		MOTIVATED_BY = Vocabularies.createIRI(NAMESPACE, "motivatedBy");
		PREFIX_PROP = Vocabularies.createIRI(NAMESPACE, "prefix");
		PROCESSING_LANGUAGE = Vocabularies.createIRI(NAMESPACE, "processingLanguage");
		REFINED_BY = Vocabularies.createIRI(NAMESPACE, "refinedBy");
		RENDERED_VIA = Vocabularies.createIRI(NAMESPACE, "renderedVia");
		SOURCE_DATE = Vocabularies.createIRI(NAMESPACE, "sourceDate");
		SOURCE_DATE_END = Vocabularies.createIRI(NAMESPACE, "sourceDateEnd");
		SOURCE_DATE_START = Vocabularies.createIRI(NAMESPACE, "sourceDateStart");
		START = Vocabularies.createIRI(NAMESPACE, "start");
		STYLE_CLASS = Vocabularies.createIRI(NAMESPACE, "styleClass");
		STYLED_BY = Vocabularies.createIRI(NAMESPACE, "styledBy");
		SUFFIX = Vocabularies.createIRI(NAMESPACE, "suffix");
		TEXT_DIRECTION = Vocabularies.createIRI(NAMESPACE, "textDirection");
		VIA = Vocabularies.createIRI(NAMESPACE, "via");

		ASSESSING = Vocabularies.createIRI(NAMESPACE, "assessing");
		BOOKMARKING = Vocabularies.createIRI(NAMESPACE, "bookmarking");
		CLASSIFYING = Vocabularies.createIRI(NAMESPACE, "classifying");
		COMMENTING = Vocabularies.createIRI(NAMESPACE, "commenting");
		DESCRIBING = Vocabularies.createIRI(NAMESPACE, "describing");
		EDITING = Vocabularies.createIRI(NAMESPACE, "editing");
		HIGHLIGHTING = Vocabularies.createIRI(NAMESPACE, "highlighting");
		IDENTIFYING = Vocabularies.createIRI(NAMESPACE, "identifying");
		LINKING = Vocabularies.createIRI(NAMESPACE, "linking");
		LTR_DIRECTION = Vocabularies.createIRI(NAMESPACE, "ltrDirection");
		MODERATING = Vocabularies.createIRI(NAMESPACE, "moderating");
		QUESTIONING = Vocabularies.createIRI(NAMESPACE, "questioning");
		REPLYING = Vocabularies.createIRI(NAMESPACE, "replying");
		RTL_DIRECTION = Vocabularies.createIRI(NAMESPACE, "rtlDirection");
		TAGGING = Vocabularies.createIRI(NAMESPACE, "tagging");
	}
}
