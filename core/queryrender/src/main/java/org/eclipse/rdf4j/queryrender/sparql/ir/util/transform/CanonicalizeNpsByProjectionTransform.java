/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.ir.util.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrExists;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrFilter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNot;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrProjectionItem;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;

/**
 * Canonicalize orientation of bare negated property set path triples ("!(...)") using SELECT projection order when
 * available: prefer the endpoint that appears earlier in the projection list as the subject. If only one endpoint
 * appears in the projection, prefer that endpoint as subject. Do not flip when either endpoint is an internal
 * _anon_path_* bridge var. Path text is inverted member-wise when flipped to preserve semantics.
 */
public final class CanonicalizeNpsByProjectionTransform extends BaseTransform {

	private CanonicalizeNpsByProjectionTransform() {
	}

	public static IrBGP apply(IrBGP bgp, IrSelect select) {
		if (bgp == null) {
			return null;
		}
		// Build projection order map: varName -> index (lower is earlier)
		final Map<String, Integer> projIndex = new HashMap<>();
		if (select != null && select.getProjection() != null) {
			List<IrProjectionItem> items = select.getProjection();
			for (int i = 0; i < items.size(); i++) {
				IrProjectionItem it = items.get(i);
				if (it != null && it.getVarName() != null && !it.getVarName().isEmpty()) {
					projIndex.putIfAbsent(it.getVarName(), i);
				}
			}
		}

		List<IrNode> out = new ArrayList<>();
		for (IrNode n : bgp.getLines()) {
			IrNode m = n;
			if (n instanceof IrPathTriple) {
				IrPathTriple pt = (IrPathTriple) n;
				String path = pt.getPathText();
				if (path != null) {
					String t = path.trim();
					if (t.startsWith("!(") && t.endsWith(")")) {
						Var s = pt.getSubject();
						Var o = pt.getObject();
						// Only flip when both are user vars (non-constants) and not anon path bridges
						if (s != null && o != null && !s.hasValue() && !o.hasValue()
								&& !isAnonPathVar(s) && !isAnonPathVar(o)) {
							String sName = s.getName();
							String oName = o.getName();
							Integer si = sName == null ? null : projIndex.get(sName);
							Integer oi = oName == null ? null : projIndex.get(oName);
							boolean flip = false;
							if (si != null && oi != null) {
								// Flip when the current subject appears later than the object in projection
								flip = si > oi;
							} else if (si == null && oi != null) {
								// Only object is projected: prefer it as subject
								flip = true;
							} else {
								flip = false; // keep as-is when neither or only subject is projected
							}
							if (flip) {
								String inv = invertNegatedPropertySet(t);
								if (inv != null) {
									m = new IrPathTriple(o, inv, s);
								}
							}
						}
					}
				}
			} else if (n instanceof IrGraph) {
				IrGraph g = (IrGraph) n;
				m = new IrGraph(g.getGraph(), apply(g.getWhere(), select));
			} else if (n instanceof IrOptional) {
				IrOptional o = (IrOptional) n;
				IrOptional no = new IrOptional(apply(o.getWhere(), select));
				no.setNewScope(o.isNewScope());
				m = no;
			} else if (n instanceof IrMinus) {
				IrMinus mi = (IrMinus) n;
				m = new IrMinus(apply(mi.getWhere(), select));
			} else if (n instanceof IrUnion) {
				// Do not alter orientation inside UNION branches; preserve branch subjects/objects.
				m = n;
			} else if (n instanceof IrFilter) {
				// Descend into FILTER EXISTS / NOT EXISTS bodies to canonicalize inner NPS orientation
				IrFilter f = (IrFilter) n;
				if (f.getBody() instanceof IrExists) {
					IrExists ex = (IrExists) f.getBody();
					IrFilter nf = new IrFilter(new IrExists(apply(ex.getWhere(), select), ex.isNewScope()));
					nf.setNewScope(f.isNewScope());
					m = nf;
				} else if (f.getBody() instanceof IrNot && ((IrNot) f.getBody()).getInner() instanceof IrExists) {
					IrNot not = (IrNot) f.getBody();
					IrExists ex = (IrExists) not.getInner();
					IrFilter nf = new IrFilter(new IrNot(new IrExists(apply(ex.getWhere(), select), ex.isNewScope())));
					nf.setNewScope(f.isNewScope());
					m = nf;
				} else {
					m = n;
				}
			} else if (n instanceof IrService) {
				IrService s = (IrService) n;
				m = new IrService(s.getServiceRefText(), s.isSilent(), apply(s.getWhere(), select));
			} else if (n instanceof IrSubSelect) {
				// keep as-is
			}
			out.add(m);
		}
		IrBGP res = new IrBGP(bgp.isNewScope());
		out.forEach(res::add);
		res.setNewScope(bgp.isNewScope());
		return res;
	}
}
