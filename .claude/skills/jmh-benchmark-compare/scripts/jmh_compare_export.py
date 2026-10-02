#!/usr/bin/env python3
"""Export functions for JMH compare tables."""

from __future__ import annotations

import csv
import html
import zipfile
from pathlib import Path
from typing import Dict, List
from xml.sax.saxutils import escape as xml_escape

from jmh_compare_core import TableData, format_value


def render_txt(table: TableData) -> str:
    rendered = [[format_value(col, row.get(col), table.percent_columns) for col in table.columns] for row in table.rows]
    widths = [len(col) for col in table.columns]
    for row in rendered:
        for idx, cell in enumerate(row):
            widths[idx] = max(widths[idx], len(cell))
    header = "  ".join(col.ljust(widths[idx]) for idx, col in enumerate(table.columns))
    sep = "  ".join("-" * width for width in widths)
    body = []
    for row in rendered:
        out = []
        for idx, cell in enumerate(row):
            col = table.columns[idx]
            out.append(cell.rjust(widths[idx]) if col in table.numeric_columns else cell.ljust(widths[idx]))
        body.append("  ".join(out))
    return "\n".join([header, sep] + body)


def render_md(table: TableData) -> str:
    header = "| " + " | ".join(table.columns) + " |"
    align = "| " + " | ".join("---:" if col in table.numeric_columns else ":---" for col in table.columns) + " |"
    rows = []
    for row in table.rows:
        values = [format_value(col, row.get(col), table.percent_columns).replace("|", "\\|") for col in table.columns]
        rows.append("| " + " | ".join(values) + " |")
    return "\n".join([header, align] + rows)


def render_csv(table: TableData, path: Path) -> None:
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(table.columns)
        for row in table.rows:
            writer.writerow([format_value(col, row.get(col), table.percent_columns) for col in table.columns])


def col_ref(index: int) -> str:
    letters: List[str] = []
    n = index
    while n > 0:
        n, rem = divmod(n - 1, 26)
        letters.append(chr(ord("A") + rem))
    return "".join(reversed(letters))


def render_xlsx(table: TableData, path: Path) -> None:
    rows_xml = []
    for r_idx, row in enumerate([dict(zip(table.columns, table.columns))] + table.rows, start=1):
        cells = []
        for c_idx, column in enumerate(table.columns, start=1):
            ref = f"{col_ref(c_idx)}{r_idx}"
            value = column if r_idx == 1 else row.get(column)
            if r_idx > 1 and isinstance(value, float):
                cell = f'<c r="{ref}"><v>{value}</v></c>'
            else:
                text = format_value(column, value, table.percent_columns)
                cell = f'<c r="{ref}" t="inlineStr"><is><t>{xml_escape(text)}</t></is></c>'
            cells.append(cell)
        rows_xml.append(f'<row r="{r_idx}">{"".join(cells)}</row>')

    sheet_xml = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">'
        f"<sheetData>{''.join(rows_xml)}</sheetData></worksheet>"
    )
    workbook_xml = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" '
        'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
        '<sheets><sheet name="comparison" sheetId="1" r:id="rId1"/></sheets></workbook>'
    )
    content_types = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
        '<Default Extension="xml" ContentType="application/xml"/>'
        '<Override PartName="/xl/workbook.xml" '
        'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
        '<Override PartName="/xl/worksheets/sheet1.xml" '
        'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
        "</Types>"
    )
    root_rels = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" '
        'Target="xl/workbook.xml"/></Relationships>'
    )
    workbook_rels = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
        '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" '
        'Target="worksheets/sheet1.xml"/></Relationships>'
    )
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("[Content_Types].xml", content_types)
        archive.writestr("_rels/.rels", root_rels)
        archive.writestr("xl/workbook.xml", workbook_xml)
        archive.writestr("xl/_rels/workbook.xml.rels", workbook_rels)
        archive.writestr("xl/worksheets/sheet1.xml", sheet_xml)


