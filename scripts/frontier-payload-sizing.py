#!/usr/bin/env python3
"""Model Frontier payload tiers from the persisted record format and builder envelope."""

from __future__ import annotations

import argparse
import math
from dataclasses import dataclass


@dataclass(frozen=True)
class Tier:
    rows: int
    budget_gb: float


DEFAULT_TIERS = (
    Tier(1_000_000_000, 0.5),
    Tier(1_000_000_000, 2.5),
    Tier(1_000_000_000, 5.0),
    Tier(10_000_000_000, 5.0),
    Tier(10_000_000_000, 25.0),
    Tier(10_000_000_000, 50.0),
)


def modeled_tier(
    tier: Tier,
    lane_copies: int,
    record_bytes: int,
    envelope_bytes: int,
    headroom: float,
    block_bytes: int,
    block_overhead_bytes: int,
    amplification: float,
) -> tuple[int, float, int, float, float]:
    budget_bytes = int(tier.budget_gb * 1_000_000_000)
    maximum_records = budget_bytes // envelope_bytes
    target_records = int(maximum_records * headroom)
    inclusion_probability = min(1.0, target_records / (tier.rows * lane_copies))
    block_records = (block_bytes - block_overhead_bytes) // record_bytes
    wire_bytes = target_records * record_bytes + math.ceil(target_records / block_records) * block_overhead_bytes
    return (
        target_records,
        inclusion_probability,
        wire_bytes,
        wire_bytes * amplification / 1_000_000_000,
        budget_bytes / max(1, wire_bytes),
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--lanes", type=int, default=4, help="design plus audit lanes")
    parser.add_argument("--directions", type=int, default=2)
    parser.add_argument("--record-bytes", type=int, default=12 * 8)
    parser.add_argument("--envelope-bytes", type=int, default=12 * 8 + 48)
    parser.add_argument("--selection-headroom", type=float, default=0.75)
    parser.add_argument("--block-bytes", type=int, default=64 * 1024)
    parser.add_argument("--block-overhead-bytes", type=int, default=48)
    parser.add_argument("--storage-amplification", type=float, default=1.4)
    args = parser.parse_args()
    if args.lanes <= 0 or args.directions <= 0:
        parser.error("lanes and directions must be positive")
    if args.record_bytes <= 0 or args.envelope_bytes < args.record_bytes:
        parser.error("record bytes must be positive and fit the selection envelope")
    if not 0.0 < args.selection_headroom <= 1.0:
        parser.error("selection headroom must be in (0, 1]")
    if args.block_bytes <= args.block_overhead_bytes:
        parser.error("block bytes must exceed block overhead")

    lane_copies = args.lanes * args.directions
    print(
        "rows,budgetGB,targetRecords,residualCenterRatePercent,"
        "wirePayloadGB,amplifiedFootprintGB,budgetToWireRatio"
    )
    for tier in DEFAULT_TIERS:
        target, probability, wire_bytes, amplified_gb, budget_ratio = modeled_tier(
            tier,
            lane_copies,
            args.record_bytes,
            args.envelope_bytes,
            args.selection_headroom,
            args.block_bytes,
            args.block_overhead_bytes,
            args.storage_amplification,
        )
        print(
            f"{tier.rows},{tier.budget_gb:g},{target},{probability * 100:.6f},"
            f"{wire_bytes / 1_000_000_000:.6f},{amplified_gb:.6f},{budget_ratio:.3f}"
        )

    print()
    print("rows,targetCenterRatePercent,requiredConfiguredGB,amplifiedWireGB")
    block_records = (args.block_bytes - args.block_overhead_bytes) // args.record_bytes
    for rows in (1_000_000_000, 10_000_000_000):
        for target_rate in (0.001, 0.005, 0.01):
            target_records = math.ceil(rows * lane_copies * target_rate)
            maximum_records = math.ceil(target_records / args.selection_headroom)
            configured_bytes = maximum_records * args.envelope_bytes
            wire_bytes = target_records * args.record_bytes
            wire_bytes += math.ceil(target_records / block_records) * args.block_overhead_bytes
            print(
                f"{rows},{target_rate * 100:.1f},{configured_bytes / 1_000_000_000:.6f},"
                f"{wire_bytes * args.storage_amplification / 1_000_000_000:.6f}"
            )


if __name__ == "__main__":
    main()
