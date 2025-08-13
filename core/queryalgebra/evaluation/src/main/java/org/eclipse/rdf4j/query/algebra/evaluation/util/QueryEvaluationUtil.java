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
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import javax.xml.datatype.DatatypeConstants;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

/**
 * Utility functions used during logical query evaluation.
 *
 * <p>
 * <b>Performance note</b>: every comparison operator now has its own specialised method. All hot paths are branch‑free
 * w.r.t. {@code CompareOp}, allowing the JVM to inline and optimise aggressively.
 * </p>
 */
public class QueryEvaluationUtil {

	/*
	 * ======================================================================= Shared (unchanged) exception instances
	 * =====================================================================
	 */
	public static final ValueExprEvaluationException INDETERMINATE_DATE_TIME_EXCEPTION = new ValueExprEvaluationException(
			"Indeterminate result for date/time comparison");
	public static final ValueExprEvaluationException STRING_WITH_OTHER_SUPPORTED_TYPE_EXCEPTION = new ValueExprEvaluationException(
			"Unable to compare strings with other supported types");
	public static final ValueExprEvaluationException NUMERIC_WITH_OTHER_SUPPORTED_TYPE_EXCEPTION = new ValueExprEvaluationException(
			"Unable to compare numeric types with other supported types");
	public static final ValueExprEvaluationException DATE_WITH_OTHER_SUPPORTED_TYPE_EXCEPTION = new ValueExprEvaluationException(
			"Unable to compare date types with other supported types");
	public static final ValueExprEvaluationException UNSUPPOERTED_TYPES_EXCEPTION = new ValueExprEvaluationException(
			"Unable to compare literals with unsupported types");
	public static final ValueExprEvaluationException NOT_COMPATIBLE_AND_ORDERED_EXCEPTION = new ValueExprEvaluationException(
			"Only literals with compatible, ordered datatypes can be compared using <, <=, > and >= operators");

	/*
	 * ======================================================================= EBV helper (unchanged)
	 * =====================================================================
	 */
	public static boolean getEffectiveBooleanValue(Value value) throws ValueExprEvaluationException {
		if (value == BooleanLiteral.TRUE) {
			return true;
		}
		if (value == BooleanLiteral.FALSE) {
			return false;
		}

		if (value.isLiteral()) {
			Literal lit = (Literal) value;
			String label = lit.getLabel();
			CoreDatatype.XSD dt = lit.getCoreDatatype().asXSDDatatypeOrNull();

			if (dt == CoreDatatype.XSD.STRING) {
				return !label.isEmpty();
			}
			if (dt == CoreDatatype.XSD.BOOLEAN) {
				return "true".equals(label) || "1".equals(label);
			}

			try {
				if (dt == CoreDatatype.XSD.DECIMAL) {
					return !"0.0".equals(XMLDatatypeUtil.normalizeDecimal(label));
				}

				if (dt != null && dt.isIntegerDatatype()) {
					return !"0".equals(XMLDatatypeUtil.normalize(label, dt));
				}

				if (dt != null && dt.isFloatingPointDatatype()) {
					String n = XMLDatatypeUtil.normalize(label, dt);
					return !("0.0E0".equals(n) || "NaN".equals(n));
				}
			} catch (IllegalArgumentException ignore) {
				/* fall through */
			}
		}
		throw new ValueExprEvaluationException();
	}

	/*
	 * ======================================================================= Tiny int‑comparators
	 * =====================================================================
	 */
	private static boolean _lt(int c) {
		return c < 0;
	}

	private static boolean _le(int c) {
		return c <= 0;
	}

	private static boolean _eq(int c) {
		return c == 0;
	}

	private static boolean _ne(int c) {
		return c != 0;
	}

	private static boolean _gt(int c) {
		return c > 0;
	}

	private static boolean _ge(int c) {
		return c >= 0;
	}

	/*
	 * ======================================================================= PUBLIC VALUE‑LEVEL SPECIALISED
	 * COMPARATORS =====================================================================
	 */

	/* -------- EQ -------- */
	public static boolean compareEQ(Value l, Value r) throws ValueExprEvaluationException {
		return compareEQ(l, r, true);
	}

