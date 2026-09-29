#!/usr/bin/env python3
"""Seeded differential corpus generator (WP9 3.11.6 Layer 3).

Usage: gen.py <seed> <ncases> [queries-per-case]

Emits datasets of 2-5 triples (TriG, optional named graph :g) with N queries each plus one write
(DELETE DATA + INSERT DATA). Format consumed by DiffRunner.java and JenaRunner.java:

### CASE <id>
### DATA
<trig>
### QUERY <idx> [flags]
<sparql>
### UPDATE
<sparql update>
### END
"""
import random
import sys

PFX = "PREFIX : <http://ex.org/>\nPREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
TRIG_PFX = "@prefix : <http://ex.org/> .\n@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
IRIS = [":a", ":b", ":c", ":d"]
PREDS = [":p", ":p", ":q", ":r"]
LITS = ['"1"^^xsd:integer', '"01"^^xsd:integer', '"1"^^xsd:int', '"01"^^xsd:int', '1.0', '"1.0e0"^^xsd:double',
        '"2"^^xsd:integer', '"a"', '"a"@en', '"a"@EN', '"a"^^xsd:string', '"b"', '"2020-01-01"^^xsd:date',
        '"2020-01-01T00:00:00Z"^^xsd:dateTime', '"true"^^xsd:boolean']
VARS = ["?s", "?o", "?x", "?y", "?z"]


