# Historical regular-file estimator fixture

These files were produced by the unmodified `SketchBasedJoinEstimator.writeManifest`
and companion writer at commit `c010e4b7b1b8ea33d8a16eb7f2e56976a21f5e6e`.
The historical `persistSnapshotThenLazyLoadOnDemand` test used one statement and
verified lazy reload. A test-only change copied both generated files before JUnit
removed the temporary directory. The main file starts with `RJES` and version 4.

* `join-estimator.rjes`: SHA-256 `93ea2345cf475d8dbf596cce5e87942407d7a2bf0a267eaa486b22e1ecd90276`
* `join-estimator.rjes.sketches`: SHA-256 `c18873598dc55977f559b1ed816cd934799e17907adcc3b11a41ab90b0c648df`

These are distinct from the authentic upstream f0 directory-based v2 fixture.
