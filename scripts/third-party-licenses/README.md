# Version-specific supplemental license evidence

These documents supplement artifacts that omit complete license texts. They are
not legal approvals. `../third-party-notices.manual.json` maps each document to
an exact component version and verifies its SHA-256 before use. Components are
included only when that exact component is discovered in the shipped inputs.

`provenance.json` records primary source URLs, hashes, and archive-entry paths.
Exact Maven Central POMs preserve the release declarations. `source-notices.txt`
files collect distinct copyright/license/attribution comment blocks throughout
the exact source JAR, byte-for-byte, including comments after package declarations
and secondary/Javadoc notices. Quoted string and character literals are skipped.
Entry names and source line ranges precede each block; source-notice-origins.json
records original source-file hashes and byte offsets for every occurrence. Their
source archive hashes are recorded. They supplement, and do not replace, complete license texts.
The source JARs themselves are not redistributed here.

The Apache license is the full canonical Apache text, applied only to releases
whose collected declarations identify Apache 2.0. Version-tagged upstream texts
supply the other missing licenses. Logback retains both EPL 2.0 and LGPL 2.1 and
its original choice statement; no license choice is made. The LGPL text is the
unaltered FSF license distributed by the GNU GCC project. JTS likewise retains
both release license texts and all captured source notices.

Graphviz and Expat source release archive SHA-256 values were checked against
Viz.js 3.31.0's installed build provenance before copying their documents.
Graphviz's broader attribution and embedded rbtree material are also retained.
LWJGL's native dependency notices are retained from its exact release, without
claiming every native classifier's dependency coverage has been reviewed.

Additional native evidence pins Brotli4j 1.23.0 to its release source commit,
including Brotli 1.2.0's MIT text, the version header, CMake source list and all
ten classifier build scripts. LWJGL 3.4.2 native headers identify LMDB 0.9.35,
libffi 3.7.0, and the bundled liburing API version.

Although zstd-jni 1.5.7-25 has no upstream release tag, commit
`b3ef8f51f372053611f65de55952fd563c7aa780` declares that exact version and all
40 Java source entries in the Central source JAR match its Git blob hashes
byte-for-byte. The comparison and exact source artifact SHA-256 are retained in
`native/zstd-source-comparison.json`. That pinned commit supplies the full
wrapper BSD-2-Clause and bundled Zstandard BSD-3-Clause and GPL-2.0 alternative
texts. No choice between the native alternatives is asserted. This does not
claim that native binaries have been reproducibly rebuilt or that every native
source obligation is resolved.

Remaining review gaps stay explicit in the manual inventory and generated
report. No completed IP or source-offer review has been manufactured, and adding
a text does not resolve those review gates or select among license alternatives.

LZ4 Java 1.11.4 native evidence pins the release source tree and its LZ4
submodule commit, retaining the native LZ4 1.10.0 and xxHash 0.6.5 BSD-2-Clause
texts, original source-header notices, version headers and native build inputs.
The native binary/source coverage review remains explicit.
