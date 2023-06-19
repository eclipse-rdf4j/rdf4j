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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * A MemoryStore-specific extension of Literal giving it node properties.
 *
 * @author Arjohn Kampman
 */
public class MemLiteral extends BaseMemValue implements Literal {

	private static final long serialVersionUID = 4288477328829845024L;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The object that created this MemLiteral.
	 */
	transient private final Object creator;

	/**
	 * The literal's label.
	 */
	private String label;
	/**
	 * The literal's language tag.
	 */
	private String language;
	// Cache Optional instance for the language, or null if not yet computed. Marked as transient because Optional is
	// not serializable.
	transient private Optional<String> optionalLanguageCache = null;
	/**
	 * The literal's datatype.
	 */
	private IRI datatype;
	// Cached CoreDatatype, or null if not yet computed.
	private CoreDatatype coreDatatype = null;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new Literal which will get the supplied label.
	 *
	 * @param creator The object that is creating this MemLiteral.
	 * @param label   The label for this literal.
	 */
	public MemLiteral(Object creator, String label) {
		setLabel(label);
		if (org.eclipse.rdf4j.model.vocabulary.RDF.LANGSTRING.equals(XSD.STRING)) {
			throw new IllegalArgumentException("datatype rdf:langString requires a language tag");
		} else if (XSD.STRING == null) {
			setDatatype(CoreDatatype.XSD.STRING);
		} else {
			setDatatype(XSD.STRING);
		}
		MemLiteral.this.optionalLanguageCache = Optional.empty();

		this.creator = creator;
	}

	/**
	 * Creates a new Literal which will get the supplied label and language code.
	 *
	 * @param creator The object that is creating this MemLiteral.
	 * @param label   The label for this literal.
	 * @param lang    The language code of the supplied label.
	 */
	public MemLiteral(Object creator, String label, String lang) {
		setLabel(label);
		setLanguage(lang);
		this.creator = creator;
	}

	/**
	 * Creates a new Literal which will get the supplied label and datatype.
	 *
	 * @param creator  The object that is creating this MemLiteral.
	 * @param label    The label for this literal.
	 * @param datatype The datatype of the supplied label.
	 */
	public MemLiteral(Object creator, String label, IRI datatype) {
		setLabel(label);
		if (org.eclipse.rdf4j.model.vocabulary.RDF.LANGSTRING.equals(datatype)) {
			throw new IllegalArgumentException("datatype rdf:langString requires a language tag");
		} else if (datatype == null) {
			setDatatype(CoreDatatype.XSD.STRING);
		} else {
			setDatatype(datatype);
		}
		MemLiteral.this.optionalLanguageCache = Optional.empty();

		this.creator = creator;
	}

	public MemLiteral(Object creator, String label, IRI datatype, CoreDatatype coreDatatype) {
		assert coreDatatype != null;
		assert datatype != null;
		assert coreDatatype == CoreDatatype.NONE || datatype == coreDatatype.getIri();

		if (CoreDatatype.RDF.LANGSTRING == coreDatatype) {
			throw new IllegalArgumentException("datatype rdf:langString requires a language tag");
		}

		setLabel(label);
		setDatatype(datatype, coreDatatype);
		MemLiteral.this.optionalLanguageCache = Optional.empty();
		this.creator = creator;
	}

