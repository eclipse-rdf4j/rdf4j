/**
 * Copyright (c) 2017 Eclipse RDF4J contributors, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the vCard Ontology.
 *
 * @see <a href="https://www.w3.org/TR/2014/NOTE-vcard-rdf-20140522/">vCard Ontology</a>
 * @see <a href="https://www.w3.org/2006/vcard/ns.ttl">vCard OWL file</a>
 *
 * @author Bart Hanssens
 */
public class VCARD4 {
	/**
	 * The VCARD4 namespace: http://www.w3.org/2006/vcard/ns#
	 */
	public static final String NAMESPACE = "http://www.w3.org/2006/vcard/ns#";

	/**
	 * Recommended prefix for the namespace: "vcard"
	 */
	public static final String PREFIX = "vcard";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** vcard:Acquaintance */
	public static final IRI ACQUAINTANCE;

	/** vcard:Address */
	public static final IRI ADDRESS;

	/** vcard:Agent */
	public static final IRI AGENT;

	/** vcard:BBS */
	@Deprecated
	public static final IRI BBS;

	/** vcard:Car */
	@Deprecated
	public static final IRI CAR;

	/** vcard:Cell */
	public static final IRI CELL;

	/** vcard:Child */
	public static final IRI CHILD;

	/** vcard:Colleague */
	public static final IRI COLLEAGUE;

	/** vcard:Contact */
	public static final IRI CONTACT;

	/** vcard:Coresident */
	public static final IRI CORESIDENT;

	/** vcard:Coworker */
	public static final IRI COWORKER;

	/** vcard:Crush */
	public static final IRI CRUSH;

	/** vcard:Date */
	public static final IRI DATE;

	/** vcard:Dom */
	@Deprecated
	public static final IRI DOM;

	/** vcard:Email */
	@Deprecated
	public static final IRI EMAIL;

	/** vcard:Emergency */
	public static final IRI EMERGENCY;

	/** vcard:Fax */
	public static final IRI FAX;

	/** vcard:Female */
	public static final IRI FEMALE;

	/** vcard:Friend */
	public static final IRI FRIEND;

	/** vcard:Gender */
	public static final IRI GENDER;

	/** vcard:Group */
	public static final IRI GROUP;

	/** vcard:Home */
	public static final IRI HOME;

	/** vcard:ISDN */
	@Deprecated
	public static final IRI ISDN;

	/** vcard:Individual */
	public static final IRI INDIVIDUAL;

	/** vcard:Internet */
	@Deprecated
	public static final IRI INTERNET;

	/** vcard:Intl */
	@Deprecated
	public static final IRI INTL;

	/** vcard:Kin */
	public static final IRI KIN;

	/** vcard:Kind */
	public static final IRI KIND;

	/** vcard:Label */
	@Deprecated
	public static final IRI LABEL;

	/** vcard:Location */
	public static final IRI LOCATION;

	/** vcard:Male */
	public static final IRI MALE;

	/** vcard:Me */
	public static final IRI ME;

	/** vcard:Met */
	public static final IRI MET;

	/** vcard:Modem */
	@Deprecated
	public static final IRI MODEM;

	/** vcard:Msg */
	@Deprecated
	public static final IRI MSG;

	/** vcard:Muse */
	public static final IRI MUSE;

	/** vcard:Name */
	public static final IRI NAME;

	/** vcard:Neighbor */
	public static final IRI NEIGHBOR;

	/** vcard:None */
	public static final IRI NONE;

	/** vcard:Organization */
	public static final IRI ORGANIZATION;

	/** vcard:Other */
	public static final IRI OTHER;

	/** vcard:PCS */
	@Deprecated
	public static final IRI PCS;

	/** vcard:Pager */
	public static final IRI PAGER;

	/** vcard:Parcel */
	@Deprecated
	public static final IRI PARCEL;

	/** vcard:Parent */
	public static final IRI PARENT;

	/** vcard:Postal */
	@Deprecated
	public static final IRI POSTAL;

	/** vcard:Pref */
	@Deprecated
	public static final IRI PREF;

	/** vcard:RelatedType */
	public static final IRI RELATED_TYPE;

