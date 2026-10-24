#!/usr/bin/env python3
"""Develop worktree only: stop maven-bundle-plugin writing OBR metadata into the shared .m2_repo/repository.xml when
building under a workspace (the branch pom's workspace-build-root profile does the same). Build-infra only."""
import sys
p = sys.argv[1]
s = open(p).read()
ins = """	<profiles>
		<profile>
			<id>diff-workspace-no-obr</id>
			<activation>
				<property>
					<name>rdf4j.build.root</name>
				</property>
			</activation>
			<build>
				<plugins>
					<plugin>
						<groupId>org.apache.felix</groupId>
						<artifactId>maven-bundle-plugin</artifactId>
						<configuration>
							<obrRepository>NONE</obrRepository>
						</configuration>
					</plugin>
				</plugins>
			</build>
		</profile>
"""
if "diff-workspace-no-obr" not in s:
    assert s.count("\t<profiles>\n") == 1
    open(p, "w").write(s.replace("\t<profiles>\n", ins, 1))
print("ok")
