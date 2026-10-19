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
 * Constants for the FRBR-aligned Bibliographic Ontology.
 *
 * @see <a href="http://purl.archive.org/spar/fabio.html">FRBR-aligned Bibliographic Ontology</a>
 *
 * @author Bart Hanssens
 */
public class FABIO {
	/**
	 * The FABIO namespace: http://purl.org/spar/fabio/
	 */
	public static final String NAMESPACE = "http://purl.org/spar/fabio/";

	/**
	 * Recommended prefix for the namespace: "fabio"
	 */
	public static final String PREFIX = "fabio";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** fabio:Abstract */
	public static final IRI Abstract;

	/** fabio:AcademicProceedings */
	public static final IRI AcademicProceedings;

	/** fabio:Addendum */
	public static final IRI Addendum;

	/** fabio:Algorithm */
	public static final IRI Algorithm;

	/** fabio:AnalogItem */
	public static final IRI AnalogItem;

	/** fabio:AnalogManifestation */
	public static final IRI AnalogManifestation;

	/** fabio:AnalogStorageMedium */
	public static final IRI AnalogStorageMedium;

	/** fabio:Announcement */
	public static final IRI Announcement;

	/** fabio:Anthology */
	public static final IRI Anthology;

	/** fabio:ApplicationProfile */
	public static final IRI ApplicationProfile;

	/** fabio:ApplicationProgrammingInterface */
	public static final IRI ApplicationProgrammingInterface;

	/** fabio:ArchivalDocument */
	public static final IRI ArchivalDocument;

	/** fabio:ArchivalDocumentSet */
	public static final IRI ArchivalDocumentSet;

	/** fabio:ArchivalRecord */
	public static final IRI ArchivalRecord;

	/** fabio:ArchivalRecordSet */
	public static final IRI ArchivalRecordSet;

	/** fabio:Article */
	public static final IRI Article;

	/** fabio:ArtisticWork */
	public static final IRI ArtisticWork;

	/** fabio:AudioDocument */
	public static final IRI AudioDocument;

	/** fabio:AuthorityFile */
	public static final IRI AuthorityFile;

	/** fabio:BachelorsThesis */
	public static final IRI BachelorsThesis;

	/** fabio:BibliographicDatabase */
	public static final IRI BibliographicDatabase;

	/** fabio:BibliographicMetadata */
	public static final IRI BibliographicMetadata;

	/** fabio:Biography */
	public static final IRI Biography;

	/** fabio:Blog */
	public static final IRI Blog;

	/** fabio:BlogPost */
	public static final IRI BlogPost;

	/** fabio:Book */
	public static final IRI Book;

	/** fabio:BookChapter */
	public static final IRI BookChapter;

	/** fabio:BookReview */
	public static final IRI BookReview;

	/** fabio:BookSeries */
	public static final IRI BookSeries;

	/** fabio:BookSet */
	public static final IRI BookSet;

	/** fabio:BriefReport */
	public static final IRI BriefReport;

	/** fabio:CallForApplications */
	public static final IRI CallForApplications;

	/** fabio:CaseForSupport */
	public static final IRI CaseForSupport;

	/** fabio:CaseForSupportDocument */
	public static final IRI CaseForSupportDocument;

	/** fabio:CaseReport */
	public static final IRI CaseReport;

	/** fabio:Catalog */
	public static final IRI Catalog;

	/** fabio:Chapter */
	public static final IRI Chapter;

	/** fabio:CitationMetadata */
	public static final IRI CitationMetadata;

	/** fabio:ClinicalCaseReport */
	public static final IRI ClinicalCaseReport;

	/** fabio:ClinicalGuideline */
	public static final IRI ClinicalGuideline;

	/** fabio:ClinicalTrialDesign */
	public static final IRI ClinicalTrialDesign;

	/** fabio:ClinicalTrialReport */
	public static final IRI ClinicalTrialReport;

	/** fabio:CollectedWorks */
	public static final IRI CollectedWorks;

	/** fabio:Comment */
	public static final IRI Comment;

	/** fabio:CompleteWorks */
	public static final IRI CompleteWorks;

	/** fabio:ComputerApplication */
	public static final IRI ComputerApplication;

	/** fabio:ComputerFile */
	public static final IRI ComputerFile;

	/** fabio:ComputerProgram */
	public static final IRI ComputerProgram;

	/** fabio:ConferencePaper */
	public static final IRI ConferencePaper;

	/** fabio:ConferencePoster */
	public static final IRI ConferencePoster;

	/** fabio:ConferenceProceedings */
	public static final IRI ConferenceProceedings;

	/** fabio:ControlledVocabulary */
	public static final IRI ControlledVocabulary;

	/** fabio:Correction */
	public static final IRI Correction;

	/** fabio:Corrigendum */
	public static final IRI Corrigendum;

	/** fabio:Cover */
	public static final IRI Cover;

	/** fabio:CriticalEdition */
	public static final IRI CriticalEdition;

	/** fabio:DataFile */
	public static final IRI DataFile;

	/** fabio:DataManagementPlan */
	public static final IRI DataManagementPlan;

	/** fabio:DataManagementPolicy */
	public static final IRI DataManagementPolicy;

	/** fabio:DataManagementPolicyDocument */
	public static final IRI DataManagementPolicyDocument;

	/** fabio:DataRepository */
	public static final IRI DataRepository;

	/** fabio:Database */
	public static final IRI Database;

	/** fabio:DatabaseManagementSystem */
	public static final IRI DatabaseManagementSystem;

	/** fabio:Dataset */
	public static final IRI Dataset;

	/** fabio:DefinitiveVersion */
	public static final IRI DefinitiveVersion;

	/** fabio:DemoPaper */
	public static final IRI DemoPaper;

	/** fabio:Diary */
	public static final IRI Diary;

	/** fabio:DigitalItem */
	public static final IRI DigitalItem;

	/** fabio:DigitalManifestation */
	public static final IRI DigitalManifestation;

	/** fabio:DigitalStorageMedium */
	public static final IRI DigitalStorageMedium;

	/** fabio:Directory */
	public static final IRI Directory;

	/** fabio:DisciplineDictionary */
	public static final IRI DisciplineDictionary;

	/** fabio:DoctoralThesis */
	public static final IRI DoctoralThesis;

	/** fabio:DocumentRepository */
	public static final IRI DocumentRepository;

	/** fabio:DustJacket */
	public static final IRI DustJacket;

	/** fabio:Editorial */
	public static final IRI Editorial;

	/** fabio:Email */
	public static final IRI Email;

	/** fabio:EntityMetadata */
	public static final IRI EntityMetadata;

	/** fabio:Entry */
	public static final IRI Entry;

	/** fabio:Erratum */
	public static final IRI Erratum;

	/** fabio:Essay */
	public static final IRI Essay;

	/** fabio:ExaminationPaper */
	public static final IRI ExaminationPaper;

