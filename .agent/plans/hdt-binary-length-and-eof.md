# Reject malformed HDT lengths and truncated binary parts

This ExecPlan is a living document governed by `.agent/PLANS.md`.

## Purpose / Big Picture

An HDT file can declare a very large bitmap, array, header, or dictionary buffer and then end almost immediately. The current reader allocates from that untrusted declaration before confirming the payload and sometimes treats a short read as a complete field. A malformed HDT file should fail with a parser error using memory proportional to bytes actually supplied, while valid HDT files, including streams that return short reads, retain their RDF content.

## Progress

- [x] Trace parser reachability and binary reads.
- [x] Run isolated root quick install.
- [x] Add smallest failing malformed-input regressions.
- [x] Reproduce VByte encoded-length boundary.
- [x] Reproduce over-limit encoded metadata.
- [x] Complete shared exact-read fix.
- [x] Verify focused and full HDT suites.
- [x] Reproduce malformed array index failures.
- [x] Validate shared index boundaries.
- [x] Reproduce wide Log64 index truncation.
- [x] Reject unrepresentable array entries.
- [x] Reverify focused and full suites.
- [x] Format and review scoped candidate diff.
- [ ] (in_progress) Publish reviewed draft and monitor checks.

## Surprises & Discoveries

`HDTBitmap.parse` computes `(bits + 7) / 8` in `int`, which overflows for high accepted bit counts. `HDTHeader`, `HDTBitmap`, `HDTArrayLog64`, and `HDTDictionarySectionPFC` allocate directly from declared lengths and issue one `read(byte[])`. `HDTPart` does the same for fixed control fields and checksums, ignores the required format terminator, and loops over EOF in `readToNull`. `VByte.decode(InputStream)` casts EOF to a byte. The experimental parser is registered through `META-INF/services`, and this module is directly used by `tools/lmdb-bulk-load`; no default Server WAR dependency was found in the source triage.

The initial offline root install failed only because `maven-dependency-plugin:3.11.0` was missing from the read-through cache. The exact online retry passed in `.mvnf/workspaces/hdt-parser/logs/20261023T015911.405190Z-4473-7be49cd3/maven.log`; all subsequent test runs return offline.

The first bitmap test failed as expected with `NegativeArraySizeException: -268435455` and was preserved in `initial-evidence.hdt-parser.txt`. The expanded 18-case class produced 14 assertion failures and 4 errors before production edits, retained in `initial-evidence.hdt-complete.txt`; failures include zero-filled truncated payloads, early EOF, one-byte bulk reads, the missing eighth VByte byte, and front-coded prefix expansion beyond the previous string. Local JDK bytecode confirms `InputStream.readNBytes(int)` starts with at most a 16 KiB chunk, so it does not allocate solely from a large declaration.

During implementation, review found an adjacent valid VByte boundary: `encodedLength(127)` reports two bytes. The first attempt changed that helper before a focused regression was captured. That helper change was reverted and is being reproduced red-first before resuming the parser fix.

The focused `VByteTest#encodedLengthUsesAllSevenBitsOfEachByte` then failed with `expected: <1> but was: <2>`, retained in `initial-evidence.hdt-vbyte-boundary.txt`. The length helper may now be corrected together with the parser boundary without an unobserved behavior change.

The first repair passed all 18 malformed-input cases in `.mvnf/workspaces/hdt-parser/logs/20261023T021436.020045Z-68452-ddf4382d/verify.log`. Review of adjacent branches found accepted counts above `int` range and Log64 bit widths above 64; their desired checked parser errors need focused red evidence before those validation branches are finalized.

Seven independent over-limit checks then failed as expected in `initial-evidence.hdt-overlimit.txt`: bitmap, array, and dictionary counts above supported range threw unchecked `UnsupportedOperationException`, and a 65-bit Log64 width parsed successfully. The other 18 cases remained green while those validation branches were reverted for red capture.

Independent review found cross-part index assumptions. Three more parser regressions failed before the index fix in `initial-evidence.hdt-indexes.txt`: missing front-coded dictionary block start, Y array shorter than Y bitmap, and more Z-level group boundaries than Y entries. A zero-bit array lookup already returned zero and remains a green edge regression. The missing dictionary index can read padding or run off the array; mismatched triple dimensions can reach `HDTArrayLog64.get` out of bounds during iteration.

The first index repair passed all 29 focused cases. A further review found that Log64 fields wider than `int` could silently truncate dictionary or triple indices. The crafted aligned 64-bit and unaligned 63-bit regression was captured red-first in `initial-evidence.hdt-wide-index.txt` (1/1 failure), while a representable unaligned value is a companion compatibility case. Since the array's API returns `int`, unrepresentable entries will be rejected with checked `IOException` while parsing the field.

The completed index repair passes 31/0/0/0 focused regressions in `.mvnf/workspaces/hdt-parser/logs/20261023T023639.676370Z-74845-b452307d/verify.log`. The Log64 reader uses long bit offsets, validates cross-part dimensions before triple iteration, and checks wide values during parse so malformed data fails through the parser's checked error path.