	public MemLiteral(Object creator, String label, CoreDatatype datatype) {
		setLabel(label);
		if (datatype == CoreDatatype.RDF.LANGSTRING) {
			throw new IllegalArgumentException("datatype rdf:langString requires a language tag");
		} else {
			setDatatype(datatype);
		}
		MemLiteral.this.optionalLanguageCache = Optional.empty();

		this.creator = creator;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public Object getCreator() {
		return creator;
	}

	@Override
	public boolean hasStatements() {
		return !objectStatements.isEmpty();
	}

	@Override
	public boolean hasSubjectStatements() {
		return false;
	}

	@Override
	public boolean hasPredicateStatements() {
		return false;
	}

	@Override
	public boolean hasContextStatements() {
		return false;
	}

	@Override
	public MemStatementList getSubjectStatementList() {
		return EMPTY_LIST;
	}

	@Override
	public int getSubjectStatementCount() {
		return 0;
	}

	@Override
	public void addSubjectStatement(MemStatement st) throws InterruptedException {
		// no-op
	}

	@Override
	public void cleanSnapshotsFromSubjectStatements(int currentSnapshot) throws InterruptedException {
		// no-op
	}

	@Override
	public MemStatementList getContextStatementList() {
		return EMPTY_LIST;
	}

	@Override
	public int getContextStatementCount() {
		return 0;
	}

	@Override
	public void addContextStatement(MemStatement st) throws InterruptedException {
//no-op
	}

	@Override
	public void cleanSnapshotsFromContextStatements(int currentSnapshot) throws InterruptedException {
		// no-op
	}

	@Override
	public MemStatementList getPredicateStatementList() {
		return EMPTY_LIST;
	}

	@Override
	public int getPredicateStatementCount() {
		return 0;
	}

	@Override
	public void addPredicateStatement(MemStatement st) throws InterruptedException {
		// no-op
	}

	@Override
	public void cleanSnapshotsFromPredicateStatements(int currentSnapshot) throws InterruptedException {
		// no-op
	}

	protected void setLabel(String label) {
		Objects.requireNonNull(label, "Literal label cannot be null");
		this.label = label;
	}

	@Override
	public String getLabel() {
		return label;
	}

	protected void setLanguage(String language) {
		Objects.requireNonNull(language);
		if (language.isEmpty()) {
			throw new IllegalArgumentException("Language tag cannot be empty");
		}
		this.language = language;
		optionalLanguageCache = Optional.of(language);
		setDatatype(CoreDatatype.RDF.LANGSTRING);
	}

	@Override
	public Optional<String> getLanguage() {
		if (optionalLanguageCache == null) {
			optionalLanguageCache = Optional.ofNullable(language);
		}
		return optionalLanguageCache;
	}

	protected void setDatatype(IRI datatype) {
		this.datatype = datatype;
		coreDatatype = CoreDatatype.from(datatype);
	}

	protected void setDatatype(IRI datatype, CoreDatatype coreDatatype) {
		assert datatype != null;
		assert coreDatatype != null;
		assert coreDatatype == CoreDatatype.NONE || datatype == coreDatatype.getIri();

		this.datatype = datatype;
		this.coreDatatype = coreDatatype;

	}

	@Deprecated(since = "4.0.0", forRemoval = true)
	protected void setDatatype(XSD.Datatype datatype) {
		this.datatype = datatype.getIri();
		coreDatatype = datatype.getCoreDatatype();
	}

	protected void setDatatype(CoreDatatype datatype) {
		Objects.requireNonNull(datatype);
		this.datatype = datatype.getIri();
		this.coreDatatype = datatype;
	}

	@Override
	public IRI getDatatype() {
		return datatype;
	}

	/**
	 * @return
	 * @deprecated Use {@link #getCoreDatatype()} instead.
	 */
	@Deprecated(since = "4.0.0", forRemoval = true)
	public Optional<XSD.Datatype> getXsdDatatype() {
		CoreDatatype coreDatatype = getCoreDatatype();

		return XSD.Datatype.from(coreDatatype.asXSDDatatype().orElse(null));
	}

	// Overrides Object.equals(Object), implements Literal.equals(Object)
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof Literal) {
			Literal other = (Literal) o;

			CoreDatatype coreDatatype = getCoreDatatype();

			// Compare core datatypes
			if (coreDatatype != ((Literal) o).getCoreDatatype()) {
				return false;
			} else if (coreDatatype == CoreDatatype.NONE) {
				// Compare other datatypes
				if (!datatype.equals(other.getDatatype())) {
					return false;
				}
			}

			// Compare labels
			if (!label.equals(other.getLabel())) {
				return false;
			}

			Optional<String> language = getLanguage();
			Optional<String> otherLanguage = other.getLanguage();

			if (language.isPresent() && otherLanguage.isPresent()) {
				return language.get().equalsIgnoreCase(otherLanguage.get());
			}
			// If only one has a language, then return false
			else {
				return language.isEmpty() && otherLanguage.isEmpty();
			}
		}

		return false;
	}

	// overrides Object.hashCode(), implements Literal.hashCode()
	@Override
	public int hashCode() {
		return label.hashCode();
	}

	/**
	 * Returns the label of the literal with its language or datatype. Note that this method does not escape the quoted
	 * label.
	 *
	 * @see org.eclipse.rdf4j.rio.ntriples.NTriplesUtil#toNTriplesString(Literal)
	 */
	@Override
	public String toString() {
		if (Literals.isLanguageLiteral(this)) {
			String sb = '"' + label + '"' +
					'@' + language;
			return sb;
		} else if (XSD.STRING.equals(datatype) || datatype == null) {
			return '"' + label + '"';
		} else {
			String sb = '"' + label + '"' +
					"^^<" + datatype + ">";
			return sb;
		}
	}

	@Override
	public String stringValue() {
		return label;
	}

	@Override
	public boolean booleanValue() {
		return XMLDatatypeUtil.parseBoolean(label);
	}

	@Override
	public byte byteValue() {
		return XMLDatatypeUtil.parseByte(label);
	}

	@Override
	public short shortValue() {
		return XMLDatatypeUtil.parseShort(label);
	}

	@Override
	public int intValue() {
		return XMLDatatypeUtil.parseInt(label);
	}

	@Override
	public long longValue() {
		return XMLDatatypeUtil.parseLong(label);
	}

	@Override
	public float floatValue() {
		return XMLDatatypeUtil.parseFloat(label);
	}

	@Override
	public double doubleValue() {
		return XMLDatatypeUtil.parseDouble(label);
	}

	@Override
	public BigInteger integerValue() {
		return XMLDatatypeUtil.parseInteger(label);
	}

	@Override
	public BigDecimal decimalValue() {
		return XMLDatatypeUtil.parseDecimal(label);
	}

	@Override
	public XMLGregorianCalendar calendarValue() {
		return XMLDatatypeUtil.parseCalendar(label);
	}

	@Override
	public CoreDatatype getCoreDatatype() {
		if (coreDatatype == null) {
			coreDatatype = CoreDatatype.from(datatype);
		}
		return coreDatatype;
	}
}
