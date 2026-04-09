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
 * Constants for the DCMI Bibliographic Ontology.
 *
 * @see <a href="https://www.dublincore.org/specifications/bibo/bibo/">DCMI Bibliographic Ontology</a>
 *
 * @author Bart Hanssens
 */
public class BIBO {
	/**
	 * The BIBO namespace: http://purl.org/ontology/bibo/
	 */
	public static final String NAMESPACE = "http://purl.org/ontology/bibo/";

	/**
	 * Recommended prefix for the namespace: "bibo"
	 */
	public static final String PREFIX = "bibo";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** bibo:AcademicArticle */
	public static final IRI ACADEMIC_ARTICLE;

	/** bibo:Article */
	public static final IRI ARTICLE;

	/** bibo:AudioDocument */
	public static final IRI AUDIO_DOCUMENT;

	/** bibo:AudioVisualDocument */
	public static final IRI AUDIO_VISUAL_DOCUMENT;

	/** bibo:Bill */
	public static final IRI BILL;

	/** bibo:Book */
	public static final IRI BOOK;

	/** bibo:BookSection */
	public static final IRI BOOK_SECTION;

	/** bibo:Brief */
	public static final IRI BRIEF;

	/** bibo:Chapter */
	public static final IRI CHAPTER;

	/** bibo:Code */
	public static final IRI CODE;

	/** bibo:CollectedDocument */
	public static final IRI COLLECTED_DOCUMENT;

	/** bibo:Collection */
	public static final IRI COLLECTION;

	/** bibo:Conference */
	public static final IRI CONFERENCE;

	/** bibo:CourtReporter */
	public static final IRI COURT_REPORTER;

	/** bibo:Document */
	public static final IRI DOCUMENT;

	/** bibo:DocumentPart */
	public static final IRI DOCUMENT_PART;

	/** bibo:DocumentStatus */
	public static final IRI DOCUMENT_STATUS;

	/** bibo:EditedBook */
	public static final IRI EDITED_BOOK;

	/** bibo:Email */
	public static final IRI EMAIL;

	/** bibo:Event */
	public static final IRI EVENT;

	/** bibo:Excerpt */
	public static final IRI EXCERPT;

	/** bibo:Film */
	public static final IRI FILM;

	/** bibo:Hearing */
	public static final IRI HEARING;

	/** bibo:Image */
	public static final IRI IMAGE;

	/** bibo:Interview */
	public static final IRI INTERVIEW;

	/** bibo:Issue */
	public static final IRI ISSUE;

	/** bibo:Journal */
	public static final IRI JOURNAL;

	/** bibo:LegalCaseDocument */
	public static final IRI LEGAL_CASE_DOCUMENT;

	/** bibo:LegalDecision */
	public static final IRI LEGAL_DECISION;

	/** bibo:LegalDocument */
	public static final IRI LEGAL_DOCUMENT;

	/** bibo:Legislation */
	public static final IRI LEGISLATION;

	/** bibo:Letter */
	public static final IRI LETTER;

	/** bibo:Magazine */
	public static final IRI MAGAZINE;

	/** bibo:Manual */
	public static final IRI MANUAL;

	/** bibo:Manuscript */
	public static final IRI MANUSCRIPT;

	/** bibo:Map */
	public static final IRI MAP;

	/** bibo:MultiVolumeBook */
	public static final IRI MULTI_VOLUME_BOOK;

	/** bibo:Newspaper */
	public static final IRI NEWSPAPER;

	/** bibo:Note */
	public static final IRI NOTE;

	/** bibo:Patent */
	public static final IRI PATENT;

	/** bibo:Performance */
	public static final IRI PERFORMANCE;

	/** bibo:Periodical */
	public static final IRI PERIODICAL;

	/** bibo:PersonalCommunication */
	public static final IRI PERSONAL_COMMUNICATION;

	/** bibo:PersonalCommunicationDocument */
	public static final IRI PERSONAL_COMMUNICATION_DOCUMENT;

	/** bibo:Proceedings */
	public static final IRI PROCEEDINGS;

	/** bibo:Quote */
	public static final IRI QUOTE;

	/** bibo:ReferenceSource */
	public static final IRI REFERENCE_SOURCE;

	/** bibo:Report */
	public static final IRI REPORT;

