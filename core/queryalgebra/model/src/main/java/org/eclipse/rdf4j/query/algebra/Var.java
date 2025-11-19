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
package org.eclipse.rdf4j.query.algebra;

import java.util.Objects;
import java.util.ServiceLoader;

import org.eclipse.rdf4j.model.Value;

/**
 * A variable that can contain a Value.
 *
 * <p>
 * <strong>Service Provider–based construction:</strong> Prefer the {@code Var.of(...)} static factory methods over
 * direct constructors. These factories delegate to a {@link Var.Provider} discovered via {@link ServiceLoader} or
 * selected via the {@link #PROVIDER_PROPERTY} system property. This allows third-party libraries to supply custom
 * {@code Var} subclasses without changing call sites. If no provider is found, construction falls back to
 * {@code new Var(...)}.
 * </p>
 *
 * <p>
 * To install a provider, add a file {@code META-INF/services/org.eclipse.rdf4j.query.algebra.Var$Provider} containing
 * the implementing class name, or set system property {@link #PROVIDER_PROPERTY} to a specific provider FQCN.
 * </p>
 *
 * @implNote In the future this class may stop extending AbstractQueryModelNode in favor of directly implementing
 *           ValueExpr and QueryModelNode.
 */
public class Var extends AbstractQueryModelNode implements ValueExpr {

	/**
	 * System property that, when set to a fully qualified class name implementing {@link Var.Provider}, selects that
	 * provider. If absent, the first provider discovered by {@link ServiceLoader} is used; if none are found, a default
	 * provider that constructs {@code Var} directly is used.
	 */
	public static final String PROVIDER_PROPERTY = "org.eclipse.rdf4j.query.algebra.Var.provider";

	private final String name;

	private final Value value;

	private final boolean anonymous;

	private boolean constant = false;

	private int cachedHashCode = 0;

	/*
	 * ========================= Static factory entry points =========================
	 */

	/**
	 * Factory mirroring {@link #Var(String)}.
	 */
	public static Var of(String name) {
		return Holder.PROVIDER.newVar(name, null, false, false);
	}

	/**
	 * Factory mirroring {@link #Var(String, boolean)}.
	 */
	public static Var of(String name, boolean anonymous) {
		return Holder.PROVIDER.newVar(name, null, anonymous, false);
	}

	/**
	 * Factory mirroring {@link #Var(String, Value)}.
	 */
	public static Var of(String name, Value value) {
		return Holder.PROVIDER.newVar(name, value, false, false);
	}

	/**
	 * Factory mirroring {@link #Var(String, Value, boolean)}.
	 */
	public static Var of(String name, Value value, boolean anonymous) {
		return Holder.PROVIDER.newVar(name, value, anonymous, false);
	}

	/**
	 * Factory mirroring {@link #Var(String, Value, boolean, boolean)}.
	 */
	public static Var of(String name, Value value, boolean anonymous, boolean constant) {
		return Holder.PROVIDER.newVar(name, value, anonymous, constant);
	}

	/*
	 * ========================= Constructors (existing API) =========================
	 */

	/**
	 * @deprecated since 5.1.5, use {@link #of(String, Value, boolean, boolean)} instead.
	 * @param name
	 * @param value
	 * @param anonymous
	 * @param constant
	 */
	@Deprecated(since = "5.1.5", forRemoval = true)
	public Var(String name, Value value, boolean anonymous, boolean constant) {
		this.name = name;
		this.value = value;
		this.anonymous = anonymous;
		this.constant = constant;

	}

	/**
	 * @deprecated since 5.1.5, use {@link #of(String)} instead.
	 * @param name
	 */
	@Deprecated(since = "5.1.5", forRemoval = true)
	public Var(String name) {
		this(name, null, false, false);
	}

	/**
	 * @deprecated since 5.1.5, use {@link #of(String, boolean)} instead.
	 * @param name
	 * @param anonymous
	 */
	@Deprecated(since = "5.1.5", forRemoval = true)
	public Var(String name, boolean anonymous) {
		this(name, null, anonymous, false);
	}

	/**
	 * @deprecated since 5.1.5, use {@link #of(String, Value)} instead.
	 * @param name
	 * @param value
	 */
	@Deprecated(since = "5.1.5", forRemoval = true)
	public Var(String name, Value value) {
		this(name, value, false, false);
	}

