/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.app;

import java.util.Locale;

import org.eclipse.rdf4j.common.lang.ObjectUtil;

/**
 * A product version in Aduna's version format (i.e. major.minor-modifier).
 * Where major stands for the major version number of the release, minor is the
 * minor version number, and modifier is a modifier for the release, e.g. beta1
 * or RC1. Combined, this results in versions like 2.0 and 4.1-beta1.
 */
public class AppVersion implements Comparable<AppVersion> {

	/**
	 * The version's major version number.
	 */
	private int major;

	/**
	 * The version's minor version number.
	 */
	private int minor;

	/**
	 * The version's micro version number.
	 */
	private int micro;

	/**
	 * The version's modifier, if any.
	 */
	private String modifier;

	/**
	 * Construct an uninitialized AppVersion.
	 */
	public AppVersion() {
		this(-1, -1, -1, null);
	}

	/**
	 * Creates a new <tt>major.minor</tt> version number, e.g. <tt>1.0</tt>.
	 */
	public AppVersion(int major, int minor) {
		this(major, minor, -1, null);
	}

	/**
	 * Creates a new <tt>major.minor.micro</tt> version number, e.g.
	 * <tt>1.0.1</tt>.
	 */
	public AppVersion(int major, int minor, int micro) {
		this(major, minor, micro, null);
	}

	/**
	 * Creates a new <tt>major.minor-modifier</tt> version number, e.g.
	 * <tt>1.0-beta1</tt>.
	 */
	public AppVersion(int major, int minor, String modifier) {
		this(major, minor, -1, modifier);
	}

	/**
	 * Creates a new <tt>major.minor.micro-modifier</tt> version number, e.g.
	 * <tt>1.0.1-SNAPSHOT</tt>.
	 */
	public AppVersion(int major, int minor, int micro, String modifier) {
		this.major = major;
		this.minor = minor;
		this.micro = micro;
		this.modifier = modifier;
	}

	/**
	 * Gets the version's major version number.
	 */
	public int getMajor() {
		return major;
	}

	public void setMajor(int major) {
		this.major = major;
	}

	/**
	 * Gets the version's minor version number.
	 */
	public int getMinor() {
		return minor;
	}

	public void setMinor(int minor) {
		this.minor = minor;
	}

	/**
	 * Gets the version's micro version number.
	 */
	public int getMicro() {
		return micro;
	}

	public void setMicro(int micro) {
		this.micro = micro;
	}

	/**
	 * Gets the version's release modifier part.
	 */
	public String getModifier() {
		return modifier;
	}

	public void setModifier(String modifier) {
		this.modifier = modifier;
	}

	@Override
	public boolean equals(Object other) {
		boolean isEqual = false;

		if (other instanceof AppVersion) {
			AppVersion o = (AppVersion)other;

			isEqual = major == o.major && minor == o.minor && micro == o.micro;

			if (isEqual) {
				isEqual = modifier == o.modifier || modifier != null && modifier.equalsIgnoreCase(o.modifier);
			}
		}

		return isEqual;
	}

	@Override
	public int hashCode() {
		int hash = 31 * (31 * major + minor);

		if (micro > 0) {
			hash += micro;
		}

		if (modifier != null) {
			hash ^= modifier.toLowerCase(Locale.ENGLISH).hashCode();
		}

		return hash;
	}

	/**
	 * Checks if this version is older than the specified version, according to
	 * the result of {@link #compareTo(AppVersion)}.
	 */
	public boolean olderThan(AppVersion other) {
		return this.compareTo(other) < 0;
	}

	/**
	 * Checks if this version is newer than the specified version, according to
	 * the result of {@link #compareTo(AppVersion)}.
	 */
	public boolean newerThan(AppVersion other) {
		return this.compareTo(other) > 0;
	}

	/**
	 * Compares two version numbers according to their major, minor and micro
	 * version numbers, ordering from oldest to newests version. If all three
	 * version numbers are equal then their modifiers are compared
	 * lexicographically (based on the Unicode value of each character), ignoring
	 * case. Versions without a modifier are considered to be the "final"
	 * versions and come after otherwise equal versions with a modifier.
	 * 
	 * @return <tt>0</tt> if both versions are equal, a negative number if this
	 *         version is older than <tt>other</tt>, or a positive number
	 *         otherwise.
	 */
	public int compareTo(AppVersion other) {
		int result = major - other.major;

		if (result == 0) {
			result = minor - other.minor;
		}

		if (result == 0) {
			result = micro - other.micro;
		}

		if (result == 0 && !ObjectUtil.nullEquals(modifier, other.modifier)) {
			if (modifier == null) {
				result = 1;
			}
			else if (other.modifier == null) {
				result = -1;
			}
			else {
				result = modifier.compareToIgnoreCase(other.modifier);
			}
		}

		return result;
	}

	/**
	 * Parses a version string into a Version object.
	 * 
	 * @param versionString
	 *        A version string, e.g. 1.0.1 or 1.0-beta1.
	 * @return The parsed Version.
	 * @exception NumberFormatException
	 *            If versionString could not be parsed to a version.
	 */
	public static AppVersion parse(String versionString) {
		if (versionString.equals("dev")) {
			return new AppVersion(-1, -1, "dev");
		}

		int minorSeperator = versionString.indexOf('.');
		int microSeperator = versionString.indexOf('.', minorSeperator + 1);
		int modifierSeperator = versionString.indexOf('-', Math.max(minorSeperator, microSeperator));

		if (minorSeperator == -1) {
			throw new NumberFormatException("Illegal version string: " + versionString);
		}

		String major = versionString.substring(0, minorSeperator);
		String minor = null;
		String micro = null;
		String modifier = null;

		if (microSeperator == -1) {
			// Without micro version number
			if (modifierSeperator == -1) {
				minor = versionString.substring(minorSeperator + 1);
			}
			else {
				minor = versionString.substring(minorSeperator + 1, modifierSeperator);
				modifier = versionString.substring(modifierSeperator + 1);
			}
		}
		else {
			// With micro version number
			minor = versionString.substring(minorSeperator + 1, microSeperator);

			if (modifierSeperator == -1) {
				micro = versionString.substring(microSeperator + 1);
			}
			else {
				micro = versionString.substring(microSeperator + 1, modifierSeperator);
				modifier = versionString.substring(modifierSeperator + 1);
			}
		}

		int majorInt = Integer.parseInt(major);
		int minorInt = Integer.parseInt(minor);
		int microInt = micro == null ? -1 : Integer.parseInt(micro);
		return new AppVersion(majorInt, minorInt, microInt, modifier);
	}

	/**
	 * Returns the string represention of this version.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(16);

		if (major >= 0) {
			sb.append(major).append('.').append(minor);
		}

		if (micro >= 0) {
			sb.append('.').append(micro);
		}

		if (modifier != null) {
			if (sb.length() > 0) {
				sb.append('-');
			}
			sb.append(modifier);
		}

		return sb.toString();
	}
}