	/** vcard:Sibling */
	public static final IRI SIBLING;

	/** vcard:Spouse */
	public static final IRI SPOUSE;

	/** vcard:Sweetheart */
	public static final IRI SWEETHEART;

	/** vcard:Tel */
	@Deprecated
	public static final IRI TEL;

	/** vcard:TelephoneType */
	public static final IRI TELEPHONE_TYPE;

	/** vcard:Text */
	public static final IRI TEXT;

	/** vcard:TextPhone */
	public static final IRI TEXT_PHONE;

	/** vcard:Type */
	public static final IRI TYPE;

	/** vcard:Unknown */
	public static final IRI UNKNOWN;

	/** vcard:VCard */
	public static final IRI VCARD;

	/** vcard:Video */
	public static final IRI VIDEO;

	/** vcard:Voice */
	public static final IRI VOICE;

	/** vcard:Work */
	public static final IRI WORK;

	/** vcard:X400 */
	@Deprecated
	public static final IRI X400;

	// Properties
	/** vcard:additional-name */
	public static final IRI ADDITIONAL_NAME;

	/** vcard:adr */
	public static final IRI ADR;

	/** vcard:agent */
	@Deprecated
	public static final IRI AGENT_PROP;

	/** vcard:anniversary */
	public static final IRI ANNIVERSARY;

	/** vcard:bday */
	public static final IRI BDAY;

	/** vcard:category */
	public static final IRI CATEGORY;

	/** vcard:class */
	@Deprecated
	public static final IRI CLASS;

	/** vcard:country-name */
	public static final IRI COUNTRY_NAME;

	/** vcard:email */
	public static final IRI EMAIL_PROP;

	/** vcard:extended-address */
	@Deprecated
	public static final IRI EXTENDED_ADDRESS;

	/** vcard:family-name */
	public static final IRI FAMILY_NAME;

	/** vcard:fn */
	public static final IRI FN;

	/** vcard:geo */
	public static final IRI GEO;

	/** vcard:given-name */
	public static final IRI GIVEN_NAME;

	/** vcard:hasAdditionalName */
	public static final IRI HAS_ADDITIONAL_NAME;

	/** vcard:hasAddress */
	public static final IRI HAS_ADDRESS;

	/** vcard:hasCalendarBusy */
	public static final IRI HAS_CALENDAR_BUSY;

	/** vcard:hasCalendarLink */
	public static final IRI HAS_CALENDAR_LINK;

	/** vcard:hasCalendarRequest */
	public static final IRI HAS_CALENDAR_REQUEST;

	/** vcard:hasCategory */
	public static final IRI HAS_CATEGORY;

	/** vcard:hasCountryName */
	public static final IRI HAS_COUNTRY_NAME;

	/** vcard:hasEmail */
	public static final IRI HAS_EMAIL;

	/** vcard:hasFN */
	public static final IRI HAS_FN;

	/** vcard:hasFamilyName */
	public static final IRI HAS_FAMILY_NAME;

	/** vcard:hasGender */
	public static final IRI HAS_GENDER;

	/** vcard:hasGeo */
	public static final IRI HAS_GEO;

	/** vcard:hasGivenName */
	public static final IRI HAS_GIVEN_NAME;

	/** vcard:hasHonorificPrefix */
	public static final IRI HAS_HONORIFIC_PREFIX;

	/** vcard:hasHonorificSuffix */
	public static final IRI HAS_HONORIFIC_SUFFIX;

	/** vcard:hasInstantMessage */
	public static final IRI HAS_INSTANT_MESSAGE;

	/** vcard:hasKey */
	public static final IRI HAS_KEY;

	/** vcard:hasLanguage */
	public static final IRI HAS_LANGUAGE;

	/** vcard:hasLocality */
	public static final IRI HAS_LOCALITY;

	/** vcard:hasLogo */
	public static final IRI HAS_LOGO;

	/** vcard:hasMember */
	public static final IRI HAS_MEMBER;

	/** vcard:hasName */
	public static final IRI HAS_NAME;

	/** vcard:hasNickname */
	public static final IRI HAS_NICKNAME;

	/** vcard:hasNote */
	public static final IRI HAS_NOTE;