	/** bibo:Series */
	public static final IRI SERIES;

	/** bibo:Slide */
	public static final IRI SLIDE;

	/** bibo:Slideshow */
	public static final IRI SLIDESHOW;

	/** bibo:Specification */
	public static final IRI SPECIFICATION;

	/** bibo:Standard */
	public static final IRI STANDARD;

	/** bibo:Statute */
	public static final IRI STATUTE;

	/** bibo:Thesis */
	public static final IRI THESIS;

	/** bibo:ThesisDegree */
	public static final IRI THESIS_DEGREE;

	/** bibo:Webpage */
	public static final IRI WEBPAGE;

	/** bibo:Website */
	public static final IRI WEBSITE;

	/** bibo:Workshop */
	public static final IRI WORKSHOP;

	// Properties
	/** bibo:abstract */
	public static final IRI ABSTRACT;

	/** bibo:affirmedBy */
	public static final IRI AFFIRMED_BY;

	/** bibo:annotates */
	public static final IRI ANNOTATES;

	/** bibo:argued */
	public static final IRI ARGUED;

	/** bibo:asin */
	public static final IRI ASIN;

	/** bibo:authorList */
	public static final IRI AUTHOR_LIST;

	/** bibo:chapter */
	public static final IRI CHAPTER_PROP;

	/** bibo:citedBy */
	public static final IRI CITED_BY;

	/** bibo:cites */
	public static final IRI CITES;

	/** bibo:coden */
	public static final IRI CODEN;

	/** bibo:content */
	@Deprecated
	public static final IRI CONTENT;

	/** bibo:contributorList */
	public static final IRI CONTRIBUTOR_LIST;

	/** bibo:court */
	public static final IRI COURT;

	/** bibo:degree */
	public static final IRI DEGREE;

	/** bibo:director */
	public static final IRI DIRECTOR;

	/** bibo:distributor */
	public static final IRI DISTRIBUTOR;

	/** bibo:doi */
	public static final IRI DOI;

	/** bibo:eanucc13 */
	public static final IRI EANUCC13;

	/** bibo:edition */
	public static final IRI EDITION;

	/** bibo:editor */
	public static final IRI EDITOR;

	/** bibo:editorList */
	public static final IRI EDITOR_LIST;

	/** bibo:eissn */
	public static final IRI EISSN;

	/** bibo:gtin14 */
	public static final IRI GTIN14;

	/** bibo:handle */
	public static final IRI HANDLE;

	/** bibo:identifier */
	public static final IRI IDENTIFIER;

	/** bibo:interviewee */
	public static final IRI INTERVIEWEE;

	/** bibo:interviewer */
	public static final IRI INTERVIEWER;

	/** bibo:isbn */
	public static final IRI ISBN;

	/** bibo:isbn10 */
	public static final IRI ISBN10;

	/** bibo:isbn13 */
	public static final IRI ISBN13;

	/** bibo:issn */
	public static final IRI ISSN;

	/** bibo:issue */
	public static final IRI ISSUE_PROP;

	/** bibo:issuer */
	public static final IRI ISSUER;

	/** bibo:lccn */
	public static final IRI LCCN;

	/** bibo:locator */
	public static final IRI LOCATOR;

	/** bibo:numPages */
	public static final IRI NUM_PAGES;

	/** bibo:numVolumes */
	public static final IRI NUM_VOLUMES;

	/** bibo:number */
	public static final IRI NUMBER;

	/** bibo:oclcnum */
	public static final IRI OCLCNUM;

	/** bibo:organizer */
	public static final IRI ORGANIZER;

	/** bibo:owner */
	public static final IRI OWNER;

	/** bibo:pageEnd */
	public static final IRI PAGE_END;

	/** bibo:pageStart */
	public static final IRI PAGE_START;

	/** bibo:pages */
	public static final IRI PAGES;

	/** bibo:performer */
	public static final IRI PERFORMER;

	/** bibo:pmid */
	public static final IRI PMID;

	/** bibo:prefixName */
	public static final IRI PREFIX_NAME;

	/** bibo:presentedAt */
	public static final IRI PRESENTED_AT;

	/** bibo:presents */
	public static final IRI PRESENTS;

	/** bibo:producer */
	public static final IRI PRODUCER;

