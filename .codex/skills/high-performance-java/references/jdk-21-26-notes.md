# JDK 21 to 26 Notes

Treat JDK behavior as version-sensitive.

## Defaults

- Repository baseline: JDK 21
- Current local runtime may be newer; in this workspace it is JDK 26
- Advice about inlining, intrinsics, vectorization, and loop optimizations must be checked on the active runtime

## What stays stable enough

- Fewer allocations usually helps
- Fewer copies usually helps
- Monomorphic hot calls are easier to inline than megamorphic ones
- Primitive, contiguous loop shapes are friendlier to optimization than object-heavy callback stacks

## What must be verified

- Whether a specific JDK method lowers to an intrinsic on this runtime
- Whether SuperWord or related loop optimizations fire for this loop shape
- Whether a call chain fully inlines on this runtime
- Whether scalar replacement / escape analysis removes the expected allocation
- Whether benchmark results carry across JDK 21 and JDK 26

## Reporting rule

When giving low-level JVM explanations, say which JDK you are talking about.

Good:
- `On JDK 26 this loop appears to inline fully and the benchmark improves by 12%.`
- `On the JDK 21 baseline, verify the same claim before treating it as settled.`

Bad:
- `HotSpot will optimize this.`
- `The JVM should vectorize it.`

## Upgrade rule

If a change is intended for the repo baseline, prefer evidence on JDK 21.

If only a newer runtime is available locally, say that clearly and lower confidence until the baseline JVM is checked.