	/** vcard:hasOrganizationName */
	public static final IRI HAS_ORGANIZATION_NAME;

	/** vcard:hasOrganizationUnit */
	public static final IRI HAS_ORGANIZATION_UNIT;

	/** vcard:hasPhoto */
	public static final IRI HAS_PHOTO;

	/** vcard:hasPostalCode */
	public static final IRI HAS_POSTAL_CODE;

	/** vcard:hasRegion */
	public static final IRI HAS_REGION;

	/** vcard:hasRelated */
	public static final IRI HAS_RELATED;

	/** vcard:hasRole */
	public static final IRI HAS_ROLE;

	/** vcard:hasSound */
	public static final IRI HAS_SOUND;

	/** vcard:hasSource */
	public static final IRI HAS_SOURCE;

	/** vcard:hasStreetAddress */
	public static final IRI HAS_STREET_ADDRESS;

	/** vcard:hasTelephone */
	public static final IRI HAS_TELEPHONE;

	/** vcard:hasTitle */
	public static final IRI HAS_TITLE;

	/** vcard:hasUID */
	public static final IRI HAS_UID;

	/** vcard:hasURL */
	public static final IRI HAS_URL;

	/** vcard:hasValue */
	public static final IRI HAS_VALUE;

	/** vcard:honorific-prefix */
	public static final IRI HONORIFIC_PREFIX;

	/** vcard:honorific-suffix */
	public static final IRI HONORIFIC_SUFFIX;

	/** vcard:key */
	public static final IRI KEY;

	/** vcard:label */
	@Deprecated
	public static final IRI LABEL_PROP;

	/** vcard:language */
	public static final IRI LANGUAGE;

	/** vcard:latitude */
	@Deprecated
	public static final IRI LATITUDE;

	/** vcard:locality */
	public static final IRI LOCALITY;

	/** vcard:logo */
	public static final IRI LOGO;

	/** vcard:longitude */
	@Deprecated
	public static final IRI LONGITUDE;

	/** vcard:mailer */
	@Deprecated
	public static final IRI MAILER;

	/** vcard:n */
	public static final IRI N;

	/** vcard:nickname */
	public static final IRI NICKNAME;

	/** vcard:note */
	public static final IRI NOTE;

	/** vcard:org */
	public static final IRI ORG;

	/** vcard:organization-name */
	public static final IRI ORGANIZATION_NAME;

	/** vcard:organization-unit */
	public static final IRI ORGANIZATION_UNIT;

	/** vcard:photo */
	public static final IRI PHOTO;

	/** vcard:post-office-box */
	@Deprecated
	public static final IRI POST_OFFICE_BOX;

	/** vcard:postal-code */
	public static final IRI POSTAL_CODE;

	/** vcard:prodid */
	public static final IRI PRODID;

	/** vcard:region */
	public static final IRI REGION;

	/** vcard:rev */
	public static final IRI REV;

	/** vcard:role */
	public static final IRI ROLE;

	/** vcard:sort-string */
	public static final IRI SORT_STRING;

	/** vcard:sound */
	public static final IRI SOUND;

	/** vcard:street-address */
	public static final IRI STREET_ADDRESS;

	/** vcard:tel */
	public static final IRI TEL_PROP;

	/** vcard:title */
	public static final IRI TITLE;

	/** vcard:tz */
	public static final IRI TZ;

	/** vcard:url */
	public static final IRI URL;

	/** vcard:value */
	public static final IRI VALUE;

