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
package org.eclipse.rdf4j.sail.memory.model;

import java.io.IOException;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;

/**
 * An extension of MemLiteral that stores a Calendar value to avoid parsing.
 *
 * @author David Huynh
 * @author Arjohn Kampman
 */
public class CalendarMemLiteral extends MemLiteral {

	private static final long serialVersionUID = -7903843639313451580L;

	/*-----------*
	 * Variables *
	 *-----------*/

	transient private XMLGregorianCalendar calendar;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public CalendarMemLiteral(Object creator, XMLGregorianCalendar calendar) {
		this(creator, calendar.toXMLFormat(), calendar);
	}

	public CalendarMemLiteral(Object creator, String label, XMLGregorianCalendar calendar) {
		this(creator, label, XMLDatatypeUtil.qnameToCoreDatatype(calendar.getXMLSchemaType()), calendar);
	}

	public CalendarMemLiteral(Object creator, String label, IRI datatype, XMLGregorianCalendar calendar) {
		super(creator, label, datatype);
		this.calendar = calendar;
	}

	public CalendarMemLiteral(Object creator, String label, CoreDatatype datatype, XMLGregorianCalendar calendar) {
		super(creator, label, datatype);
		this.calendar = calendar;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public XMLGregorianCalendar calendarValue() {
		return calendar;
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException {
		try {
			in.defaultReadObject();
			calendar = XMLDatatypeUtil.parseCalendar(this.getLabel());
		} catch (ClassNotFoundException e) {
			throw new IOException(e.getMessage());
		}
	}
}