	/** fabio:Excerpt */
	public static final IRI Excerpt;

	/** fabio:ExecutiveSummary */
	public static final IRI ExecutiveSummary;

	/** fabio:ExperimentalProtocol */
	public static final IRI ExperimentalProtocol;

	/** fabio:Expression */
	public static final IRI Expression;

	/** fabio:ExpressionCollection */
	public static final IRI ExpressionCollection;

	/** fabio:Figure */
	public static final IRI Figure;

	/** fabio:Film */
	public static final IRI Film;

	/** fabio:Folksonomy */
	public static final IRI Folksonomy;

	/** fabio:GanttChart */
	public static final IRI GanttChart;

	/** fabio:GrantApplication */
	public static final IRI GrantApplication;

	/** fabio:GrantApplicationDocument */
	public static final IRI GrantApplicationDocument;

	/** fabio:Hardback */
	public static final IRI Hardback;

	/** fabio:Image */
	public static final IRI Image;

	/** fabio:InBrief */
	public static final IRI InBrief;

	/** fabio:InUsePaper */
	public static final IRI InUsePaper;

	/** fabio:Index */
	public static final IRI Index;

	/** fabio:InstructionManual */
	public static final IRI InstructionManual;

	/** fabio:InstructionalWork */
	public static final IRI InstructionalWork;

	/** fabio:Item */
	public static final IRI Item;

	/** fabio:ItemCollection */
	public static final IRI ItemCollection;

	/** fabio:Journal */
	public static final IRI Journal;

	/** fabio:JournalArticle */
	public static final IRI JournalArticle;

	/** fabio:JournalEditorial */
	public static final IRI JournalEditorial;

	/** fabio:JournalIssue */
	public static final IRI JournalIssue;

	/** fabio:JournalNewsItem */
	public static final IRI JournalNewsItem;

	/** fabio:JournalVolume */
	public static final IRI JournalVolume;

	/** fabio:LaboratoryNotebook */
	public static final IRI LaboratoryNotebook;

	/** fabio:LectureNotes */
	public static final IRI LectureNotes;

	/** fabio:LegalOpinion */
	public static final IRI LegalOpinion;

	/** fabio:Letter */
	public static final IRI Letter;

	/** fabio:LibraryCatalog */
	public static final IRI LibraryCatalog;

	/** fabio:LiteraryArtisticWork */
	public static final IRI LiteraryArtisticWork;

	/** fabio:Magazine */
	public static final IRI Magazine;

	/** fabio:MagazineArticle */
	public static final IRI MagazineArticle;

	/** fabio:MagazineEditorial */
	public static final IRI MagazineEditorial;

	/** fabio:MagazineIssue */
	public static final IRI MagazineIssue;

	/** fabio:MagazineNewsItem */
	public static final IRI MagazineNewsItem;

	/** fabio:Manifestation */
	public static final IRI Manifestation;

	/** fabio:ManifestationCollection */
	public static final IRI ManifestationCollection;

	/** fabio:Manuscript */
	public static final IRI Manuscript;

	/** fabio:MastersThesis */
	public static final IRI MastersThesis;

	/** fabio:MeetingReport */
	public static final IRI MeetingReport;

	/** fabio:Metadata */
	public static final IRI Metadata;

	/** fabio:MetadataDocument */
	public static final IRI MetadataDocument;

	/** fabio:MethodsPaper */
	public static final IRI MethodsPaper;

	/** fabio:Microblog */
	public static final IRI Microblog;

	/** fabio:Micropost */
	public static final IRI Micropost;

	/** fabio:MinimalInformationStandard */
	public static final IRI MinimalInformationStandard;

	/** fabio:Model */
	public static final IRI Model;

	/** fabio:Movie */
	public static final IRI Movie;

	/** fabio:MovingImage */
	public static final IRI MovingImage;

	/** fabio:MusicalComposition */
	public static final IRI MusicalComposition;

	/** fabio:Nanopublication */
	public static final IRI Nanopublication;

	/** fabio:NewsItem */
	public static final IRI NewsItem;

	/** fabio:NewsReport */
	public static final IRI NewsReport;

	/** fabio:Newspaper */
	public static final IRI Newspaper;

	/** fabio:NewspaperArticle */
	public static final IRI NewspaperArticle;

	/** fabio:NewspaperEditorial */
	public static final IRI NewspaperEditorial;

	/** fabio:NewspaperIssue */
	public static final IRI NewspaperIssue;

	/** fabio:NewspaperNewsItem */
	public static final IRI NewspaperNewsItem;

	/** fabio:Notebook */
	public static final IRI Notebook;

	/** fabio:NotificationOfReceipt */
	public static final IRI NotificationOfReceipt;

	/** fabio:Novel */
	public static final IRI Novel;

	/** fabio:Obituary */
	public static final IRI Obituary;

	/** fabio:Ontology */
	public static final IRI Ontology;

	/** fabio:OntologyDocument */
	public static final IRI OntologyDocument;

	/** fabio:Opinion */
	public static final IRI Opinion;

	/** fabio:Oration */
	public static final IRI Oration;

	/** fabio:Page */
	public static final IRI Page;

	/** fabio:Paperback */
	public static final IRI Paperback;

	/** fabio:Patent */
	public static final IRI Patent;

	/** fabio:PatentApplication */
	public static final IRI PatentApplication;

	/** fabio:PatentApplicationDocument */
	public static final IRI PatentApplicationDocument;

	/** fabio:PatentDocument */
	public static final IRI PatentDocument;

	/** fabio:Periodical */
	public static final IRI Periodical;

	/** fabio:PeriodicalIssue */
	public static final IRI PeriodicalIssue;

	/** fabio:PeriodicalItem */
	public static final IRI PeriodicalItem;

	/** fabio:PeriodicalVolume */
	public static final IRI PeriodicalVolume;

	/** fabio:PersonalCommunication */
	public static final IRI PersonalCommunication;

	/** fabio:PhDSymposiumPaper */
	public static final IRI PhDSymposiumPaper;

	/** fabio:Play */
	public static final IRI Play;

	/** fabio:Poem */
	public static final IRI Poem;

	/** fabio:Policy */
	public static final IRI Policy;

	/** fabio:PolicyDocument */
	public static final IRI PolicyDocument;

	/** fabio:PositionPaper */
	public static final IRI PositionPaper;

	/** fabio:PosterPaper */
	public static final IRI PosterPaper;

	/** fabio:Postprint */
	public static final IRI Postprint;

	/** fabio:Preprint */
	public static final IRI Preprint;

	/** fabio:Presentation */
	public static final IRI Presentation;

	/** fabio:PressRelease */
	public static final IRI PressRelease;

	/** fabio:PrintObject */
	public static final IRI PrintObject;

	/** fabio:ProceedingsPaper */
	public static final IRI ProceedingsPaper;

	/** fabio:ProductReview */
	public static final IRI ProductReview;

