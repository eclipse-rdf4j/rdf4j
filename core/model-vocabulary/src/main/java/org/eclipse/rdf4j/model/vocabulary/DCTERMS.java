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
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Vocabulary constants for the Dublin Core Metadata Initiative Metadata Terms.
 *
 * @author Peter Ansell
 * @see <a href="http://dublincore.org/documents/dcmi-terms/">DCMI Metadata Terms</a>
 */
public class DCTERMS {

	/**
	 * Dublin Core Terms namespace: http://purl.org/dc/terms/
	 */
	public static final String NAMESPACE = "http://purl.org/dc/terms/";

	/**
	 * Recommend prefix for the Dublin Core Terms namespace: "dcterms"
	 */
	public static final String PREFIX = "dcterms";

	/**
	 * An immutable {@link Namespace} constant that represents the Dublin Core Terms namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// ----------------------------------------
	// Properties common to Dublin Core Elements set
	// ----------------------------------------

	/**
	 * http://purl.org/dc/terms/contributor
	 */
	public static final IRI CONTRIBUTOR;

	/**
	 * http://purl.org/dc/terms/coverage
	 */
	public static final IRI COVERAGE;

	/**
	 * http://purl.org/dc/terms/creator
	 */
	public static final IRI CREATOR;

	/**
	 * http://purl.org/dc/terms/date
	 */
	public static final IRI DATE;

	/**
	 * http://purl.org/dc/terms/description
	 */
	public static final IRI DESCRIPTION;

	/**
	 * http://purl.org/dc/terms/format
	 */
	public static final IRI FORMAT;

	/**
	 * http://purl.org/dc/terms/identifier
	 */
	public static final IRI IDENTIFIER;

	/**
	 * http://purl.org/dc/terms/language
	 */
	public static final IRI LANGUAGE;

	/**
	 * http://purl.org/dc/terms/publisher
	 */
	public static final IRI PUBLISHER;

	/**
	 * http://purl.org/dc/terms/relation
	 */
	public static final IRI RELATION;

	/**
	 * http://purl.org/dc/terms/rights
	 */
	public static final IRI RIGHTS;

	/**
	 * http://purl.org/dc/terms/source
	 */
	public static final IRI SOURCE;

	/**
	 * http://purl.org/dc/terms/subject
	 */
	public static final IRI SUBJECT;

	/**
	 * http://purl.org/dc/terms/title
	 */
	public static final IRI TITLE;

	/**
	 * http://purl.org/dc/terms/type
	 */
	public static final IRI TYPE;

	// ----------------------------------------
	// Properties unique to Dublin Core Terms set
	// ----------------------------------------

	/**
	 * http://purl.org/dc/terms/abstract
	 */
	public static final IRI ABSTRACT;

	/**
	 * http://purl.org/dc/terms/accessRights
	 */
	public static final IRI ACCESS_RIGHTS;

	/**
	 * http://purl.org/dc/terms/accrualMethod
	 */
	public static final IRI ACCRUAL_METHOD;

	/**
	 * http://purl.org/dc/terms/accrualPeriodicity
	 */
	public static final IRI ACCRUAL_PERIODICITY;

	/**
	 * http://purl.org/dc/terms/ accrualPolicy
	 */
	public static final IRI ACCRUAL_POLICY;

	/**
	 * http://purl.org/dc/terms/alternative
	 */
	public static final IRI ALTERNATIVE;

	/**
	 * http://purl.org/dc/terms/audience
	 */
	public static final IRI AUDIENCE;

	/**
	 * http://purl.org/dc/terms/available
	 */
	public static final IRI AVAILABLE;

	/**
	 * http://purl.org/dc/terms/bibliographicCitation
	 */
	public static final IRI BIBLIOGRAPHIC_CITATION;

	/**
	 * http://purl.org/dc/terms/conformsTo
	 */
	public static final IRI CONFORMS_TO;

	/**
	 * http://purl.org/dc/terms/created
	 */
	public static final IRI CREATED;

	/**
	 * http://purl.org/dc/terms/dateAccepted
	 */
	public static final IRI DATE_ACCEPTED;

	/**
	 * http://purl.org/dc/terms/dateCopyrighted
	 */
	public static final IRI DATE_COPYRIGHTED;

	/**
	 * http://purl.org/dc/terms/dateSubmitted
	 */
	public static final IRI DATE_SUBMITTED;

	/**
	 * http://purl.org/dc/terms/educationLevel
	 */
	public static final IRI EDUCATION_LEVEL;