class Gen:
    def __init__(self, rnd):
        self.r = rnd
        self.alias = 0
        self.data = []

    def pick(self, xs):
        return self.r.choice(xs)

    def chance(self, p):
        return self.r.random() < p

    # ---------------- data ----------------
    def term_obj(self):
        if self.chance(0.5):
            return self.pick(IRIS[:3]) if self.chance(0.8) else ":d"
        return self.pick(LITS)

    def dataset(self):
        n = self.r.randint(2, 5)
        triples = set()
        while len(triples) < n:
            triples.add((self.pick(IRIS[:3]), self.pick(PREDS), self.term_obj()))
        triples = sorted(triples)
        named = []
        if self.chance(0.25) and len(triples) > 2:
            named = [triples.pop()]
        return triples, named

    def trig(self, triples, named):
        out = [TRIG_PFX]
        for s, p, o in triples:
            out.append(f"{s} {p} {o} .\n")
        if named:
            out.append(":g {\n")
            for s, p, o in named:
                out.append(f"  {s} {p} {o} .\n")
            out.append("}\n")
        return "".join(out)

    def update(self, triples, named):
        allt = triples + named
        dele = self.pick(allt)
        ins = set()
        for _ in range(self.r.randint(1, 2)):
            ins.add((self.pick(IRIS[:3]), self.pick(PREDS), self.term_obj()))
        ins -= set(allt)
        parts = []
        if dele in named:
            parts.append(f"DELETE DATA {{ GRAPH :g {{ {dele[0]} {dele[1]} {dele[2]} }} }}")
        else:
            parts.append(f"DELETE DATA {{ {dele[0]} {dele[1]} {dele[2]} }}")
        if ins:
            body = " ".join(f"{s} {p} {o} ." for s, p, o in sorted(ins))
            parts.append(f"INSERT DATA {{ {body} }}")
        return PFX + " ;\n".join(parts)

    # ---------------- expressions ----------------
    def const(self):
        if self.data and self.chance(0.7):
            t = self.pick(self.data)
            return t[2] if self.chance(0.7) else t[0]
        return self.pick(IRIS) if self.chance(0.35) else self.pick(LITS)

    def expr(self, scope, depth=0):
        vs = sorted(scope) or ["?s"]
        v = self.pick(vs)
        w = self.pick(vs + ["?y"])
        k = self.r.randint(0, 17)
        if k == 0:
            return f"{v} = {self.const()}"
        if k == 1:
            return f"{v} != {self.const()}"
        if k == 2:
            return f"sameTerm({v}, {w})"
        if k == 3:
            return f"sameTerm({v}, {self.const()})"
        if k == 4:
            return f"BOUND({v})"
        if k == 5:
            return f"!BOUND({v})"
        if k == 6:
            return f"isLiteral({v})"
        if k == 7:
            return f"isIRI({v})"
        if k == 8:
            return f'langMatches(lang({v}), "en")'
        if k == 9:
            return f"datatype({v}) = xsd:integer"
        if k == 10:
            return f'str({v}) = "1"'
        if k == 11:
            return f"{v} = {w}"
        if k == 12:
            return f"{v} < 2"
        if k == 13:
            return f"COALESCE({v}, {self.const()}) = {self.const()}"
        if k == 14:
            return f"{v} + 1 = 2"
        if k == 15:
            return f"{v} IN ({self.const()}, {self.const()})"
        if k == 16 and depth < 1:
            return f"({self.expr(scope, depth + 1)} || {self.expr(scope, depth + 1)})"
        if depth < 1:
            return f"({self.expr(scope, depth + 1)} && {self.expr(scope, depth + 1)})"
        return f"BOUND({v})"

    def bind_expr(self, scope):
        v = self.pick(sorted(scope) or ["?s"])
        return self.pick([f"str({v})", f'IF(BOUND({v}), "Y", "N")', f'COALESCE({v}, "d")', f"{v} + 1",
                          f"lang({v})", f'CONCAT(str({v}), "x")', f"datatype({v})"])

    # ---------------- patterns ----------------
    def node(self, scope, subj):
        if self.chance(0.8):
            if scope and self.chance(0.6):
                return self.pick(sorted(scope))
            return self.pick(VARS)
        return self.pick(IRIS[:3]) if subj else self.const()

    def pred(self):
        if self.chance(0.12):
            return self.pick([":p*", ":p+", ":p?", ":p/:q", "^:p", "(:p|:q)", "!(:p)", "(:p/:p)*", ":q*"])
        if self.data and self.chance(0.8):
            return self.pick(self.data)[1]
        return self.pick(PREDS)

    def triple(self, scope):
        s = self.node(scope, True)
        o = self.node(scope, False)
        return f"{s} {self.pred()} {o} .", {x for x in (s, o) if x.startswith("?")}

    def values(self):
        vs = self.r.sample(VARS, self.r.randint(1, 2))
        rows = []
        for _ in range(self.r.randint(1, 3)):
            rows.append("(" + " ".join("UNDEF" if self.chance(0.25) else self.const() for _ in vs) + ")")
        return f"VALUES ({' '.join(vs)}) {{ {' '.join(rows)} }}", set(vs)

    def group(self, scope, depth):
        """Returns (text, vars bound by the group). scope = outer in-scope vars (for correlated filters)."""
        local = set()
        parts = []
        t, vs = self.triple(scope | local)
        parts.append(t)
        local |= vs
        for _ in range(self.r.randint(1, 3)):
            k = self.r.randint(0, 13) if depth < 3 else 0
            if k in (0, 1, 13):
                t, vs = self.triple(scope | local)
                parts.append(t)
                local |= vs
            elif k == 2:
                g, vs = self.group(scope | local, depth + 1)
                if self.chance(0.35):
                    g = g[:-1] + f"FILTER({self.expr(local | vs | scope)}) }}"
                parts.append(f"OPTIONAL {g}")
                local |= vs
            elif k == 3:
                g, _ = self.group(local, depth + 1)
                parts.append(f"MINUS {g}")
            elif k in (4, 5):
                g, _ = self.group(local, depth + 1)
                neg = "NOT " if self.chance(0.5) else ""
                if self.chance(0.2):
                    parts.append(f"FILTER({self.expr(local | scope)} || {neg}EXISTS {g})")
                else:
                    parts.append(f"FILTER {neg}EXISTS {g}")
            elif k == 6:
                parts.append(f"FILTER({self.expr(local | scope)})")
            elif k == 7:
                cand = [v for v in VARS + ["?w", "?b"] if v not in local]
                if cand:
                    nv = self.pick(cand)
                    if self.chance(0.2):
                        g, _ = self.group(local, depth + 1)
                        parts.append(f"BIND(EXISTS {g} AS {nv})")
                    else:
                        parts.append(f"BIND({self.bind_expr(local | scope)} AS {nv})")
                    local.add(nv)
            elif k == 8:
                t, vs = self.values()
                parts.append(t)
                local |= vs
            elif k == 9:
                g, vs = self.subselect(depth + 1)
                parts.append(g)
                local |= vs
            elif k == 10:
                g1, v1 = self.group(scope | local, depth + 1)
                g2, v2 = self.group(scope | local, depth + 1)
                parts.append(f"{g1} UNION {g2}")
                local |= v1 | v2
            elif k == 11:
                g, vs = self.group(scope | local, depth + 1)
                if self.chance(0.5):
                    parts.append(f"GRAPH ?g {g}")
                    local |= vs | {"?g"}
                else:
                    parts.append(f"GRAPH :g {g}")
                    local |= vs
            elif k == 12:
                g, vs = self.group(local, depth + 1)
                parts.append(f"LATERAL {g}")
                local |= vs
        return "{ " + " ".join(parts) + " }", local

    def subselect(self, depth):
        body, vs = self.group(set(), depth)
        vs = sorted(vs) or ["?s"]
        if self.chance(0.35):
            gv = self.pick(vs)
            other = self.pick(vs)
            self.alias += 1
            a = f"?c{self.alias}"
            agg = self.pick(["COUNT(*)", f"COUNT(DISTINCT {other})", f"SUM({other})", f"MIN({other})",
                             f"MAX({other})", f"AVG({other})"])
            having = " HAVING(COUNT(*) > 1)" if self.chance(0.15) else ""
            return f"{{ SELECT {gv} ({agg} AS {a}) WHERE {body} GROUP BY {gv}{having} }}", {gv, a}
        proj = sorted(self.r.sample(vs, self.r.randint(1, len(vs))))
        dist = "DISTINCT " if self.chance(0.3) else ""
        tail = ""
        if self.chance(0.15):
            tail = " ORDER BY " + " ".join(proj) + " LIMIT 1"
        return f"{{ SELECT {dist}{' '.join(proj)} WHERE {body}{tail} }}", set(proj)

    def query(self):
        g, vs = self.group(set(), 0)
        vs = sorted(vs)
        k = self.r.randint(0, 9)
        if k == 0 and vs:
            return PFX + f"ASK {g}", ["ask"]
        if k == 1 and vs:
            gv = self.pick(vs)
            other = self.pick(vs)
            agg = self.pick(["COUNT(*)", f"COUNT(DISTINCT {other})", f"SUM({other})", f"MIN({other})",
                             f"MAX({other})"])
            return PFX + f"SELECT {gv} ({agg} AS ?agg) WHERE {g} GROUP BY {gv}", ["agg"]
        if k in (2, 3) and vs:
            proj = sorted(self.r.sample(vs, self.r.randint(1, len(vs))))
            dist = "DISTINCT " if self.chance(0.4) else ""
            return PFX + f"SELECT {dist}{' '.join(proj)} WHERE {g}", []
        if k == 4 and vs:
            return PFX + f"SELECT {' '.join(vs)} WHERE {g} ORDER BY {' '.join(vs)} LIMIT 2", ["ordered-limit"]
        return PFX + f"SELECT * WHERE {g}", []


