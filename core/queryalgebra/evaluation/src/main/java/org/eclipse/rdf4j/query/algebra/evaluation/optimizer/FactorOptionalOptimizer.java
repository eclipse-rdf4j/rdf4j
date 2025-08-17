package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.*;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.*;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.TupleExprs;

/**
 * Query optimizer that factors nested OPTIONALs of the form LeftJoin( LeftJoin(X, R1), R2 ) where R2 ≈ R1' ⋈ D into
 * LeftJoin( X, LeftJoin(R1', D) )
 *
 * Preconditions: - both LeftJoin nodes have no join condition - R1 and R2 are Basic Graph Patterns (BGPs): only
 * StatementPattern + Join - R1 is homomorphically contained in R2 (var->var and var->const allowed)
 *
 * See: RDF4J algebra (LeftJoin, Join, StatementPattern), QueryOptimizer SPI.
 */
public final class FactorOptionalOptimizer implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		boolean changed;
		// apply to fixpoint (conservative: we only rewrite when we can prove safety)
		do {
			Rewriter v = new Rewriter();
			tupleExpr.visit(v);
			changed = v.changed();
		} while (changed);
	}

	// -------- rewriter --------

	private static final class Rewriter extends AbstractQueryModelVisitor<RuntimeException> {
		private boolean changed = false;

		boolean changed() {
			return changed;
		}

		@Override
		public void meet(LeftJoin outer) {
			// rewrite children first (bottom-up)
			super.meet(outer);

			if (outer.hasCondition())
				return;
			TupleExpr left = outer.getLeftArg();
			TupleExpr right = outer.getRightArg();

			if (!(left instanceof LeftJoin))
				return;
			LeftJoin inner = (LeftJoin) left;
			if (inner.hasCondition())
				return;

			TupleExpr X = inner.getLeftArg();
			TupleExpr R1 = inner.getRightArg();
			TupleExpr R2 = right;

			// collect BGP atoms and check support
			Optional<BGP> oR1 = BGP.from(R1);
			Optional<BGP> oR2 = BGP.from(R2);
			if (oR1.isEmpty() || oR2.isEmpty())
				return;

			BGP b1 = oR1.get();
			BGP b2 = oR2.get();

			// compute a homomorphism (R1 -> R2)
			Optional<Unifier> unifier = Unifier.find(b1.atoms, b2.atoms);
			if (unifier.isEmpty())
				return;

			Unifier u = unifier.get();

			// compute R1' = alpha-rename variables of R1 to match R2 (only var->var)
			Map<String, String> var2var = u.varToVarMapping();
			TupleExpr R1prime = R1.clone();
			if (!var2var.isEmpty()) {
				VarRenamer.rename(R1prime, var2var);
			}

			// compute D = R2 \ R1' (as atoms); build a TupleExpr for D
			// We use triple keys so var/const identity matches exactly.
			Set<AtomKey> r1pKeys = AtomKey.keysOf(BGP.from(R1prime).get().atoms);
			List<StatementPattern> dAtoms = new ArrayList<>();
			for (StatementPattern sp : b2.atoms) {
				AtomKey k = AtomKey.of(sp);
				if (!r1pKeys.remove(k)) { // r1pKeys is a multiset emulated by remove-first
					dAtoms.add((StatementPattern) sp.clone());
				}
			}
			TupleExpr D = joinOf(dAtoms);

			// if D is empty, we can simply use R1'
			TupleExpr rightNew = (D == null) ? R1prime : new LeftJoin(R1prime, D);

			// Build the final replacement: LeftJoin(X, rightNew)
			LeftJoin replacement = new LeftJoin(X, rightNew);

			// Replace the outer LJ with the new one
			outer.replaceWith(replacement);
			changed = true;
		}
	}

	// -------- utilities --------

	/**
	 * A basic graph pattern: just StatementPattern and Join nodes.
	 */
	private static final class BGP {
		final List<StatementPattern> atoms;

		private BGP(List<StatementPattern> atoms) {
			this.atoms = atoms;
		}

		static Optional<BGP> from(TupleExpr t) {
			List<StatementPattern> out = new ArrayList<>();
			if (!collectBGP(t, out))
				return Optional.empty();
			return Optional.of(new BGP(out));
		}

		private static boolean collectBGP(TupleExpr t, List<StatementPattern> out) {
			if (t instanceof StatementPattern) {
				out.add((StatementPattern) t);
				return true;
			}
			if (t instanceof Join) {
				Join j = (Join) t;
				return collectBGP(j.getLeftArg(), out) && collectBGP(j.getRightArg(), out);
			}
			// We only accept pure BGPs. Everything else is not handled by this optimizer.
			return false;
		}
	}

	/**
	 * Unifier from R1 atoms to R2 atoms (homomorphism), supports var->var and var->const.
	 */
	private static final class Unifier {
		// mapping from R1 var-name -> either var-name in R2 or a Value
		private final Map<String, String> var2var = new HashMap<>();
		private final Map<String, Value> var2const = new HashMap<>();

		Map<String, String> varToVarMapping() {
			return Collections.unmodifiableMap(var2var);
		}

		static Optional<Unifier> find(List<StatementPattern> r1, List<StatementPattern> r2) {
			Unifier u = new Unifier();
			boolean ok = backtrack(r1, r2, 0, new boolean[r2.size()], u);
			return ok ? Optional.of(u) : Optional.empty();
		}

		private static boolean backtrack(List<StatementPattern> r1, List<StatementPattern> r2,
				int idx, boolean[] used, Unifier u) {
			if (idx == r1.size())
				return true;

			StatementPattern sp1 = r1.get(idx);

			for (int j = 0; j < r2.size(); j++) {
				if (used[j])
					continue;
				StatementPattern sp2 = r2.get(j);
				// snapshot mappings for backtracking
				Map<String, String> var2varSnap = new HashMap<>(u.var2var);
				Map<String, Value> var2conSnap = new HashMap<>(u.var2const);
				if (unify(sp1.getSubjectVar(), sp2.getSubjectVar(), u) &&
						unify(sp1.getPredicateVar(), sp2.getPredicateVar(), u) &&
						unify(sp1.getObjectVar(), sp2.getObjectVar(), u) &&
						unify(sp1.getContextVar(), sp2.getContextVar(), u)) {
					used[j] = true;
					if (backtrack(r1, r2, idx + 1, used, u))
						return true;
					used[j] = false;
				}
				// restore
				u.var2var.clear();
				u.var2var.putAll(var2varSnap);
				u.var2const.clear();
				u.var2const.putAll(var2conSnap);
			}
			return false;
		}

		private static boolean unify(Var v1, Var v2, Unifier u) {
			if (v1 == null && v2 == null)
				return true;
			if (v1 == null || v2 == null)
				return false;

			boolean c1 = v1.hasValue();
			boolean c2 = v2.hasValue();

			if (c1 && c2) {
				return v1.getValue().equals(v2.getValue());
			} else if (c1) {
				// R1 constant must match exactly a constant in R2
				return false;
			} else {
				// v1 is a variable
				String n1 = v1.getName();
				if (u.var2var.containsKey(n1)) {
					if (c2)
						return false; // mapped to var earlier, now const -> mismatch
					return u.var2var.get(n1).equals(v2.getName());
				}
				if (u.var2const.containsKey(n1)) {
					if (!c2)
						return false; // mapped to const earlier, now var -> mismatch
					return u.var2const.get(n1).equals(v2.getValue());
				}
				// first time we see n1: bind to var or const
				if (c2) {
					u.var2const.put(n1, v2.getValue());
				} else {
					u.var2var.put(n1, v2.getName());
				}
				return true;
			}
		}
	}

	/**
	 * Variable renamer: applies old->new to Var nodes (ignores constants).
	 */
	private static final class VarRenamer extends AbstractQueryModelVisitor<RuntimeException> {
		private final Map<String, String> rename;

		private VarRenamer(Map<String, String> rename) {
			this.rename = rename;
		}

		static void rename(TupleExpr t, Map<String, String> rename) {
			new VarRenamer(rename).meetNode(t);
		}

		@Override
		public void meet(Var var) {
			if (!var.hasValue()) {
				String n = var.getName();
				String nn = rename.get(n);
				if (nn != null && !nn.equals(n)) {
					Var var1 = new Var(nn, var.getValue(), var.isAnonymous(), var.isConstant());
					var.replaceWith(var1);
				}
			}
		}
	}

	/**
	 * AtomKey: structural identity of a StatementPattern (var names and constants). Used to compute D = R2 \ R1'.
	 */
	private static final class AtomKey {
		final String s, p, o, c;

		private AtomKey(String s, String p, String o, String c) {
			this.s = s;
			this.p = p;
			this.o = o;
			this.c = c;
		}

		static AtomKey of(StatementPattern sp) {
			return new AtomKey(term(sp.getSubjectVar()),
					term(sp.getPredicateVar()),
					term(sp.getObjectVar()),
					term(sp.getContextVar()));
		}

		static Set<AtomKey> keysOf(List<StatementPattern> atoms) {
			// emulate multiset: we store counts by keeping duplicates in a list-backed set
			// A simple trick: use a LinkedList + remove-first to track multiplicity.
			// But we need O(1) membership; we’ll just store as a LinkedList-backed HashMap<AtomKey, Integer>.
			Map<AtomKey, Integer> mult = new HashMap<>();
			for (StatementPattern sp : atoms) {
				AtomKey k = of(sp);
				mult.put(k, mult.getOrDefault(k, 0) + 1);
			}
			return new Multiset(mult);
		}

		private static String term(Var v) {
			if (v == null)
				return "_"; // no context
			if (v.hasValue())
				return "v:" + v.getValue().toString();
			return "?" + v.getName();
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof AtomKey))
				return false;
			AtomKey k = (AtomKey) o;
			return s.equals(k.s) && p.equals(k.p) && o.equals(k.o) && c.equals(k.c);
		}

		@Override
		public int hashCode() {
			return Objects.hash(s, p, o, c);
		}

		// Simple multiset wrapper that supports remove-first semantics.
		private static final class Multiset extends AbstractSet<AtomKey> {
			private final Map<AtomKey, Integer> m;

			Multiset(Map<AtomKey, Integer> m) {
				this.m = m;
			}

			@Override
			public boolean contains(Object o) {
				return m.getOrDefault(o, 0) > 0;
			}

			@Override
			public boolean remove(Object o) {
				Integer cnt = m.get(o);
				if (cnt == null || cnt == 0)
					return false;
				if (cnt == 1)
					m.remove(o);
				else
					m.put((AtomKey) o, cnt - 1);
				return true;
			}

			@Override
			public Iterator<AtomKey> iterator() {
				return m.keySet().iterator();
			}

			@Override
			public int size() {
				int n = 0;
				for (Integer i : m.values())
					n += i;
				return n;
			}
		}
	}

	/** Build a left‑deep Join tree from a list of statement patterns, or return null if empty. */
	private static TupleExpr joinOf(List<StatementPattern> atoms) {
		if (atoms.isEmpty())
			return null;
		Iterator<StatementPattern> it = atoms.iterator();
		TupleExpr t = it.next();
		while (it.hasNext()) {
			t = new Join(t, it.next());
		}
		return t;
	}
}