	/**
	 * http://purl.org/dc/terms/extent
	 */
	public static final IRI EXTENT;

	/**
	 * http://purl.org/dc/terms/hasFormat
	 */
	public static final IRI HAS_FORMAT;

	/**
	 * http://purl.org/dc/terms/hasPart
	 */
	public static final IRI HAS_PART;

	/**
	 * http://purl.org/dc/terms/hasVersion
	 */
	public static final IRI HAS_VERSION;

	/**
	 * http://purl.org/dc/terms/instructionalMethod
	 */
	public static final IRI INSTRUCTIONAL_METHOD;

	/**
	 * http://purl.org/dc/terms/isFormatOf
	 */
	public static final IRI IS_FORMAT_OF;

	/**
	 * http://purl.org/dc/terms/isPartOf
	 */
	public static final IRI IS_PART_OF;

	/**
	 * http://purl.org/dc/terms/isReferencedBy
	 */
	public static final IRI IS_REFERENCED_BY;

	/**
	 * http://purl.org/dc/terms/isReplacedBy
	 */
	public static final IRI IS_REPLACED_BY;

	/**
	 * http://purl.org/dc/terms/isRequiredBy
	 */
	public static final IRI IS_REQUIRED_BY;

	/**
	 * http://purl.org/dc/terms/issued
	 */
	public static final IRI ISSUED;

	/**
	 * http://purl.org/dc/terms/isVersionOf
	 */
	public static final IRI IS_VERSION_OF;

	/**
	 * http://purl.org/dc/terms/license
	 */
	public static final IRI LICENSE;

	/**
	 * http://purl.org/dc/terms/mediator
	 */
	public static final IRI MEDIATOR;

	/**
	 * http://purl.org/dc/terms/medium
	 */
	public static final IRI MEDIUM;

	/**
	 * http://purl.org/dc/terms/modified
	 */
	public static final IRI MODIFIED;

	/**
	 * http://purl.org/dc/terms/provenance
	 */
	public static final IRI PROVENANCE;

	/**
	 * http://purl.org/dc/terms/references
	 */
	public static final IRI REFERENCES;

	/**
	 * http://purl.org/dc/terms/replaces
	 */
	public static final IRI REPLACES;

	/**
	 * http://purl.org/dc/terms/requires
	 */
	public static final IRI REQUIRES;

	/**
	 * http://purl.org/dc/terms/rightsHolder
	 */
	public static final IRI RIGHTS_HOLDER;

	/**
	 * http://purl.org/dc/terms/spatial
	 */
	public static final IRI SPATIAL;

	/**
	 * http://purl.org/dc/terms/tableOfContents
	 */
	public static final IRI TABLE_OF_CONTENTS;

	/**
	 * http://purl.org/dc/terms/temporal
	 */
	public static final IRI TEMPORAL;

	/**
	 * http://purl.org/dc/terms/valid
	 */
	public static final IRI VALID;

	// ----------------------------------------
	// Vocabulary encoding schemes in Dublin Core Terms
	// ----------------------------------------

	/**
	 * http://purl.org/dc/terms/DCMIType
	 */
	public static final IRI DCMI_TYPE;

	/**
	 * http://purl.org/dc/terms/DDC
	 */
	public static final IRI DDC;

	/**
	 * http://purl.org/dc/terms/IMT
	 */
	public static final IRI IMT;

	/**
	 * http://purl.org/dc/terms/LCC
	 */
	public static final IRI LCC;

	/**
	 * http://purl.org/dc/terms/LCSH
	 */
	public static final IRI LCSH;

	/**
	 * http://purl.org/dc/terms/MESH
	 */
	public static final IRI MESH;

	/**
	 * http://purl.org/dc/terms/NLM
	 */
	public static final IRI NLM;

	/**
	 * http://purl.org/dc/terms/TGN
	 */
	public static final IRI TGN;

	/**
	 * http://purl.org/dc/terms/UDC
	 */
	public static final IRI UDC;

	// ----------------------------------------
	// Syntax encoding schemes in Dublin Core Terms
	// ----------------------------------------

	/**
	 * http://purl.org/dc/terms/Box
	 */
	public static final IRI BOX;

	/**
	 * http://purl.org/dc/terms/ISO3166
	 */
	public static final IRI ISO3166;

	/**
	 * http://purl.org/dc/terms/ISO639-2
	 */
	public static final IRI ISO639_2;

	/**
	 * http://purl.org/dc/terms/ISO639-3
	 */
	public static final IRI ISO639_3;

