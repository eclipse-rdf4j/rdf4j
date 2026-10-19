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
 * Constants for the Activity Streams 2.0 Vocabulary.
 *
 * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/"> Activity Streams 2.0 Vocabulary</a>
 *
 * @author Bart Hanssens
 */
public class AS {
	/**
	 * The AS namespace: https://www.w3.org/ns/activitystreams#
	 */
	public static final String NAMESPACE = "https://www.w3.org/ns/activitystreams#";

	/**
	 * Recommended prefix for the namespace: "as"
	 */
	public static final String PREFIX = "as";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// Classes
	/** as:Accept */
	public static final IRI Accept;

	/** as:Activity */
	public static final IRI Activity;

	/** as:Add */
	public static final IRI Add;

	/** as:Announce */
	public static final IRI Announce;

	/** as:Application */
	public static final IRI Application;

	/** as:Arrive */
	public static final IRI Arrive;

	/** as:Article */
	public static final IRI Article;

	/** as:Audio */
	public static final IRI Audio;

	/** as:Block */
	public static final IRI Block;

	/** as:Collection */
	public static final IRI Collection;

	/** as:CollectionPage */
	public static final IRI CollectionPage;

	/** as:Create */
	public static final IRI Create;

	/** as:Delete */
	public static final IRI Delete;

	/** as:Dislike */
	public static final IRI Dislike;

	/** as:Document */
	public static final IRI Document;

	/** as:Event */
	public static final IRI Event;

	/** as:Flag */
	public static final IRI Flag;

	/** as:Follow */
	public static final IRI Follow;

	/** as:Group */
	public static final IRI Group;

	/** as:Ignore */
	public static final IRI Ignore;

	/** as:Image */
	public static final IRI Image;

	/** as:IntransitiveActivity */
	public static final IRI IntransitiveActivity;

	/** as:Invite */
	public static final IRI Invite;

	/** as:Join */
	public static final IRI Join;

	/** as:Leave */
	public static final IRI Leave;

	/** as:Like */
	public static final IRI Like;

	/** as:Link */
	public static final IRI Link;

	/** as:Listen */
	public static final IRI Listen;

	/** as:Mention */
	public static final IRI Mention;

	/** as:Move */
	public static final IRI Move;

	/** as:Note */
	public static final IRI Note;

	/** as:Object */
	public static final IRI Object;

	/** as:Offer */
	public static final IRI Offer;

	/** as:OrderedCollection */
	public static final IRI OrderedCollection;

	/** as:OrderedCollectionPage */
	public static final IRI OrderedCollectionPage;

	/** as:OrderedItems */
	public static final IRI OrderedItems;

	/** as:Organization */
	public static final IRI Organization;

	/** as:Page */
	public static final IRI Page;

	/** as:Person */
	public static final IRI Person;

	/** as:Place */
	public static final IRI Place;

	/** as:Profile */
	public static final IRI Profile;

	/** as:Question */
	public static final IRI Question;

	/** as:Read */
	public static final IRI Read;

	/** as:Reject */
	public static final IRI Reject;

	/** as:Relationship */
	public static final IRI Relationship;

	/** as:Remove */
	public static final IRI Remove;

	/** as:Service */
	public static final IRI Service;

	/** as:TentativeAccept */
	public static final IRI TentativeAccept;

	/** as:TentativeReject */
	public static final IRI TentativeReject;

	/** as:Tombstone */
	public static final IRI Tombstone;

	/** as:Travel */
	public static final IRI Travel;

	/** as:Undo */
	public static final IRI Undo;

	/** as:Update */
	public static final IRI Update;

	/** as:Video */
	public static final IRI Video;

	/** as:View */
	public static final IRI View;

	// Properties
	/** as:accuracy */
	public static final IRI accuracy;

	/** as:actor */
	public static final IRI actor;

	/** as:altitude */
	public static final IRI altitude;

	/** as:anyOf */
	public static final IRI anyOf;

	/** as:attachment */
	public static final IRI attachment;

	/** as:attachments */
	public static final IRI attachments;

	/** as:attributedTo */
	public static final IRI attributedTo;

	/** as:audience */
	public static final IRI audience;

	/** as:author */
	public static final IRI author;