	/** fabio:ProjectMetadata */
	public static final IRI ProjectMetadata;

	/** fabio:ProjectPlan */
	public static final IRI ProjectPlan;

	/** fabio:ProjectReport */
	public static final IRI ProjectReport;

	/** fabio:ProjectReportDocument */
	public static final IRI ProjectReportDocument;

	/** fabio:Proof */
	public static final IRI Proof;

	/** fabio:Proposition */
	public static final IRI Proposition;

	/** fabio:Questionnaire */
	public static final IRI Questionnaire;

	/** fabio:Quotation */
	public static final IRI Quotation;

	/** fabio:RapidCommunication */
	public static final IRI RapidCommunication;

	/** fabio:ReferenceBook */
	public static final IRI ReferenceBook;

	/** fabio:ReferenceEntry */
	public static final IRI ReferenceEntry;

	/** fabio:ReferenceWork */
	public static final IRI ReferenceWork;

	/** fabio:RelationalDatabase */
	public static final IRI RelationalDatabase;

	/** fabio:Reply */
	public static final IRI Reply;

	/** fabio:Report */
	public static final IRI Report;

	/** fabio:ReportDocument */
	public static final IRI ReportDocument;

	/** fabio:ReportingStandard */
	public static final IRI ReportingStandard;

	/** fabio:Repository */
	public static final IRI Repository;

	/** fabio:ResearchPaper */
	public static final IRI ResearchPaper;

	/** fabio:ResourcePaper */
	public static final IRI ResourcePaper;

	/** fabio:Retraction */
	public static final IRI Retraction;

	/** fabio:RetractionNotice */
	public static final IRI RetractionNotice;

	/** fabio:Review */
	public static final IRI Review;

	/** fabio:ReviewArticle */
	public static final IRI ReviewArticle;

	/** fabio:ReviewPaper */
	public static final IRI ReviewPaper;

	/** fabio:ScholarlyWork */
	public static final IRI ScholarlyWork;

	/** fabio:Screenplay */
	public static final IRI Screenplay;

	/** fabio:Script */
	public static final IRI Script;

	/** fabio:Series */
	public static final IRI Series;

	/** fabio:ShortStory */
	public static final IRI ShortStory;

	/** fabio:Song */
	public static final IRI Song;

	/** fabio:SoundRecording */
	public static final IRI SoundRecording;

	/** fabio:Specification */
	public static final IRI Specification;

	/** fabio:SpecificationDocument */
	public static final IRI SpecificationDocument;

	/** fabio:Spreadsheet */
	public static final IRI Spreadsheet;

	/** fabio:StandardOperatingProcedure */
	public static final IRI StandardOperatingProcedure;

	/** fabio:StillImage */
	public static final IRI StillImage;

	/** fabio:StorageMedium */
	public static final IRI StorageMedium;

	/** fabio:StructuredSummary */
	public static final IRI StructuredSummary;

	/** fabio:SubjectDiscipline */
	public static final IRI SubjectDiscipline;

	/** fabio:SubjectTerm */
	public static final IRI SubjectTerm;

	/** fabio:Supplement */
	public static final IRI Supplement;

	/** fabio:SupplementaryInformation */
	public static final IRI SupplementaryInformation;

	/** fabio:SystematicReview */
	public static final IRI SystematicReview;

	/** fabio:Table */
	public static final IRI Table;

	/** fabio:TableOfContents */
	public static final IRI TableOfContents;

	/** fabio:Taxonomy */
	public static final IRI Taxonomy;

	/** fabio:TechnicalReport */
	public static final IRI TechnicalReport;

	/** fabio:TechnicalStandard */
	public static final IRI TechnicalStandard;

	/** fabio:TermDictionary */
	public static final IRI TermDictionary;

	/** fabio:Textbook */
	public static final IRI Textbook;

	/** fabio:Thesaurus */
	public static final IRI Thesaurus;

	/** fabio:Thesis */
	public static final IRI Thesis;

	/** fabio:Timetable */
	public static final IRI Timetable;

	/** fabio:TrialReport */
	public static final IRI TrialReport;

	/** fabio:Triplestore */
	public static final IRI Triplestore;

	/** fabio:Tweet */
	public static final IRI Tweet;

	/** fabio:UncontrolledVocabulary */
	public static final IRI UncontrolledVocabulary;

	/** fabio:Vocabulary */
	public static final IRI Vocabulary;

	/** fabio:VocabularyDocument */
	public static final IRI VocabularyDocument;

	/** fabio:VocabularyMapping */
	public static final IRI VocabularyMapping;

	/** fabio:VocabularyMappingDocument */
	public static final IRI VocabularyMappingDocument;

	/** fabio:WebArchive */
	public static final IRI WebArchive;

	/** fabio:WebContent */
	public static final IRI WebContent;

	/** fabio:WebManifestation */
	public static final IRI WebManifestation;

	/** fabio:WebPage */
	public static final IRI WebPage;

	/** fabio:WebSite */
	public static final IRI WebSite;

	/** fabio:WhitePaper */
	public static final IRI WhitePaper;

	/** fabio:Wiki */
	public static final IRI Wiki;

	/** fabio:WikiEntry */
	public static final IRI WikiEntry;

	/** fabio:WikipediaEntry */
	public static final IRI WikipediaEntry;

	/** fabio:Work */
	public static final IRI Work;

	/** fabio:WorkCollection */
	public static final IRI WorkCollection;

	/** fabio:WorkPackage */
	public static final IRI WorkPackage;

	/** fabio:Workflow */
	public static final IRI Workflow;

	/** fabio:WorkingPaper */
	public static final IRI WorkingPaper;

	/** fabio:WorkshopPaper */
	public static final IRI WorkshopPaper;

	/** fabio:WorkshopProceedings */
	public static final IRI WorkshopProceedings;

	// Properties
	/** fabio:dateLastUpdated */
	public static final IRI dateLastUpdated;

	/** fabio:hasAccessDate */
	public static final IRI hasAccessDate;

	/** fabio:hasArXivId */
	public static final IRI hasArXivId;

	/** fabio:hasCODEN */
	public static final IRI hasCODEN;

	/** fabio:hasCharacterCount */
	public static final IRI hasCharacterCount;

	/** fabio:hasCopyrightYear */
	public static final IRI hasCopyrightYear;

	/** fabio:hasCorrectionDate */
	public static final IRI hasCorrectionDate;

	/** fabio:hasDateCollected */
	public static final IRI hasDateCollected;

	/** fabio:hasDateReceived */
	public static final IRI hasDateReceived;

	/** fabio:hasDeadline */
	public static final IRI hasDeadline;

	/** fabio:hasDecisionDate */
	public static final IRI hasDecisionDate;

	/** fabio:hasDepositDate */
	public static final IRI hasDepositDate;

	/** fabio:hasDiscipline */
	public static final IRI hasDiscipline;

	/** fabio:hasDistributionDate */
	public static final IRI hasDistributionDate;