	/**
	 * http://purl.org/dc/terms/Period
	 */
	public static final IRI PERIOD;

	/**
	 * http://purl.org/dc/terms/Point
	 */
	public static final IRI POINT;

	/**
	 * http://purl.org/dc/terms/RFC1766
	 */
	public static final IRI RFC1766;

	/**
	 * http://purl.org/dc/terms/RFC3066
	 */
	public static final IRI RFC3066;

	/**
	 * http://purl.org/dc/terms/RFC4646
	 */
	public static final IRI RFC4646;

	/**
	 * http://purl.org/dc/terms/RFC5646
	 */
	public static final IRI RFC5646;

	/**
	 * http://purl.org/dc/terms/URI
	 */
	public static final IRI URI;

	/**
	 * http://purl.org/dc/terms/W3CDTF
	 */
	public static final IRI W3CDTF;

	// ----------------------------------------
	// Classes in Dublin Core Terms
	// ----------------------------------------

	/**
	 * http://purl.org/dc/terms/Agent
	 */
	public static final IRI AGENT;

	/**
	 * http://purl.org/dc/terms/AgentClass
	 */
	public static final IRI AGENT_CLASS;

	/**
	 * http://purl.org/dc/terms/BibliographicResource
	 */
	public static final IRI BIBLIOGRAPHIC_RESOURCE;

	/**
	 * http://purl.org/dc/terms/FileFormat
	 */
	public static final IRI FILE_FORMAT;

	/**
	 * http://purl.org/dc/terms/Frequency
	 */
	public static final IRI FREQUENCY;

	/**
	 * http://purl.org/dc/terms/Jurisdiction
	 */
	public static final IRI JURISDICTION;

	/**
	 * http://purl.org/dc/terms/LicenseDocument
	 */
	public static final IRI LICENSE_DOCUMENT;

	/**
	 * http://purl.org/dc/terms/LinguisticSystem
	 */
	public static final IRI LINGUISTIC_SYSTEM;

	/**
	 * http://purl.org/dc/terms/Location
	 */
	public static final IRI LOCATION;

	/**
	 * http://purl.org/dc/terms/LocationPeriodOrJurisdiction
	 */
	public static final IRI LOCATION_PERIOD_OR_JURISDICTION;

	/**
	 * http://purl.org/dc/terms/MediaType
	 */
	public static final IRI MEDIA_TYPE;

	/**
	 * http://purl.org/dc/terms/MediaTypeOrExtent
	 */
	public static final IRI MEDIA_TYPE_OR_EXTENT;

	/**
	 * http://purl.org/dc/terms/MethodOfAccrual
	 */
	public static final IRI METHOD_OF_ACCRUAL;

	/**
	 * http://purl.org/dc/terms/MethodOfInstruction
	 */
	public static final IRI METHOD_OF_INSTRUCTION;

	/**
	 * http://purl.org/dc/terms/PeriodOfTime
	 */
	public static final IRI PERIOD_OF_TIME;

	/**
	 * http://purl.org/dc/terms/PhysicalMedium
	 */
	public static final IRI PHYSICAL_MEDIUM;

	/**
	 * http://purl.org/dc/terms/PhysicalResource
	 */
	public static final IRI PHYSICAL_RESOURCE;

	/**
	 * http://purl.org/dc/terms/Policy
	 */
	public static final IRI POLICY;

	/**
	 * http://purl.org/dc/terms/ProvenanceStatement
	 */
	public static final IRI PROVENANCE_STATEMENT;

	/**
	 * http://purl.org/dc/terms/RightsStatement
	 */
	public static final IRI RIGHTS_STATEMENT;

	/**
	 * http://purl.org/dc/terms/SizeOrDuration
	 */
	public static final IRI SIZE_OR_DURATION;

	/**
	 * http://purl.org/dc/terms/Standard
	 */
	public static final IRI STANDARD;

	// Static initializer for fields