	/** as:bcc */
	public static final IRI bcc;

	/** as:bto */
	public static final IRI bto;

	/** as:cc */
	public static final IRI cc;

	/** as:content */
	public static final IRI content;

	/** as:context */
	public static final IRI context;

	/** as:current */
	public static final IRI current;

	/** as:deleted */
	public static final IRI deleted;

	/** as:describes */
	public static final IRI describes;

	/** as:downstreamDuplicates */
	public static final IRI downstreamDuplicates;

	/** as:duration */
	public static final IRI duration;

	/** as:endTime */
	public static final IRI endTime;

	/** as:first */
	public static final IRI first;

	/** as:formerType */
	public static final IRI formerType;

	/** as:generator */
	public static final IRI generator;

	/** as:height */
	public static final IRI height;

	/** as:href */
	public static final IRI href;

	/** as:hreflang */
	public static final IRI hreflang;

	/** as:icon */
	public static final IRI icon;

	/** as:id */
	public static final IRI id;

	/** as:image */
	public static final IRI image;

	/** as:inReplyTo */
	public static final IRI inReplyTo;

	/** as:instrument */
	public static final IRI instrument;

	/** as:items */
	public static final IRI items;

	/** as:last */
	public static final IRI last;

	/** as:latitude */
	public static final IRI latitude;

	/** as:location */
	public static final IRI location;

	/** as:longitude */
	public static final IRI longitude;

	/** as:mediaType */
	public static final IRI mediaType;

	/** as:name */
	public static final IRI name;

	/** as:next */
	public static final IRI next;

	/** as:object */
	public static final IRI object;

	/** as:objectType */
	public static final IRI objectType;

	/** as:oneOf */
	public static final IRI oneOf;

	/** as:origin */
	public static final IRI origin;

	/** as:partOf */
	public static final IRI partOf;

	/** as:prev */
	public static final IRI prev;

	/** as:preview */
	public static final IRI preview;

	/** as:provider */
	public static final IRI provider;

	/** as:published */
	public static final IRI published;

	/** as:radius */
	public static final IRI radius;

	/** as:rating */
	public static final IRI rating;

	/** as:rel */
	public static final IRI rel;

	/** as:relationship */
	public static final IRI relationship;

	/** as:replies */
	public static final IRI replies;

	/** as:result */
	public static final IRI result;

	/** as:startIndex */
	public static final IRI startIndex;

	/** as:startTime */
	public static final IRI startTime;

	/** as:subject */
	public static final IRI subject;

	/** as:summary */
	public static final IRI summary;

	/** as:tag */
	public static final IRI tag;

	/** as:tags */
	public static final IRI tags;

	/** as:target */
	public static final IRI target;

	/** as:to */
	public static final IRI to;

	/** as:totalItems */
	public static final IRI totalItems;

	/** as:units */
	public static final IRI units;

	/** as:updated */
	public static final IRI updated;

	/** as:upstreamDuplicates */
	public static final IRI upstreamDuplicates;

	/** as:url */
	public static final IRI url;

	/** as:verb */
	public static final IRI verb;

	/** as:width */
	public static final IRI width;