SEEDS = [
    # 3.11.4 critique seeds (sameTerm with an outer-correlated variable inside EXISTS) and the mirror
    "SELECT * WHERE { ?s :p ?x FILTER EXISTS { ?s :q ?o . ?s :r ?x FILTER(sameTerm(?o, ?x)) } }",
    "SELECT * WHERE { ?s :p ?x FILTER EXISTS { ?s :q ?o . ?s :r ?x FILTER(sameTerm(?x, ?o)) } }",
    # develop UnionTest#EmptyUnion vs branch #EmptyOptionalSubselect
    'SELECT ?visibility WHERE { OPTIONAL { SELECT ?var WHERE { :s a :MyType . BIND (:s as ?var ) .} } . '
    'BIND (IF(BOUND(?var), "VISIBLE", "HIDDEN") as ?visibility) }',
    # lateral()[2]-like: LATERAL + OPTIONAL over a sub-select with LIMIT
    "SELECT * WHERE { ?s :p ?o LATERAL { OPTIONAL { SELECT * WHERE { ?s :q ?label } LIMIT 1 } } }",
    # OPTIONAL with condition over a sub-SELECT
    "SELECT * WHERE { ?s :p ?o OPTIONAL { { SELECT ?s ?x WHERE { ?s :q ?x } } FILTER(?x != ?o) } }",
    # VALUES with UNDEF
    "SELECT * WHERE { VALUES (?s ?o) { (:a UNDEF) (UNDEF :b) } ?s :p ?o }",
    # MINUS without shared variables must not remove rows
    "SELECT * WHERE { ?s :p ?o MINUS { ?x :q ?y } }",
    # scoped BIND and VALUES (review 3.11 normalizer finding)
    "SELECT * WHERE { { ?s :p ?o BIND(?o AS ?b) } VALUES ?b { :a } }",
    "SELECT * WHERE { ?s :p ?o FILTER NOT EXISTS { { SELECT ?s WHERE { ?s :q ?z } } } }",
    "SELECT * WHERE { ?s :p ?o OPTIONAL { ?o :p ?x } FILTER(!BOUND(?x)) }",
    "SELECT * WHERE { ?s :p* ?o }",
    "SELECT * WHERE { :a (:p|:q)+ ?o }",
]


def main():
    seed = int(sys.argv[1])
    ncases = int(sys.argv[2])
    nq = int(sys.argv[3]) if len(sys.argv) > 3 else 12
    gen = Gen(random.Random(seed))
    out = sys.stdout
    for c in range(ncases):
        triples, named = gen.dataset()
        gen.data = triples + named
        out.write(f"### CASE s{seed}c{c}\n### DATA\n{gen.trig(triples, named)}")
        qs = []
        qs.append((PFX + SEEDS[c % len(SEEDS)], ["seed"]))
        while len(qs) < nq:
            qs.append(gen.query())
        for i, (q, flags) in enumerate(qs):
            out.write(f"### QUERY {i} {','.join(flags)}\n{q}\n")
        out.write(f"### UPDATE\n{gen.update(triples, named)}\n### END\n")


if __name__ == "__main__":
    main()
