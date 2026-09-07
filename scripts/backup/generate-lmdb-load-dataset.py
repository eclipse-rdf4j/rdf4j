#!/usr/bin/env python3
"""Generate deterministic, batch-partitioned N-Quads data for LMDB load tests."""

from __future__ import annotations

import argparse
import json
import random
import string
from dataclasses import asdict, dataclass
from pathlib import Path


MEBIBYTE = 1024 * 1024
ALPHANUMERIC = string.ascii_lowercase + string.digits


@dataclass(frozen=True)
class DatasetParameters:
    target_mebibytes: int
    seed: int
    average_iri_length: int
    average_literal_length: int
    context_percentage: int
    inferred_percentage: int
    batch_size: int
    triple_indexes: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True, type=Path, help="Empty destination directory.")
    parser.add_argument(
        "--target-mib",
        type=int,
        choices=(100, 250),
        required=True,
        help="Approximate total serialized N-Quads size.",
    )
    parser.add_argument("--seed", type=int, default=20260903, help="Fixed random seed.")
    parser.add_argument("--average-iri-length", type=int, default=48)
    parser.add_argument("--average-literal-length", type=int, default=96)
    parser.add_argument("--context-percentage", type=int, default=30)
    parser.add_argument("--inferred-percentage", type=int, default=15)
    parser.add_argument("--batch-size", type=int, default=5_000)
    parser.add_argument("--triple-indexes", default="spoc,posc", help="Production LMDB triple-index setting.")
    return parser.parse_args()


def validate(args: argparse.Namespace) -> DatasetParameters:
    for name in ("average_iri_length", "average_literal_length", "batch_size"):
        if getattr(args, name) <= 0:
            raise SystemExit(f"--{name.replace('_', '-')} must be positive")
    for name in ("context_percentage", "inferred_percentage"):
        value = getattr(args, name)
        if not 0 <= value <= 100:
            raise SystemExit(f"--{name.replace('_', '-')} must be between 0 and 100")
    if args.output.exists() and any(args.output.iterdir()):
        raise SystemExit(f"Output directory must be empty: {args.output}")
    return DatasetParameters(
        target_mebibytes=args.target_mib,
        seed=args.seed,
        average_iri_length=args.average_iri_length,
        average_literal_length=args.average_literal_length,
        context_percentage=args.context_percentage,
        inferred_percentage=args.inferred_percentage,
        batch_size=args.batch_size,
        triple_indexes=args.triple_indexes,
    )


def token(rng: random.Random, size: int) -> str:
    return "".join(rng.choices(ALPHANUMERIC, k=size))


def iri(params: DatasetParameters, prefix: str, average_length: int, identifier: int) -> str:
    suffix_length = max(8, average_length - len("urn:load:") - len(prefix) - len(str(identifier)) - 2)
    identity_rng = random.Random(f"{params.seed}:{prefix}:{identifier}")
    return f"<urn:load:{prefix}:{identifier}:{token(identity_rng, suffix_length)}>"


def statement(rng: random.Random, params: DatasetParameters, identifier: int) -> tuple[str, str | None]:
    subject = iri(params, "subject", params.average_iri_length, identifier % 50_000)
    predicate = iri(params, "predicate", params.average_iri_length, identifier % 128)
    literal_length = max(8, params.average_literal_length - len(str(identifier)) - 1)
    literal = f'"{token(rng, literal_length)}-{identifier}"'
    context = None
    if rng.randrange(100) < params.context_percentage:
        context = iri(params, "graph", params.average_iri_length, identifier % 64)
    return f"{subject} {predicate} {literal}" + (f" {context}" if context else "") + " .\n", context


def write_partition(
    root: Path, name: str, target_bytes: int, rng: random.Random, params: DatasetParameters, start_identifier: int
) -> tuple[int, int, int, dict[str, str]]:
    directory = root / f"{name}-batches"
    directory.mkdir()
    statement_count = total_bytes = batch_count = 0
    fixtures: dict[str, str] = {}
    current = None
    try:
        while total_bytes < target_bytes:
            if statement_count % params.batch_size == 0:
                if current:
                    current.close()
                batch_count += 1
                current = (directory / f"batch-{batch_count:05d}.nq").open("w", encoding="ascii", newline="\n")
            line, _ = statement(rng, params, start_identifier + statement_count)
            if not fixtures:
                subject, predicate, literal, *context_and_dot = line.rstrip().split(" ")
                fixtures = {
                    "subject": subject,
                    "predicate": predicate,
                    "object": literal,
                    "context": context_and_dot[0] if len(context_and_dot) == 2 else "",
                }
            current.write(line)
            total_bytes += len(line)
            statement_count += 1
    finally:
        if current:
            current.close()
    return statement_count, total_bytes, batch_count, fixtures


def main() -> None:
    args = parse_args()
    params = validate(args)
    output = args.output
    output.mkdir(parents=True, exist_ok=True)
    rng = random.Random(params.seed)
    target_bytes = params.target_mebibytes * MEBIBYTE
    inferred_bytes = target_bytes * params.inferred_percentage // 100
    explicit_bytes = target_bytes - inferred_bytes

    explicit_count, explicit_size, explicit_batches, explicit_fixtures = write_partition(
        output, "explicit", explicit_bytes, rng, params, 0
    )
    inferred_count, inferred_size, inferred_batches, inferred_fixtures = write_partition(
        output, "inferred", inferred_bytes, rng, params, explicit_count
    )
    manifest = {
        "parameters": asdict(params),
        "partitions": {
            "explicit": {"statements": explicit_count, "bytes": explicit_size, "batches": explicit_batches},
            "inferred": {"statements": inferred_count, "bytes": inferred_size, "batches": inferred_batches},
        },
        "total": {"statements": explicit_count + inferred_count, "bytes": explicit_size + inferred_size},
        "read_fixtures": {
            "explicit_statement": explicit_fixtures,
            "inferred_statement": inferred_fixtures,
            "predicate_count": 128,
            "named_graph_count": 64,
            "note": "Use the generated identifiers to build point, selective, graph, and broad scan queries.",
        },
        "import_note": (
            "Load explicit batches with RepositoryConnection.add. Load inferred batches through "
            "LmdbStoreConnection.addInferredStatement so their explicit/inferred classification is retained."
        ),
    }
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="ascii")
    print(json.dumps(manifest["total"]))
    print(f"Manifest: {output / 'manifest.json'}")


if __name__ == "__main__":
    main()
