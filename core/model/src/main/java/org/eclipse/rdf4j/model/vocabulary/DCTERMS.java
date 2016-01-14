/* 
 * Licensed to Aduna under one or more contributor license agreements.  
 * See the NOTICE.txt file distributed with this work for additional 
 * information regarding copyright ownership. 
 *
 * Aduna licenses this file to you under the terms of the Aduna BSD 
 * License (the "License"); you may not use this file except in compliance 
 * with the License. See the LICENSE.txt file distributed with this work 
 * for the full License.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Vocabulary constants for the Dublin Core Metadata Initiative Metadata Terms.
 * 
 * @see <a href="http://dublincore.org/documents/dcmi-terms/">DCMI Metadata
 *      Terms</a>
 * @author Peter Ansell
 * @since 2.7.0
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
	 * An immutable {@link Namespace} constant that represents the Dublin Core
	 * Terms namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

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
		final ValueFactory f = SimpleValueFactory.getInstance();

		// Properties common to Dublin Core Elements
		CONTRIBUTOR = f.createIRI(NAMESPACE, "contributor");
		COVERAGE = f.createIRI(NAMESPACE, "coverage");
		CREATOR = f.createIRI(NAMESPACE, "creator");
		DATE = f.createIRI(NAMESPACE, "date");
		DESCRIPTION = f.createIRI(NAMESPACE, "description");
		FORMAT = f.createIRI(NAMESPACE, "format");
		IDENTIFIER = f.createIRI(NAMESPACE, "identifier");
		LANGUAGE = f.createIRI(NAMESPACE, "language");
		PUBLISHER = f.createIRI(NAMESPACE, "publisher");
		RELATION = f.createIRI(NAMESPACE, "relation");
		RIGHTS = f.createIRI(NAMESPACE, "rights");
		SOURCE = f.createIRI(NAMESPACE, "source");
		SUBJECT = f.createIRI(NAMESPACE, "subject");
		TITLE = f.createIRI(NAMESPACE, "title");
		TYPE = f.createIRI(NAMESPACE, "type");

		// Properties unique to Dublin Core Terms
		ABSTRACT = f.createIRI(NAMESPACE, "abstract");
		ACCESS_RIGHTS = f.createIRI(NAMESPACE, "accessRights");
		ACCRUAL_METHOD = f.createIRI(NAMESPACE, "accuralMethod");
		ACCRUAL_PERIODICITY = f.createIRI(NAMESPACE, "accrualPeriodicity");
		ACCRUAL_POLICY = f.createIRI(NAMESPACE, "accrualPolicy");
		ALTERNATIVE = f.createIRI(NAMESPACE, "alternative");
		AUDIENCE = f.createIRI(NAMESPACE, "audience");
		AVAILABLE = f.createIRI(NAMESPACE, "available");
		BIBLIOGRAPHIC_CITATION = f.createIRI(NAMESPACE, "bibliographicCitation");
		CONFORMS_TO = f.createIRI(NAMESPACE, "conformsTo");
		CREATED = f.createIRI(NAMESPACE, "created");
		DATE_ACCEPTED = f.createIRI(NAMESPACE, "dateAccepted");
		DATE_COPYRIGHTED = f.createIRI(NAMESPACE, "dateCopyrighted");
		DATE_SUBMITTED = f.createIRI(NAMESPACE, "dateSubmitted");
		EDUCATION_LEVEL = f.createIRI(NAMESPACE, "educationLevel");
		EXTENT = f.createIRI(NAMESPACE, "extent");
		HAS_FORMAT = f.createIRI(NAMESPACE, "hasFormat");
		HAS_PART = f.createIRI(NAMESPACE, "hasPart");
		HAS_VERSION = f.createIRI(NAMESPACE, "hasVersion");
		INSTRUCTIONAL_METHOD = f.createIRI(NAMESPACE, "instructionalMethod");
		IS_FORMAT_OF = f.createIRI(NAMESPACE, "isFormatOf");
		IS_PART_OF = f.createIRI(NAMESPACE, "isPartOf");
		IS_REFERENCED_BY = f.createIRI(NAMESPACE, "isReferencedBy");
		IS_REPLACED_BY = f.createIRI(NAMESPACE, "isReplacedBy");
		IS_REQUIRED_BY = f.createIRI(NAMESPACE, "isRequiredBy");
		IS_VERSION_OF = f.createIRI(NAMESPACE, "isVersionOf");
		ISSUED = f.createIRI(NAMESPACE, "issued");
		LICENSE = f.createIRI(NAMESPACE, "license");
		MEDIATOR = f.createIRI(NAMESPACE, "mediator");
		MEDIUM = f.createIRI(NAMESPACE, "medium");
		MODIFIED = f.createIRI(NAMESPACE, "modified");
		PROVENANCE = f.createIRI(NAMESPACE, "provenance");
		REFERENCES = f.createIRI(NAMESPACE, "references");
		REPLACES = f.createIRI(NAMESPACE, "replaces");
		REQUIRES = f.createIRI(NAMESPACE, "requires");
		RIGHTS_HOLDER = f.createIRI(NAMESPACE, "rightsHolder");
		SPATIAL = f.createIRI(NAMESPACE, "spatial");
		TABLE_OF_CONTENTS = f.createIRI(NAMESPACE, "tableOfContents");
		TEMPORAL = f.createIRI(NAMESPACE, "temporal");
		VALID = f.createIRI(NAMESPACE, "valid");

		// Vocabulary encoding schemes in Dublin Core Terms

		DCMI_TYPE = f.createIRI(NAMESPACE, "DCMIType");
		DDC = f.createIRI(NAMESPACE, "DDC");
		IMT = f.createIRI(NAMESPACE, "IMT");
		LCC = f.createIRI(NAMESPACE, "LCC");
		LCSH = f.createIRI(NAMESPACE, "LCSH");
		MESH = f.createIRI(NAMESPACE, "MESH");
		NLM = f.createIRI(NAMESPACE, "NLM");
		TGN = f.createIRI(NAMESPACE, "TGN");
		UDC = f.createIRI(NAMESPACE, "UDC");

		// Syntax encoding schemes in Dublin Core Terms

		BOX = f.createIRI(NAMESPACE, "Box");
		ISO3166 = f.createIRI(NAMESPACE, "ISO3166");
		ISO639_2 = f.createIRI(NAMESPACE, "ISO639-2");
		ISO639_3 = f.createIRI(NAMESPACE, "ISO639-3");
		PERIOD = f.createIRI(NAMESPACE, "Period");
		POINT = f.createIRI(NAMESPACE, "Point");
		RFC1766 = f.createIRI(NAMESPACE, "RFC1766");
		RFC3066 = f.createIRI(NAMESPACE, "RFC3066");
		RFC4646 = f.createIRI(NAMESPACE, "RFC4646");
		RFC5646 = f.createIRI(NAMESPACE, "RFC5646");
		URI = f.createIRI(NAMESPACE, "URI");
		W3CDTF = f.createIRI(NAMESPACE, "W3CDTF");

		// Classes in Dublin Core Terms

		AGENT = f.createIRI(NAMESPACE, "Agent");
		AGENT_CLASS = f.createIRI(NAMESPACE, "AgentClass");
		BIBLIOGRAPHIC_RESOURCE = f.createIRI(NAMESPACE, "BibliographicResource");
		FILE_FORMAT = f.createIRI(NAMESPACE, "FileFormat");
		FREQUENCY = f.createIRI(NAMESPACE, "Frequency");
		JURISDICTION = f.createIRI(NAMESPACE, "Jurisdiction");
		LICENSE_DOCUMENT = f.createIRI(NAMESPACE, "LicenseDocument");
		LINGUISTIC_SYSTEM = f.createIRI(NAMESPACE, "LinguisticSystem");
		LOCATION = f.createIRI(NAMESPACE, "Location");
		LOCATION_PERIOD_OR_JURISDICTION = f.createIRI(NAMESPACE, "LocationPeriodOrJurisdiction");
		MEDIA_TYPE = f.createIRI(NAMESPACE, "MediaType");
		MEDIA_TYPE_OR_EXTENT = f.createIRI(NAMESPACE, "MediaTypeOrExtent");
		METHOD_OF_ACCRUAL = f.createIRI(NAMESPACE, "MethodOfAccrual");
		METHOD_OF_INSTRUCTION = f.createIRI(NAMESPACE, "MethodOfInstruction");
		PERIOD_OF_TIME = f.createIRI(NAMESPACE, "PeriodOfTime");
		PHYSICAL_MEDIUM = f.createIRI(NAMESPACE, "PhysicalMedium");
		PHYSICAL_RESOURCE = f.createIRI(NAMESPACE, "PhysicalResource");
		POLICY = f.createIRI(NAMESPACE, "Policy");
		PROVENANCE_STATEMENT = f.createIRI(NAMESPACE, "ProvenanceStatement");
		RIGHTS_STATEMENT = f.createIRI(NAMESPACE, "RightsStatement");
		SIZE_OR_DURATION = f.createIRI(NAMESPACE, "SizeOrDuration");
		STANDARD = f.createIRI(NAMESPACE, "Standard");
	}
}
