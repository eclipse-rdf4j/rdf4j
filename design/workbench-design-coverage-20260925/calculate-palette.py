#!/usr/bin/env python3
"""Derive an sRGB-safe complementary palette from the original RDF4J logo."""

from __future__ import annotations

import hashlib
import json
import math
import struct
import zlib
from collections import Counter
from pathlib import Path


DESIGN_DIR = Path(__file__).resolve().parent
REPO_ROOT = DESIGN_DIR.parents[1]
LOGO = REPO_ROOT / "tools/workbench/src/main/webapp/images/logo.png"
ORANGE_HUE_MIN = 20.0
ORANGE_HUE_MAX = 70.0
ORANGE_MIN_CHROMA = 0.08


def decode_logo_rgba(path: Path) -> tuple[int, int, list[tuple[int, int, int, int]]]:
	"""Decode non-interlaced 8-bit RGBA PNG using only the Python standard library."""
	data = path.read_bytes()
	if not data.startswith(b"\x89PNG\r\n\x1a\n"):
		raise ValueError(f"Not a PNG file: {path}")

	position = 8
	width = height = color_type = bit_depth = interlace = None
	compressed = bytearray()
	while position < len(data):
		length = struct.unpack_from(">I", data, position)[0]
		kind = data[position + 4 : position + 8]
		payload = data[position + 8 : position + 8 + length]
		crc = struct.unpack_from(">I", data, position + 8 + length)[0]
		if zlib.crc32(kind + payload) & 0xFFFFFFFF != crc:
			raise ValueError(f"Bad PNG chunk CRC: {kind!r}")
		position += length + 12
		if kind == b"IHDR":
			width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(
				">IIBBBBB", payload
			)
			if (bit_depth, color_type, compression, filtering, interlace) != (8, 6, 0, 0, 0):
				raise ValueError("Expected non-interlaced 8-bit RGBA PNG")
		elif kind == b"IDAT":
			compressed.extend(payload)
		elif kind == b"IEND":
			break

	assert width is not None and height is not None
	bytes_per_pixel = 4
	stride = width * bytes_per_pixel
	raw = zlib.decompress(compressed)
	if len(raw) != height * (stride + 1):
		raise ValueError("Unexpected decoded PNG byte count")

	rows: list[bytearray] = []
	previous = bytearray(stride)
	position = 0
	for _ in range(height):
		filter_type = raw[position]
		position += 1
		row = bytearray(raw[position : position + stride])
		position += stride
		for index in range(stride):
			left = row[index - bytes_per_pixel] if index >= bytes_per_pixel else 0
			above = previous[index]
			upper_left = previous[index - bytes_per_pixel] if index >= bytes_per_pixel else 0
			if filter_type == 1:
				predictor = left
			elif filter_type == 2:
				predictor = above
			elif filter_type == 3:
				predictor = (left + above) // 2
			elif filter_type == 4:
				predictor = _paeth(left, above, upper_left)
			elif filter_type == 0:
				predictor = 0
			else:
				raise ValueError(f"Unsupported PNG filter: {filter_type}")
			row[index] = (row[index] + predictor) & 0xFF
		rows.append(row)
		previous = row

	pixels = [tuple(row[i : i + 4]) for row in rows for i in range(0, stride, 4)]
	return width, height, pixels


def _paeth(left: int, above: int, upper_left: int) -> int:
	p = left + above - upper_left
	pa = abs(p - left)
	pb = abs(p - above)
	pc = abs(p - upper_left)
	return left if pa <= pb and pa <= pc else above if pb <= pc else upper_left


def srgb_to_linear(channel: float) -> float:
	return channel / 12.92 if channel <= 0.04045 else ((channel + 0.055) / 1.055) ** 2.4


def linear_to_srgb(channel: float) -> float:
	return 12.92 * channel if channel <= 0.0031308 else 1.055 * channel ** (1 / 2.4) - 0.055


def rgb_to_oklch(rgb: tuple[int, int, int]) -> tuple[float, float, float | None]:
	r, g, b = (srgb_to_linear(value / 255) for value in rgb)
	l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
	m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
	s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b
	l_root, m_root, s_root = (math.copysign(abs(value) ** (1 / 3), value) for value in (l, m, s))
	lightness = 0.2104542553 * l_root + 0.7936177850 * m_root - 0.0040720468 * s_root
	a = 1.9779984951 * l_root - 2.4285922050 * m_root + 0.4505937099 * s_root
	b = 0.0259040371 * l_root + 0.7827717662 * m_root - 0.8086757660 * s_root
	chroma = math.hypot(a, b)
	return lightness, chroma, math.degrees(math.atan2(b, a)) % 360 if chroma >= 4e-6 else None


