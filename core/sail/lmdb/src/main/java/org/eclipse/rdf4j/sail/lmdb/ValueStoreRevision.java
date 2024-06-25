/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.Serializable;
import java.util.Objects;

import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * A {@link ValueStore ValueStore} revision for {@link LmdbValue LmdbValue} objects. For a cached value ID of a
 * LmdbValue to be valid, the revision object needs to be equal to the concerning ValueStore's revision object. The
 * ValueStore's revision object is changed whenever values are removed from it or IDs are changed.
 */
public interface ValueStoreRevision {
	abstract class Base implements ValueStoreRevision {
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof ValueStoreRevision)) {
				return false;
			}
			ValueStoreRevision other = (ValueStoreRevision) o;
			return getRevisionId() == other.getRevisionId() && Objects.equals(getValueStore(), other.getValueStore());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getValueStore(), getRevisionId());
		}
	}

	class Default extends Base implements Serializable {
		private static final long serialVersionUID = -2434063125560285009L;

		private static volatile long revisionIdCounter = 0;

		transient private final ValueStore valueStore;

		private final long revisionId = ++revisionIdCounter;

		public Default(ValueStore valueStore) {
			this.valueStore = valueStore;
		}

		public long getRevisionId() {
			return revisionId;
		}

		public ValueStore getValueStore() {
			return valueStore;
		}

		public boolean resolveValue(long id, LmdbValue value) {
			return valueStore.resolveValue(id, value);
		}
	}

	class Lazy extends Base implements Serializable {
		private static final long serialVersionUID = -2434063125560285009L;

		private final ValueStoreRevision revision;
		private final long revisionId;
		private final ValueStore valueStore;

		public Lazy(ValueStoreRevision revision) {
			this.revision = revision;
			this.revisionId = revision.getRevisionId();
			this.valueStore = revision.getValueStore();
		}

		@Override
		public long getRevisionId() {
			return revisionId;
		}

		@Override
		public ValueStore getValueStore() {
			return valueStore;
		}

		@Override
		public boolean resolveValue(long id, LmdbValue value) {
			if (valueStore.resolveValue(id, value)) {
				// set unwrapped version of revision
				value.setInternalID(id, revision);
				return true;
			}
			return false;
		}
	}

	long getRevisionId();

	ValueStore getValueStore();

	boolean resolveValue(long id, LmdbValue value);
}
