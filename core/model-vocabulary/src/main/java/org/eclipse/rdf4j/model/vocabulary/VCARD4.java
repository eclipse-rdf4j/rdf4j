/**
 * Copyright (c) 2017 Eclipse RDF4J contributors, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.model.vocabulary;

import static org.eclipse.rdf4j.model.base.AbstractIRI.createIRI;
import static org.eclipse.rdf4j.model.base.AbstractNamespace.createNamespace;

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
	public static final Namespace NS = createNamespace(PREFIX, NAMESPACE);

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

		ACQUAINTANCE = createIRI(NAMESPACE, "Acquaintance");
		ADDRESS = createIRI(NAMESPACE, "Address");
		AGENT = createIRI(NAMESPACE, "Agent");
		BBS = createIRI(NAMESPACE, "BBS");
		CAR = createIRI(NAMESPACE, "Car");
		CELL = createIRI(NAMESPACE, "Cell");
		CHILD = createIRI(NAMESPACE, "Child");
		COLLEAGUE = createIRI(NAMESPACE, "Colleague");
		CONTACT = createIRI(NAMESPACE, "Contact");
		CORESIDENT = createIRI(NAMESPACE, "Coresident");
		COWORKER = createIRI(NAMESPACE, "Coworker");
		CRUSH = createIRI(NAMESPACE, "Crush");
		DATE = createIRI(NAMESPACE, "Date");
		DOM = createIRI(NAMESPACE, "Dom");
		EMAIL = createIRI(NAMESPACE, "Email");
		EMERGENCY = createIRI(NAMESPACE, "Emergency");
		FAX = createIRI(NAMESPACE, "Fax");
		FEMALE = createIRI(NAMESPACE, "Female");
		FRIEND = createIRI(NAMESPACE, "Friend");
		GENDER = createIRI(NAMESPACE, "Gender");
		GROUP = createIRI(NAMESPACE, "Group");
		HOME = createIRI(NAMESPACE, "Home");
		ISDN = createIRI(NAMESPACE, "ISDN");
		INDIVIDUAL = createIRI(NAMESPACE, "Individual");
		INTERNET = createIRI(NAMESPACE, "Internet");
		INTL = createIRI(NAMESPACE, "Intl");
		KIN = createIRI(NAMESPACE, "Kin");
		KIND = createIRI(NAMESPACE, "Kind");
		LABEL = createIRI(NAMESPACE, "Label");
		LOCATION = createIRI(NAMESPACE, "Location");
		MALE = createIRI(NAMESPACE, "Male");
		ME = createIRI(NAMESPACE, "Me");
		MET = createIRI(NAMESPACE, "Met");
		MODEM = createIRI(NAMESPACE, "Modem");
		MSG = createIRI(NAMESPACE, "Msg");
		MUSE = createIRI(NAMESPACE, "Muse");
		NAME = createIRI(NAMESPACE, "Name");
		NEIGHBOR = createIRI(NAMESPACE, "Neighbor");
		NONE = createIRI(NAMESPACE, "None");
		ORGANIZATION = createIRI(NAMESPACE, "Organization");
		OTHER = createIRI(NAMESPACE, "Other");
		PCS = createIRI(NAMESPACE, "PCS");
		PAGER = createIRI(NAMESPACE, "Pager");
		PARCEL = createIRI(NAMESPACE, "Parcel");
		PARENT = createIRI(NAMESPACE, "Parent");
		POSTAL = createIRI(NAMESPACE, "Postal");
		PREF = createIRI(NAMESPACE, "Pref");
		RELATED_TYPE = createIRI(NAMESPACE, "RelatedType");
		SIBLING = createIRI(NAMESPACE, "Sibling");
		SPOUSE = createIRI(NAMESPACE, "Spouse");
		SWEETHEART = createIRI(NAMESPACE, "Sweetheart");
		TEL = createIRI(NAMESPACE, "Tel");
		TELEPHONE_TYPE = createIRI(NAMESPACE, "TelephoneType");
		TEXT = createIRI(NAMESPACE, "Text");
		TEXT_PHONE = createIRI(NAMESPACE, "TextPhone");
		TYPE = createIRI(NAMESPACE, "Type");
		UNKNOWN = createIRI(NAMESPACE, "Unknown");
		VCARD = createIRI(NAMESPACE, "VCard");
		VIDEO = createIRI(NAMESPACE, "Video");
		VOICE = createIRI(NAMESPACE, "Voice");
		WORK = createIRI(NAMESPACE, "Work");
		X400 = createIRI(NAMESPACE, "X400");

		ADDITIONAL_NAME = createIRI(NAMESPACE, "additional-name");
		ADR = createIRI(NAMESPACE, "adr");
		AGENT_PROP = createIRI(NAMESPACE, "agent");
		ANNIVERSARY = createIRI(NAMESPACE, "anniversary");
		BDAY = createIRI(NAMESPACE, "bday");
		CATEGORY = createIRI(NAMESPACE, "category");
		CLASS = createIRI(NAMESPACE, "class");
		COUNTRY_NAME = createIRI(NAMESPACE, "country-name");
		EMAIL_PROP = createIRI(NAMESPACE, "email");
		EXTENDED_ADDRESS = createIRI(NAMESPACE, "extended-address");
		FAMILY_NAME = createIRI(NAMESPACE, "family-name");
		FN = createIRI(NAMESPACE, "fn");
		GEO = createIRI(NAMESPACE, "geo");
		GIVEN_NAME = createIRI(NAMESPACE, "given-name");
		HAS_ADDITIONAL_NAME = createIRI(NAMESPACE, "hasAdditionalName");
		HAS_ADDRESS = createIRI(NAMESPACE, "hasAddress");
		HAS_CALENDAR_BUSY = createIRI(NAMESPACE, "hasCalendarBusy");
		HAS_CALENDAR_LINK = createIRI(NAMESPACE, "hasCalendarLink");
		HAS_CALENDAR_REQUEST = createIRI(NAMESPACE, "hasCalendarRequest");
		HAS_CATEGORY = createIRI(NAMESPACE, "hasCategory");
		HAS_COUNTRY_NAME = createIRI(NAMESPACE, "hasCountryName");
		HAS_EMAIL = createIRI(NAMESPACE, "hasEmail");
		HAS_FN = createIRI(NAMESPACE, "hasFN");
		HAS_FAMILY_NAME = createIRI(NAMESPACE, "hasFamilyName");
		HAS_GENDER = createIRI(NAMESPACE, "hasGender");
		HAS_GEO = createIRI(NAMESPACE, "hasGeo");
		HAS_GIVEN_NAME = createIRI(NAMESPACE, "hasGivenName");
		HAS_HONORIFIC_PREFIX = createIRI(NAMESPACE, "hasHonorificPrefix");
		HAS_HONORIFIC_SUFFIX = createIRI(NAMESPACE, "hasHonorificSuffix");
		HAS_INSTANT_MESSAGE = createIRI(NAMESPACE, "hasInstantMessage");
		HAS_KEY = createIRI(NAMESPACE, "hasKey");
		HAS_LANGUAGE = createIRI(NAMESPACE, "hasLanguage");
		HAS_LOCALITY = createIRI(NAMESPACE, "hasLocality");
		HAS_LOGO = createIRI(NAMESPACE, "hasLogo");
		HAS_MEMBER = createIRI(NAMESPACE, "hasMember");
		HAS_NAME = createIRI(NAMESPACE, "hasName");
		HAS_NICKNAME = createIRI(NAMESPACE, "hasNickname");
		HAS_NOTE = createIRI(NAMESPACE, "hasNote");
		HAS_ORGANIZATION_NAME = createIRI(NAMESPACE, "hasOrganizationName");
		HAS_ORGANIZATION_UNIT = createIRI(NAMESPACE, "hasOrganizationUnit");
		HAS_PHOTO = createIRI(NAMESPACE, "hasPhoto");
		HAS_POSTAL_CODE = createIRI(NAMESPACE, "hasPostalCode");
		HAS_REGION = createIRI(NAMESPACE, "hasRegion");
		HAS_RELATED = createIRI(NAMESPACE, "hasRelated");
		HAS_ROLE = createIRI(NAMESPACE, "hasRole");
		HAS_SOUND = createIRI(NAMESPACE, "hasSound");
		HAS_SOURCE = createIRI(NAMESPACE, "hasSource");
		HAS_STREET_ADDRESS = createIRI(NAMESPACE, "hasStreetAddress");
		HAS_TELEPHONE = createIRI(NAMESPACE, "hasTelephone");
		HAS_TITLE = createIRI(NAMESPACE, "hasTitle");
		HAS_UID = createIRI(NAMESPACE, "hasUID");
		HAS_URL = createIRI(NAMESPACE, "hasURL");
		HAS_VALUE = createIRI(NAMESPACE, "hasValue");
		HONORIFIC_PREFIX = createIRI(NAMESPACE, "honorific-prefix");
		HONORIFIC_SUFFIX = createIRI(NAMESPACE, "honorific-suffix");
		KEY = createIRI(NAMESPACE, "key");
		LABEL_PROP = createIRI(NAMESPACE, "label");
		LANGUAGE = createIRI(NAMESPACE, "language");
		LATITUDE = createIRI(NAMESPACE, "latitude");
		LOCALITY = createIRI(NAMESPACE, "locality");
		LOGO = createIRI(NAMESPACE, "logo");
		LONGITUDE = createIRI(NAMESPACE, "longitude");
		MAILER = createIRI(NAMESPACE, "mailer");
		N = createIRI(NAMESPACE, "n");
		NICKNAME = createIRI(NAMESPACE, "nickname");
		NOTE = createIRI(NAMESPACE, "note");
		ORG = createIRI(NAMESPACE, "org");
		ORGANIZATION_NAME = createIRI(NAMESPACE, "organization-name");
		ORGANIZATION_UNIT = createIRI(NAMESPACE, "organization-unit");
		PHOTO = createIRI(NAMESPACE, "photo");
		POST_OFFICE_BOX = createIRI(NAMESPACE, "post-office-box");
		POSTAL_CODE = createIRI(NAMESPACE, "postal-code");
		PRODID = createIRI(NAMESPACE, "prodid");
		REGION = createIRI(NAMESPACE, "region");
		REV = createIRI(NAMESPACE, "rev");
		ROLE = createIRI(NAMESPACE, "role");
		SORT_STRING = createIRI(NAMESPACE, "sort-string");
		SOUND = createIRI(NAMESPACE, "sound");
		STREET_ADDRESS = createIRI(NAMESPACE, "street-address");
		TEL_PROP = createIRI(NAMESPACE, "tel");
		TITLE = createIRI(NAMESPACE, "title");
		TZ = createIRI(NAMESPACE, "tz");
		URL = createIRI(NAMESPACE, "url");
		VALUE = createIRI(NAMESPACE, "value");
	}
}