def oklch_to_linear_srgb(lightness: float, chroma: float, hue: float) -> tuple[float, float, float]:
	a = chroma * math.cos(math.radians(hue))
	b = chroma * math.sin(math.radians(hue))
	l_root = lightness + 0.3963377774 * a + 0.2158037573 * b
	m_root = lightness - 0.1055613458 * a - 0.0638541728 * b
	s_root = lightness - 0.0894841775 * a - 1.2914855480 * b
	l, m, s = l_root**3, m_root**3, s_root**3
	return (
		4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
		-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
		-0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
	)


def oklch_to_hex(lightness: float, chroma: float, hue: float) -> tuple[str, bool]:
	linear = oklch_to_linear_srgb(lightness, chroma, hue)
	in_gamut = all(0 <= channel <= 1 for channel in linear)
	if not in_gamut:
		low, high = 0.0, chroma
		for _ in range(64):
			candidate = (low + high) / 2
			if all(0 <= channel <= 1 for channel in oklch_to_linear_srgb(lightness, candidate, hue)):
				low = candidate
			else:
				high = candidate
		chroma = low
		linear = oklch_to_linear_srgb(lightness, chroma, hue)
	encoded = [round(max(0.0, min(1.0, linear_to_srgb(channel))) * 255) for channel in linear]
	return "#" + "".join(f"{channel:02X}" for channel in encoded), in_gamut


def relative_luminance(hex_color: str) -> float:
	rgb = [int(hex_color[index : index + 2], 16) / 255 for index in (1, 3, 5)]
	r, g, b = (srgb_to_linear(channel) for channel in rgb)
	return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast_ratio(first: str, second: str) -> float:
	one, two = relative_luminance(first), relative_luminance(second)
	return (max(one, two) + 0.05) / (min(one, two) + 0.05)


