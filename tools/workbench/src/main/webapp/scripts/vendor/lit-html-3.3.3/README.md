# Vendored lit-html

This directory contains the standalone browser ESM distribution of `lit-html`
3.3.3, pinned so the Workbench can render without a runtime package download.

- Source: <https://registry.npmjs.org/lit-html/-/lit-html-3.3.3.tgz>
- Upstream project: <https://github.com/lit/lit/tree/main/packages/lit-html>
- License: BSD-3-Clause; see `LICENSE`.
- npm tarball SHA-512: `7a5f0cea32b6a37457067ad21d7dd92abb0df33115eb7a521314ced70609cfb427746619f37e777b66b93cf5fea877b66866b27e7721426900a2369601110e20`
- Extracted `lit-html.js` SHA-256: `b878e7f95dec8a9b6e9b217faca6b6a11bad82ebc44c0caa77419ed83edd81f2`

`../../workbench-lit-html.mjs` imports this release and exposes only the template,
render, and `nothing` APIs used by the Workbench views.