	static {

		// Properties common to Dublin Core Elements
		CONTRIBUTOR = Vocabularies.createIRI(NAMESPACE, "contributor");
		COVERAGE = Vocabularies.createIRI(NAMESPACE, "coverage");
		CREATOR = Vocabularies.createIRI(NAMESPACE, "creator");
		DATE = Vocabularies.createIRI(NAMESPACE, "date");
		DESCRIPTION = Vocabularies.createIRI(NAMESPACE, "description");
		FORMAT = Vocabularies.createIRI(NAMESPACE, "format");
		IDENTIFIER = Vocabularies.createIRI(NAMESPACE, "identifier");
		LANGUAGE = Vocabularies.createIRI(NAMESPACE, "language");
		PUBLISHER = Vocabularies.createIRI(NAMESPACE, "publisher");
		RELATION = Vocabularies.createIRI(NAMESPACE, "relation");
		RIGHTS = Vocabularies.createIRI(NAMESPACE, "rights");
		SOURCE = Vocabularies.createIRI(NAMESPACE, "source");
		SUBJECT = Vocabularies.createIRI(NAMESPACE, "subject");
		TITLE = Vocabularies.createIRI(NAMESPACE, "title");
		TYPE = Vocabularies.createIRI(NAMESPACE, "type");

		// Properties unique to Dublin Core Terms
		ABSTRACT = Vocabularies.createIRI(NAMESPACE, "abstract");
		ACCESS_RIGHTS = Vocabularies.createIRI(NAMESPACE, "accessRights");
		ACCRUAL_METHOD = Vocabularies.createIRI(NAMESPACE, "accuralMethod");
		ACCRUAL_PERIODICITY = Vocabularies.createIRI(NAMESPACE, "accrualPeriodicity");
		ACCRUAL_POLICY = Vocabularies.createIRI(NAMESPACE, "accrualPolicy");
		ALTERNATIVE = Vocabularies.createIRI(NAMESPACE, "alternative");
		AUDIENCE = Vocabularies.createIRI(NAMESPACE, "audience");
		AVAILABLE = Vocabularies.createIRI(NAMESPACE, "available");
		BIBLIOGRAPHIC_CITATION = Vocabularies.createIRI(NAMESPACE, "bibliographicCitation");
		CONFORMS_TO = Vocabularies.createIRI(NAMESPACE, "conformsTo");
		CREATED = Vocabularies.createIRI(NAMESPACE, "created");
		DATE_ACCEPTED = Vocabularies.createIRI(NAMESPACE, "dateAccepted");
		DATE_COPYRIGHTED = Vocabularies.createIRI(NAMESPACE, "dateCopyrighted");
		DATE_SUBMITTED = Vocabularies.createIRI(NAMESPACE, "dateSubmitted");
		EDUCATION_LEVEL = Vocabularies.createIRI(NAMESPACE, "educationLevel");
		EXTENT = Vocabularies.createIRI(NAMESPACE, "extent");
		HAS_FORMAT = Vocabularies.createIRI(NAMESPACE, "hasFormat");
		HAS_PART = Vocabularies.createIRI(NAMESPACE, "hasPart");
		HAS_VERSION = Vocabularies.createIRI(NAMESPACE, "hasVersion");
		INSTRUCTIONAL_METHOD = Vocabularies.createIRI(NAMESPACE, "instructionalMethod");
		IS_FORMAT_OF = Vocabularies.createIRI(NAMESPACE, "isFormatOf");
		IS_PART_OF = Vocabularies.createIRI(NAMESPACE, "isPartOf");
		IS_REFERENCED_BY = Vocabularies.createIRI(NAMESPACE, "isReferencedBy");
		IS_REPLACED_BY = Vocabularies.createIRI(NAMESPACE, "isReplacedBy");
		IS_REQUIRED_BY = Vocabularies.createIRI(NAMESPACE, "isRequiredBy");
		IS_VERSION_OF = Vocabularies.createIRI(NAMESPACE, "isVersionOf");
		ISSUED = Vocabularies.createIRI(NAMESPACE, "issued");
		LICENSE = Vocabularies.createIRI(NAMESPACE, "license");
		MEDIATOR = Vocabularies.createIRI(NAMESPACE, "mediator");
		MEDIUM = Vocabularies.createIRI(NAMESPACE, "medium");
		MODIFIED = Vocabularies.createIRI(NAMESPACE, "modified");
		PROVENANCE = Vocabularies.createIRI(NAMESPACE, "provenance");
		REFERENCES = Vocabularies.createIRI(NAMESPACE, "references");
		REPLACES = Vocabularies.createIRI(NAMESPACE, "replaces");
		REQUIRES = Vocabularies.createIRI(NAMESPACE, "requires");
		RIGHTS_HOLDER = Vocabularies.createIRI(NAMESPACE, "rightsHolder");
		SPATIAL = Vocabularies.createIRI(NAMESPACE, "spatial");
		TABLE_OF_CONTENTS = Vocabularies.createIRI(NAMESPACE, "tableOfContents");
		TEMPORAL = Vocabularies.createIRI(NAMESPACE, "temporal");
		VALID = Vocabularies.createIRI(NAMESPACE, "valid");

		// Vocabulary encoding schemes in Dublin Core Terms

		DCMI_TYPE = Vocabularies.createIRI(NAMESPACE, "DCMIType");
		DDC = Vocabularies.createIRI(NAMESPACE, "DDC");
		IMT = Vocabularies.createIRI(NAMESPACE, "IMT");
		LCC = Vocabularies.createIRI(NAMESPACE, "LCC");
		LCSH = Vocabularies.createIRI(NAMESPACE, "LCSH");
		MESH = Vocabularies.createIRI(NAMESPACE, "MESH");
		NLM = Vocabularies.createIRI(NAMESPACE, "NLM");
		TGN = Vocabularies.createIRI(NAMESPACE, "TGN");
		UDC = Vocabularies.createIRI(NAMESPACE, "UDC");

		// Syntax encoding schemes in Dublin Core Terms

		BOX = Vocabularies.createIRI(NAMESPACE, "Box");
		ISO3166 = Vocabularies.createIRI(NAMESPACE, "ISO3166");
		ISO639_2 = Vocabularies.createIRI(NAMESPACE, "ISO639-2");
		ISO639_3 = Vocabularies.createIRI(NAMESPACE, "ISO639-3");
		PERIOD = Vocabularies.createIRI(NAMESPACE, "Period");
		POINT = Vocabularies.createIRI(NAMESPACE, "Point");
		RFC1766 = Vocabularies.createIRI(NAMESPACE, "RFC1766");
		RFC3066 = Vocabularies.createIRI(NAMESPACE, "RFC3066");
		RFC4646 = Vocabularies.createIRI(NAMESPACE, "RFC4646");
		RFC5646 = Vocabularies.createIRI(NAMESPACE, "RFC5646");
		URI = Vocabularies.createIRI(NAMESPACE, "URI");
		W3CDTF = Vocabularies.createIRI(NAMESPACE, "W3CDTF");

		// Classes in Dublin Core Terms

		AGENT = Vocabularies.createIRI(NAMESPACE, "Agent");
		AGENT_CLASS = Vocabularies.createIRI(NAMESPACE, "AgentClass");
		BIBLIOGRAPHIC_RESOURCE = Vocabularies.createIRI(NAMESPACE, "BibliographicResource");
		FILE_FORMAT = Vocabularies.createIRI(NAMESPACE, "FileFormat");
		FREQUENCY = Vocabularies.createIRI(NAMESPACE, "Frequency");
		JURISDICTION = Vocabularies.createIRI(NAMESPACE, "Jurisdiction");
		LICENSE_DOCUMENT = Vocabularies.createIRI(NAMESPACE, "LicenseDocument");
		LINGUISTIC_SYSTEM = Vocabularies.createIRI(NAMESPACE, "LinguisticSystem");
		LOCATION = Vocabularies.createIRI(NAMESPACE, "Location");
		LOCATION_PERIOD_OR_JURISDICTION = Vocabularies.createIRI(NAMESPACE, "LocationPeriodOrJurisdiction");
		MEDIA_TYPE = Vocabularies.createIRI(NAMESPACE, "MediaType");
		MEDIA_TYPE_OR_EXTENT = Vocabularies.createIRI(NAMESPACE, "MediaTypeOrExtent");
		METHOD_OF_ACCRUAL = Vocabularies.createIRI(NAMESPACE, "MethodOfAccrual");
		METHOD_OF_INSTRUCTION = Vocabularies.createIRI(NAMESPACE, "MethodOfInstruction");
		PERIOD_OF_TIME = Vocabularies.createIRI(NAMESPACE, "PeriodOfTime");
		PHYSICAL_MEDIUM = Vocabularies.createIRI(NAMESPACE, "PhysicalMedium");
		PHYSICAL_RESOURCE = Vocabularies.createIRI(NAMESPACE, "PhysicalResource");
		POLICY = Vocabularies.createIRI(NAMESPACE, "Policy");
		PROVENANCE_STATEMENT = Vocabularies.createIRI(NAMESPACE, "ProvenanceStatement");
		RIGHTS_STATEMENT = Vocabularies.createIRI(NAMESPACE, "RightsStatement");
		SIZE_OR_DURATION = Vocabularies.createIRI(NAMESPACE, "SizeOrDuration");
		STANDARD = Vocabularies.createIRI(NAMESPACE, "Standard");
	}
}
