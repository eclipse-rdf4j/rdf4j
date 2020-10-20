/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import static org.eclipse.rdf4j.model.base.AbstractIRI.createIRI;
import static org.eclipse.rdf4j.model.base.AbstractNamespace.createNamespace;

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
	public static final Namespace NS = createNamespace(PREFIX, NAMESPACE);

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
		CONTRIBUTOR = createIRI(NAMESPACE, "contributor");
		COVERAGE = createIRI(NAMESPACE, "coverage");
		CREATOR = createIRI(NAMESPACE, "creator");
		DATE = createIRI(NAMESPACE, "date");
		DESCRIPTION = createIRI(NAMESPACE, "description");
		FORMAT = createIRI(NAMESPACE, "format");
		IDENTIFIER = createIRI(NAMESPACE, "identifier");
		LANGUAGE = createIRI(NAMESPACE, "language");
		PUBLISHER = createIRI(NAMESPACE, "publisher");
		RELATION = createIRI(NAMESPACE, "relation");
		RIGHTS = createIRI(NAMESPACE, "rights");
		SOURCE = createIRI(NAMESPACE, "source");
		SUBJECT = createIRI(NAMESPACE, "subject");
		TITLE = createIRI(NAMESPACE, "title");
		TYPE = createIRI(NAMESPACE, "type");

		// Properties unique to Dublin Core Terms
		ABSTRACT = createIRI(NAMESPACE, "abstract");
		ACCESS_RIGHTS = createIRI(NAMESPACE, "accessRights");
		ACCRUAL_METHOD = createIRI(NAMESPACE, "accuralMethod");
		ACCRUAL_PERIODICITY = createIRI(NAMESPACE, "accrualPeriodicity");
		ACCRUAL_POLICY = createIRI(NAMESPACE, "accrualPolicy");
		ALTERNATIVE = createIRI(NAMESPACE, "alternative");
		AUDIENCE = createIRI(NAMESPACE, "audience");
		AVAILABLE = createIRI(NAMESPACE, "available");
		BIBLIOGRAPHIC_CITATION = createIRI(NAMESPACE, "bibliographicCitation");
		CONFORMS_TO = createIRI(NAMESPACE, "conformsTo");
		CREATED = createIRI(NAMESPACE, "created");
		DATE_ACCEPTED = createIRI(NAMESPACE, "dateAccepted");
		DATE_COPYRIGHTED = createIRI(NAMESPACE, "dateCopyrighted");
		DATE_SUBMITTED = createIRI(NAMESPACE, "dateSubmitted");
		EDUCATION_LEVEL = createIRI(NAMESPACE, "educationLevel");
		EXTENT = createIRI(NAMESPACE, "extent");
		HAS_FORMAT = createIRI(NAMESPACE, "hasFormat");
		HAS_PART = createIRI(NAMESPACE, "hasPart");
		HAS_VERSION = createIRI(NAMESPACE, "hasVersion");
		INSTRUCTIONAL_METHOD = createIRI(NAMESPACE, "instructionalMethod");
		IS_FORMAT_OF = createIRI(NAMESPACE, "isFormatOf");
		IS_PART_OF = createIRI(NAMESPACE, "isPartOf");
		IS_REFERENCED_BY = createIRI(NAMESPACE, "isReferencedBy");
		IS_REPLACED_BY = createIRI(NAMESPACE, "isReplacedBy");
		IS_REQUIRED_BY = createIRI(NAMESPACE, "isRequiredBy");
		IS_VERSION_OF = createIRI(NAMESPACE, "isVersionOf");
		ISSUED = createIRI(NAMESPACE, "issued");
		LICENSE = createIRI(NAMESPACE, "license");
		MEDIATOR = createIRI(NAMESPACE, "mediator");
		MEDIUM = createIRI(NAMESPACE, "medium");
		MODIFIED = createIRI(NAMESPACE, "modified");
		PROVENANCE = createIRI(NAMESPACE, "provenance");
		REFERENCES = createIRI(NAMESPACE, "references");
		REPLACES = createIRI(NAMESPACE, "replaces");
		REQUIRES = createIRI(NAMESPACE, "requires");
		RIGHTS_HOLDER = createIRI(NAMESPACE, "rightsHolder");
		SPATIAL = createIRI(NAMESPACE, "spatial");
		TABLE_OF_CONTENTS = createIRI(NAMESPACE, "tableOfContents");
		TEMPORAL = createIRI(NAMESPACE, "temporal");
		VALID = createIRI(NAMESPACE, "valid");

		// Vocabulary encoding schemes in Dublin Core Terms

		DCMI_TYPE = createIRI(NAMESPACE, "DCMIType");
		DDC = createIRI(NAMESPACE, "DDC");
		IMT = createIRI(NAMESPACE, "IMT");
		LCC = createIRI(NAMESPACE, "LCC");
		LCSH = createIRI(NAMESPACE, "LCSH");
		MESH = createIRI(NAMESPACE, "MESH");
		NLM = createIRI(NAMESPACE, "NLM");
		TGN = createIRI(NAMESPACE, "TGN");
		UDC = createIRI(NAMESPACE, "UDC");

		// Syntax encoding schemes in Dublin Core Terms

		BOX = createIRI(NAMESPACE, "Box");
		ISO3166 = createIRI(NAMESPACE, "ISO3166");
		ISO639_2 = createIRI(NAMESPACE, "ISO639-2");
		ISO639_3 = createIRI(NAMESPACE, "ISO639-3");
		PERIOD = createIRI(NAMESPACE, "Period");
		POINT = createIRI(NAMESPACE, "Point");
		RFC1766 = createIRI(NAMESPACE, "RFC1766");
		RFC3066 = createIRI(NAMESPACE, "RFC3066");
		RFC4646 = createIRI(NAMESPACE, "RFC4646");
		RFC5646 = createIRI(NAMESPACE, "RFC5646");
		URI = createIRI(NAMESPACE, "URI");
		W3CDTF = createIRI(NAMESPACE, "W3CDTF");

		// Classes in Dublin Core Terms

		AGENT = createIRI(NAMESPACE, "Agent");
		AGENT_CLASS = createIRI(NAMESPACE, "AgentClass");
		BIBLIOGRAPHIC_RESOURCE = createIRI(NAMESPACE, "BibliographicResource");
		FILE_FORMAT = createIRI(NAMESPACE, "FileFormat");
		FREQUENCY = createIRI(NAMESPACE, "Frequency");
		JURISDICTION = createIRI(NAMESPACE, "Jurisdiction");
		LICENSE_DOCUMENT = createIRI(NAMESPACE, "LicenseDocument");
		LINGUISTIC_SYSTEM = createIRI(NAMESPACE, "LinguisticSystem");
		LOCATION = createIRI(NAMESPACE, "Location");
		LOCATION_PERIOD_OR_JURISDICTION = createIRI(NAMESPACE, "LocationPeriodOrJurisdiction");
		MEDIA_TYPE = createIRI(NAMESPACE, "MediaType");
		MEDIA_TYPE_OR_EXTENT = createIRI(NAMESPACE, "MediaTypeOrExtent");
		METHOD_OF_ACCRUAL = createIRI(NAMESPACE, "MethodOfAccrual");
		METHOD_OF_INSTRUCTION = createIRI(NAMESPACE, "MethodOfInstruction");
		PERIOD_OF_TIME = createIRI(NAMESPACE, "PeriodOfTime");
		PHYSICAL_MEDIUM = createIRI(NAMESPACE, "PhysicalMedium");
		PHYSICAL_RESOURCE = createIRI(NAMESPACE, "PhysicalResource");
		POLICY = createIRI(NAMESPACE, "Policy");
		PROVENANCE_STATEMENT = createIRI(NAMESPACE, "ProvenanceStatement");
		RIGHTS_STATEMENT = createIRI(NAMESPACE, "RightsStatement");
		SIZE_OR_DURATION = createIRI(NAMESPACE, "SizeOrDuration");
		STANDARD = createIRI(NAMESPACE, "Standard");
	}
}