	public static boolean compareEQ(Value l, Value r, boolean strict)
			throws ValueExprEvaluationException {
		if (l == null || r == null) {
			return l == r; // null is equal to null, but not to anything else
		}
		if (l == r) {
			return true;
		}
		if (l.isLiteral() && r.isLiteral()) {
			return doCompareLiteralsEQ((Literal) l, (Literal) r, strict);
		}
		return l.equals(r);
	}

	/* -------- NE -------- */
	public static boolean compareNE(Value l, Value r) throws ValueExprEvaluationException {
		return compareNE(l, r, true);
	}

	public static boolean compareNE(Value l, Value r, boolean strict)
			throws ValueExprEvaluationException {
		if (l == null || r == null) {
			return l != r; // null is equal to null, but not to anything else
		}
		if (l == r) {
			return false;
		}
		if (l.isLiteral() && r.isLiteral()) {
			return doCompareLiteralsNE((Literal) l, (Literal) r, strict);
		}
		return !l.equals(r);
	}

	/* -------- LT -------- */
	public static boolean compareLT(Value l, Value r) throws ValueExprEvaluationException {
		return compareLT(l, r, true);
	}

	public static boolean compareLT(Value l, Value r, boolean strict)
			throws ValueExprEvaluationException {
		if (l == r) {
			return false;
		}
		if (l != null && l.isLiteral() && r != null && r.isLiteral()) {
			return doCompareLiteralsLT((Literal) l, (Literal) r, strict);
		}
		throw NOT_COMPATIBLE_AND_ORDERED_EXCEPTION;
	}

	/* -------- LE -------- */
	public static boolean compareLE(Value l, Value r) throws ValueExprEvaluationException {
		return compareLE(l, r, true);
	}

	public static boolean compareLE(Value l, Value r, boolean strict)
			throws ValueExprEvaluationException {
		if (l == r) {
			return true;
		}
		if (l != null && l.isLiteral() && r != null && r.isLiteral()) {
			return doCompareLiteralsLE((Literal) l, (Literal) r, strict);
		}
		throw NOT_COMPATIBLE_AND_ORDERED_EXCEPTION;
	}

	/* -------- GT -------- */
	public static boolean compareGT(Value l, Value r) throws ValueExprEvaluationException {
		return compareGT(l, r, true);
	}

	public static boolean compareGT(Value l, Value r, boolean strict)
			throws ValueExprEvaluationException {
		if (l == r) {
			return false;
		}
		if (l != null && l.isLiteral() && r != null && r.isLiteral()) {
			return doCompareLiteralsGT((Literal) l, (Literal) r, strict);
		}
		throw NOT_COMPATIBLE_AND_ORDERED_EXCEPTION;
	}

	/* -------- GE -------- */
	public static boolean compareGE(Value l, Value r) throws ValueExprEvaluationException {
		return compareGE(l, r, true);
	}

	public static boolean compareGE(Value l, Value r, boolean strict)
			throws ValueExprEvaluationException {
		if (l == r) {
			return true;
		}
		if (l != null && l.isLiteral() && r != null && r.isLiteral()) {
			return doCompareLiteralsGE((Literal) l, (Literal) r, strict);
		}
		throw NOT_COMPATIBLE_AND_ORDERED_EXCEPTION;
	}

	/*
	 * ======================================================================= PUBLIC LITERAL‑LEVEL SPECIALISED
	 * COMPARATORS =====================================================================
	 */

	/* -- EQ -- */
	public static boolean compareLiteralsEQ(Literal l, Literal r) throws ValueExprEvaluationException {
		return compareLiteralsEQ(l, r, true);
	}

	public static boolean compareLiteralsEQ(Literal l, Literal r, boolean strict)
			throws ValueExprEvaluationException {
		return doCompareLiteralsEQ(l, r, strict);
	}

	/* -- NE -- */
	public static boolean compareLiteralsNE(Literal l, Literal r) throws ValueExprEvaluationException {
		return compareLiteralsNE(l, r, true);
	}

	public static boolean compareLiteralsNE(Literal l, Literal r, boolean strict)
			throws ValueExprEvaluationException {
		return doCompareLiteralsNE(l, r, strict);
	}

