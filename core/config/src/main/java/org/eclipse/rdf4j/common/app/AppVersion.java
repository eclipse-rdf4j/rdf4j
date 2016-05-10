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
 * A product version in Aduna's version format (i.e. major.minor-modifier). Where major stands for the major
 * version number of the release, minor is the minor version number, and modifier is a modifier for the
 * release, e.g. beta1 or RC1. Combined, this results in versions like 2.0 and 4.1-beta1.
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
	 * The version's patch version number, if any.
	 */
	private int patch;

	/**
	 * The version's milestone number, if any.
	 */
	private int milestone;

	/**
	 * The version's modifier, if any.
	 */
	private String modifier;

	/**
	 * Construct an uninitialized AppVersion.
	 */
	public AppVersion() {
		this(-1, -1, -1, -1, null);
	}

	/**
	 * Creates a new <tt>major.minor</tt> version number, e.g. <tt>1.0</tt>.
	 */
	public AppVersion(int major, int minor) {
		this(major, minor, -1, -1, null);
	}

	/**
	 * Creates a new <tt>major.minor.patch</tt> version number, e.g. <tt>1.0.1</tt>.
	 */
	public AppVersion(int major, int minor, int patch) {
		this(major, minor, patch, -1, null);
	}

	/**
	 * Creates a new <tt>major.minor-modifier</tt> version number, e.g. <tt>1.0-beta1</tt>.
	 */
	public AppVersion(int major, int minor, String modifier) {
		this(major, minor, -1, -1, modifier);
	}

	/**
	 * Creates a new <tt>major.minor.patch-modifier</tt> version number, e.g. <tt>1.0.1-SNAPSHOT</tt>.
	 */
	public AppVersion(int major, int minor, int patch, String modifier) {
		this(major, minor, patch, -1, modifier);
	}

	/**
	 * Creates a new <tt>major.minor.patchMmilestone-modifier</tt> version number, e.g.
	 * <tt>1.0.1M1-SNAPSHOT</tt>.
	 */
	public AppVersion(int major, int minor, int patch, int milestone, String modifier) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.milestone = milestone;
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
	public int getPatch() {
		return patch;
	}

	public void setPatch(int micro) {
		this.patch = micro;
	}

	public void setMilestone(int milestone) {
		this.milestone = milestone;
	}

	public int getMilestone() {
		return milestone;
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

			isEqual = major == o.major && minor == o.minor && patch == o.patch && milestone == o.milestone;

			if (isEqual) {
				isEqual = modifier == o.modifier || modifier != null && modifier.equalsIgnoreCase(o.modifier);
			}
		}

		return isEqual;
	}

	@Override
	public int hashCode() {
		int hash = 31 * (31 * major + minor);

		if (patch > 0) {
			hash += patch;
		}

		if (milestone > 0) {
			hash += milestone;
		}

		if (modifier != null) {
			hash ^= modifier.toLowerCase(Locale.ENGLISH).hashCode();
		}

		return hash;
	}

	/**
	 * Checks if this version is older than the specified version, according to the result of
	 * {@link #compareTo(AppVersion)}.
	 */
	public boolean olderThan(AppVersion other) {
		return this.compareTo(other) < 0;
	}

	/**
	 * Checks if this version is newer than the specified version, according to the result of
	 * {@link #compareTo(AppVersion)}.
	 */
	public boolean newerThan(AppVersion other) {
		return this.compareTo(other) > 0;
	}

	/**
	 * Compares two version numbers according to their major, minor, patch and milestone version numbers,
	 * ordering from oldest to newest version. If all version numbers are equal then their modifiers are
	 * compared lexicographically (based on the Unicode value of each character), ignoring case. Versions
	 * without a modifier or milestone are considered to be the "final" versions and come after otherwise
	 * equal versions with a modifier or milestone.
	 * 
	 * @return <tt>0</tt> if both versions are equal, a negative number if this version is older than
	 *         <tt>other</tt>, or a positive number otherwise.
	 */
	public int compareTo(AppVersion other) {
		int result = major - other.major;

		if (result == 0) {
			result = minor - other.minor;
		}

		if (result == 0) {
			result = patch - other.patch;
		}

		if (result == 0 && (milestone > -1 || other.milestone > -1)) {
			if (milestone > -1) {
				if (other.milestone == -1) {
					result = -1;
				}
				else {
					result = milestone - other.milestone;
				}
			}
			else {
				if (other.milestone > -1) {
					result = 1;
				}
			}
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

		int minorSeparator = versionString.indexOf('.');
		int patchSeparator = versionString.indexOf('.', minorSeparator + 1);
		int milestoneSeparator = versionString.indexOf('M', Math.max(minorSeparator, patchSeparator));
		int modifierSeparator = versionString.indexOf('-', Math.max(minorSeparator, milestoneSeparator));

		if (minorSeparator == -1) {
			throw new NumberFormatException("Illegal version string: " + versionString);
		}

		final boolean hasPatch = patchSeparator > -1;
		final boolean hasMilestone = milestoneSeparator > -1;
		final boolean hasModifier = modifierSeparator > -1;

		String major = versionString.substring(0, minorSeparator);
		String minor = null;
		String patch = null;
		String milestone = null;
		String modifier = null;

		if (hasModifier) {
			modifier = versionString.substring(modifierSeparator + 1);
		}

		if (hasMilestone) {
			if (hasModifier) {
				milestone = versionString.substring(milestoneSeparator + 1, modifierSeparator);
			}
			else {
				milestone = versionString.substring(milestoneSeparator + 1);
			}
		}

		// determine patch and minor versions
		if (hasPatch) {
			if (hasMilestone) {
				patch = versionString.substring(patchSeparator + 1, milestoneSeparator);
			}
			else if (hasModifier) {
				patch = versionString.substring(patchSeparator + 1, modifierSeparator);
			}
			else {
				patch = versionString.substring(patchSeparator + 1);
			}
			minor = versionString.substring(minorSeparator + 1, patchSeparator);
		}
		else {
			if (hasMilestone) {
				minor = versionString.substring(minorSeparator + 1, milestoneSeparator);
			}
			else if (hasModifier) {
				minor = versionString.substring(minorSeparator + 1, modifierSeparator);
			}
			else {
				minor = versionString.substring(minorSeparator + 1);
			}

		}

		int majorInt = Integer.parseInt(major);
		int minorInt = Integer.parseInt(minor);
		int patchInt = patch == null ? -1 : Integer.parseInt(patch);
		int milestoneInt = milestone == null ? -1 : Integer.parseInt(milestone);
		return new AppVersion(majorInt, minorInt, patchInt, milestoneInt, modifier);
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

		if (patch >= 0) {
			sb.append('.').append(patch);
		}

		if (milestone >= 0) {
			sb.append('M').append(milestone);
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