	/** fabio:hasElectronicArticleIdentifier */
	public static final IRI hasElectronicArticleIdentifier;

	/** fabio:hasEmbargoDate */
	public static final IRI hasEmbargoDate;

	/** fabio:hasEmbargoDuration */
	public static final IRI hasEmbargoDuration;

	/** fabio:hasHandle */
	public static final IRI hasHandle;

	/** fabio:hasIssnL */
	public static final IRI hasIssnL;

	/** fabio:hasManifestation */
	public static final IRI hasManifestation;

	/** fabio:hasNLMJournalTitleAbbreviation */
	public static final IRI hasNLMJournalTitleAbbreviation;

	/** fabio:hasNationalLibraryOfMedicineJournalId */
	public static final IRI hasNationalLibraryOfMedicineJournalId;

	/** fabio:hasPII */
	public static final IRI hasPII;

	/** fabio:hasPageCount */
	public static final IRI hasPageCount;

	/** fabio:hasPatentNumber */
	public static final IRI hasPatentNumber;

	/** fabio:hasPlaceOfPublication */
	public static final IRI hasPlaceOfPublication;

	/** fabio:hasPortrayal */
	public static final IRI hasPortrayal;

	/** fabio:hasPrimarySubjectTerm */
	public static final IRI hasPrimarySubjectTerm;

	/** fabio:hasPubMedCentralId */
	public static final IRI hasPubMedCentralId;

	/** fabio:hasPubMedId */
	public static final IRI hasPubMedId;

	/** fabio:hasPublicationYear */
	public static final IRI hasPublicationYear;

	/** fabio:hasRepresentation */
	public static final IRI hasRepresentation;

	/** fabio:hasRequestDate */
	public static final IRI hasRequestDate;

	/** fabio:hasRetractionDate */
	public static final IRI hasRetractionDate;

	/** fabio:hasSICI */
	public static final IRI hasSICI;

	/** fabio:hasSeason */
	public static final IRI hasSeason;

	/** fabio:hasSequenceIdentifier */
	public static final IRI hasSequenceIdentifier;

	/** fabio:hasShortTitle */
	public static final IRI hasShortTitle;

	/** fabio:hasStandardNumber */
	public static final IRI hasStandardNumber;

	/** fabio:hasSubjectTerm */
	public static final IRI hasSubjectTerm;

	/** fabio:hasSubtitle */
	public static final IRI hasSubtitle;

	/** fabio:hasTranslatedSubtitle */
	public static final IRI hasTranslatedSubtitle;

	/** fabio:hasTranslatedTitle */
	public static final IRI hasTranslatedTitle;

	/** fabio:hasURL */
	public static final IRI hasURL;

	/** fabio:hasVolumeCount */
	public static final IRI hasVolumeCount;

	/** fabio:isDisciplineOf */
	public static final IRI isDisciplineOf;

	/** fabio:isManifestationOf */
	public static final IRI isManifestationOf;

	/** fabio:isPortrayalOf */
	public static final IRI isPortrayalOf;

	/** fabio:isRepresentationOf */
	public static final IRI isRepresentationOf;

	/** fabio:isSchemeOf */
	public static final IRI isSchemeOf;

	/** fabio:isStoredOn */
	public static final IRI isStoredOn;

	/** fabio:stores */
	public static final IRI stores;

	/** fabio:usesCalendar */
	public static final IRI usesCalendar;

	// Individuals
	/** fabio:analog-magnetic-tape */
	public static final IRI analog_magnetic_tape;

	/** fabio:cd */
	public static final IRI cd;

	/** fabio:cloud */
	public static final IRI cloud;

	/** fabio:digital-magnetic-tape */
	public static final IRI digital_magnetic_tape;

	/** fabio:dvd */
	public static final IRI dvd;

	/** fabio:film */
	public static final IRI film;

	/** fabio:floppy-disk */
	public static final IRI floppy_disk;

	/** fabio:hard-drive */
	public static final IRI hard_drive;

	/** fabio:internet */
	public static final IRI internet;

	/** fabio:intranet */
	public static final IRI intranet;

	/** fabio:paper */
	public static final IRI paper;

	/** fabio:ram */
	public static final IRI ram;

	/** fabio:solid-state-memory */
	public static final IRI solid_state_memory;

	/** fabio:vinyl-disk */
	public static final IRI vinyl_disk;

	/** fabio:web */
	public static final IRI web;

