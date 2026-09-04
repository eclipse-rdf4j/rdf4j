# JFR Reading

Use both CLI and JMC. CLI is fast for triage. JMC is better for shape recognition.

## CLI

Summary:

```bash
jfr summary /path/to/profile.jfr
```

CPU-time hot methods:

```bash
jfr view cpu-time-hot-methods /path/to/profile.jfr
```

CPU-time samples and lost samples:

```bash
jfr print \
  --events jdk.CPUTimeSample,jdk.CPUTimeSamplesLost \
  --stack-depth 20 \
  /path/to/profile.jfr
```

Read these first:

- total event counts
- whether `jdk.CPUTimeSamplesLost` appears
- hottest methods by CPU time
- recurring stack shapes

## JMC

Open the recording in JDK Mission Control and check:

- Flame Graph: quickest view of dominant stack width and call shape
- Threads: whether one thread or many consume the CPU time
- Lock Instances: whether apparent slowness is actually contention
- Memory: whether allocation / GC pressure dominates instead
- Automated Analysis: quick sanity check for obvious JVM or application issues

## Interpretation rules

### Self vs inclusive cost

- Hot leaf: usually best when it has real self cost and clear local waste.
- Wide parent: often better when many hot leaves share the same parent frame or repeated adapter layer.

### Wide parents vs hot leaves

- Prefer the parent when the stack fan-out is broad but the same wrapper, iterator, decode, or dispatch layer appears everywhere.
- Prefer the leaf when one method body clearly burns CPU on its own.

### Thread concentration

- One dominant thread: optimize that path first.
- Many similar hot threads: look for shared contention, shared allocations, or repeated framework overhead.

### Lost-sample confidence

- No lost-sample events: normal confidence.
- Some lost samples: lower confidence, but still useful if hotspot ranking is stable.
- Heavy lost samples: avoid strong conclusions from tiny deltas.

### CPU-bound or not

Say "not CPU-bound" when the recording points elsewhere:

- Threads mostly waiting or blocked
- Lock views dominate
- GC / allocation pressure dominates memory views
- I/O wait or native wait dominates wall time

In those cases pivot to the real limiter.

### Amdahl discipline

Even a perfect fix to a 10% hotspot cannot produce a large end-to-end gain. Estimate likely upside before over-investing.

### Multiple bottlenecks

Do not assume the first hotspot explains all lost time. Fixing one issue can surface another that previously hid behind it. Re-profile after each meaningful fix.
