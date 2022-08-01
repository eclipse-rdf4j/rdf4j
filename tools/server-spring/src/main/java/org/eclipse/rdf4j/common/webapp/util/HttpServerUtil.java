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
package org.eclipse.rdf4j.common.webapp.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class HttpServerUtil {

	/**
	 * Extracts the MIME type from the specified content type string. This method parses the content type string and
	 * returns just the MIME type, ignoring any parameters that are included.
	 *
	 * @param contentType A content type string, e.g. <var>application/xml; charset=utf-8</var> .
	 * @return The MIME type part of the specified content type string, or <var>null</var> if the specified content type
	 *         string was <var>null</var>.
	 */
	public static String getMIMEType(String contentType) {
		if (contentType == null) {
			return null;
		}

		return HeaderElement.parse(contentType).getValue();
	}

	/**
	 * Selects from a set of MIME types, the MIME type that has the highest quality score when matched with the Accept
	 * headers in the supplied request.
	 *
	 * @param mimeTypes The set of available MIME types.
	 * @param request   The request to match the MIME types against.
	 * @return The MIME type that best matches the types that the client finds acceptable, or <var>null</var> in case no
	 *         acceptable MIME type could be found.
	 */
	public static String selectPreferredMIMEType(Iterator<String> mimeTypes, HttpServletRequest request) {
		List<HeaderElement> acceptElements = getHeaderElements(request, "Accept");

		if (acceptElements.isEmpty()) {
			// Client does not specify any requirements, return first MIME type
			// from the list
			if (mimeTypes.hasNext()) {
				return mimeTypes.next();
			} else {
				return null;
			}
		}

		String result = null;
		HeaderElement matchingAcceptType = null;

		double highestQuality = 0.0;

		while (mimeTypes.hasNext()) {
			String mimeType = mimeTypes.next();
			HeaderElement acceptType = matchAcceptHeader(mimeType, acceptElements);

			if (acceptType != null) {
				// quality defaults to 1.0
				double quality = 1.0;

				String qualityStr = acceptType.getParameterValue("q");
				if (qualityStr != null) {
					try {
						quality = Double.parseDouble(qualityStr);
					} catch (NumberFormatException e) {
						// Illegal quality value, assume it has a different meaning
						// and ignore it
					}
				}

				if (quality > highestQuality) {
					result = mimeType;
					matchingAcceptType = acceptType;
					highestQuality = quality;
				} else if (quality == highestQuality) {
					// found a match with equal quality preference. check if the
					// accept type is more specific
					// than the previous match.
					if (isMoreSpecificType(acceptType, matchingAcceptType)) {
						result = mimeType;
						matchingAcceptType = acceptType;
					}
				}
			}
		}

		return result;
	}

	/**
	 * Checks if the first supplied MIME type is more specific than the second supplied MIME type.
	 *
	 * @param leftMimeTypeElem
	 * @param rightMimeTypeElem
	 * @return true iff leftMimeTypeElem is a more specific MIME type spec than rightMimeTypeElem, false otherwise.
	 */
	private static boolean isMoreSpecificType(HeaderElement leftMimeTypeElem, HeaderElement rightMimeTypeElem) {

		String[] leftMimeType = splitMIMEType(leftMimeTypeElem.getValue());
		String[] rightMimeType = splitMIMEType(rightMimeTypeElem.getValue());

		if (rightMimeType != null) {
			if (rightMimeType[1].equals("*")) {
				if (!leftMimeType[1].equals("*")) {
					return true;
				}
			}
			if (rightMimeType[0].equals("*")) {
				if (!leftMimeType[0].equals("*")) {
					return true;
				}
			}

			return false;
		} else {
			return true;
		}
	}

	private static String[] splitMIMEType(String mimeTypeString) {
		int slashIdx = mimeTypeString.indexOf('/');
		if (slashIdx > 0) {
			String type = mimeTypeString.substring(0, slashIdx);
			String subType = mimeTypeString.substring(slashIdx + 1);
			return new String[] { type, subType };
		} else {
			// invalid mime type
			return null;
		}
	}

	/**
	 * Gets the elements of the request header with the specified name.
	 *
	 * @param request    The request to get the header from.
	 * @param headerName The name of the header to get the elements of.
	 * @return A List of {@link HeaderElement} objects.
	 */
	public static List<HeaderElement> getHeaderElements(HttpServletRequest request, String headerName) {
		List<HeaderElement> elemList = new ArrayList<>(8);

		@SuppressWarnings("unchecked")
		Enumeration<String> headerValues = request.getHeaders(headerName);
		while (headerValues.hasMoreElements()) {
			String value = headerValues.nextElement();

			List<String> subValues = splitHeaderString(value, ',');

			for (String subValue : subValues) {
				// Ignore any empty header elements
				subValue = subValue.trim();
				if (subValue.length() > 0) {
					elemList.add(HeaderElement.parse(subValue));
				}
			}
		}

		return elemList;
	}

	/**
	 * Splits the supplied string into sub parts using the specified splitChar as a separator, ignoring occurrences of
	 * this character inside quoted strings.
	 *
	 * @param s         The header string to split into sub parts.
	 * @param splitChar The character to use as separator.
	 * @return A <var>List</var> of <var>String</var>s.
	 */
	public static List<String> splitHeaderString(String s, char splitChar) {
		List<String> result = new ArrayList<>(8);

		boolean parsingQuotedString = false;
		int i, startIdx = 0;

		for (i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (c == splitChar && !parsingQuotedString) {
				result.add(s.substring(startIdx, i));
				startIdx = i + 1;
			} else if (c == '"') {
				parsingQuotedString = !parsingQuotedString;
			}
		}

		if (startIdx < s.length()) {
			result.add(s.substring(startIdx));
		}

		return result;
	}

	/**
	 * Tries to match the specified MIME type spec against the list of Accept header elements, returning the applicable
	 * header element if available.
	 *
	 * @param mimeTypeSpec   The MIME type to determine the quality for, e.g. "text/plain" or "application/xml;
	 *                       charset=utf-8".
	 * @param acceptElements A List of {@link HeaderElement} objects.
	 * @return The Accept header element that matches the MIME type spec most closely, or <var>null</var> if no such
	 *         header element could be found.
	 */
	public static HeaderElement matchAcceptHeader(String mimeTypeSpec, List<HeaderElement> acceptElements) {
		HeaderElement mimeTypeElem = HeaderElement.parse(mimeTypeSpec);

		while (mimeTypeElem != null) {
			for (HeaderElement acceptElem : acceptElements) {
				if (matchesAcceptHeader(mimeTypeElem, acceptElem)) {
					return acceptElem;
				}
			}

			// No match found, generalize the MIME type spec and try again
			mimeTypeElem = generalizeMIMEType(mimeTypeElem);
		}

		return null;
	}

	private static boolean matchesAcceptHeader(HeaderElement mimeTypeElem, HeaderElement acceptElem) {
		if (!mimeTypeElem.getValue().equals(acceptElem.getValue())) {
			return false;
		}

		// Values match, check parameters
		if (mimeTypeElem.getParameterCount() > acceptElem.getParameterCount()) {
			return false;
		}

		for (int i = 0; i < mimeTypeElem.getParameterCount(); i++) {
			if (!mimeTypeElem.getParameter(i).equals(acceptElem.getParameter(i))) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Generalizes a MIME type element. The following steps are taken for generalization:
	 * <ul>
	 * <li>If the MIME type element has one or more parameters, the last parameter is removed.
	 * <li>Otherwise, if the MIME type element's subtype is not equal to '*' then it is set to this value.
	 * <li>Otherwise, if the MIME type element's type is not equal to '*' then it is set to this value.
	 * <li>Otherwise, the MIME type is equal to "*&slash;*" and cannot be generalized any further; <var>null</var> is
	 * returned.
	 * </ul>
	 * <p>
	 * Example generalizations:
	 * </p>
	 * <table>
	 * <tr>
	 * <th>input</th>
	 * <th>result</th>
	 * </tr>
	 * <tr>
	 * <td>application/xml; charset=utf-8</td>
	 * <td>application/xml</td>
	 * </tr>
	 * <tr>
	 * <td>application/xml</td>
	 * <td>application/*</td>
	 * </tr>
	 * <tr>
	 * <td>application/*</td>
	 * <td>&slash;*</td>
	 * </tr>
	 * <tr>
	 * <td>&slash;*</td>
	 * <td><var>null</var></td>
	 * </tr>
	 * </table>
	 *
	 * @param mimeTypeElem The MIME type element that should be generalized.
	 * @return The generalized MIME type element, or <var>null</var> if it could not be generalized any further.
	 */
	private static HeaderElement generalizeMIMEType(HeaderElement mimeTypeElem) {
		int parameterCount = mimeTypeElem.getParameterCount();
		if (parameterCount > 0) {
			// remove last parameter
			mimeTypeElem.removeParameter(parameterCount - 1);
		} else {
			String mimeType = mimeTypeElem.getValue();

			int slashIdx = mimeType.indexOf('/');
			if (slashIdx > 0) {
				String type = mimeType.substring(0, slashIdx);
				String subType = mimeType.substring(slashIdx + 1);

				if (!subType.equals("*")) {
					// generalize subtype
					mimeTypeElem.setValue(type + "/*");
				} else if (!type.equals("*")) {
					// generalize type
					mimeTypeElem.setValue("*/*");
				} else {
					// Cannot generalize any further
					mimeTypeElem = null;
				}
			} else {
				// invalid MIME type
				mimeTypeElem = null;
			}
		}

		return mimeTypeElem;
	}

	/**
	 * Gets the trimmed value of a request parameter as a String.
	 *
	 * @return The trimmed value, or null if the parameter does not exist.
	 */
	public static String getPostDataParameter(Map<String, Object> formData, String name) {
		String result = null;

		try {
			Object param = formData.get(name);
			if (param instanceof String[]) {
				String[] paramArray = (String[]) param;
				if (paramArray.length > 0) {
					result = paramArray[0];
				}
			} else if (param instanceof String) {
				result = (String) param;
			}

			if (result != null) {
				result = result.trim();
			}
		} catch (ClassCastException cce) {
			// ignore, return null
		}

		return result;
	}

	/**
	 * @return true if the string is either null or equal to ""
	 */
	public static boolean isEmpty(String string) {
		boolean result = false;
		if (string == null || string.trim().isEmpty()) {
			result = true;
		}
		return result;
	}

	/**
	 * @return true if the string is !isEmpty and equal to "true"
	 */
	public static boolean isTrue(String string) {
		boolean result = false;
		if (!isEmpty(string) && (string.equalsIgnoreCase("true") || string.equalsIgnoreCase("on"))) {
			result = true;
		}
		return result;
	}

	/**
	 * @return true if the string is !isEmpty and equal to "false"
	 */
	public static boolean isFalse(String string) {
		boolean result = false;
		if (!isEmpty(string) && (string.equalsIgnoreCase("false") || string.equalsIgnoreCase("off"))) {
			result = true;
		}
		return result;
	}
}