	/**
	 * @deprecated since 5.1.5, use {@link #of(String, Value, boolean)} instead.
	 * @param name
	 * @param value
	 * @param anonymous
	 */
	@Deprecated(since = "5.1.5", forRemoval = true)
	public Var(String name, Value value, boolean anonymous) {
		this(name, value, anonymous, false);
	}

	/*
	 * ========================= Service Provider Interface (SPI) =========================
	 */

	/**
	 * Service Provider Interface for globally controlling {@link Var} instantiation.
	 *
	 * <p>
	 * Implementations may return custom subclasses of {@code Var}. Implementations should be registered via
	 * {@code META-INF/services/org.eclipse.rdf4j.query.algebra.Var$Provider} or selected with
	 * {@link #PROVIDER_PROPERTY}.
	 * </p>
	 *
	 * <p>
	 * <strong>Important:</strong> Implementations must not call {@code Var.of(...)} from within
	 * {@link #newVar(String, Value, boolean, boolean)} to avoid infinite recursion. Call a constructor directly (e.g.,
	 * {@code return new CustomVar(...); }).
	 * </p>
	 */
	@FunctionalInterface
	public interface Provider {
		/**
		 * Mirror of the primary 4-argument {@link Var} constructor.
		 */
		Var newVar(String name, Value value, boolean anonymous, boolean constant);
	}

	public boolean isAnonymous() {
		return anonymous;
	}

	public String getName() {
		return name;
	}

	public boolean hasValue() {
		return value != null;
	}

	public Value getValue() {
		return value;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		// no-op
	}

	@Override
	public void setParentNode(QueryModelNode parent) {
		assert getParentNode() == null;
		super.setParentNode(parent);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {

	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(64);

		sb.append(this.getClass().getSimpleName());

		sb.append(" (name=").append(name);

		if (value != null) {
			sb.append(", value=").append(value);
		}

		if (anonymous) {
			sb.append(", anonymous");
		}

		sb.append(")");

		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Var)) {
			return false;
		}
		Var var = (Var) o;

		if (cachedHashCode != 0 && var.cachedHashCode != 0 && cachedHashCode != var.cachedHashCode) {
			return false;
		}

		return anonymous == var.anonymous && !(name == null && var.name != null || value == null && var.value != null)
				&& Objects.equals(name, var.name) && Objects.equals(value, var.value);
	}

	@Override
	public int hashCode() {
		if (cachedHashCode == 0) {
			int result = 1;
			result = 31 * result + (name == null ? 0 : name.hashCode());
			result = 31 * result + (value == null ? 0 : value.hashCode());
			result = 31 * result + Boolean.hashCode(anonymous);
			cachedHashCode = result;
		}
		return cachedHashCode;
	}

	@Override
	public Var clone() {
		Var var = Var.of(name, value, anonymous, constant);
		var.setVariableScopeChange(this.isVariableScopeChange());
		return var;
	}

	/**
	 * @return Returns the constant.
	 */
	public boolean isConstant() {
		return constant;
	}

	private static final class Holder {
		private static final Provider DEFAULT = Var::new;

		static final Provider PROVIDER = initProvider();

		private static Provider initProvider() {
			// 1) Explicit override via system property (FQCN of Var.Provider)
			String fqcn = null;
			try {
				fqcn = System.getProperty(PROVIDER_PROPERTY);
			} catch (SecurityException se) {
				// Restricted environments may deny property access; ignore and fall back to discovery/default.
			}
			if (fqcn != null && !fqcn.isEmpty()) {
				try {
					Class<?> cls = Class.forName(fqcn, true, Var.class.getClassLoader());
					if (Provider.class.isAssignableFrom(cls)) {
						@SuppressWarnings("unchecked")
						Class<? extends Provider> pcls = (Class<? extends Provider>) cls;
						return pcls.getDeclaredConstructor().newInstance();
					}
					// Fall through to discovery if class does not implement Provider
				} catch (Throwable t) {
					// Swallow and fall back to discovery; avoid linking to any logging framework here.
				}
			}

			// 2) ServiceLoader discovery: pick the first provider found
			try {
				ServiceLoader<Provider> loader = ServiceLoader.load(Provider.class);
				for (Provider p : loader) {
					return p; // first one wins
				}
			} catch (Throwable t) {
				// ignore and fall back
			}

			// 3) Fallback: direct construction
			return DEFAULT;
		}
	}

}
