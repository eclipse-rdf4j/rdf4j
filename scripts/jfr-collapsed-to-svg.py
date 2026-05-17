#!/usr/bin/env python3
"""Render collapsed stack samples as a standalone SVG flame graph."""

from __future__ import annotations

import argparse
import hashlib
import html
from dataclasses import dataclass, field
from pathlib import Path


@dataclass
class Node:
    name: str
    value: float = 0.0
    children: dict[str, "Node"] = field(default_factory=dict)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("collapsed", type=Path)
    parser.add_argument("svg", type=Path)
    parser.add_argument("--width", type=int, default=8000)
    parser.add_argument("--frame-height", type=int, default=16)
    parser.add_argument("--title", default="JFR CPU-time flame graph")
    return parser.parse_args()


def add_stack(root: Node, stack: str, value: float) -> None:
    frames = [frame for frame in stack.split(";") if frame]
    if not frames:
        return

    root.value += value
    node = root
    for frame in frames:
        node = node.children.setdefault(frame, Node(frame))
        node.value += value


def read_collapsed(path: Path) -> Node:
    root = Node("all")
    with path.open("r", encoding="utf-8", errors="replace") as input_file:
        for line in input_file:
            line = line.rstrip()
            if not line:
                continue
            try:
                stack, value_text = line.rsplit(" ", 1)
                value = float(value_text)
            except ValueError:
                continue
            add_stack(root, stack, value)
    return root


def max_depth(node: Node) -> int:
    if not node.children:
        return 1
    return 1 + max(max_depth(child) for child in node.children.values())


def color_for(name: str) -> str:
    digest = hashlib.sha1(name.encode("utf-8", errors="replace")).digest()
    hue = digest[0] * 360 // 255
    saturation = 52 + digest[1] % 28
    lightness = 58 + digest[2] % 16
    return f"hsl({hue}, {saturation}%, {lightness}%)"


def text_for(name: str, width: float) -> str:
    max_chars = int(max(0, (width - 8) / 7))
    if max_chars < 4:
        return ""
    if len(name) <= max_chars:
        return name
    return name[: max_chars - 1] + "..."


def render_node(lines: list[str], node: Node, x: float, y: int, width: float, frame_height: int, total: float) -> None:
    if node.name != "all":
        escaped_name = html.escape(node.name)
        escaped_text = html.escape(text_for(node.name, width))
        percent = 100.0 * node.value / total if total else 0.0
        lines.append(
            f'<g><title>{escaped_name} ({node.value:.0f} samples, {percent:.2f}%)</title>'
            f'<rect x="{x:.3f}" y="{y}" width="{width:.3f}" height="{frame_height - 1}" '
            f'rx="2" ry="2" fill="{color_for(node.name)}"/>'
            f'<text x="{x + 3:.3f}" y="{y + frame_height - 4}" '
            f'font-size="11" font-family="Menlo, Consolas, monospace" fill="#111">{escaped_text}</text></g>'
        )

    child_x = x
    child_y = y if node.name == "all" else y - frame_height
    for child in sorted(node.children.values(), key=lambda item: item.value, reverse=True):
        child_width = width * child.value / node.value if node.value else 0.0
        if child_width >= 0.5:
            render_node(lines, child, child_x, child_y, child_width, frame_height, total)
        child_x += child_width


def main() -> None:
    args = parse_args()
    root = read_collapsed(args.collapsed)
    if root.value <= 0:
        raise SystemExit(f"No samples found in {args.collapsed}")

    margin_top = 44
    margin_bottom = 28
    depth = max_depth(root)
    height = margin_top + margin_bottom + depth * args.frame_height
    graph_width = max(320, args.width)
    graph_height = depth * args.frame_height
    graph_top = margin_top
    root_y = graph_top + graph_height - args.frame_height

    lines = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{graph_width}" height="{height}" '
        f'viewBox="0 0 {graph_width} {height}">',
        '<style>text{pointer-events:none} rect{stroke:#fff;stroke-width:.5}</style>',
        '<rect width="100%" height="100%" fill="#f8f8f8"/>',
        f'<text x="12" y="24" font-size="18" font-family="Menlo, Consolas, monospace" '
        f'fill="#111">{html.escape(args.title)}</text>',
        f'<text x="12" y="40" font-size="12" font-family="Menlo, Consolas, monospace" '
        f'fill="#555">total samples: {root.value:.0f}</text>',
    ]
    render_node(lines, root, 0.0, root_y, float(graph_width), args.frame_height, root.value)
    lines.append("</svg>")

    args.svg.parent.mkdir(parents=True, exist_ok=True)
    args.svg.write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
