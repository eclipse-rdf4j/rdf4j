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

package org.eclipse.rdf4j.model.vocabulary.biblio;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.Vocabularies;

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
	public static final IRI AcademicArticle;

	/** bibo:Article */
	public static final IRI Article;

	/** bibo:AudioDocument */
	public static final IRI AudioDocument;

	/** bibo:AudioVisualDocument */
	public static final IRI AudioVisualDocument;

	/** bibo:Bill */
	public static final IRI Bill;

	/** bibo:Book */
	public static final IRI Book;

	/** bibo:BookSection */
	public static final IRI BookSection;

	/** bibo:Brief */
	public static final IRI Brief;

	/** bibo:Chapter */
	public static final IRI Chapter;

	/** bibo:Code */
	public static final IRI Code;

	/** bibo:CollectedDocument */
	public static final IRI CollectedDocument;

	/** bibo:Collection */
	public static final IRI Collection;

	/** bibo:Conference */
	public static final IRI Conference;

	/** bibo:CourtReporter */
	public static final IRI CourtReporter;

	/** bibo:Document */
	public static final IRI Document;

	/** bibo:DocumentPart */
	public static final IRI DocumentPart;

	/** bibo:DocumentStatus */
	public static final IRI DocumentStatus;

	/** bibo:EditedBook */
	public static final IRI EditedBook;

	/** bibo:Email */
	public static final IRI Email;

	/** bibo:Event */
	public static final IRI Event;

	/** bibo:Excerpt */
	public static final IRI Excerpt;

	/** bibo:Film */
	public static final IRI Film;

	/** bibo:Hearing */
	public static final IRI Hearing;

	/** bibo:Image */
	public static final IRI Image;

	/** bibo:Interview */
	public static final IRI Interview;

	/** bibo:Issue */
	public static final IRI Issue;

	/** bibo:Journal */
	public static final IRI Journal;

	/** bibo:LegalCaseDocument */
	public static final IRI LegalCaseDocument;

	/** bibo:LegalDecision */
	public static final IRI LegalDecision;

	/** bibo:LegalDocument */
	public static final IRI LegalDocument;

	/** bibo:Legislation */
	public static final IRI Legislation;

	/** bibo:Letter */
	public static final IRI Letter;

	/** bibo:Magazine */
	public static final IRI Magazine;

	/** bibo:Manual */
	public static final IRI Manual;

	/** bibo:Manuscript */
	public static final IRI Manuscript;

	/** bibo:Map */
	public static final IRI Map;

	/** bibo:MultiVolumeBook */
	public static final IRI MultiVolumeBook;

	/** bibo:Newspaper */
	public static final IRI Newspaper;

	/** bibo:Note */
	public static final IRI Note;

	/** bibo:Patent */
	public static final IRI Patent;

	/** bibo:Performance */
	public static final IRI Performance;

	/** bibo:Periodical */
	public static final IRI Periodical;

	/** bibo:PersonalCommunication */
	public static final IRI PersonalCommunication;

	/** bibo:PersonalCommunicationDocument */
	public static final IRI PersonalCommunicationDocument;

	/** bibo:Proceedings */
	public static final IRI Proceedings;

	/** bibo:Quote */
	public static final IRI Quote;

	/** bibo:ReferenceSource */
	public static final IRI ReferenceSource;

	/** bibo:Report */
	public static final IRI Report;

	/** bibo:Series */
	public static final IRI Series;

	/** bibo:Slide */
	public static final IRI Slide;

	/** bibo:Slideshow */
	public static final IRI Slideshow;

	/** bibo:Specification */
	public static final IRI Specification;

	/** bibo:Standard */
	public static final IRI Standard;

	/** bibo:Statute */
	public static final IRI Statute;

	/** bibo:Thesis */
	public static final IRI Thesis;

	/** bibo:ThesisDegree */
	public static final IRI ThesisDegree;

	/** bibo:Webpage */
	public static final IRI Webpage;

	/** bibo:Website */
	public static final IRI Website;

	/** bibo:Workshop */
	public static final IRI Workshop;

	// Properties
	/** bibo:abstract */
	public static final IRI abstract_;

	/** bibo:affirmedBy */
	public static final IRI affirmedBy;

	/** bibo:annotates */
	public static final IRI annotates;

	/** bibo:argued */
	public static final IRI argued;

	/** bibo:asin */
	public static final IRI asin;

	/** bibo:authorList */
	public static final IRI authorList;

	/** bibo:chapter */
	public static final IRI chapter;

	/** bibo:citedBy */
	public static final IRI citedBy;

	/** bibo:cites */
	public static final IRI cites;

	/** bibo:coden */
	public static final IRI coden;

	/** bibo:content */
	@Deprecated
	public static final IRI content;

	/** bibo:contributorList */
	public static final IRI contributorList;

	/** bibo:court */
	public static final IRI court;

	/** bibo:degree */
	public static final IRI degree;

	/** bibo:director */
	public static final IRI director;

	/** bibo:distributor */
	public static final IRI distributor;

	/** bibo:doi */
	public static final IRI doi;

	/** bibo:eanucc13 */
	public static final IRI eanucc13;

	/** bibo:edition */
	public static final IRI edition;

	/** bibo:editor */
	public static final IRI editor;

	/** bibo:editorList */
	public static final IRI editorList;

	/** bibo:eissn */
	public static final IRI eissn;

	/** bibo:gtin14 */
	public static final IRI gtin14;

	/** bibo:handle */
	public static final IRI handle;

	/** bibo:identifier */
	public static final IRI identifier;

	/** bibo:interviewee */
	public static final IRI interviewee;

	/** bibo:interviewer */
	public static final IRI interviewer;

	/** bibo:isbn */
	public static final IRI isbn;

	/** bibo:isbn10 */
	public static final IRI isbn10;

	/** bibo:isbn13 */
	public static final IRI isbn13;

	/** bibo:issn */
	public static final IRI issn;

	/** bibo:issue */
	public static final IRI issue;

	/** bibo:issuer */
	public static final IRI issuer;

	/** bibo:lccn */
	public static final IRI lccn;

	/** bibo:locator */
	public static final IRI locator;

	/** bibo:numPages */
	public static final IRI numPages;

	/** bibo:numVolumes */
	public static final IRI numVolumes;

	/** bibo:number */
	public static final IRI number;

	/** bibo:oclcnum */
	public static final IRI oclcnum;

	/** bibo:organizer */
	public static final IRI organizer;

	/** bibo:owner */
	public static final IRI owner;

	/** bibo:pageEnd */
	public static final IRI pageEnd;

	/** bibo:pageStart */
	public static final IRI pageStart;

	/** bibo:pages */
	public static final IRI pages;

	/** bibo:performer */
	public static final IRI performer;

	/** bibo:pmid */
	public static final IRI pmid;

	/** bibo:prefixName */
	public static final IRI prefixName;

	/** bibo:presentedAt */
	public static final IRI presentedAt;

	/** bibo:presents */
	public static final IRI presents;

	/** bibo:producer */
	public static final IRI producer;

	/** bibo:recipient */
	public static final IRI recipient;

	/** bibo:reproducedIn */
	public static final IRI reproducedIn;

	/** bibo:reversedBy */
	public static final IRI reversedBy;

	/** bibo:reviewOf */
	public static final IRI reviewOf;

	/** bibo:section */
	public static final IRI section;

	/** bibo:shortDescription */
	public static final IRI shortDescription;

	/** bibo:shortTitle */
	public static final IRI shortTitle;

	/** bibo:sici */
	public static final IRI sici;

	/** bibo:status */
	public static final IRI status;

	/** bibo:subsequentLegalDecision */
	public static final IRI subsequentLegalDecision;

	/** bibo:suffixName */
	public static final IRI suffixName;

	/** bibo:transcriptOf */
	public static final IRI transcriptOf;

	/** bibo:translationOf */
	public static final IRI translationOf;

	/** bibo:translator */
	public static final IRI translator;

	/** bibo:upc */
	public static final IRI upc;

	/** bibo:uri */
	public static final IRI uri;

	/** bibo:volume */
	public static final IRI volume;

	static {
		AcademicArticle = Vocabularies.createIRI(NAMESPACE, "AcademicArticle");
		Article = Vocabularies.createIRI(NAMESPACE, "Article");
		AudioDocument = Vocabularies.createIRI(NAMESPACE, "AudioDocument");
		AudioVisualDocument = Vocabularies.createIRI(NAMESPACE, "AudioVisualDocument");
		Bill = Vocabularies.createIRI(NAMESPACE, "Bill");
		Book = Vocabularies.createIRI(NAMESPACE, "Book");
		BookSection = Vocabularies.createIRI(NAMESPACE, "BookSection");
		Brief = Vocabularies.createIRI(NAMESPACE, "Brief");
		Chapter = Vocabularies.createIRI(NAMESPACE, "Chapter");
		Code = Vocabularies.createIRI(NAMESPACE, "Code");
		CollectedDocument = Vocabularies.createIRI(NAMESPACE, "CollectedDocument");
		Collection = Vocabularies.createIRI(NAMESPACE, "Collection");
		Conference = Vocabularies.createIRI(NAMESPACE, "Conference");
		CourtReporter = Vocabularies.createIRI(NAMESPACE, "CourtReporter");
		Document = Vocabularies.createIRI(NAMESPACE, "Document");
		DocumentPart = Vocabularies.createIRI(NAMESPACE, "DocumentPart");
		DocumentStatus = Vocabularies.createIRI(NAMESPACE, "DocumentStatus");
		EditedBook = Vocabularies.createIRI(NAMESPACE, "EditedBook");
		Email = Vocabularies.createIRI(NAMESPACE, "Email");
		Event = Vocabularies.createIRI(NAMESPACE, "Event");
		Excerpt = Vocabularies.createIRI(NAMESPACE, "Excerpt");
		Film = Vocabularies.createIRI(NAMESPACE, "Film");
		Hearing = Vocabularies.createIRI(NAMESPACE, "Hearing");
		Image = Vocabularies.createIRI(NAMESPACE, "Image");
		Interview = Vocabularies.createIRI(NAMESPACE, "Interview");
		Issue = Vocabularies.createIRI(NAMESPACE, "Issue");
		Journal = Vocabularies.createIRI(NAMESPACE, "Journal");
		LegalCaseDocument = Vocabularies.createIRI(NAMESPACE, "LegalCaseDocument");
		LegalDecision = Vocabularies.createIRI(NAMESPACE, "LegalDecision");
		LegalDocument = Vocabularies.createIRI(NAMESPACE, "LegalDocument");
		Legislation = Vocabularies.createIRI(NAMESPACE, "Legislation");
		Letter = Vocabularies.createIRI(NAMESPACE, "Letter");
		Magazine = Vocabularies.createIRI(NAMESPACE, "Magazine");
		Manual = Vocabularies.createIRI(NAMESPACE, "Manual");
		Manuscript = Vocabularies.createIRI(NAMESPACE, "Manuscript");
		Map = Vocabularies.createIRI(NAMESPACE, "Map");
		MultiVolumeBook = Vocabularies.createIRI(NAMESPACE, "MultiVolumeBook");
		Newspaper = Vocabularies.createIRI(NAMESPACE, "Newspaper");
		Note = Vocabularies.createIRI(NAMESPACE, "Note");
		Patent = Vocabularies.createIRI(NAMESPACE, "Patent");
		Performance = Vocabularies.createIRI(NAMESPACE, "Performance");
		Periodical = Vocabularies.createIRI(NAMESPACE, "Periodical");
		PersonalCommunication = Vocabularies.createIRI(NAMESPACE, "PersonalCommunication");
		PersonalCommunicationDocument = Vocabularies.createIRI(NAMESPACE, "PersonalCommunicationDocument");
		Proceedings = Vocabularies.createIRI(NAMESPACE, "Proceedings");
		Quote = Vocabularies.createIRI(NAMESPACE, "Quote");
		ReferenceSource = Vocabularies.createIRI(NAMESPACE, "ReferenceSource");
		Report = Vocabularies.createIRI(NAMESPACE, "Report");
		Series = Vocabularies.createIRI(NAMESPACE, "Series");
		Slide = Vocabularies.createIRI(NAMESPACE, "Slide");
		Slideshow = Vocabularies.createIRI(NAMESPACE, "Slideshow");
		Specification = Vocabularies.createIRI(NAMESPACE, "Specification");
		Standard = Vocabularies.createIRI(NAMESPACE, "Standard");
		Statute = Vocabularies.createIRI(NAMESPACE, "Statute");
		Thesis = Vocabularies.createIRI(NAMESPACE, "Thesis");
		ThesisDegree = Vocabularies.createIRI(NAMESPACE, "ThesisDegree");
		Webpage = Vocabularies.createIRI(NAMESPACE, "Webpage");
		Website = Vocabularies.createIRI(NAMESPACE, "Website");
		Workshop = Vocabularies.createIRI(NAMESPACE, "Workshop");

		abstract_ = Vocabularies.createIRI(NAMESPACE, "abstract");
		affirmedBy = Vocabularies.createIRI(NAMESPACE, "affirmedBy");
		annotates = Vocabularies.createIRI(NAMESPACE, "annotates");
		argued = Vocabularies.createIRI(NAMESPACE, "argued");
		asin = Vocabularies.createIRI(NAMESPACE, "asin");
		authorList = Vocabularies.createIRI(NAMESPACE, "authorList");
		chapter = Vocabularies.createIRI(NAMESPACE, "chapter");
		citedBy = Vocabularies.createIRI(NAMESPACE, "citedBy");
		cites = Vocabularies.createIRI(NAMESPACE, "cites");
		coden = Vocabularies.createIRI(NAMESPACE, "coden");
		content = Vocabularies.createIRI(NAMESPACE, "content");
		contributorList = Vocabularies.createIRI(NAMESPACE, "contributorList");
		court = Vocabularies.createIRI(NAMESPACE, "court");
		degree = Vocabularies.createIRI(NAMESPACE, "degree");
		director = Vocabularies.createIRI(NAMESPACE, "director");
		distributor = Vocabularies.createIRI(NAMESPACE, "distributor");
		doi = Vocabularies.createIRI(NAMESPACE, "doi");
		eanucc13 = Vocabularies.createIRI(NAMESPACE, "eanucc13");
		edition = Vocabularies.createIRI(NAMESPACE, "edition");
		editor = Vocabularies.createIRI(NAMESPACE, "editor");
		editorList = Vocabularies.createIRI(NAMESPACE, "editorList");
		eissn = Vocabularies.createIRI(NAMESPACE, "eissn");
		gtin14 = Vocabularies.createIRI(NAMESPACE, "gtin14");
		handle = Vocabularies.createIRI(NAMESPACE, "handle");
		identifier = Vocabularies.createIRI(NAMESPACE, "identifier");
		interviewee = Vocabularies.createIRI(NAMESPACE, "interviewee");
		interviewer = Vocabularies.createIRI(NAMESPACE, "interviewer");
		isbn = Vocabularies.createIRI(NAMESPACE, "isbn");
		isbn10 = Vocabularies.createIRI(NAMESPACE, "isbn10");
		isbn13 = Vocabularies.createIRI(NAMESPACE, "isbn13");
		issn = Vocabularies.createIRI(NAMESPACE, "issn");
		issue = Vocabularies.createIRI(NAMESPACE, "issue");
		issuer = Vocabularies.createIRI(NAMESPACE, "issuer");
		lccn = Vocabularies.createIRI(NAMESPACE, "lccn");
		locator = Vocabularies.createIRI(NAMESPACE, "locator");
		numPages = Vocabularies.createIRI(NAMESPACE, "numPages");
		numVolumes = Vocabularies.createIRI(NAMESPACE, "numVolumes");
		number = Vocabularies.createIRI(NAMESPACE, "number");
		oclcnum = Vocabularies.createIRI(NAMESPACE, "oclcnum");
		organizer = Vocabularies.createIRI(NAMESPACE, "organizer");
		owner = Vocabularies.createIRI(NAMESPACE, "owner");
		pageEnd = Vocabularies.createIRI(NAMESPACE, "pageEnd");
		pageStart = Vocabularies.createIRI(NAMESPACE, "pageStart");
		pages = Vocabularies.createIRI(NAMESPACE, "pages");
		performer = Vocabularies.createIRI(NAMESPACE, "performer");
		pmid = Vocabularies.createIRI(NAMESPACE, "pmid");
		prefixName = Vocabularies.createIRI(NAMESPACE, "prefixName");
		presentedAt = Vocabularies.createIRI(NAMESPACE, "presentedAt");
		presents = Vocabularies.createIRI(NAMESPACE, "presents");
		producer = Vocabularies.createIRI(NAMESPACE, "producer");
		recipient = Vocabularies.createIRI(NAMESPACE, "recipient");
		reproducedIn = Vocabularies.createIRI(NAMESPACE, "reproducedIn");
		reversedBy = Vocabularies.createIRI(NAMESPACE, "reversedBy");
		reviewOf = Vocabularies.createIRI(NAMESPACE, "reviewOf");
		section = Vocabularies.createIRI(NAMESPACE, "section");
		shortDescription = Vocabularies.createIRI(NAMESPACE, "shortDescription");
		shortTitle = Vocabularies.createIRI(NAMESPACE, "shortTitle");
		sici = Vocabularies.createIRI(NAMESPACE, "sici");
		status = Vocabularies.createIRI(NAMESPACE, "status");
		subsequentLegalDecision = Vocabularies.createIRI(NAMESPACE, "subsequentLegalDecision");
		suffixName = Vocabularies.createIRI(NAMESPACE, "suffixName");
		transcriptOf = Vocabularies.createIRI(NAMESPACE, "transcriptOf");
		translationOf = Vocabularies.createIRI(NAMESPACE, "translationOf");
		translator = Vocabularies.createIRI(NAMESPACE, "translator");
		upc = Vocabularies.createIRI(NAMESPACE, "upc");
		uri = Vocabularies.createIRI(NAMESPACE, "uri");
		volume = Vocabularies.createIRI(NAMESPACE, "volume");
	}
}