	/** bibo:recipient */
	public static final IRI RECIPIENT;

	/** bibo:reproducedIn */
	public static final IRI REPRODUCED_IN;

	/** bibo:reversedBy */
	public static final IRI REVERSED_BY;

	/** bibo:reviewOf */
	public static final IRI REVIEW_OF;

	/** bibo:section */
	public static final IRI SECTION;

	/** bibo:shortDescription */
	public static final IRI SHORT_DESCRIPTION;

	/** bibo:shortTitle */
	public static final IRI SHORT_TITLE;

	/** bibo:sici */
	public static final IRI SICI;

	/** bibo:status */
	public static final IRI STATUS;

	/** bibo:subsequentLegalDecision */
	public static final IRI SUBSEQUENT_LEGAL_DECISION;

	/** bibo:suffixName */
	public static final IRI SUFFIX_NAME;

	/** bibo:transcriptOf */
	public static final IRI TRANSCRIPT_OF;

	/** bibo:translationOf */
	public static final IRI TRANSLATION_OF;

	/** bibo:translator */
	public static final IRI TRANSLATOR;

	/** bibo:upc */
	public static final IRI UPC;

	/** bibo:uri */
	public static final IRI URI;

	/** bibo:volume */
	public static final IRI VOLUME;

	static {
		ACADEMIC_ARTICLE = Vocabularies.createIRI(NAMESPACE, "AcademicArticle");
		ARTICLE = Vocabularies.createIRI(NAMESPACE, "Article");
		AUDIO_DOCUMENT = Vocabularies.createIRI(NAMESPACE, "AudioDocument");
		AUDIO_VISUAL_DOCUMENT = Vocabularies.createIRI(NAMESPACE, "AudioVisualDocument");
		BILL = Vocabularies.createIRI(NAMESPACE, "Bill");
		BOOK = Vocabularies.createIRI(NAMESPACE, "Book");
		BOOK_SECTION = Vocabularies.createIRI(NAMESPACE, "BookSection");
		BRIEF = Vocabularies.createIRI(NAMESPACE, "Brief");
		CHAPTER = Vocabularies.createIRI(NAMESPACE, "Chapter");
		CODE = Vocabularies.createIRI(NAMESPACE, "Code");
		COLLECTED_DOCUMENT = Vocabularies.createIRI(NAMESPACE, "CollectedDocument");
		COLLECTION = Vocabularies.createIRI(NAMESPACE, "Collection");
		CONFERENCE = Vocabularies.createIRI(NAMESPACE, "Conference");
		COURT_REPORTER = Vocabularies.createIRI(NAMESPACE, "CourtReporter");
		DOCUMENT = Vocabularies.createIRI(NAMESPACE, "Document");
		DOCUMENT_PART = Vocabularies.createIRI(NAMESPACE, "DocumentPart");
		DOCUMENT_STATUS = Vocabularies.createIRI(NAMESPACE, "DocumentStatus");
		EDITED_BOOK = Vocabularies.createIRI(NAMESPACE, "EditedBook");
		EMAIL = Vocabularies.createIRI(NAMESPACE, "Email");
		EVENT = Vocabularies.createIRI(NAMESPACE, "Event");
		EXCERPT = Vocabularies.createIRI(NAMESPACE, "Excerpt");
		FILM = Vocabularies.createIRI(NAMESPACE, "Film");
		HEARING = Vocabularies.createIRI(NAMESPACE, "Hearing");
		IMAGE = Vocabularies.createIRI(NAMESPACE, "Image");
		INTERVIEW = Vocabularies.createIRI(NAMESPACE, "Interview");
		ISSUE = Vocabularies.createIRI(NAMESPACE, "Issue");
		JOURNAL = Vocabularies.createIRI(NAMESPACE, "Journal");
		LEGAL_CASE_DOCUMENT = Vocabularies.createIRI(NAMESPACE, "LegalCaseDocument");
		LEGAL_DECISION = Vocabularies.createIRI(NAMESPACE, "LegalDecision");
		LEGAL_DOCUMENT = Vocabularies.createIRI(NAMESPACE, "LegalDocument");
		LEGISLATION = Vocabularies.createIRI(NAMESPACE, "Legislation");
		LETTER = Vocabularies.createIRI(NAMESPACE, "Letter");
		MAGAZINE = Vocabularies.createIRI(NAMESPACE, "Magazine");
		MANUAL = Vocabularies.createIRI(NAMESPACE, "Manual");
		MANUSCRIPT = Vocabularies.createIRI(NAMESPACE, "Manuscript");
		MAP = Vocabularies.createIRI(NAMESPACE, "Map");
		MULTI_VOLUME_BOOK = Vocabularies.createIRI(NAMESPACE, "MultiVolumeBook");
		NEWSPAPER = Vocabularies.createIRI(NAMESPACE, "Newspaper");
		NOTE = Vocabularies.createIRI(NAMESPACE, "Note");
		PATENT = Vocabularies.createIRI(NAMESPACE, "Patent");
		PERFORMANCE = Vocabularies.createIRI(NAMESPACE, "Performance");
		PERIODICAL = Vocabularies.createIRI(NAMESPACE, "Periodical");
		PERSONAL_COMMUNICATION = Vocabularies.createIRI(NAMESPACE, "PersonalCommunication");
		PERSONAL_COMMUNICATION_DOCUMENT = Vocabularies.createIRI(NAMESPACE, "PersonalCommunicationDocument");
		PROCEEDINGS = Vocabularies.createIRI(NAMESPACE, "Proceedings");
		QUOTE = Vocabularies.createIRI(NAMESPACE, "Quote");
		REFERENCE_SOURCE = Vocabularies.createIRI(NAMESPACE, "ReferenceSource");
		REPORT = Vocabularies.createIRI(NAMESPACE, "Report");
		SERIES = Vocabularies.createIRI(NAMESPACE, "Series");
		SLIDE = Vocabularies.createIRI(NAMESPACE, "Slide");
		SLIDESHOW = Vocabularies.createIRI(NAMESPACE, "Slideshow");
		SPECIFICATION = Vocabularies.createIRI(NAMESPACE, "Specification");
		STANDARD = Vocabularies.createIRI(NAMESPACE, "Standard");
		STATUTE = Vocabularies.createIRI(NAMESPACE, "Statute");
		THESIS = Vocabularies.createIRI(NAMESPACE, "Thesis");
		THESIS_DEGREE = Vocabularies.createIRI(NAMESPACE, "ThesisDegree");
		WEBPAGE = Vocabularies.createIRI(NAMESPACE, "Webpage");
		WEBSITE = Vocabularies.createIRI(NAMESPACE, "Website");
		WORKSHOP = Vocabularies.createIRI(NAMESPACE, "Workshop");

		ABSTRACT = Vocabularies.createIRI(NAMESPACE, "abstract");
		AFFIRMED_BY = Vocabularies.createIRI(NAMESPACE, "affirmedBy");
		ANNOTATES = Vocabularies.createIRI(NAMESPACE, "annotates");
		ARGUED = Vocabularies.createIRI(NAMESPACE, "argued");
		ASIN = Vocabularies.createIRI(NAMESPACE, "asin");
		AUTHOR_LIST = Vocabularies.createIRI(NAMESPACE, "authorList");
		CHAPTER_PROP = Vocabularies.createIRI(NAMESPACE, "chapter");
		CITED_BY = Vocabularies.createIRI(NAMESPACE, "citedBy");
		CITES = Vocabularies.createIRI(NAMESPACE, "cites");
		CODEN = Vocabularies.createIRI(NAMESPACE, "coden");
		CONTENT = Vocabularies.createIRI(NAMESPACE, "content");
		CONTRIBUTOR_LIST = Vocabularies.createIRI(NAMESPACE, "contributorList");
		COURT = Vocabularies.createIRI(NAMESPACE, "court");
		DEGREE = Vocabularies.createIRI(NAMESPACE, "degree");
		DIRECTOR = Vocabularies.createIRI(NAMESPACE, "director");
		DISTRIBUTOR = Vocabularies.createIRI(NAMESPACE, "distributor");
		DOI = Vocabularies.createIRI(NAMESPACE, "doi");
		EANUCC13 = Vocabularies.createIRI(NAMESPACE, "eanucc13");
		EDITION = Vocabularies.createIRI(NAMESPACE, "edition");
		EDITOR = Vocabularies.createIRI(NAMESPACE, "editor");
		EDITOR_LIST = Vocabularies.createIRI(NAMESPACE, "editorList");
		EISSN = Vocabularies.createIRI(NAMESPACE, "eissn");
		GTIN14 = Vocabularies.createIRI(NAMESPACE, "gtin14");
		HANDLE = Vocabularies.createIRI(NAMESPACE, "handle");
		IDENTIFIER = Vocabularies.createIRI(NAMESPACE, "identifier");
		INTERVIEWEE = Vocabularies.createIRI(NAMESPACE, "interviewee");
		INTERVIEWER = Vocabularies.createIRI(NAMESPACE, "interviewer");
		ISBN = Vocabularies.createIRI(NAMESPACE, "isbn");
		ISBN10 = Vocabularies.createIRI(NAMESPACE, "isbn10");
		ISBN13 = Vocabularies.createIRI(NAMESPACE, "isbn13");
		ISSN = Vocabularies.createIRI(NAMESPACE, "issn");
		ISSUE_PROP = Vocabularies.createIRI(NAMESPACE, "issue");
		ISSUER = Vocabularies.createIRI(NAMESPACE, "issuer");
		LCCN = Vocabularies.createIRI(NAMESPACE, "lccn");
		LOCATOR = Vocabularies.createIRI(NAMESPACE, "locator");
		NUM_PAGES = Vocabularies.createIRI(NAMESPACE, "numPages");
		NUM_VOLUMES = Vocabularies.createIRI(NAMESPACE, "numVolumes");
		NUMBER = Vocabularies.createIRI(NAMESPACE, "number");
		OCLCNUM = Vocabularies.createIRI(NAMESPACE, "oclcnum");
		ORGANIZER = Vocabularies.createIRI(NAMESPACE, "organizer");
		OWNER = Vocabularies.createIRI(NAMESPACE, "owner");
		PAGE_END = Vocabularies.createIRI(NAMESPACE, "pageEnd");
		PAGE_START = Vocabularies.createIRI(NAMESPACE, "pageStart");
		PAGES = Vocabularies.createIRI(NAMESPACE, "pages");
		PERFORMER = Vocabularies.createIRI(NAMESPACE, "performer");
		PMID = Vocabularies.createIRI(NAMESPACE, "pmid");
		PREFIX_NAME = Vocabularies.createIRI(NAMESPACE, "prefixName");
		PRESENTED_AT = Vocabularies.createIRI(NAMESPACE, "presentedAt");
		PRESENTS = Vocabularies.createIRI(NAMESPACE, "presents");
		PRODUCER = Vocabularies.createIRI(NAMESPACE, "producer");
		RECIPIENT = Vocabularies.createIRI(NAMESPACE, "recipient");
		REPRODUCED_IN = Vocabularies.createIRI(NAMESPACE, "reproducedIn");
		REVERSED_BY = Vocabularies.createIRI(NAMESPACE, "reversedBy");
		REVIEW_OF = Vocabularies.createIRI(NAMESPACE, "reviewOf");
		SECTION = Vocabularies.createIRI(NAMESPACE, "section");
		SHORT_DESCRIPTION = Vocabularies.createIRI(NAMESPACE, "shortDescription");
		SHORT_TITLE = Vocabularies.createIRI(NAMESPACE, "shortTitle");
		SICI = Vocabularies.createIRI(NAMESPACE, "sici");
		STATUS = Vocabularies.createIRI(NAMESPACE, "status");
		SUBSEQUENT_LEGAL_DECISION = Vocabularies.createIRI(NAMESPACE, "subsequentLegalDecision");
		SUFFIX_NAME = Vocabularies.createIRI(NAMESPACE, "suffixName");
		TRANSCRIPT_OF = Vocabularies.createIRI(NAMESPACE, "transcriptOf");
		TRANSLATION_OF = Vocabularies.createIRI(NAMESPACE, "translationOf");
		TRANSLATOR = Vocabularies.createIRI(NAMESPACE, "translator");
		UPC = Vocabularies.createIRI(NAMESPACE, "upc");
		URI = Vocabularies.createIRI(NAMESPACE, "uri");
		VOLUME = Vocabularies.createIRI(NAMESPACE, "volume");

	}
}
