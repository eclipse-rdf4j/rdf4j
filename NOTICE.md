# Notices for Eclipse RDF4J

This content is produced and maintained by the Eclipse RDF4J project.

* Project home: https://projects.eclipse.org/projects/technology.rdf4j
* Project website: https://rdf4j.org
* Source code: https://github.com/eclipse-rdf4j/rdf4j

## Trademarks

Eclipse RDF4J is a trademark of the Eclipse Foundation. Eclipse and the Eclipse
logo are registered trademarks of the Eclipse Foundation. Java and all Java-based
trademarks are trademarks of Oracle Corporation. Other names may be trademarks of
their respective owners.

## Copyright

All content is the property of the respective authors or their employers. See the
copyright notices in individual files and the source code repository history for
information about authorship. This project includes software originally developed
by Aduna as part of OpenRDF Sesame; its copyright notices are retained in the
source files and the project license.

## Declared Project License

All rights reserved. This program and the accompanying materials are made
available under the terms of the Eclipse Distribution License v1.0 which
accompanies this distribution, and is available at
https://www.eclipse.org/org/documents/edl-v10.php.

SPDX-License-Identifier: BSD-3-Clause

The full project license is provided in LICENSE and edl-v1.0.txt. Third-party
content identified below or in individual files is subject to its own terms.

## Third-party Content

The source tree includes third-party material with its own copyright and license
notices. Those notices must be retained when the corresponding content is
redistributed. In particular:

* Workbench's vendored JavaScript includes jQuery, CodeMirror, YASQE, yasgui-utils
  and store (MIT), diff and lit-html (BSD-3-Clause), and svg-pan-zoom (BSD-2-Clause).
  Versions, source archives, bundled components and file hashes are recorded in
  tools/workbench/frontend-vendor-dependencies.json. The complete license texts
  accompany the files under tools/workbench/src/main/frontend/static/scripts/.
  Additional notices, including the public-domain notice for bundled json2 code,
  are retained in the YASQE sources.
* LWJGL-derived code is identified in
  core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbUtil.java, with the
  original copyright notice. Its full license is reproduced below.
* W3C test-suite material retains its applicable W3C notices, licenses and
  provenance in the corresponding test-resource directories. These include the
  Turtle and TriG RDF 1.1 test suites and the SPARQL 1.0 data-r2 suite.
* The license and provenance for the modified datagovbe-valid.ttl benchmark data
  are provided in
  testsuites/benchmark-common/src/main/resources/benchmarkFiles/LICENSE.

### Dependencies and Binary Distributions

Dependency declarations and lockfiles identify additional third-party software
used to build, test or run RDF4J. They are not a list of the content in every
RDF4J artifact. A dependency referenced by a Maven POM is not necessarily bundled
in an individual RDF4J JAR.

The SDK includes dependency archives under lib/ and the server and Workbench WARs
under war/. The Workbench also contains bundled frontend dependencies. The
licenses and notices of the third-party content included in each artifact apply
in addition to the project license. Preserve the notices and license texts
accompanying that content when redistributing it.

The dependency-checking workflow is documented in
scripts/check-dependency-licenses.py. It checks Maven dependencies, npm lockfiles
and the vendored frontend inventory; its reports support license due diligence
and do not replace the notices or license texts required in distributed content.

Workbench's Legal information links open the project license, this notice and
third-party notices packaged with that installed version. The third-party report
is generated from the resolved runtime JARs, emitted frontend bundles and vendored
assets. The collection and manual evidence workflow is documented in
scripts/third-party-notices.md. An incomplete report identifies evidence that
must be resolved before release; it is not an IP approval.

## LWJGL-derived Portions

The LWJGL-derived portions of LmdbUtil.java retain this source attribution:

Copyright LWJGL. All rights reserved.
License terms: https://www.lwjgl.org/license

The LWJGL license text follows:

Copyright (c) 2012-present Lightweight Java Game Library
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

- Neither the name Lightweight Java Game Library nor the names of
  its contributors may be used to endorse or promote products derived
  from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## Cryptography

Content may contain encryption software. The country in which you are currently
may have restrictions on the import, possession, and use, and/or re-export to
another country, of encryption software. BEFORE using any encryption software,
please check the country's laws, regulations and policies concerning the import,
possession, or use, and re-export of encryption software, to see if this is
permitted.