	static {
		Abstract = Vocabularies.createIRI(NAMESPACE, "Abstract");
		AcademicProceedings = Vocabularies.createIRI(NAMESPACE, "AcademicProceedings");
		Addendum = Vocabularies.createIRI(NAMESPACE, "Addendum");
		Algorithm = Vocabularies.createIRI(NAMESPACE, "Algorithm");
		AnalogItem = Vocabularies.createIRI(NAMESPACE, "AnalogItem");
		AnalogManifestation = Vocabularies.createIRI(NAMESPACE, "AnalogManifestation");
		AnalogStorageMedium = Vocabularies.createIRI(NAMESPACE, "AnalogStorageMedium");
		Announcement = Vocabularies.createIRI(NAMESPACE, "Announcement");
		Anthology = Vocabularies.createIRI(NAMESPACE, "Anthology");
		ApplicationProfile = Vocabularies.createIRI(NAMESPACE, "ApplicationProfile");
		ApplicationProgrammingInterface = Vocabularies.createIRI(NAMESPACE, "ApplicationProgrammingInterface");
		ArchivalDocument = Vocabularies.createIRI(NAMESPACE, "ArchivalDocument");
		ArchivalDocumentSet = Vocabularies.createIRI(NAMESPACE, "ArchivalDocumentSet");
		ArchivalRecord = Vocabularies.createIRI(NAMESPACE, "ArchivalRecord");
		ArchivalRecordSet = Vocabularies.createIRI(NAMESPACE, "ArchivalRecordSet");
		Article = Vocabularies.createIRI(NAMESPACE, "Article");
		ArtisticWork = Vocabularies.createIRI(NAMESPACE, "ArtisticWork");
		AudioDocument = Vocabularies.createIRI(NAMESPACE, "AudioDocument");
		AuthorityFile = Vocabularies.createIRI(NAMESPACE, "AuthorityFile");
		BachelorsThesis = Vocabularies.createIRI(NAMESPACE, "BachelorsThesis");
		BibliographicDatabase = Vocabularies.createIRI(NAMESPACE, "BibliographicDatabase");
		BibliographicMetadata = Vocabularies.createIRI(NAMESPACE, "BibliographicMetadata");
		Biography = Vocabularies.createIRI(NAMESPACE, "Biography");
		Blog = Vocabularies.createIRI(NAMESPACE, "Blog");
		BlogPost = Vocabularies.createIRI(NAMESPACE, "BlogPost");
		Book = Vocabularies.createIRI(NAMESPACE, "Book");
		BookChapter = Vocabularies.createIRI(NAMESPACE, "BookChapter");
		BookReview = Vocabularies.createIRI(NAMESPACE, "BookReview");
		BookSeries = Vocabularies.createIRI(NAMESPACE, "BookSeries");
		BookSet = Vocabularies.createIRI(NAMESPACE, "BookSet");
		BriefReport = Vocabularies.createIRI(NAMESPACE, "BriefReport");
		CallForApplications = Vocabularies.createIRI(NAMESPACE, "CallForApplications");
		CaseForSupport = Vocabularies.createIRI(NAMESPACE, "CaseForSupport");
		CaseForSupportDocument = Vocabularies.createIRI(NAMESPACE, "CaseForSupportDocument");
		CaseReport = Vocabularies.createIRI(NAMESPACE, "CaseReport");
		Catalog = Vocabularies.createIRI(NAMESPACE, "Catalog");
		Chapter = Vocabularies.createIRI(NAMESPACE, "Chapter");
		CitationMetadata = Vocabularies.createIRI(NAMESPACE, "CitationMetadata");
		ClinicalCaseReport = Vocabularies.createIRI(NAMESPACE, "ClinicalCaseReport");
		ClinicalGuideline = Vocabularies.createIRI(NAMESPACE, "ClinicalGuideline");
		ClinicalTrialDesign = Vocabularies.createIRI(NAMESPACE, "ClinicalTrialDesign");
		ClinicalTrialReport = Vocabularies.createIRI(NAMESPACE, "ClinicalTrialReport");
		CollectedWorks = Vocabularies.createIRI(NAMESPACE, "CollectedWorks");
		Comment = Vocabularies.createIRI(NAMESPACE, "Comment");
		CompleteWorks = Vocabularies.createIRI(NAMESPACE, "CompleteWorks");
		ComputerApplication = Vocabularies.createIRI(NAMESPACE, "ComputerApplication");
		ComputerFile = Vocabularies.createIRI(NAMESPACE, "ComputerFile");
		ComputerProgram = Vocabularies.createIRI(NAMESPACE, "ComputerProgram");
		ConferencePaper = Vocabularies.createIRI(NAMESPACE, "ConferencePaper");
		ConferencePoster = Vocabularies.createIRI(NAMESPACE, "ConferencePoster");
		ConferenceProceedings = Vocabularies.createIRI(NAMESPACE, "ConferenceProceedings");
		ControlledVocabulary = Vocabularies.createIRI(NAMESPACE, "ControlledVocabulary");
		Correction = Vocabularies.createIRI(NAMESPACE, "Correction");
		Corrigendum = Vocabularies.createIRI(NAMESPACE, "Corrigendum");
		Cover = Vocabularies.createIRI(NAMESPACE, "Cover");
		CriticalEdition = Vocabularies.createIRI(NAMESPACE, "CriticalEdition");
		DataFile = Vocabularies.createIRI(NAMESPACE, "DataFile");
		DataManagementPlan = Vocabularies.createIRI(NAMESPACE, "DataManagementPlan");
		DataManagementPolicy = Vocabularies.createIRI(NAMESPACE, "DataManagementPolicy");
		DataManagementPolicyDocument = Vocabularies.createIRI(NAMESPACE, "DataManagementPolicyDocument");
		DataRepository = Vocabularies.createIRI(NAMESPACE, "DataRepository");
		Database = Vocabularies.createIRI(NAMESPACE, "Database");
		DatabaseManagementSystem = Vocabularies.createIRI(NAMESPACE, "DatabaseManagementSystem");
		Dataset = Vocabularies.createIRI(NAMESPACE, "Dataset");
		DefinitiveVersion = Vocabularies.createIRI(NAMESPACE, "DefinitiveVersion");
		DemoPaper = Vocabularies.createIRI(NAMESPACE, "DemoPaper");
		Diary = Vocabularies.createIRI(NAMESPACE, "Diary");
		DigitalItem = Vocabularies.createIRI(NAMESPACE, "DigitalItem");
		DigitalManifestation = Vocabularies.createIRI(NAMESPACE, "DigitalManifestation");
		DigitalStorageMedium = Vocabularies.createIRI(NAMESPACE, "DigitalStorageMedium");
		Directory = Vocabularies.createIRI(NAMESPACE, "Directory");
		DisciplineDictionary = Vocabularies.createIRI(NAMESPACE, "DisciplineDictionary");
		DoctoralThesis = Vocabularies.createIRI(NAMESPACE, "DoctoralThesis");
		DocumentRepository = Vocabularies.createIRI(NAMESPACE, "DocumentRepository");
		DustJacket = Vocabularies.createIRI(NAMESPACE, "DustJacket");
		Editorial = Vocabularies.createIRI(NAMESPACE, "Editorial");
		Email = Vocabularies.createIRI(NAMESPACE, "Email");
		EntityMetadata = Vocabularies.createIRI(NAMESPACE, "EntityMetadata");
		Entry = Vocabularies.createIRI(NAMESPACE, "Entry");
		Erratum = Vocabularies.createIRI(NAMESPACE, "Erratum");
		Essay = Vocabularies.createIRI(NAMESPACE, "Essay");
		ExaminationPaper = Vocabularies.createIRI(NAMESPACE, "ExaminationPaper");
		Excerpt = Vocabularies.createIRI(NAMESPACE, "Excerpt");
		ExecutiveSummary = Vocabularies.createIRI(NAMESPACE, "ExecutiveSummary");
		ExperimentalProtocol = Vocabularies.createIRI(NAMESPACE, "ExperimentalProtocol");
		Expression = Vocabularies.createIRI(NAMESPACE, "Expression");
		ExpressionCollection = Vocabularies.createIRI(NAMESPACE, "ExpressionCollection");
		Figure = Vocabularies.createIRI(NAMESPACE, "Figure");
		Film = Vocabularies.createIRI(NAMESPACE, "Film");
		Folksonomy = Vocabularies.createIRI(NAMESPACE, "Folksonomy");
		GanttChart = Vocabularies.createIRI(NAMESPACE, "GanttChart");
		GrantApplication = Vocabularies.createIRI(NAMESPACE, "GrantApplication");
		GrantApplicationDocument = Vocabularies.createIRI(NAMESPACE, "GrantApplicationDocument");
		Hardback = Vocabularies.createIRI(NAMESPACE, "Hardback");
		Image = Vocabularies.createIRI(NAMESPACE, "Image");
		InBrief = Vocabularies.createIRI(NAMESPACE, "InBrief");
		InUsePaper = Vocabularies.createIRI(NAMESPACE, "InUsePaper");
		Index = Vocabularies.createIRI(NAMESPACE, "Index");
		InstructionManual = Vocabularies.createIRI(NAMESPACE, "InstructionManual");
		InstructionalWork = Vocabularies.createIRI(NAMESPACE, "InstructionalWork");
		Item = Vocabularies.createIRI(NAMESPACE, "Item");
		ItemCollection = Vocabularies.createIRI(NAMESPACE, "ItemCollection");
		Journal = Vocabularies.createIRI(NAMESPACE, "Journal");
		JournalArticle = Vocabularies.createIRI(NAMESPACE, "JournalArticle");
		JournalEditorial = Vocabularies.createIRI(NAMESPACE, "JournalEditorial");
		JournalIssue = Vocabularies.createIRI(NAMESPACE, "JournalIssue");
		JournalNewsItem = Vocabularies.createIRI(NAMESPACE, "JournalNewsItem");
		JournalVolume = Vocabularies.createIRI(NAMESPACE, "JournalVolume");
		LaboratoryNotebook = Vocabularies.createIRI(NAMESPACE, "LaboratoryNotebook");
		LectureNotes = Vocabularies.createIRI(NAMESPACE, "LectureNotes");
		LegalOpinion = Vocabularies.createIRI(NAMESPACE, "LegalOpinion");
		Letter = Vocabularies.createIRI(NAMESPACE, "Letter");
		LibraryCatalog = Vocabularies.createIRI(NAMESPACE, "LibraryCatalog");
		LiteraryArtisticWork = Vocabularies.createIRI(NAMESPACE, "LiteraryArtisticWork");
		Magazine = Vocabularies.createIRI(NAMESPACE, "Magazine");
		MagazineArticle = Vocabularies.createIRI(NAMESPACE, "MagazineArticle");
		MagazineEditorial = Vocabularies.createIRI(NAMESPACE, "MagazineEditorial");
		MagazineIssue = Vocabularies.createIRI(NAMESPACE, "MagazineIssue");
		MagazineNewsItem = Vocabularies.createIRI(NAMESPACE, "MagazineNewsItem");
		Manifestation = Vocabularies.createIRI(NAMESPACE, "Manifestation");
		ManifestationCollection = Vocabularies.createIRI(NAMESPACE, "ManifestationCollection");
		Manuscript = Vocabularies.createIRI(NAMESPACE, "Manuscript");
		MastersThesis = Vocabularies.createIRI(NAMESPACE, "MastersThesis");
		MeetingReport = Vocabularies.createIRI(NAMESPACE, "MeetingReport");
		Metadata = Vocabularies.createIRI(NAMESPACE, "Metadata");
		MetadataDocument = Vocabularies.createIRI(NAMESPACE, "MetadataDocument");
		MethodsPaper = Vocabularies.createIRI(NAMESPACE, "MethodsPaper");
		Microblog = Vocabularies.createIRI(NAMESPACE, "Microblog");
		Micropost = Vocabularies.createIRI(NAMESPACE, "Micropost");
		MinimalInformationStandard = Vocabularies.createIRI(NAMESPACE, "MinimalInformationStandard");
		Model = Vocabularies.createIRI(NAMESPACE, "Model");
		Movie = Vocabularies.createIRI(NAMESPACE, "Movie");
		MovingImage = Vocabularies.createIRI(NAMESPACE, "MovingImage");
		MusicalComposition = Vocabularies.createIRI(NAMESPACE, "MusicalComposition");
		Nanopublication = Vocabularies.createIRI(NAMESPACE, "Nanopublication");
		NewsItem = Vocabularies.createIRI(NAMESPACE, "NewsItem");
		NewsReport = Vocabularies.createIRI(NAMESPACE, "NewsReport");
		Newspaper = Vocabularies.createIRI(NAMESPACE, "Newspaper");
		NewspaperArticle = Vocabularies.createIRI(NAMESPACE, "NewspaperArticle");
		NewspaperEditorial = Vocabularies.createIRI(NAMESPACE, "NewspaperEditorial");
		NewspaperIssue = Vocabularies.createIRI(NAMESPACE, "NewspaperIssue");
		NewspaperNewsItem = Vocabularies.createIRI(NAMESPACE, "NewspaperNewsItem");
		Notebook = Vocabularies.createIRI(NAMESPACE, "Notebook");
		NotificationOfReceipt = Vocabularies.createIRI(NAMESPACE, "NotificationOfReceipt");
		Novel = Vocabularies.createIRI(NAMESPACE, "Novel");
		Obituary = Vocabularies.createIRI(NAMESPACE, "Obituary");
		Ontology = Vocabularies.createIRI(NAMESPACE, "Ontology");
		OntologyDocument = Vocabularies.createIRI(NAMESPACE, "OntologyDocument");
		Opinion = Vocabularies.createIRI(NAMESPACE, "Opinion");
		Oration = Vocabularies.createIRI(NAMESPACE, "Oration");
		Page = Vocabularies.createIRI(NAMESPACE, "Page");
		Paperback = Vocabularies.createIRI(NAMESPACE, "Paperback");
		Patent = Vocabularies.createIRI(NAMESPACE, "Patent");
		PatentApplication = Vocabularies.createIRI(NAMESPACE, "PatentApplication");
		PatentApplicationDocument = Vocabularies.createIRI(NAMESPACE, "PatentApplicationDocument");
		PatentDocument = Vocabularies.createIRI(NAMESPACE, "PatentDocument");
		Periodical = Vocabularies.createIRI(NAMESPACE, "Periodical");
		PeriodicalIssue = Vocabularies.createIRI(NAMESPACE, "PeriodicalIssue");
		PeriodicalItem = Vocabularies.createIRI(NAMESPACE, "PeriodicalItem");
		PeriodicalVolume = Vocabularies.createIRI(NAMESPACE, "PeriodicalVolume");
		PersonalCommunication = Vocabularies.createIRI(NAMESPACE, "PersonalCommunication");
		PhDSymposiumPaper = Vocabularies.createIRI(NAMESPACE, "PhDSymposiumPaper");
		Play = Vocabularies.createIRI(NAMESPACE, "Play");
		Poem = Vocabularies.createIRI(NAMESPACE, "Poem");
		Policy = Vocabularies.createIRI(NAMESPACE, "Policy");
		PolicyDocument = Vocabularies.createIRI(NAMESPACE, "PolicyDocument");
		PositionPaper = Vocabularies.createIRI(NAMESPACE, "PositionPaper");
		PosterPaper = Vocabularies.createIRI(NAMESPACE, "PosterPaper");
		Postprint = Vocabularies.createIRI(NAMESPACE, "Postprint");
		Preprint = Vocabularies.createIRI(NAMESPACE, "Preprint");
		Presentation = Vocabularies.createIRI(NAMESPACE, "Presentation");
		PressRelease = Vocabularies.createIRI(NAMESPACE, "PressRelease");
		PrintObject = Vocabularies.createIRI(NAMESPACE, "PrintObject");
		ProceedingsPaper = Vocabularies.createIRI(NAMESPACE, "ProceedingsPaper");
		ProductReview = Vocabularies.createIRI(NAMESPACE, "ProductReview");
		ProjectMetadata = Vocabularies.createIRI(NAMESPACE, "ProjectMetadata");
		ProjectPlan = Vocabularies.createIRI(NAMESPACE, "ProjectPlan");
		ProjectReport = Vocabularies.createIRI(NAMESPACE, "ProjectReport");
		ProjectReportDocument = Vocabularies.createIRI(NAMESPACE, "ProjectReportDocument");
		Proof = Vocabularies.createIRI(NAMESPACE, "Proof");
		Proposition = Vocabularies.createIRI(NAMESPACE, "Proposition");
		Questionnaire = Vocabularies.createIRI(NAMESPACE, "Questionnaire");
		Quotation = Vocabularies.createIRI(NAMESPACE, "Quotation");
		RapidCommunication = Vocabularies.createIRI(NAMESPACE, "RapidCommunication");
		ReferenceBook = Vocabularies.createIRI(NAMESPACE, "ReferenceBook");
		ReferenceEntry = Vocabularies.createIRI(NAMESPACE, "ReferenceEntry");
		ReferenceWork = Vocabularies.createIRI(NAMESPACE, "ReferenceWork");
		RelationalDatabase = Vocabularies.createIRI(NAMESPACE, "RelationalDatabase");
		Reply = Vocabularies.createIRI(NAMESPACE, "Reply");
		Report = Vocabularies.createIRI(NAMESPACE, "Report");
		ReportDocument = Vocabularies.createIRI(NAMESPACE, "ReportDocument");
		ReportingStandard = Vocabularies.createIRI(NAMESPACE, "ReportingStandard");
		Repository = Vocabularies.createIRI(NAMESPACE, "Repository");
		ResearchPaper = Vocabularies.createIRI(NAMESPACE, "ResearchPaper");
		ResourcePaper = Vocabularies.createIRI(NAMESPACE, "ResourcePaper");
		Retraction = Vocabularies.createIRI(NAMESPACE, "Retraction");
		RetractionNotice = Vocabularies.createIRI(NAMESPACE, "RetractionNotice");
		Review = Vocabularies.createIRI(NAMESPACE, "Review");
		ReviewArticle = Vocabularies.createIRI(NAMESPACE, "ReviewArticle");
		ReviewPaper = Vocabularies.createIRI(NAMESPACE, "ReviewPaper");
		ScholarlyWork = Vocabularies.createIRI(NAMESPACE, "ScholarlyWork");
		Screenplay = Vocabularies.createIRI(NAMESPACE, "Screenplay");
		Script = Vocabularies.createIRI(NAMESPACE, "Script");
		Series = Vocabularies.createIRI(NAMESPACE, "Series");
		ShortStory = Vocabularies.createIRI(NAMESPACE, "ShortStory");
		Song = Vocabularies.createIRI(NAMESPACE, "Song");
		SoundRecording = Vocabularies.createIRI(NAMESPACE, "SoundRecording");
		Specification = Vocabularies.createIRI(NAMESPACE, "Specification");
		SpecificationDocument = Vocabularies.createIRI(NAMESPACE, "SpecificationDocument");
		Spreadsheet = Vocabularies.createIRI(NAMESPACE, "Spreadsheet");
		StandardOperatingProcedure = Vocabularies.createIRI(NAMESPACE, "StandardOperatingProcedure");
		StillImage = Vocabularies.createIRI(NAMESPACE, "StillImage");
		StorageMedium = Vocabularies.createIRI(NAMESPACE, "StorageMedium");
		StructuredSummary = Vocabularies.createIRI(NAMESPACE, "StructuredSummary");
		SubjectDiscipline = Vocabularies.createIRI(NAMESPACE, "SubjectDiscipline");
		SubjectTerm = Vocabularies.createIRI(NAMESPACE, "SubjectTerm");
		Supplement = Vocabularies.createIRI(NAMESPACE, "Supplement");
		SupplementaryInformation = Vocabularies.createIRI(NAMESPACE, "SupplementaryInformation");
		SystematicReview = Vocabularies.createIRI(NAMESPACE, "SystematicReview");
		Table = Vocabularies.createIRI(NAMESPACE, "Table");
		TableOfContents = Vocabularies.createIRI(NAMESPACE, "TableOfContents");
		Taxonomy = Vocabularies.createIRI(NAMESPACE, "Taxonomy");
		TechnicalReport = Vocabularies.createIRI(NAMESPACE, "TechnicalReport");
		TechnicalStandard = Vocabularies.createIRI(NAMESPACE, "TechnicalStandard");
		TermDictionary = Vocabularies.createIRI(NAMESPACE, "TermDictionary");
		Textbook = Vocabularies.createIRI(NAMESPACE, "Textbook");
		Thesaurus = Vocabularies.createIRI(NAMESPACE, "Thesaurus");
		Thesis = Vocabularies.createIRI(NAMESPACE, "Thesis");
		Timetable = Vocabularies.createIRI(NAMESPACE, "Timetable");
		TrialReport = Vocabularies.createIRI(NAMESPACE, "TrialReport");
		Triplestore = Vocabularies.createIRI(NAMESPACE, "Triplestore");
		Tweet = Vocabularies.createIRI(NAMESPACE, "Tweet");
		UncontrolledVocabulary = Vocabularies.createIRI(NAMESPACE, "UncontrolledVocabulary");
		Vocabulary = Vocabularies.createIRI(NAMESPACE, "Vocabulary");
		VocabularyDocument = Vocabularies.createIRI(NAMESPACE, "VocabularyDocument");
		VocabularyMapping = Vocabularies.createIRI(NAMESPACE, "VocabularyMapping");
		VocabularyMappingDocument = Vocabularies.createIRI(NAMESPACE, "VocabularyMappingDocument");
		WebArchive = Vocabularies.createIRI(NAMESPACE, "WebArchive");
		WebContent = Vocabularies.createIRI(NAMESPACE, "WebContent");
		WebManifestation = Vocabularies.createIRI(NAMESPACE, "WebManifestation");
		WebPage = Vocabularies.createIRI(NAMESPACE, "WebPage");
		WebSite = Vocabularies.createIRI(NAMESPACE, "WebSite");
		WhitePaper = Vocabularies.createIRI(NAMESPACE, "WhitePaper");
		Wiki = Vocabularies.createIRI(NAMESPACE, "Wiki");
		WikiEntry = Vocabularies.createIRI(NAMESPACE, "WikiEntry");
		WikipediaEntry = Vocabularies.createIRI(NAMESPACE, "WikipediaEntry");
		Work = Vocabularies.createIRI(NAMESPACE, "Work");
		WorkCollection = Vocabularies.createIRI(NAMESPACE, "WorkCollection");
		WorkPackage = Vocabularies.createIRI(NAMESPACE, "WorkPackage");
		Workflow = Vocabularies.createIRI(NAMESPACE, "Workflow");
		WorkingPaper = Vocabularies.createIRI(NAMESPACE, "WorkingPaper");
		WorkshopPaper = Vocabularies.createIRI(NAMESPACE, "WorkshopPaper");
		WorkshopProceedings = Vocabularies.createIRI(NAMESPACE, "WorkshopProceedings");

		dateLastUpdated = Vocabularies.createIRI(NAMESPACE, "dateLastUpdated");
		hasAccessDate = Vocabularies.createIRI(NAMESPACE, "hasAccessDate");
		hasArXivId = Vocabularies.createIRI(NAMESPACE, "hasArXivId");
		hasCODEN = Vocabularies.createIRI(NAMESPACE, "hasCODEN");
		hasCharacterCount = Vocabularies.createIRI(NAMESPACE, "hasCharacterCount");
		hasCopyrightYear = Vocabularies.createIRI(NAMESPACE, "hasCopyrightYear");
		hasCorrectionDate = Vocabularies.createIRI(NAMESPACE, "hasCorrectionDate");
		hasDateCollected = Vocabularies.createIRI(NAMESPACE, "hasDateCollected");
		hasDateReceived = Vocabularies.createIRI(NAMESPACE, "hasDateReceived");
		hasDeadline = Vocabularies.createIRI(NAMESPACE, "hasDeadline");
		hasDecisionDate = Vocabularies.createIRI(NAMESPACE, "hasDecisionDate");
		hasDepositDate = Vocabularies.createIRI(NAMESPACE, "hasDepositDate");
		hasDiscipline = Vocabularies.createIRI(NAMESPACE, "hasDiscipline");
		hasDistributionDate = Vocabularies.createIRI(NAMESPACE, "hasDistributionDate");
		hasElectronicArticleIdentifier = Vocabularies.createIRI(NAMESPACE, "hasElectronicArticleIdentifier");
		hasEmbargoDate = Vocabularies.createIRI(NAMESPACE, "hasEmbargoDate");
		hasEmbargoDuration = Vocabularies.createIRI(NAMESPACE, "hasEmbargoDuration");
		hasHandle = Vocabularies.createIRI(NAMESPACE, "hasHandle");
		hasIssnL = Vocabularies.createIRI(NAMESPACE, "hasIssnL");
		hasManifestation = Vocabularies.createIRI(NAMESPACE, "hasManifestation");
		hasNLMJournalTitleAbbreviation = Vocabularies.createIRI(NAMESPACE, "hasNLMJournalTitleAbbreviation");
		hasNationalLibraryOfMedicineJournalId = Vocabularies.createIRI(NAMESPACE,
				"hasNationalLibraryOfMedicineJournalId");
		hasPII = Vocabularies.createIRI(NAMESPACE, "hasPII");
		hasPageCount = Vocabularies.createIRI(NAMESPACE, "hasPageCount");
		hasPatentNumber = Vocabularies.createIRI(NAMESPACE, "hasPatentNumber");
		hasPlaceOfPublication = Vocabularies.createIRI(NAMESPACE, "hasPlaceOfPublication");
		hasPortrayal = Vocabularies.createIRI(NAMESPACE, "hasPortrayal");
		hasPrimarySubjectTerm = Vocabularies.createIRI(NAMESPACE, "hasPrimarySubjectTerm");
		hasPubMedCentralId = Vocabularies.createIRI(NAMESPACE, "hasPubMedCentralId");
		hasPubMedId = Vocabularies.createIRI(NAMESPACE, "hasPubMedId");
		hasPublicationYear = Vocabularies.createIRI(NAMESPACE, "hasPublicationYear");
		hasRepresentation = Vocabularies.createIRI(NAMESPACE, "hasRepresentation");
		hasRequestDate = Vocabularies.createIRI(NAMESPACE, "hasRequestDate");
		hasRetractionDate = Vocabularies.createIRI(NAMESPACE, "hasRetractionDate");
		hasSICI = Vocabularies.createIRI(NAMESPACE, "hasSICI");
		hasSeason = Vocabularies.createIRI(NAMESPACE, "hasSeason");
		hasSequenceIdentifier = Vocabularies.createIRI(NAMESPACE, "hasSequenceIdentifier");
		hasShortTitle = Vocabularies.createIRI(NAMESPACE, "hasShortTitle");
		hasStandardNumber = Vocabularies.createIRI(NAMESPACE, "hasStandardNumber");
		hasSubjectTerm = Vocabularies.createIRI(NAMESPACE, "hasSubjectTerm");
		hasSubtitle = Vocabularies.createIRI(NAMESPACE, "hasSubtitle");
		hasTranslatedSubtitle = Vocabularies.createIRI(NAMESPACE, "hasTranslatedSubtitle");
		hasTranslatedTitle = Vocabularies.createIRI(NAMESPACE, "hasTranslatedTitle");
		hasURL = Vocabularies.createIRI(NAMESPACE, "hasURL");
		hasVolumeCount = Vocabularies.createIRI(NAMESPACE, "hasVolumeCount");
		isDisciplineOf = Vocabularies.createIRI(NAMESPACE, "isDisciplineOf");
		isManifestationOf = Vocabularies.createIRI(NAMESPACE, "isManifestationOf");
		isPortrayalOf = Vocabularies.createIRI(NAMESPACE, "isPortrayalOf");
		isRepresentationOf = Vocabularies.createIRI(NAMESPACE, "isRepresentationOf");
		isSchemeOf = Vocabularies.createIRI(NAMESPACE, "isSchemeOf");
		isStoredOn = Vocabularies.createIRI(NAMESPACE, "isStoredOn");
		stores = Vocabularies.createIRI(NAMESPACE, "stores");
		usesCalendar = Vocabularies.createIRI(NAMESPACE, "usesCalendar");

		analog_magnetic_tape = Vocabularies.createIRI(NAMESPACE, "analog-magnetic-tape");
		cd = Vocabularies.createIRI(NAMESPACE, "cd");
		cloud = Vocabularies.createIRI(NAMESPACE, "cloud");
		digital_magnetic_tape = Vocabularies.createIRI(NAMESPACE, "digital-magnetic-tape");
		dvd = Vocabularies.createIRI(NAMESPACE, "dvd");
		film = Vocabularies.createIRI(NAMESPACE, "film");
		floppy_disk = Vocabularies.createIRI(NAMESPACE, "floppy-disk");
		hard_drive = Vocabularies.createIRI(NAMESPACE, "hard-drive");
		internet = Vocabularies.createIRI(NAMESPACE, "internet");
		intranet = Vocabularies.createIRI(NAMESPACE, "intranet");
		paper = Vocabularies.createIRI(NAMESPACE, "paper");
		ram = Vocabularies.createIRI(NAMESPACE, "ram");
		solid_state_memory = Vocabularies.createIRI(NAMESPACE, "solid-state-memory");
		vinyl_disk = Vocabularies.createIRI(NAMESPACE, "vinyl-disk");
		web = Vocabularies.createIRI(NAMESPACE, "web");
	}
}