	/* -- LT -- */
	public static boolean compareLiteralsLT(Literal l, Literal r) throws ValueExprEvaluationException {
		return compareLiteralsLT(l, r, true);
	}

	public static boolean compareLiteralsLT(Literal l, Literal r, boolean strict)
			throws ValueExprEvaluationException {
		return doCompareLiteralsLT(l, r, strict);
	}

	/* -- LE -- */
	public static boolean compareLiteralsLE(Literal l, Literal r) throws ValueExprEvaluationException {
		return compareLiteralsLE(l, r, true);
	}

	public static boolean compareLiteralsLE(Literal l, Literal r, boolean strict)
			throws ValueExprEvaluationException {
		return doCompareLiteralsLE(l, r, strict);
	}

	/* -- GT -- */
	public static boolean compareLiteralsGT(Literal l, Literal r) throws ValueExprEvaluationException {
		return compareLiteralsGT(l, r, true);
	}

	public static boolean compareLiteralsGT(Literal l, Literal r, boolean strict)
			throws ValueExprEvaluationException {
		return doCompareLiteralsGT(l, r, strict);
	}

	/* -- GE -- */
	public static boolean compareLiteralsGE(Literal l, Literal r) throws ValueExprEvaluationException {
		return compareLiteralsGE(l, r, true);
	}

	public static boolean compareLiteralsGE(Literal l, Literal r, boolean strict)
			throws ValueExprEvaluationException {
		return doCompareLiteralsGE(l, r, strict);
	}

	/*
	 * ======================================================================= LEGACY PUBLIC APIs – retained for
	 * compatibility =====================================================================
	 */

	/** @deprecated use the specialised compareXX methods instead. */
	@Deprecated
	public static boolean compare(Value l, Value r, CompareOp op)
			throws ValueExprEvaluationException {
		return compare(l, r, op, true);
	}

	/** @deprecated use the specialised compareXX methods instead. */
	@Deprecated
	public static boolean compare(Value l, Value r, CompareOp op, boolean strict)
			throws ValueExprEvaluationException {
		switch (op) {
		case EQ:
			return compareEQ(l, r, strict);
		case NE:
			return compareNE(l, r, strict);
		case LT:
			return compareLT(l, r, strict);
		case LE:
			return compareLE(l, r, strict);
		case GT:
			return compareGT(l, r, strict);
		case GE:
			return compareGE(l, r, strict);
		default:
			throw new IllegalArgumentException("Unknown operator: " + op);
		}
	}

	/** @deprecated use the specialised compareLiteralsXX methods instead. */
	@Deprecated
	public static boolean compareLiterals(Literal l, Literal r, CompareOp op)
			throws ValueExprEvaluationException {
		return compareLiterals(l, r, op, true);
	}

	/** @deprecated use the specialised compareLiteralsXX methods instead. */
	@Deprecated
	public static boolean compareLiterals(Literal l, Literal r, CompareOp op, boolean strict)
			throws ValueExprEvaluationException {
		switch (op) {
		case EQ:
			return compareLiteralsEQ(l, r, strict);
		case NE:
			return compareLiteralsNE(l, r, strict);
		case LT:
			return compareLiteralsLT(l, r, strict);
		case LE:
			return compareLiteralsLE(l, r, strict);
		case GT:
			return compareLiteralsGT(l, r, strict);
		case GE:
			return compareLiteralsGE(l, r, strict);
		default:
			throw new IllegalArgumentException("Unknown operator: " + op);
		}
	}

	/* Still referenced by some external code */
	public static boolean compareWithOperator(CompareOp op, int c) {
		switch (op) {
		case LT:
			return _lt(c);
		case LE:
			return _le(c);
		case EQ:
			return _eq(c);
		case NE:
			return _ne(c);
		case GE:
			return _ge(c);
		case GT:
			return _gt(c);
		default:
			throw new IllegalArgumentException("Unknown operator: " + op);
		}
	}

	/*
	 * ======================================================================= PRIVATE HEAVY LITERAL COMPARATORS
	 * (prefixed with do… to avoid signature clashes with public wrappers)
	 * =====================================================================
	 */

