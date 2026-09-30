#!/usr/bin/env python3
"""Apply the identical throw-away result-dump instrumentation to a worktree (develop or branch).

Usage: instrument.py <worktree> <DiffDump.java>
Edits: testsuites/sparql manifest harness (query + 1.1/1.2 update runTest) and compliance/pom.xml (phrased Failsafe
method names, workspace-only OBR suppression is handled separately). Idempotent.
"""
import pathlib
import sys

root = pathlib.Path(sys.argv[1])
m = root / "testsuites/sparql/src/main/java/org/eclipse/rdf4j/testsuite/query/parser/sparql/manifest"
(m / "DiffDump.java").write_text(pathlib.Path(sys.argv[2]).read_text())

q = m / "SPARQLQueryComplianceTest.java"
s = q.read_text()
if "DiffDump" not in s:
    reps = [
        ("""					TupleQueryResult actualResult = ((TupleQuery) query).evaluate();""",
         """					TupleQueryResult actualResult;
					try {
						actualResult = DiffDump.tuple(SPARQLQueryComplianceTest.this, getDisplayName(), getTestURI(),
								((TupleQuery) query).evaluate(), ordered);
					} catch (Exception | Error e) {
						DiffDump.error(SPARQLQueryComplianceTest.this, getDisplayName(), getTestURI(), e);
						throw e;
					}"""),
        ("""					GraphQueryResult gqr = ((GraphQuery) query).evaluate();
					Set<Statement> actualResult = Iterations.asSet(gqr);""",
         """					Set<Statement> actualResult;
					try {
						GraphQueryResult gqr = ((GraphQuery) query).evaluate();
						actualResult = Iterations.asSet(gqr);
						DiffDump.graph(SPARQLQueryComplianceTest.this, getDisplayName(), getTestURI(), "construct",
								actualResult);
					} catch (Exception | Error e) {
						DiffDump.error(SPARQLQueryComplianceTest.this, getDisplayName(), getTestURI(), e);
						throw e;
					}"""),
        ("""					boolean actualResult = ((BooleanQuery) query).evaluate();""",
         """					boolean actualResult;
					try {
						actualResult = ((BooleanQuery) query).evaluate();
						DiffDump.bool(SPARQLQueryComplianceTest.this, getDisplayName(), getTestURI(), actualResult);
					} catch (Exception | Error e) {
						DiffDump.error(SPARQLQueryComplianceTest.this, getDisplayName(), getTestURI(), e);
						throw e;
					}"""),
    ]
    for o, n in reps:
        assert s.count(o) == 1, o
        s = s.replace(o, n)
    q.write_text(s)

TAIL = """					DiffDump.graph(%(c)s.this, getDisplayName(), getTestURI(), "default",
							Iterations.asList(con.getStatements(null, null, null, true, (Resource) null)));
					for (String namedGraph : new java.util.TreeSet<>(inputNamedGraphs.keySet())) {
						IRI dumpContext = con.getValueFactory().createIRI(namedGraph.replaceAll("\\"", ""));
						DiffDump.graph(%(c)s.this, getDisplayName(), getTestURI(), namedGraph,
								Iterations.asList(con.getStatements(null, null, null, true, dumpContext)));
					}
"""
for name, old in (("SPARQL11UpdateComplianceTest", """					update.setDataset(dataset);
					update.execute();

					con.commit();
"""), ("SPARQL12UpdateComplianceTest", """					update.execute();

					con.commit();
""")):
    u = m / (name + ".java")
    s = u.read_text()
    if "DiffDump" in s:
        continue
    lead = "					update.setDataset(dataset);\n" if name == "SPARQL11UpdateComplianceTest" else ""
    new = lead + """					try {
						update.execute();

						con.commit();
					} catch (Exception | Error e) {
						DiffDump.error(%(c)s.this, getDisplayName(), getTestURI(), e);
						throw e;
					}
""" % {"c": name} + TAIL % {"c": name}
    assert s.count(old) == 1, name
    u.write_text(s.replace(old, new))

p = root / "compliance/pom.xml"
s = p.read_text()
old_p = """					<forkCount>1</forkCount>
					<reuseForks>true</reuseForks>"""
if "statelessTestsetReporter" not in s:
    assert s.count(old_p) == 1
    s = s.replace(old_p, old_p + """
					<statelessTestsetReporter implementation="org.apache.maven.plugin.surefire.extensions.junit5.JUnit5Xml30StatelessReporter">
						<usePhrasedFileName>false</usePhrasedFileName>
						<usePhrasedTestSuiteClassName>false</usePhrasedTestSuiteClassName>
						<usePhrasedTestCaseClassName>false</usePhrasedTestCaseClassName>
						<usePhrasedTestCaseMethodName>true</usePhrasedTestCaseMethodName>
					</statelessTestsetReporter>""")
    p.write_text(s)
print("instrumented", root)
