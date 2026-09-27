# Calculated Workbench palette

This palette uses the original RDF4J logo only as a measured hue source. It does not claim that color mathematics proves aesthetic quality; the hue relation is computed, while token lightness and chroma are design choices verified against accessibility contrast thresholds.

The source is [`tools/workbench/src/main/webapp/images/logo.png`](../../tools/workbench/src/main/webapp/images/logo.png), 132×80 RGBA PNG, SHA-256 `d5badfdce7a705c5790b72506eea98f696de5f9a9345ddb07bc820a839e7ea2e`. The exact original is copied to [images/rdf4j-logo-reference.png](images/rdf4j-logo-reference.png) without editing.

Pixels count as opaque only when alpha is exactly 255; translucent/antialiased pixels are excluded. Of 3,298 opaque pixels, the documented orange detector (OKLCH hue 20–70°, chroma ≥ 0.08) selects 1,685. The dominant pixel is `#DA5800` (1,682 pixels), OKLCH `L=0.62069 C=0.17991 h=44.20649°`.

The count-weighted circular mean hue across those orange pixels is 44.206467° (the most common pixel is 44.206490°). Its complementary hue `(h + 180°) mod 360°` is **224.206467°**. For each chromatic token, the chosen OKLCH lightness/chroma is converted in D65 linear-light sRGB; the hue stays at this complement. Requested L/C are explicit design choices. When a request falls outside sRGB, the script reduces chroma by binary search while keeping L/h; final contrast is measured from the rounded 8-bit hex output. Near-achromatic output (`C < 0.000004`) stores hue as null rather than assigning an unstable angle.

## Exact CSS swatches

The gallery renders these same 8-bit hex values with CSS. The unchanged orange logo appears only as the required source asset; the interface tokens below contain no orange.

| Token | Hex | Chosen OKLCH (L C h°) | Purpose |
|---|---|---|---|
| Canvas | `#F5F9FB` | `0.980 0.005 224.206` | cool neutral page backdrop |
| Surface | `#FFFFFF` | `neutral white` | content surfaces |
| Primary ink | `#14242B` | `0.250 0.025 224.206` | headings and primary text |
| Body text | `#2F4047` | `0.360 0.025 224.206` | standard reading text |
| Muted text | `#4F6168` | `0.480 0.025 224.206` | secondary text |
| Control outline | `#778A92` | `0.620 0.025 224.206` | visible field/control outline |
| Primary action / focus | `#20576A` | `0.430 0.065 224.206` | primary action and focus |
| Primary hover | `#16495A` | `0.380 0.060 224.206` | primary hover state |
| Selected surface | `#DEF3FB` | `0.950 0.025 224.206` | selected/checked background |

## Exact dark-theme CSS swatches

Dark surfaces use the same computed complementary hue. The bright primary action uses the dark canvas token for its text; white text would fail contrast on that light-blue action.

| Token | Hex | Chosen OKLCH (L C h°) | Purpose |
|---|---|---|---|
| canvas | `#080E11` | `0.159 0.012 224.206` | dark page backdrop |
| surface | `#111A1D` | `0.210 0.014 224.206` | dark content surface |
| raisedSurface | `#182428` | `0.251 0.018 224.206` | raised menu and control surface |
| ink | `#E8F0F3` | `0.950 0.009 224.206` | headings and primary reading text |
| body | `#CEDADF` | `0.881 0.015 224.206` | standard reading text |
| muted | `#9AABB2` | `0.730 0.022 224.206` | secondary text |
| controlOutline | `#687E87` | `0.579 0.029 224.206` | visible control boundary |
| primary | `#73C4E2` | `0.780 0.090 224.206` | primary action and focus |
| primaryHover | `#94D6EF` | `0.841 0.075 224.206` | primary hover state |
| selectedSurface | `#15323D` | `0.300 0.040 224.206` | selected/checked background |
| focusRing | `#73C4E2` | `0.780 0.090 224.206` | keyboard-visible focus |
| primaryText | `#080E11` | `0.159 0.012 224.206` | text on bright primary actions |

## Contrast computed from final hex values

Body and muted text target at least 4.5:1 on both page canvas and white surfaces. Control outline and focus ring target at least 3:1 against adjacent white/canvas surfaces. Primary button text is white. Ratios below use exact 8-bit token hex values, not unrounded OKLCH intermediates.

| Pair | Ratio | Threshold | Result |
|---|---:|---:|---|
| Body text on white | 10.796:1 | 4.5:1 | Pass |
| Body text on canvas | 10.192:1 | 4.5:1 | Pass |
| Muted text on white | 6.477:1 | 4.5:1 | Pass |
| Muted text on canvas | 6.115:1 | 4.5:1 | Pass |
| Control outline on white | 3.599:1 | 3.0:1 | Pass |
| Control outline on canvas | 3.398:1 | 3.0:1 | Pass |
| Control outline on selected surface | 3.140:1 | 3.0:1 | Pass |
| Focus ring on white | 7.976:1 | 3.0:1 | Pass |
| Focus ring on canvas | 7.530:1 | 3.0:1 | Pass |
| White button text on primary | 7.976:1 | 4.5:1 | Pass |
| White button text on hover | 9.837:1 | 4.5:1 | Pass |
| Primary ink on selected surface | 13.910:1 | 4.5:1 | Pass |

## Dark-theme contrast computed from final hex values

Body and muted text, control outlines, focus ring, selected state, and primary-action text are checked against their actual adjacent 8-bit dark-theme colors. Primary actions use `primaryText` rather than white text.

| Pair | Ratio | Threshold | Result |
|---|---:|---:|---|
| Body text on dark surface | 12.375:1 | 4.5:1 | Pass |
| Body text on dark canvas | 13.613:1 | 4.5:1 | Pass |
| Muted text on dark surface | 7.434:1 | 4.5:1 | Pass |
| Muted text on dark canvas | 8.178:1 | 4.5:1 | Pass |
| Muted text on raised surface | 6.689:1 | 4.5:1 | Pass |
| Control outline on dark surface | 4.139:1 | 3.0:1 | Pass |
| Control outline on dark canvas | 4.553:1 | 3.0:1 | Pass |
| Control outline on raised surface | 3.724:1 | 3.0:1 | Pass |
| Control outline on selected surface | 3.165:1 | 3.0:1 | Pass |
| Focus ring on dark surface | 9.016:1 | 3.0:1 | Pass |
| Focus ring on dark canvas | 9.917:1 | 3.0:1 | Pass |
| Dark primary text on primary | 9.917:1 | 4.5:1 | Pass |
| Dark primary text on hover | 12.138:1 | 4.5:1 | Pass |
| Ink on selected surface | 11.694:1 | 4.5:1 | Pass |

Control geometry, errors, warnings, and status must remain distinguishable without hue alone. Use a non-orange red/rose for errors and non-orange semantic colors for other statuses; do not introduce orange in UI chrome or syntax highlighting.

## Reproduction and references

Run `python3 design/workbench-design-coverage-20260925/calculate-palette.py` from the repository root to regenerate `palette.json` and this report. The converter uses OKLab/OKLCH and gamut handling described by the [W3C CSS Color Module Level 4](https://www.w3.org/TR/css-color-4/#ok-lab). Contrast thresholds follow the [WCAG 2.2 minimum contrast guidance](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html) and [non-text contrast guidance](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html).