	private static boolean doCompareLiteralsEQ(Literal l, Literal r, boolean strict)
			throws ValueExprEvaluationException {
		if (l == r) {
			return true;
		}

		CoreDatatype ld = l.getCoreDatatype();
		CoreDatatype rd = r.getCoreDatatype();

		if (ld == rd) {
			if (ld == CoreDatatype.XSD.STRING) {
				return l.getLabel().equals(r.getLabel());
			}
			if (ld == CoreDatatype.RDF.LANGSTRING) {
				return l.getLanguage().equals(r.getLanguage()) && l.getLabel().equals(r.getLabel());
			}
		}

		boolean lLang = Literals.isLanguageLiteral(l);
		boolean rLang = Literals.isLanguageLiteral(r);

		if (!(lLang || rLang)) {
			CoreDatatype.XSD common = getCommonDatatype(strict, ld.asXSDDatatypeOrNull(), rd.asXSDDatatypeOrNull());
			if (common != null) {
				try {
					if (common == CoreDatatype.XSD.DOUBLE) {
						return l.doubleValue() == r.doubleValue();
					}
					if (common == CoreDatatype.XSD.FLOAT) {
						return l.floatValue() == r.floatValue();
					}
					if (common == CoreDatatype.XSD.DECIMAL) {
						return l.decimalValue().equals(r.decimalValue());
					}
					if (common.isIntegerDatatype()) {
						return l.integerValue().equals(r.integerValue());
					}
					if (common == CoreDatatype.XSD.BOOLEAN) {
						return l.booleanValue() == r.booleanValue();
					}
					if (common.isCalendarDatatype()) {
						if (ld == rd) {
							if (l.getLabel().equals(r.getLabel())) {
								return true; // same label, same calendar value
							}
						}

						int c = l.calendarValue().compare(r.calendarValue());
						if (c == DatatypeConstants.INDETERMINATE &&
								ld == CoreDatatype.XSD.DATETIME &&
								rd == CoreDatatype.XSD.DATETIME) {
							throw INDETERMINATE_DATE_TIME_EXCEPTION;
						}
						return _eq(c);
					}
					if (!strict && common.isDurationDatatype()) {
						if (ld == rd) {
							if (l.getLabel().equals(r.getLabel())) {
								return true; // same label, same calendar value
							}
						}

						int c = XMLDatatypeUtil.parseDuration(l.getLabel())
								.compare(XMLDatatypeUtil.parseDuration(r.getLabel()));
						if (c != DatatypeConstants.INDETERMINATE) {
							return _eq(c);
						}
					}
					if (common == CoreDatatype.XSD.STRING) {
						return l.getLabel().equals(r.getLabel());
					}
				} catch (IllegalArgumentException iae) {
					// lexical‑to‑value failed; fall through
				}
			}
		}
		return otherCasesEQ(l, r, ld.asXSDDatatypeOrNull(), rd.asXSDDatatypeOrNull(), lLang, rLang, strict);
	}

	private static boolean doCompareLiteralsNE(Literal l, Literal r, boolean strict)
			throws ValueExprEvaluationException {
		if (l.equals(r)) {
			return false;
		}
		return !doCompareLiteralsEQ(l, r, strict);
	}