def main() -> None:
	width, height, pixels = decode_logo_rgba(LOGO)
	opaque = [pixel[:3] for pixel in pixels if pixel[3] == 255]
	orange_histogram: Counter[tuple[int, int, int]] = Counter()
	for rgb in opaque:
		_, chroma, hue = rgb_to_oklch(rgb)
		if hue is not None and ORANGE_HUE_MIN <= hue <= ORANGE_HUE_MAX and chroma >= ORANGE_MIN_CHROMA:
			orange_histogram[rgb] += 1
	if not orange_histogram:
		raise ValueError("No opaque orange pixels matched the documented selection rule")

	sin_total = sum(count * math.sin(math.radians(rgb_to_oklch(rgb)[2])) for rgb, count in orange_histogram.items())
	cos_total = sum(count * math.cos(math.radians(rgb_to_oklch(rgb)[2])) for rgb, count in orange_histogram.items())
	orange_hue = math.degrees(math.atan2(sin_total, cos_total)) % 360
	complement_hue = (orange_hue + 180) % 360
	dominant_rgb, dominant_count = orange_histogram.most_common(1)[0]
	logo_bytes = LOGO.read_bytes()

	# These OKLCH lightness/chroma pairs are design choices on the measured complementary hue.
	# The converter preserves the requested hue and reduces chroma only if sRGB gamut requires it.
	token_specs = {
		"canvas": (0.98, 0.005),
		"surface": None,
		"ink": (0.25, 0.025),
		"body": (0.36, 0.025),
		"muted": (0.48, 0.025),
		"controlOutline": (0.62, 0.025),
		"primary": (0.43, 0.065),
		"primaryHover": (0.38, 0.060),
		"selectedSurface": (0.95, 0.025),
		"focusRing": (0.43, 0.065),
	}
	dark_token_specs = {
		"canvas": (0.158771844, 0.011572725),
		"surface": (0.210321796, 0.014431327),
		"raisedSurface": (0.250920352, 0.018318816),
		"ink": (0.949966823, 0.009460191),
		"body": (0.880725779, 0.014711673),
		"muted": (0.730197651, 0.021597588),
		"controlOutline": (0.578738501, 0.029112560),
		"primary": (0.779554447, 0.090040505),
		"primaryHover": (0.840521868, 0.075084490),
		"selectedSurface": (0.300000920, 0.039987400),
		"focusRing": (0.779554447, 0.090040505),
		"primaryText": (0.158771844, 0.011572725),
	}

	def build_tokens(specs):
		result = {}
		for name, spec in specs.items():
			if spec is None:
				hex_color, in_gamut = "#FFFFFF", True
				expected_oklch = None
			else:
				lightness, chroma = spec
				hex_color, in_gamut = oklch_to_hex(lightness, chroma, complement_hue)
				expected_oklch = {"L": lightness, "C": chroma, "h": complement_hue}
			rgb = tuple(int(hex_color[index : index + 2], 16) for index in (1, 3, 5))
			actual_lightness, actual_chroma, actual_hue = rgb_to_oklch(rgb)
			result[name] = {
				"hex": hex_color,
				"sourceOKLCHChoice": expected_oklch,
				"actual8bitOKLCH": {
					"L": actual_lightness,
					"C": actual_chroma,
					"h": actual_hue,
				},
				"inSRGBGamutBefore8bitRounding": in_gamut,
			}
		return result

	tokens = build_tokens(token_specs)
	dark_tokens = build_tokens(dark_token_specs)

	colors = {name: record["hex"] for name, record in tokens.items()}
	dark_colors = {name: record["hex"] for name, record in dark_tokens.items()}
	contrast = {
		"bodyOnWhite": contrast_ratio(colors["body"], colors["surface"]),
		"bodyOnCanvas": contrast_ratio(colors["body"], colors["canvas"]),
		"mutedOnWhite": contrast_ratio(colors["muted"], colors["surface"]),
		"mutedOnCanvas": contrast_ratio(colors["muted"], colors["canvas"]),
		"controlOutlineAgainstWhite": contrast_ratio(colors["controlOutline"], colors["surface"]),
		"controlOutlineAgainstCanvas": contrast_ratio(colors["controlOutline"], colors["canvas"]),
		"controlOutlineAgainstSelectedSurface": contrast_ratio(colors["controlOutline"], colors["selectedSurface"]),
		"focusRingAgainstWhite": contrast_ratio(colors["focusRing"], colors["surface"]),
		"focusRingAgainstCanvas": contrast_ratio(colors["focusRing"], colors["canvas"]),
		"whiteTextOnPrimary": contrast_ratio(colors["surface"], colors["primary"]),
		"whiteTextOnPrimaryHover": contrast_ratio(colors["surface"], colors["primaryHover"]),
		"inkOnSelectedSurface": contrast_ratio(colors["ink"], colors["selectedSurface"]),
	}
	dark_contrast = {
		"bodyOnSurface": contrast_ratio(dark_colors["body"], dark_colors["surface"]),
		"bodyOnCanvas": contrast_ratio(dark_colors["body"], dark_colors["canvas"]),
		"mutedOnSurface": contrast_ratio(dark_colors["muted"], dark_colors["surface"]),
		"mutedOnCanvas": contrast_ratio(dark_colors["muted"], dark_colors["canvas"]),
		"mutedOnRaisedSurface": contrast_ratio(dark_colors["muted"], dark_colors["raisedSurface"]),
		"controlOutlineAgainstSurface": contrast_ratio(dark_colors["controlOutline"], dark_colors["surface"]),
		"controlOutlineAgainstCanvas": contrast_ratio(dark_colors["controlOutline"], dark_colors["canvas"]),
		"controlOutlineAgainstRaisedSurface": contrast_ratio(
			dark_colors["controlOutline"], dark_colors["raisedSurface"]
		),
		"controlOutlineAgainstSelectedSurface": contrast_ratio(
			dark_colors["controlOutline"], dark_colors["selectedSurface"]
		),
		"focusRingAgainstSurface": contrast_ratio(dark_colors["focusRing"], dark_colors["surface"]),
		"focusRingAgainstCanvas": contrast_ratio(dark_colors["focusRing"], dark_colors["canvas"]),
		"primaryTextOnPrimary": contrast_ratio(dark_colors["primaryText"], dark_colors["primary"]),
		"primaryTextOnHover": contrast_ratio(dark_colors["primaryText"], dark_colors["primaryHover"]),
		"inkOnSelectedSurface": contrast_ratio(dark_colors["ink"], dark_colors["selectedSurface"]),
	}
	checks = {
		name: {
			"ratio": ratio,
			"required": 3.0 if "Outline" in name or "focusRing" in name else 4.5,
			"pass": ratio >= (3.0 if "Outline" in name or "focusRing" in name else 4.5),
		}
		for name, ratio in contrast.items()
	}
	dark_checks = {
		name: {
			"ratio": ratio,
			"required": 3.0 if "Outline" in name or "focusRing" in name else 4.5,
			"pass": ratio >= (3.0 if "Outline" in name or "focusRing" in name else 4.5),
		}
		for name, ratio in dark_contrast.items()
	}
	if not all(check["pass"] for check in (*checks.values(), *dark_checks.values())):
		raise ValueError("Chosen 8-bit palette failed a declared contrast threshold")

	palette = {
		"source": {
			"logoPath": str(LOGO.relative_to(REPO_ROOT)),
			"dimensions": [width, height],
			"sha256": hashlib.sha256(logo_bytes).hexdigest(),
			"opaquePixelRule": "alpha == 255; antialiased/translucent pixels are excluded",
			"opaquePixels": len(opaque),
			"distinctOpaqueColors": len(Counter(opaque)),
			"orangePixelRule": (
				f"OKLCH hue {ORANGE_HUE_MIN:g}..{ORANGE_HUE_MAX:g} degrees and chroma >= "
				f"{ORANGE_MIN_CHROMA:g}, computed from opaque pixels"
			),
			"opaqueOrangePixels": sum(orange_histogram.values()),
			"countWeightedCircularMeanHueDegrees": orange_hue,
			"dominantOpaqueOrange": {
				"hex": "#" + "".join(f"{channel:02X}" for channel in dominant_rgb),
				"rgb8": dominant_rgb,
				"pixels": dominant_count,
				"OKLCH": dict(zip(("L", "C", "h"), rgb_to_oklch(dominant_rgb))),
			},
			"opaqueOrangeHistogram": [
				{
					"hex": "#" + "".join(f"{channel:02X}" for channel in rgb),
					"rgb8": rgb,
					"pixels": count,
					"OKLCH": dict(zip(("L", "C", "h"), rgb_to_oklch(rgb))),
				}
				for rgb, count in orange_histogram.most_common()
			],
		},
		"method": {
			"colorModel": "CSS Color 4 OKLab/OKLCH, D65, conversion performed in linear-light sRGB",
			"complementDefinition": "Use the count-weighted circular mean hue of detected opaque orange pixels, then add 180 degrees modulo 360; preserve lightness and chosen chroma per token.",
			"countWeightedOrangeHueDegrees": orange_hue,
			"complementHueDegrees": complement_hue,
			"intentionalDesignChoices": "Token lightness/chroma are design choices; the logo-derived complementary hue is the computed part.",
			"sRGBGamut": "Reduce chroma by binary search only when requested L/C/h is outside sRGB; no clipping before 8-bit encoding.",
			"contrastMethod": "WCAG relative luminance from final 8-bit sRGB hex values, then (L1 + 0.05) / (L2 + 0.05).",
		},
		"tokens": tokens,
		"darkTokens": dark_tokens,
		"contrast": checks,
		"darkContrast": dark_checks,
	}
	(DESIGN_DIR / "palette.json").write_text(json.dumps(palette, indent=2) + "\n")
	write_palette_markdown(palette)
	print(f"logo dominant opaque orange: #{dominant_rgb[0]:02X}{dominant_rgb[1]:02X}{dominant_rgb[2]:02X} "
		f"({dominant_count} pixels), OKLCH {rgb_to_oklch(dominant_rgb)}")
	print(f"opaque orange pixels: {sum(orange_histogram.values())}; complementary OKLCH hue: {complement_hue:.6f} degrees")
	for name, record in tokens.items():
		print(f"{name}: {record['hex']}")
	for name, check in checks.items():
		print(f"{name}: {check['ratio']:.3f}:1 (required {check['required']:.1f}:1) {'PASS' if check['pass'] else 'FAIL'}")
	for name, record in dark_tokens.items():
		print(f"dark.{name}: {record['hex']}")
	for name, check in dark_checks.items():
		print(f"dark.{name}: {check['ratio']:.3f}:1 (required {check['required']:.1f}:1) {'PASS' if check['pass'] else 'FAIL'}")


