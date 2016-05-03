/* 
 * Licensed to Aduna under one or more contributor license agreements.  
 * See the NOTICE.txt file distributed with this work for additional 
 * information regarding copyright ownership. 
 *
 * Aduna licenses this file to you under the terms of the Aduna BSD 
 * License (the "License"); you may not use this file except in compliance 
 * with the License. See the LICENSE.txt file distributed with this work 
 * for the full License.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.openrdf.query;

import java.util.Iterator;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;

/**
 * Abstract base class for {@link BindingSet} implementations, providing a.o.
 * consistent implementations of {@link BindingSet#equals(Object)} and
 * {@link BindingSet#hashCode()}.
 *
 * @author Jeen Broekstra
 */
public abstract class AbstractBindingSet implements BindingSet {

	private static final long serialVersionUID = -2594123329154106048L;

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other == null || !(other instanceof BindingSet)) {
			return false;
		}

		BindingSet that = (BindingSet)other;
		
		if (this.size() != that.size()) {
			return false;
		}

		// Compare other's bindings to own
		for (Binding binding : that) {
			Value ownValue = getValue(binding.getName());

			if (!binding.getValue().equals(ownValue)) {
				// Unequal bindings for this name
				return false;
			}
		}

		return true;
	}

	@Override
	public final int hashCode() {
		int hashCode = 0;

		for (Binding binding : this) {
			hashCode ^= binding.getName().hashCode() ^ binding.getValue().hashCode();
		}

		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(32 * size());

		sb.append('[');

		Iterator<Binding> iter = iterator();
		while (iter.hasNext()) {
			sb.append(iter.next().toString());
			if (iter.hasNext()) {
				sb.append(';');
			}
		}

		sb.append(']');

		return sb.toString();
	}

}