	private static boolean doCompareLiteralsLT(Literal l, Literal r, boolean strict)
			throws ValueExprEvaluationException {
		CoreDatatype.XSD ld = l.getCoreDatatype().asXSDDatatypeOrNull();
		CoreDatatype.XSD rd = r.getCoreDatatype().asXSDDatatypeOrNull();
		boolean lLang = Literals.isLanguageLiteral(l);
		boolean rLang = Literals.isLanguageLiteral(r);

		if (isSimpleLiteral(lLang, ld) && isSimpleLiteral(rLang, rd)) {
			return _lt(l.getLabel().compareTo(r.getLabel()));
		}

		if (!(lLang || rLang)) {
			CoreDatatype.XSD common = getCommonDatatype(strict, ld, rd);
			if (common != null) {
				try {
					if (common == CoreDatatype.XSD.DOUBLE) {
						return _lt(Double.compare(l.doubleValue(), r.doubleValue()));
					}
					if (common == CoreDatatype.XSD.FLOAT) {
						return _lt(Float.compare(l.floatValue(), r.floatValue()));
					}
					if (common == CoreDatatype.XSD.DECIMAL) {
						return _lt(l.decimalValue().compareTo(r.decimalValue()));
					}
					if (common.isIntegerDatatype()) {
						return _lt(l.integerValue().compareTo(r.integerValue()));
					}
					if (common == CoreDatatype.XSD.BOOLEAN) {
						return _lt(Boolean.compare(l.booleanValue(), r.booleanValue()));
					}
					if (common.isCalendarDatatype()) {
						int c = l.calendarValue().compare(r.calendarValue());
						if (c == DatatypeConstants.INDETERMINATE &&
								ld == CoreDatatype.XSD.DATETIME &&
								rd == CoreDatatype.XSD.DATETIME) {
							throw INDETERMINATE_DATE_TIME_EXCEPTION;
						}
						return _lt(c);
					}
					if (!strict && common.isDurationDatatype()) {
						int c = XMLDatatypeUtil.parseDuration(l.getLabel())
								.compare(XMLDatatypeUtil.parseDuration(r.getLabel()));
						if (c != DatatypeConstants.INDETERMINATE) {
							return _lt(c);
						}
					}
					if (common == CoreDatatype.XSD.STRING) {
						return _lt(l.getLabel().compareTo(r.getLabel()));
					}
				} catch (IllegalArgumentException iae) {
					throw new ValueExprEvaluationException(iae);
				}
			}
		}

		if (!isSupportedDatatype(ld) || !isSupportedDatatype(rd)) {
			throw UNSUPPOERTED_TYPES_EXCEPTION;
		}

		validateDatatypeCompatibility(strict, ld, rd);

		throw NOT_COMPATIBLE_AND_ORDERED_EXCEPTION;
	}

	private static boolean doCompareLiteralsLE(Literal l, Literal r, boolean strict)
			throws ValueExprEvaluationException {
		return doCompareLiteralsLT(l, r, strict) || doCompareLiteralsEQ(l, r, strict);
	}

	private static boolean doCompareLiteralsGT(Literal l, Literal r, boolean strict)
			throws ValueExprEvaluationException {
		return !doCompareLiteralsLE(l, r, strict);
	}

	private static boolean doCompareLiteralsGE(Literal l, Literal r, boolean strict)
			throws ValueExprEvaluationException {
		return !doCompareLiteralsLT(l, r, strict);
	}

	/*
	 * ======================================================================= Fallback for EQ otherCases (unchanged
	 * from previous draft) =====================================================================
	 */
	private static boolean otherCasesEQ(Literal left, Literal right,
			CoreDatatype.XSD ldt, CoreDatatype.XSD rdt,
			boolean lLang, boolean rLang, boolean strict)
			throws ValueExprEvaluationException {

		boolean equal = left.equals(right);

		if (!equal) {
			if (!lLang && !rLang && isSupportedDatatype(ldt) && isSupportedDatatype(rdt)) {
				if (!XMLDatatypeUtil.isValidValue(left.getLabel(), ldt)) {
					throw new ValueExprEvaluationException("not a valid datatype value: " + left);
				}
				if (!XMLDatatypeUtil.isValidValue(right.getLabel(), rdt)) {
					throw new ValueExprEvaluationException("not a valid datatype value: " + right);
				}
				validateDatatypeCompatibility(strict, ldt, rdt);
			} else if (!lLang && !rLang) {
				throw UNSUPPOERTED_TYPES_EXCEPTION;
			}
		}
		return equal;
	}

	/*
	 * ======================================================================= Datatype helpers & misc (unchanged)
	 * =====================================================================
	 */
	private static void validateDatatypeCompatibility(boolean strict,
			CoreDatatype.XSD ld, CoreDatatype.XSD rd)
			throws ValueExprEvaluationException {
		if (!strict) {
			return;
		}
		boolean leftString = ld == CoreDatatype.XSD.STRING;
		boolean rightString = rd == CoreDatatype.XSD.STRING;
		if (leftString != rightString) {
			throw STRING_WITH_OTHER_SUPPORTED_TYPE_EXCEPTION;
		}

		boolean leftNum = ld.isNumericDatatype();
		boolean rightNum = rd.isNumericDatatype();
		if (leftNum != rightNum) {
			throw NUMERIC_WITH_OTHER_SUPPORTED_TYPE_EXCEPTION;
		}

		boolean leftDate = ld.isCalendarDatatype();
		boolean rightDate = rd.isCalendarDatatype();
		if (leftDate != rightDate) {
			throw DATE_WITH_OTHER_SUPPORTED_TYPE_EXCEPTION;
		}
	}