	static {
		Accept = Vocabularies.createIRI(NAMESPACE, "Accept");
		Activity = Vocabularies.createIRI(NAMESPACE, "Activity");
		Add = Vocabularies.createIRI(NAMESPACE, "Add");
		Announce = Vocabularies.createIRI(NAMESPACE, "Announce");
		Application = Vocabularies.createIRI(NAMESPACE, "Application");
		Arrive = Vocabularies.createIRI(NAMESPACE, "Arrive");
		Article = Vocabularies.createIRI(NAMESPACE, "Article");
		Audio = Vocabularies.createIRI(NAMESPACE, "Audio");
		Block = Vocabularies.createIRI(NAMESPACE, "Block");
		Collection = Vocabularies.createIRI(NAMESPACE, "Collection");
		CollectionPage = Vocabularies.createIRI(NAMESPACE, "CollectionPage");
		Create = Vocabularies.createIRI(NAMESPACE, "Create");
		Delete = Vocabularies.createIRI(NAMESPACE, "Delete");
		Dislike = Vocabularies.createIRI(NAMESPACE, "Dislike");
		Document = Vocabularies.createIRI(NAMESPACE, "Document");
		Event = Vocabularies.createIRI(NAMESPACE, "Event");
		Flag = Vocabularies.createIRI(NAMESPACE, "Flag");
		Follow = Vocabularies.createIRI(NAMESPACE, "Follow");
		Group = Vocabularies.createIRI(NAMESPACE, "Group");
		Ignore = Vocabularies.createIRI(NAMESPACE, "Ignore");
		Image = Vocabularies.createIRI(NAMESPACE, "Image");
		IntransitiveActivity = Vocabularies.createIRI(NAMESPACE, "IntransitiveActivity");
		Invite = Vocabularies.createIRI(NAMESPACE, "Invite");
		Join = Vocabularies.createIRI(NAMESPACE, "Join");
		Leave = Vocabularies.createIRI(NAMESPACE, "Leave");
		Like = Vocabularies.createIRI(NAMESPACE, "Like");
		Link = Vocabularies.createIRI(NAMESPACE, "Link");
		Listen = Vocabularies.createIRI(NAMESPACE, "Listen");
		Mention = Vocabularies.createIRI(NAMESPACE, "Mention");
		Move = Vocabularies.createIRI(NAMESPACE, "Move");
		Note = Vocabularies.createIRI(NAMESPACE, "Note");
		Object = Vocabularies.createIRI(NAMESPACE, "Object");
		Offer = Vocabularies.createIRI(NAMESPACE, "Offer");
		OrderedCollection = Vocabularies.createIRI(NAMESPACE, "OrderedCollection");
		OrderedCollectionPage = Vocabularies.createIRI(NAMESPACE, "OrderedCollectionPage");
		OrderedItems = Vocabularies.createIRI(NAMESPACE, "OrderedItems");
		Organization = Vocabularies.createIRI(NAMESPACE, "Organization");
		Page = Vocabularies.createIRI(NAMESPACE, "Page");
		Person = Vocabularies.createIRI(NAMESPACE, "Person");
		Place = Vocabularies.createIRI(NAMESPACE, "Place");
		Profile = Vocabularies.createIRI(NAMESPACE, "Profile");
		Question = Vocabularies.createIRI(NAMESPACE, "Question");
		Read = Vocabularies.createIRI(NAMESPACE, "Read");
		Reject = Vocabularies.createIRI(NAMESPACE, "Reject");
		Relationship = Vocabularies.createIRI(NAMESPACE, "Relationship");
		Remove = Vocabularies.createIRI(NAMESPACE, "Remove");
		Service = Vocabularies.createIRI(NAMESPACE, "Service");
		TentativeAccept = Vocabularies.createIRI(NAMESPACE, "TentativeAccept");
		TentativeReject = Vocabularies.createIRI(NAMESPACE, "TentativeReject");
		Tombstone = Vocabularies.createIRI(NAMESPACE, "Tombstone");
		Travel = Vocabularies.createIRI(NAMESPACE, "Travel");
		Undo = Vocabularies.createIRI(NAMESPACE, "Undo");
		Update = Vocabularies.createIRI(NAMESPACE, "Update");
		Video = Vocabularies.createIRI(NAMESPACE, "Video");
		View = Vocabularies.createIRI(NAMESPACE, "View");

		accuracy = Vocabularies.createIRI(NAMESPACE, "accuracy");
		actor = Vocabularies.createIRI(NAMESPACE, "actor");
		altitude = Vocabularies.createIRI(NAMESPACE, "altitude");
		anyOf = Vocabularies.createIRI(NAMESPACE, "anyOf");
		attachment = Vocabularies.createIRI(NAMESPACE, "attachment");
		attachments = Vocabularies.createIRI(NAMESPACE, "attachments");
		attributedTo = Vocabularies.createIRI(NAMESPACE, "attributedTo");
		audience = Vocabularies.createIRI(NAMESPACE, "audience");
		author = Vocabularies.createIRI(NAMESPACE, "author");
		bcc = Vocabularies.createIRI(NAMESPACE, "bcc");
		bto = Vocabularies.createIRI(NAMESPACE, "bto");
		cc = Vocabularies.createIRI(NAMESPACE, "cc");
		content = Vocabularies.createIRI(NAMESPACE, "content");
		context = Vocabularies.createIRI(NAMESPACE, "context");
		current = Vocabularies.createIRI(NAMESPACE, "current");
		deleted = Vocabularies.createIRI(NAMESPACE, "deleted");
		describes = Vocabularies.createIRI(NAMESPACE, "describes");
		downstreamDuplicates = Vocabularies.createIRI(NAMESPACE, "downstreamDuplicates");
		duration = Vocabularies.createIRI(NAMESPACE, "duration");
		endTime = Vocabularies.createIRI(NAMESPACE, "endTime");
		first = Vocabularies.createIRI(NAMESPACE, "first");
		formerType = Vocabularies.createIRI(NAMESPACE, "formerType");
		generator = Vocabularies.createIRI(NAMESPACE, "generator");
		height = Vocabularies.createIRI(NAMESPACE, "height");
		href = Vocabularies.createIRI(NAMESPACE, "href");
		hreflang = Vocabularies.createIRI(NAMESPACE, "hreflang");
		icon = Vocabularies.createIRI(NAMESPACE, "icon");
		id = Vocabularies.createIRI(NAMESPACE, "id");
		image = Vocabularies.createIRI(NAMESPACE, "image");
		inReplyTo = Vocabularies.createIRI(NAMESPACE, "inReplyTo");
		instrument = Vocabularies.createIRI(NAMESPACE, "instrument");
		items = Vocabularies.createIRI(NAMESPACE, "items");
		last = Vocabularies.createIRI(NAMESPACE, "last");
		latitude = Vocabularies.createIRI(NAMESPACE, "latitude");
		location = Vocabularies.createIRI(NAMESPACE, "location");
		longitude = Vocabularies.createIRI(NAMESPACE, "longitude");
		mediaType = Vocabularies.createIRI(NAMESPACE, "mediaType");
		name = Vocabularies.createIRI(NAMESPACE, "name");
		next = Vocabularies.createIRI(NAMESPACE, "next");
		object = Vocabularies.createIRI(NAMESPACE, "object");
		objectType = Vocabularies.createIRI(NAMESPACE, "objectType");
		oneOf = Vocabularies.createIRI(NAMESPACE, "oneOf");
		origin = Vocabularies.createIRI(NAMESPACE, "origin");
		partOf = Vocabularies.createIRI(NAMESPACE, "partOf");
		prev = Vocabularies.createIRI(NAMESPACE, "prev");
		preview = Vocabularies.createIRI(NAMESPACE, "preview");
		provider = Vocabularies.createIRI(NAMESPACE, "provider");
		published = Vocabularies.createIRI(NAMESPACE, "published");
		radius = Vocabularies.createIRI(NAMESPACE, "radius");
		rating = Vocabularies.createIRI(NAMESPACE, "rating");
		rel = Vocabularies.createIRI(NAMESPACE, "rel");
		relationship = Vocabularies.createIRI(NAMESPACE, "relationship");
		replies = Vocabularies.createIRI(NAMESPACE, "replies");
		result = Vocabularies.createIRI(NAMESPACE, "result");
		startIndex = Vocabularies.createIRI(NAMESPACE, "startIndex");
		startTime = Vocabularies.createIRI(NAMESPACE, "startTime");
		subject = Vocabularies.createIRI(NAMESPACE, "subject");
		summary = Vocabularies.createIRI(NAMESPACE, "summary");
		tag = Vocabularies.createIRI(NAMESPACE, "tag");
		tags = Vocabularies.createIRI(NAMESPACE, "tags");
		target = Vocabularies.createIRI(NAMESPACE, "target");
		to = Vocabularies.createIRI(NAMESPACE, "to");
		totalItems = Vocabularies.createIRI(NAMESPACE, "totalItems");
		units = Vocabularies.createIRI(NAMESPACE, "units");
		updated = Vocabularies.createIRI(NAMESPACE, "updated");
		upstreamDuplicates = Vocabularies.createIRI(NAMESPACE, "upstreamDuplicates");
		url = Vocabularies.createIRI(NAMESPACE, "url");
		verb = Vocabularies.createIRI(NAMESPACE, "verb");
		width = Vocabularies.createIRI(NAMESPACE, "width");
	}
}
