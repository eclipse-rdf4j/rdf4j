# Optional stress-pair WebAssembly kernel

`stress-pairs.c` implements the same sequential Fisher–Yates shuffle and pair epoch as the JavaScript fallback in
`graph-layout-stress.ts`. Coordinates and arithmetic remain `f64`; hops remain `f32`, packed endpoints remain `u32`,
and the xorshift state uses unsigned 32-bit operations. The build disables fast math, floating-point contraction,
and vectorization. This is a scalar kernel with no GPU, threads, shared memory, imports, or runtime dependencies.

The layout worker starts an optional fetch/compile when it loads. A requested calculation does **not** wait for that
fetch: an early request uses JavaScript, and later requests can use the compiled kernel in the retained worker.
Initialization is attempted once per worker. Fetch, compilation, CSP, unavailable WebAssembly, and instance or memory
setup failures leave the JavaScript backend available. A numerical error during an epoch propagates; it is never
silently replayed in JavaScript after random state, pair order, or positions may have advanced.

Each solver call gets independent Wasm memory. Pair templates are copied from the distance cache once, and coordinates
are copied in and out around each epoch. Springs, anchors, bounds, overlap/occlusion hinges, and all iteration budgets
remain in the existing solver. Aliased coordinate/input views, shared buffers, unsuitable array shapes, or requests
outside the 64 MiB Wasm memory bound use JavaScript. Exact coordinate bytes, random states, and shuffle order are
verified for valid finite solver inputs; arbitrary invalid inputs or NaN payload encodings are not an exactness claim.

## Rebuild

The checked-in module is built with [Zig 0.15.1](https://ziglang.org/download/0.15.1/release-notes.html), which includes the
Clang/LLVM compiler and Wasm linker. Run this from the workbench directory after unpacking that compiler:

```sh
GRAPH_LAYOUT_ZIG=/absolute/path/to/zig bash src/main/frontend/graph-explorer/wasm/build.sh
```

`build.sh` checks the pinned version and contains every compiler/linker flag. The normal Vite build consumes the
checked-in binary and needs no C compiler.
`build-metadata.json` records its byte count and SHA-256 digest; update that metadata if the source is deliberately changed.

## Explicit Node initialization

Benchmarks can use the exact production backend without depending on a browser fetch:

```js
import {readFile} from 'node:fs/promises';
import {initializeStressWasm} from './src/main/frontend/graph-explorer/graph-layout-wasm.ts';

const ready = await initializeStressWasm(() => readFile(
  new URL('./src/main/frontend/graph-explorer/wasm/stress-pairs.wasm', import.meta.url),
));
if (!ready) throw new Error('The benchmark requires the Wasm backend');
// Import/use the normal synchronous stress or organic APIs in this same module graph.
```

Measure JavaScript and initialized Wasm in separate fresh processes. Include initialization separately when reporting
startup, and include per-call setup and coordinate copies in layout timings. The focused Wasm tests also compare the
retained JavaScript kernel, cache templates, changes between epochs, fallback behavior, and runtime-error propagation.