	private static CoreDatatype.XSD getCommonDatatype(boolean strict,
			CoreDatatype.XSD ld, CoreDatatype.XSD rd) {
		if (ld != null && rd != null) {
			if (ld == rd) {
				return ld;
			}
			if (ld.isNumericDatatype() && rd.isNumericDatatype()) {
				if (ld == CoreDatatype.XSD.DOUBLE || rd == CoreDatatype.XSD.DOUBLE) {
					return CoreDatatype.XSD.DOUBLE;
				}
				if (ld == CoreDatatype.XSD.FLOAT || rd == CoreDatatype.XSD.FLOAT) {
					return CoreDatatype.XSD.FLOAT;
				}
				if (ld == CoreDatatype.XSD.DECIMAL || rd == CoreDatatype.XSD.DECIMAL) {
					return CoreDatatype.XSD.DECIMAL;
				}
				return CoreDatatype.XSD.INTEGER;
			}
			if (!strict && ld.isCalendarDatatype() && rd.isCalendarDatatype()) {
				return CoreDatatype.XSD.DATETIME;
			}
			if (!strict && ld.isDurationDatatype() && rd.isDurationDatatype()) {
				return CoreDatatype.XSD.DURATION;
			}
		}
		return null;
	}

	public static boolean isPlainLiteral(Value v) {
		return v.isLiteral() && isPlainLiteral((Literal) v);
	}

	public static boolean isPlainLiteral(Literal l) {
		assert l.getLanguage().isEmpty() || l.getCoreDatatype() == CoreDatatype.RDF.LANGSTRING;
		return l.getCoreDatatype() == CoreDatatype.XSD.STRING ||
				l.getCoreDatatype() == CoreDatatype.RDF.LANGSTRING;
	}

	public static boolean isSimpleLiteral(Value v) {
		return v.isLiteral() && isSimpleLiteral((Literal) v);
	}

	public static boolean isSimpleLiteral(Literal l) {
		return l.getCoreDatatype() == CoreDatatype.XSD.STRING && !Literals.isLanguageLiteral(l);
	}

	public static boolean isSimpleLiteral(boolean lang, CoreDatatype dt) {
		return !lang && dt == CoreDatatype.XSD.STRING;
	}

	public static boolean isStringLiteral(Value v) {
		return v.isLiteral() && isStringLiteral((Literal) v);
	}

	public static boolean isStringLiteral(Literal l) {
		return l.getCoreDatatype() == CoreDatatype.XSD.STRING || Literals.isLanguageLiteral(l);
	}

	private static boolean isSupportedDatatype(CoreDatatype.XSD dt) {
		return dt != null && (dt == CoreDatatype.XSD.STRING || dt.isNumericDatatype() || dt.isCalendarDatatype());
	}

	/**
	 * Checks whether the supplied two literal arguments are 'argument compatible' according to the SPARQL definition.
	 *
	 * @param arg1 the first argument
	 * @param arg2 the second argument
	 * @return true iff the two supplied arguments are argument compatible, false otherwise
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#func-arg-compatibility">SPARQL Argument Compatibility
	 *      Rules</a>
	 */
	public static boolean compatibleArguments(Literal arg1, Literal arg2) {
		// 1. The arguments are literals typed as CoreDatatype.XSD:string
		// 2. The arguments are language literals with identical language tags
		// 3. The first argument is a language literal and the second
		// argument is a literal typed as CoreDatatype.XSD:string

		return isSimpleLiteral(arg1) && isSimpleLiteral(arg2)
				|| Literals.isLanguageLiteral(arg1) && Literals.isLanguageLiteral(arg2)
						&& arg1.getLanguage().equals(arg2.getLanguage())
				|| Literals.isLanguageLiteral(arg1) && isSimpleLiteral(arg2);
	}
}