After copyright and formatting checks, the full HDT module passed 43/0/0/0 in `.mvnf/workspaces/hdt-parser/logs/20261023T023931.733394Z-95159-285f759b/verify.log`.

After checked range validation, all 25 malformed-input tests passed in `.mvnf/workspaces/hdt-parser/logs/20261023T022019.749297Z-91063-9ba0a815/verify.log`. The shared reader uses `readNBytes` and compares the returned length before accepting a field; large declarations therefore do not allocate their full target buffer before bytes arrive. The front-coded dictionary now validates its block width and shared-prefix length before constructing decoded strings.

## Decision Log

- Decision: Reproduce format-level failures through the existing package-private parts and public VByte reader before editing production. Rationale: direct binary fixtures make overflow, EOF, and short-read behavior deterministic without building a full HDT document. Date/Author: 2026-10-23 Codex.
- Decision: Preserve valid large-file semantics without an arbitrary default size cap. Rationale: a declared length alone must not allocate a large array; a valid payload of that length genuinely requires storage in the current random-access representation. The parser should accumulate only verified bytes before finalizing a section buffer. Date/Author: 2026-10-23 Codex.

## Outcomes & Retrospective

The final focused malformed-input class passes 31/0/0/0 and the full HDT module passes 43/0/0/0. Declared-only large lengths now fail at EOF without allocating the declared payload, short bulk reads are accepted, and malformed VByte, control, checksum, array, dictionary, and cross-part index metadata produce checked parser errors. Existing HDT fixtures remain green. The parser still retains actual large HDT sections for random access; that memory use is proportional to supplied content. This parser is experimental and directly used by the bulk loader, not the default Server WAR in the source dependency inventory.

## Context and Orientation

`core/rio/hdt/src/main/java/org/eclipse/rdf4j/rio/hdt/HDTParser.java` parses the Global, Header, Dictionary, and Triples parts from an input stream. `HDTPart` owns common control, format, property, and checksum reads. The variable-byte helper is `VByte`; bitmap, Log64 array, header, and front-coded dictionary section classes retain binary payloads for random access. Tests belong under `core/rio/hdt/src/test/java/org/eclipse/rdf4j/rio/hdt/` and can call package-private parser parts.

## Plan of Work

First add a small test class with crafted HDT part bytes and a stream that returns one byte per bulk read. Cover bitmap length overflow, truncated payload accepted with zero-filled data, valid short-read payloads, control/format/checksum truncation, EOF in null-terminated properties and VByte, and dictionary block-size zero. Capture the first failing Surefire report at the repository root before any production edit. Then implement a shared exact-read routine that distinguishes EOF from data, reads large declared payloads incrementally before allocating their final representation, and performs length arithmetic in `long`. Keep part checksums and valid file parsing unchanged. Run the red selectors again and the complete `core/rio/hdt` module suite; use parser-backed existing fixtures as compatibility checks.

## Concrete Steps

From `/Users/havardottestad/.codex/worktrees/security-hdt-parser/rdf4j`, run `python3 .codex/skills/mvnf/scripts/mvnf.py --workspace hdt-parser --retain-logs HDTMalformedInputTest#<first-method>` and save the compact Surefire failure to `initial-evidence.hdt-parser.txt`. The runner installs dependencies with tests skipped, then verifies only `core/rio/hdt` with tests enabled and without Maven `-am` or `-q`. Its reports live under `.mvnf/workspaces/hdt-parser/build/org.eclipse.rdf4j/rdf4j-rio-hdt/6.2.0-SNAPSHOT/surefire-reports/`. After the fix, rerun the same selector, the new class, and `core/rio/hdt`.

## Validation and Acceptance

Malformed lengths and EOF produce a checked `IOException` or RDF parser error before a large declared-only allocation. A valid part supplied one byte at a time parses identically to an ordinary stream. Existing `test.hdt` still produces 43 statements, while unsupported POS ordering retains its defined rejection. The final module run has zero new failures; format, copyright, and `git diff --check` pass.

## Idempotence and Recovery

Keep untracked initial evidence and Maven logs. Stage only the reviewed source, tests, and this ExecPlan. If an offline dependency miss recurs, retry the same command online once, then return offline. Do not weaken semantic assertions to make malformed input pass.

## Artifacts and Notes

The initial root quick install passed after the permitted online retry. Red test logs are `.mvnf/workspaces/hdt-parser/logs/20261023T020307.678628Z-25378-c23e3bfb/verify.log`, `.mvnf/workspaces/hdt-parser/logs/20261023T020754.280052Z-28035-dfe10781/verify.log`, and the retained `initial-evidence.hdt-vbyte-boundary.txt` and `initial-evidence.hdt-overlimit.txt`. Green focused run: `.mvnf/workspaces/hdt-parser/logs/20261023T022019.749297Z-91063-9ba0a815/verify.log`. Green full module run: `.mvnf/workspaces/hdt-parser/logs/20261023T022401.757275Z-31382-eda41967/verify.log`.

## Interfaces and Dependencies

No new dependency is expected. Use `InputStream`, existing CRC implementations, and ordinary Java IO exceptions. Avoid a new public parser setting unless a proven valid-input compatibility case requires it.