def write_palette_markdown(palette: dict) -> None:
	source = palette["source"]
	dominant = source["dominantOpaqueOrange"]
	tokens = palette["tokens"]
	dark_tokens = palette["darkTokens"]
	checks = palette["contrast"]
	dark_checks = palette["darkContrast"]
	rows = [
		("Canvas", "canvas"),
		("Surface", "surface"),
		("Primary ink", "ink"),
		("Body text", "body"),
		("Muted text", "muted"),
		("Control outline", "controlOutline"),
		("Primary action / focus", "primary"),
		("Primary hover", "primaryHover"),
		("Selected surface", "selectedSurface"),
	]
	contrast_rows = [
		("Body text on white", "bodyOnWhite"),
		("Body text on canvas", "bodyOnCanvas"),
		("Muted text on white", "mutedOnWhite"),
		("Muted text on canvas", "mutedOnCanvas"),
		("Control outline on white", "controlOutlineAgainstWhite"),
		("Control outline on canvas", "controlOutlineAgainstCanvas"),
		("Control outline on selected surface", "controlOutlineAgainstSelectedSurface"),
		("Focus ring on white", "focusRingAgainstWhite"),
		("Focus ring on canvas", "focusRingAgainstCanvas"),
		("White button text on primary", "whiteTextOnPrimary"),
		("White button text on hover", "whiteTextOnPrimaryHover"),
		("Primary ink on selected surface", "inkOnSelectedSurface"),
	]
	lines = [
		"# Calculated Workbench palette",
		"",
		"This palette uses the original RDF4J logo only as a measured hue source. It does not claim that color mathematics proves aesthetic quality; the hue relation is computed, while token lightness and chroma are design choices verified against accessibility contrast thresholds.",
		"",
		f"The source is [`{source['logoPath']}`](../../{source['logoPath']}), {source['dimensions'][0]}×{source['dimensions'][1]} RGBA PNG, SHA-256 `{source['sha256']}`. The exact original is copied to [images/rdf4j-logo-reference.png](images/rdf4j-logo-reference.png) without editing.",
		"",
		f"Pixels count as opaque only when alpha is exactly 255; translucent/antialiased pixels are excluded. Of {source['opaquePixels']:,} opaque pixels, the documented orange detector (OKLCH hue {ORANGE_HUE_MIN:g}–{ORANGE_HUE_MAX:g}°, chroma ≥ {ORANGE_MIN_CHROMA:g}) selects {source['opaqueOrangePixels']:,}. The dominant pixel is `{dominant['hex']}` ({dominant['pixels']:,} pixels), OKLCH `L={dominant['OKLCH']['L']:.5f} C={dominant['OKLCH']['C']:.5f} h={dominant['OKLCH']['h']:.5f}°`.",
		"",
		f"The count-weighted circular mean hue across those orange pixels is {source['countWeightedCircularMeanHueDegrees']:.6f}° (the most common pixel is {dominant['OKLCH']['h']:.6f}°). Its complementary hue `(h + 180°) mod 360°` is **{palette['method']['complementHueDegrees']:.6f}°**. For each chromatic token, the chosen OKLCH lightness/chroma is converted in D65 linear-light sRGB; the hue stays at this complement. Requested L/C are explicit design choices. When a request falls outside sRGB, the script reduces chroma by binary search while keeping L/h; final contrast is measured from the rounded 8-bit hex output. Near-achromatic output (`C < 0.000004`) stores hue as null rather than assigning an unstable angle.",
		"",
		"## Exact CSS swatches",
		"",
		"The gallery renders these same 8-bit hex values with CSS. The unchanged orange logo appears only as the required source asset; the interface tokens below contain no orange.",
		"",
		"| Token | Hex | Chosen OKLCH (L C h°) | Purpose |",
		"|---|---|---|---|",
	]
	purpose = {
		"canvas": "cool neutral page backdrop",
		"surface": "content surfaces",
		"ink": "headings and primary text",
		"body": "standard reading text",
		"muted": "secondary text",
		"controlOutline": "visible field/control outline",
		"primary": "primary action and focus",
		"primaryHover": "primary hover state",
		"focusRing": "keyboard-visible focus",
		"selectedSurface": "selected/checked background",
	}
	for label, name in rows:
		token = tokens[name]
		choice = token["sourceOKLCHChoice"]
		choice_text = "neutral white" if choice is None else f"{choice['L']:.3f} {choice['C']:.3f} {choice['h']:.3f}"
		lines.append(f"| {label} | `{token['hex']}` | `{choice_text}` | {purpose[name]} |")
	lines += [
		"",
		"## Exact dark-theme CSS swatches",
		"",
		"Dark surfaces use the same computed complementary hue. The bright primary action uses the dark canvas token for its text; white text would fail contrast on that light-blue action.",
		"",
		"| Token | Hex | Chosen OKLCH (L C h°) | Purpose |",
		"|---|---|---|---|",
	]
	dark_purpose = {
		"canvas": "dark page backdrop",
		"surface": "dark content surface",
		"raisedSurface": "raised menu and control surface",
		"ink": "headings and primary reading text",
		"body": "standard reading text",
		"muted": "secondary text",
		"controlOutline": "visible control boundary",
		"primary": "primary action and focus",
		"primaryHover": "primary hover state",
		"focusRing": "keyboard-visible focus",
		"selectedSurface": "selected/checked background",
		"primaryText": "text on bright primary actions",
	}
	for name, token in dark_tokens.items():
		choice = token["sourceOKLCHChoice"]
		choice_text = f"{choice['L']:.3f} {choice['C']:.3f} {choice['h']:.3f}"
		lines.append(f"| {name} | `{token['hex']}` | `{choice_text}` | {dark_purpose[name]} |")
	lines += [
		"",
		"## Contrast computed from final hex values",
		"",
		"Body and muted text target at least 4.5:1 on both page canvas and white surfaces. Control outline and focus ring target at least 3:1 against adjacent white/canvas surfaces. Primary button text is white. Ratios below use exact 8-bit token hex values, not unrounded OKLCH intermediates.",
		"",
		"| Pair | Ratio | Threshold | Result |",
		"|---|---:|---:|---|",
	]
	for label, name in contrast_rows:
		check = checks[name]
		lines.append(f"| {label} | {check['ratio']:.3f}:1 | {check['required']:.1f}:1 | {'Pass' if check['pass'] else 'Fail'} |")
	lines += [
		"",
		"## Dark-theme contrast computed from final hex values",
		"",
		"Body and muted text, control outlines, focus ring, selected state, and primary-action text are checked against their actual adjacent 8-bit dark-theme colors. Primary actions use `primaryText` rather than white text.",
		"",
		"| Pair | Ratio | Threshold | Result |",
		"|---|---:|---:|---|",
	]
	dark_contrast_rows = [
		("Body text on dark surface", "bodyOnSurface"),
		("Body text on dark canvas", "bodyOnCanvas"),
		("Muted text on dark surface", "mutedOnSurface"),
		("Muted text on dark canvas", "mutedOnCanvas"),
		("Muted text on raised surface", "mutedOnRaisedSurface"),
		("Control outline on dark surface", "controlOutlineAgainstSurface"),
		("Control outline on dark canvas", "controlOutlineAgainstCanvas"),
		("Control outline on raised surface", "controlOutlineAgainstRaisedSurface"),
		("Control outline on selected surface", "controlOutlineAgainstSelectedSurface"),
		("Focus ring on dark surface", "focusRingAgainstSurface"),
		("Focus ring on dark canvas", "focusRingAgainstCanvas"),
		("Dark primary text on primary", "primaryTextOnPrimary"),
		("Dark primary text on hover", "primaryTextOnHover"),
		("Ink on selected surface", "inkOnSelectedSurface"),
	]
	for label, name in dark_contrast_rows:
		check = dark_checks[name]
		lines.append(f"| {label} | {check['ratio']:.3f}:1 | {check['required']:.1f}:1 | {'Pass' if check['pass'] else 'Fail'} |")
	lines += [
		"",
		"Control geometry, errors, warnings, and status must remain distinguishable without hue alone. Use a non-orange red/rose for errors and non-orange semantic colors for other statuses; do not introduce orange in UI chrome or syntax highlighting.",
		"",
		"## Reproduction and references",
		"",
		"Run `python3 design/workbench-design-coverage-20260925/calculate-palette.py` from the repository root to regenerate `palette.json` and this report. The converter uses OKLab/OKLCH and gamut handling described by the [W3C CSS Color Module Level 4](https://www.w3.org/TR/css-color-4/#ok-lab). Contrast thresholds follow the [WCAG 2.2 minimum contrast guidance](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html) and [non-text contrast guidance](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html).",
		"",
	]
	(DESIGN_DIR / "PALETTE.md").write_text("\n".join(lines))


if __name__ == "__main__":
	main()