def render_html(table: TableData) -> str:
    headers = []
    for col in table.columns:
        col_type = "number" if col in table.numeric_columns else "string"
        headers.append(f'<th data-type="{col_type}" onclick="sortTable(this)">{html.escape(col)}</th>')

    body_rows = []
    for row in table.rows:
        status_by_col = row.get("__status_by_column", {})
        cells = []
        for col in table.columns:
            value = row.get(col)
            text = format_value(col, value, table.percent_columns)
            if isinstance(value, float):
                sort_value = f"{value:.15g}"
            elif value is None:
                sort_value = ""
            else:
                sort_value = str(value)
            klass = ""
            if isinstance(status_by_col, dict):
                status = status_by_col.get(col, "")
                if status in {"regression", "improvement", "neutral"}:
                    klass = f' class="{status}"'
            cells.append(
                f'<td{klass} data-sort="{html.escape(sort_value)}">{html.escape(text)}</td>'
            )
        body_rows.append("<tr>" + "".join(cells) + "</tr>")

    return f"""<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <title>JMH Benchmark Compare</title>
  <style>
    :root {{ --bg:#f7f8fa; --fg:#0f172a; --table:#ffffff; --border:#cbd5e1; --reg:#ffebe9; --imp:#e8f5e9; --neu:#f5f5f5; }}
    [data-theme="high-contrast"] {{ --bg:#111827; --fg:#f9fafb; --table:#1f2937; --border:#4b5563; --reg:#7f1d1d; --imp:#14532d; --neu:#374151; }}
    [data-theme="colorblind"] {{ --bg:#f6f9ff; --fg:#111827; --table:#ffffff; --border:#94a3b8; --reg:#ffe4cc; --imp:#d9f2d9; --neu:#eef2ff; }}
    body {{ background:var(--bg); color:var(--fg); font-family:ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; margin:0; padding:16px; }}
    .toolbar {{ display:flex; gap:8px; align-items:center; margin-bottom:10px; }}
    table {{ border-collapse:collapse; width:100%; background:var(--table); font-size:13px; }}
    th, td {{ border:1px solid var(--border); padding:6px 8px; }}
    th {{ cursor:pointer; position:sticky; top:0; background:var(--table); }}
    td.regression {{ background:var(--reg); }}
    td.improvement {{ background:var(--imp); }}
    td.neutral {{ background:var(--neu); }}
  </style>
</head>
<body data-theme="default">
  <div class="toolbar">
    <label for="theme">Color theme:</label>
    <select id="theme" onchange="document.body.setAttribute('data-theme', this.value)">
      <option value="default">Default</option>
      <option value="high-contrast">High contrast</option>
      <option value="colorblind">Colorblind friendly</option>
    </select>
  </div>
  <table id="bench">
    <thead><tr>{''.join(headers)}</tr></thead>
    <tbody>{''.join(body_rows)}</tbody>
  </table>
  <script>
    function parseSortValue(cell, type) {{
      const raw = cell.getAttribute('data-sort');
      if (type === 'number') {{
        const n = Number(raw);
        return Number.isNaN(n) ? -Infinity : n;
      }}
      return (raw || '').toLowerCase();
    }}
    function sortTable(th) {{
      const table = document.getElementById('bench');
      const idx = Array.from(th.parentElement.children).indexOf(th);
      const type = th.getAttribute('data-type');
      const tbody = table.tBodies[0];
      const rows = Array.from(tbody.rows);
      const asc = th.getAttribute('data-order') !== 'asc';
      rows.sort((a, b) => {{
        const va = parseSortValue(a.cells[idx], type);
        const vb = parseSortValue(b.cells[idx], type);
        if (va < vb) return asc ? -1 : 1;
        if (va > vb) return asc ? 1 : -1;
        return 0;
      }});
      Array.from(table.querySelectorAll('th')).forEach(x => x.removeAttribute('data-order'));
      th.setAttribute('data-order', asc ? 'asc' : 'desc');
      rows.forEach(r => tbody.appendChild(r));
    }}
  </script>
</body>
</html>
"""


def write_table(table: TableData, fmt: str, path: Path) -> None:
    if fmt == "txt":
        path.write_text(render_txt(table) + "\n", encoding="utf-8")
        return
    if fmt == "md":
        path.write_text(render_md(table) + "\n", encoding="utf-8")
        return
    if fmt == "csv":
        render_csv(table, path)
        return
    if fmt == "xlsx":
        render_xlsx(table, path)
        return
    if fmt == "html":
        path.write_text(render_html(table), encoding="utf-8")
        return
    raise ValueError(f"Unsupported format: {fmt}")


def output_targets(output: str, output_dir: str, output_base: str, formats: List[str], suffix: str = "") -> Dict[str, Path]:
    if output:
        if len(formats) != 1:
            raise ValueError("--output can only be used with one export format")
        return {formats[0]: Path(output).expanduser().resolve()}
    out_dir = Path(output_dir).expanduser().resolve()
    out_dir.mkdir(parents=True, exist_ok=True)
    targets = {}
    for fmt in formats:
        ext = "xlsx" if fmt == "xlsx" else fmt
        targets[fmt] = out_dir / f"{output_base}{suffix}.{ext}"
    return targets