	static {

		ACQUAINTANCE = Vocabularies.createIRI(NAMESPACE, "Acquaintance");
		ADDRESS = Vocabularies.createIRI(NAMESPACE, "Address");
		AGENT = Vocabularies.createIRI(NAMESPACE, "Agent");
		BBS = Vocabularies.createIRI(NAMESPACE, "BBS");
		CAR = Vocabularies.createIRI(NAMESPACE, "Car");
		CELL = Vocabularies.createIRI(NAMESPACE, "Cell");
		CHILD = Vocabularies.createIRI(NAMESPACE, "Child");
		COLLEAGUE = Vocabularies.createIRI(NAMESPACE, "Colleague");
		CONTACT = Vocabularies.createIRI(NAMESPACE, "Contact");
		CORESIDENT = Vocabularies.createIRI(NAMESPACE, "Coresident");
		COWORKER = Vocabularies.createIRI(NAMESPACE, "Coworker");
		CRUSH = Vocabularies.createIRI(NAMESPACE, "Crush");
		DATE = Vocabularies.createIRI(NAMESPACE, "Date");
		DOM = Vocabularies.createIRI(NAMESPACE, "Dom");
		EMAIL = Vocabularies.createIRI(NAMESPACE, "Email");
		EMERGENCY = Vocabularies.createIRI(NAMESPACE, "Emergency");
		FAX = Vocabularies.createIRI(NAMESPACE, "Fax");
		FEMALE = Vocabularies.createIRI(NAMESPACE, "Female");
		FRIEND = Vocabularies.createIRI(NAMESPACE, "Friend");
		GENDER = Vocabularies.createIRI(NAMESPACE, "Gender");
		GROUP = Vocabularies.createIRI(NAMESPACE, "Group");
		HOME = Vocabularies.createIRI(NAMESPACE, "Home");
		ISDN = Vocabularies.createIRI(NAMESPACE, "ISDN");
		INDIVIDUAL = Vocabularies.createIRI(NAMESPACE, "Individual");
		INTERNET = Vocabularies.createIRI(NAMESPACE, "Internet");
		INTL = Vocabularies.createIRI(NAMESPACE, "Intl");
		KIN = Vocabularies.createIRI(NAMESPACE, "Kin");
		KIND = Vocabularies.createIRI(NAMESPACE, "Kind");
		LABEL = Vocabularies.createIRI(NAMESPACE, "Label");
		LOCATION = Vocabularies.createIRI(NAMESPACE, "Location");
		MALE = Vocabularies.createIRI(NAMESPACE, "Male");
		ME = Vocabularies.createIRI(NAMESPACE, "Me");
		MET = Vocabularies.createIRI(NAMESPACE, "Met");
		MODEM = Vocabularies.createIRI(NAMESPACE, "Modem");
		MSG = Vocabularies.createIRI(NAMESPACE, "Msg");
		MUSE = Vocabularies.createIRI(NAMESPACE, "Muse");
		NAME = Vocabularies.createIRI(NAMESPACE, "Name");
		NEIGHBOR = Vocabularies.createIRI(NAMESPACE, "Neighbor");
		NONE = Vocabularies.createIRI(NAMESPACE, "None");
		ORGANIZATION = Vocabularies.createIRI(NAMESPACE, "Organization");
		OTHER = Vocabularies.createIRI(NAMESPACE, "Other");
		PCS = Vocabularies.createIRI(NAMESPACE, "PCS");
		PAGER = Vocabularies.createIRI(NAMESPACE, "Pager");
		PARCEL = Vocabularies.createIRI(NAMESPACE, "Parcel");
		PARENT = Vocabularies.createIRI(NAMESPACE, "Parent");
		POSTAL = Vocabularies.createIRI(NAMESPACE, "Postal");
		PREF = Vocabularies.createIRI(NAMESPACE, "Pref");
		RELATED_TYPE = Vocabularies.createIRI(NAMESPACE, "RelatedType");
		SIBLING = Vocabularies.createIRI(NAMESPACE, "Sibling");
		SPOUSE = Vocabularies.createIRI(NAMESPACE, "Spouse");
		SWEETHEART = Vocabularies.createIRI(NAMESPACE, "Sweetheart");
		TEL = Vocabularies.createIRI(NAMESPACE, "Tel");
		TELEPHONE_TYPE = Vocabularies.createIRI(NAMESPACE, "TelephoneType");
		TEXT = Vocabularies.createIRI(NAMESPACE, "Text");
		TEXT_PHONE = Vocabularies.createIRI(NAMESPACE, "TextPhone");
		TYPE = Vocabularies.createIRI(NAMESPACE, "Type");
		UNKNOWN = Vocabularies.createIRI(NAMESPACE, "Unknown");
		VCARD = Vocabularies.createIRI(NAMESPACE, "VCard");
		VIDEO = Vocabularies.createIRI(NAMESPACE, "Video");
		VOICE = Vocabularies.createIRI(NAMESPACE, "Voice");
		WORK = Vocabularies.createIRI(NAMESPACE, "Work");
		X400 = Vocabularies.createIRI(NAMESPACE, "X400");

		ADDITIONAL_NAME = Vocabularies.createIRI(NAMESPACE, "additional-name");
		ADR = Vocabularies.createIRI(NAMESPACE, "adr");
		AGENT_PROP = Vocabularies.createIRI(NAMESPACE, "agent");
		ANNIVERSARY = Vocabularies.createIRI(NAMESPACE, "anniversary");
		BDAY = Vocabularies.createIRI(NAMESPACE, "bday");
		CATEGORY = Vocabularies.createIRI(NAMESPACE, "category");
		CLASS = Vocabularies.createIRI(NAMESPACE, "class");
		COUNTRY_NAME = Vocabularies.createIRI(NAMESPACE, "country-name");
		EMAIL_PROP = Vocabularies.createIRI(NAMESPACE, "email");
		EXTENDED_ADDRESS = Vocabularies.createIRI(NAMESPACE, "extended-address");
		FAMILY_NAME = Vocabularies.createIRI(NAMESPACE, "family-name");
		FN = Vocabularies.createIRI(NAMESPACE, "fn");
		GEO = Vocabularies.createIRI(NAMESPACE, "geo");
		GIVEN_NAME = Vocabularies.createIRI(NAMESPACE, "given-name");
		HAS_ADDITIONAL_NAME = Vocabularies.createIRI(NAMESPACE, "hasAdditionalName");
		HAS_ADDRESS = Vocabularies.createIRI(NAMESPACE, "hasAddress");
		HAS_CALENDAR_BUSY = Vocabularies.createIRI(NAMESPACE, "hasCalendarBusy");
		HAS_CALENDAR_LINK = Vocabularies.createIRI(NAMESPACE, "hasCalendarLink");
		HAS_CALENDAR_REQUEST = Vocabularies.createIRI(NAMESPACE, "hasCalendarRequest");
		HAS_CATEGORY = Vocabularies.createIRI(NAMESPACE, "hasCategory");
		HAS_COUNTRY_NAME = Vocabularies.createIRI(NAMESPACE, "hasCountryName");
		HAS_EMAIL = Vocabularies.createIRI(NAMESPACE, "hasEmail");
		HAS_FN = Vocabularies.createIRI(NAMESPACE, "hasFN");
		HAS_FAMILY_NAME = Vocabularies.createIRI(NAMESPACE, "hasFamilyName");
		HAS_GENDER = Vocabularies.createIRI(NAMESPACE, "hasGender");
		HAS_GEO = Vocabularies.createIRI(NAMESPACE, "hasGeo");
		HAS_GIVEN_NAME = Vocabularies.createIRI(NAMESPACE, "hasGivenName");
		HAS_HONORIFIC_PREFIX = Vocabularies.createIRI(NAMESPACE, "hasHonorificPrefix");
		HAS_HONORIFIC_SUFFIX = Vocabularies.createIRI(NAMESPACE, "hasHonorificSuffix");
		HAS_INSTANT_MESSAGE = Vocabularies.createIRI(NAMESPACE, "hasInstantMessage");
		HAS_KEY = Vocabularies.createIRI(NAMESPACE, "hasKey");
		HAS_LANGUAGE = Vocabularies.createIRI(NAMESPACE, "hasLanguage");
		HAS_LOCALITY = Vocabularies.createIRI(NAMESPACE, "hasLocality");
		HAS_LOGO = Vocabularies.createIRI(NAMESPACE, "hasLogo");
		HAS_MEMBER = Vocabularies.createIRI(NAMESPACE, "hasMember");
		HAS_NAME = Vocabularies.createIRI(NAMESPACE, "hasName");
		HAS_NICKNAME = Vocabularies.createIRI(NAMESPACE, "hasNickname");
		HAS_NOTE = Vocabularies.createIRI(NAMESPACE, "hasNote");
		HAS_ORGANIZATION_NAME = Vocabularies.createIRI(NAMESPACE, "hasOrganizationName");
		HAS_ORGANIZATION_UNIT = Vocabularies.createIRI(NAMESPACE, "hasOrganizationUnit");
		HAS_PHOTO = Vocabularies.createIRI(NAMESPACE, "hasPhoto");
		HAS_POSTAL_CODE = Vocabularies.createIRI(NAMESPACE, "hasPostalCode");
		HAS_REGION = Vocabularies.createIRI(NAMESPACE, "hasRegion");
		HAS_RELATED = Vocabularies.createIRI(NAMESPACE, "hasRelated");
		HAS_ROLE = Vocabularies.createIRI(NAMESPACE, "hasRole");
		HAS_SOUND = Vocabularies.createIRI(NAMESPACE, "hasSound");
		HAS_SOURCE = Vocabularies.createIRI(NAMESPACE, "hasSource");
		HAS_STREET_ADDRESS = Vocabularies.createIRI(NAMESPACE, "hasStreetAddress");
		HAS_TELEPHONE = Vocabularies.createIRI(NAMESPACE, "hasTelephone");
		HAS_TITLE = Vocabularies.createIRI(NAMESPACE, "hasTitle");
		HAS_UID = Vocabularies.createIRI(NAMESPACE, "hasUID");
		HAS_URL = Vocabularies.createIRI(NAMESPACE, "hasURL");
		HAS_VALUE = Vocabularies.createIRI(NAMESPACE, "hasValue");
		HONORIFIC_PREFIX = Vocabularies.createIRI(NAMESPACE, "honorific-prefix");
		HONORIFIC_SUFFIX = Vocabularies.createIRI(NAMESPACE, "honorific-suffix");
		KEY = Vocabularies.createIRI(NAMESPACE, "key");
		LABEL_PROP = Vocabularies.createIRI(NAMESPACE, "label");
		LANGUAGE = Vocabularies.createIRI(NAMESPACE, "language");
		LATITUDE = Vocabularies.createIRI(NAMESPACE, "latitude");
		LOCALITY = Vocabularies.createIRI(NAMESPACE, "locality");
		LOGO = Vocabularies.createIRI(NAMESPACE, "logo");
		LONGITUDE = Vocabularies.createIRI(NAMESPACE, "longitude");
		MAILER = Vocabularies.createIRI(NAMESPACE, "mailer");
		N = Vocabularies.createIRI(NAMESPACE, "n");
		NICKNAME = Vocabularies.createIRI(NAMESPACE, "nickname");
		NOTE = Vocabularies.createIRI(NAMESPACE, "note");
		ORG = Vocabularies.createIRI(NAMESPACE, "org");
		ORGANIZATION_NAME = Vocabularies.createIRI(NAMESPACE, "organization-name");
		ORGANIZATION_UNIT = Vocabularies.createIRI(NAMESPACE, "organization-unit");
		PHOTO = Vocabularies.createIRI(NAMESPACE, "photo");
		POST_OFFICE_BOX = Vocabularies.createIRI(NAMESPACE, "post-office-box");
		POSTAL_CODE = Vocabularies.createIRI(NAMESPACE, "postal-code");
		PRODID = Vocabularies.createIRI(NAMESPACE, "prodid");
		REGION = Vocabularies.createIRI(NAMESPACE, "region");
		REV = Vocabularies.createIRI(NAMESPACE, "rev");
		ROLE = Vocabularies.createIRI(NAMESPACE, "role");
		SORT_STRING = Vocabularies.createIRI(NAMESPACE, "sort-string");
		SOUND = Vocabularies.createIRI(NAMESPACE, "sound");
		STREET_ADDRESS = Vocabularies.createIRI(NAMESPACE, "street-address");
		TEL_PROP = Vocabularies.createIRI(NAMESPACE, "tel");
		TITLE = Vocabularies.createIRI(NAMESPACE, "title");
		TZ = Vocabularies.createIRI(NAMESPACE, "tz");
		URL = Vocabularies.createIRI(NAMESPACE, "url");
		VALUE = Vocabularies.createIRI(NAMESPACE, "value");
	}
}
