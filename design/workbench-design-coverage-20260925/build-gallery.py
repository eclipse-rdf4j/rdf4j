from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SPECS = json.loads((ROOT / "board-specs.json").read_text())
PALETTE = json.loads((ROOT / "palette.json").read_text())
boards = []
for spec in SPECS:
    matches = sorted(
        path for path in (ROOT / "images").glob(f"{spec['id']}-*.png")
        if not path.stem.endswith("-default")
    )
    images = [
        {
            "theme": "dark" if path.stem.endswith("-dark") else "light",
            "src": path.relative_to(ROOT).as_posix(),
        }
        for path in matches
    ]
    boards.append({
        "id": spec["id"],
        "group": spec.get("group", "Workbench"),
        "title": spec["title"],
        "routes": spec.get("routes", []),
        "prompt": spec["contentPrompt"],
        "caption": spec.get("galleryCaption", ""),
        "images": images,
    })

html = r'''<!doctype html>
<html lang="en"><head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="color-scheme" content="light">
<link rel="icon" href="data:,">
<title>RDF4J Workbench · Design study</title>
<style>
:root{color-scheme:light;--canvas:#F5F9FB;--surface:#FFF;--ink:#14242B;--body:#2F4047;--muted:#4F6168;--outline:#778A92;--primary:#20576A;--hover:#16495A;--selected:#DEF3FB;--line:#D9E3E7;--subtle:#EEF3F5;font:14px/1.5 Inter,ui-sans-serif,system-ui,-apple-system,"Segoe UI",sans-serif;color:var(--body);background:var(--canvas)}
*{box-sizing:border-box}body{margin:0;min-width:320px;background:var(--canvas)}button,input,select,textarea{font:inherit;color:inherit}a{color:var(--primary)}
:focus-visible{outline:3px solid var(--primary);outline-offset:2px}
.page{max-width:1480px;margin:auto;padding:30px clamp(16px,3vw,40px) 60px}
header{display:grid;grid-template-columns:minmax(0,1fr) auto;align-items:start;gap:22px;margin-bottom:28px}
.brand{display:flex;align-items:center;gap:14px;min-width:0}.brand img{width:66px;height:40px;object-fit:contain;flex:none}
.eyebrow{margin:0 0 3px;color:var(--muted);font-size:11px;font-weight:700;letter-spacing:.06em;text-transform:uppercase}
h1,h2,h3,p{margin-top:0}h1{margin:0 0 6px;color:var(--ink);font-size:clamp(24px,3vw,32px);line-height:1.15;letter-spacing:-.025em}
.lede{max-width:780px;margin:0;color:var(--muted)}.chip{display:inline-flex;align-items:center;gap:7px;padding:7px 10px;color:var(--primary);background:var(--selected);border:1px solid #B9D9E3;border-radius:999px;font-size:11px;font-weight:700;white-space:nowrap}.chip:before{width:7px;height:7px;border-radius:50%;background:var(--primary);content:""}
main{display:grid;gap:28px}.head{display:flex;justify-content:space-between;align-items:end;gap:16px;margin-bottom:13px}.head h2{margin:0;color:var(--ink);font-size:20px;line-height:1.25}.head p{max-width:720px;margin:4px 0 0;color:var(--muted)}
.panel{min-width:0;padding:clamp(15px,2vw,23px);background:var(--surface);border:1px solid var(--line);border-radius:11px;box-shadow:0 1px 2px rgb(20 36 43 / 5%)}
.palette{display:grid;grid-template-columns:minmax(240px,.72fr) minmax(0,1.4fr);gap:22px}.logo-box{display:flex;align-items:center;gap:13px;margin-bottom:14px;padding:12px;background:var(--canvas);border:1px solid var(--line);border-radius:7px}.logo-box img{width:66px;height:40px;object-fit:contain}.logo-box p{margin:0;color:var(--muted);font-size:11px}
.palette-sets{display:grid;gap:12px}.swatch-set h3{margin:0 0 6px;font-size:12px}.swatches{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px}.swatch{overflow:hidden;border:1px solid var(--line);border-radius:7px}.swatch-color{height:38px;border-bottom:1px solid rgb(20 36 43 / 9%)}.swatch-info{padding:7px}.swatch-name{display:block;color:var(--ink);font-size:11px;font-weight:650}.swatch-hex{display:block;color:var(--muted);font:10px ui-monospace,monospace}
h3{color:var(--ink)}.contrast h3{margin:0 0 5px;font-size:15px}.contrast p{margin:0 0 10px;color:var(--muted);font-size:11px}.contrast-scroll{overflow-x:auto}.contrast-table{width:100%;border-collapse:collapse;font-size:11px}.contrast-table th,.contrast-table td{padding:7px 8px;border-bottom:1px solid var(--line);text-align:left}.contrast-table th{color:var(--muted);font-weight:650}.contrast-table td:last-child,.contrast-table th:last-child{text-align:right;white-space:nowrap}.ratio-pass{color:var(--primary);font-weight:700}.aa-sample{display:inline-flex;align-items:center;justify-content:center;width:30px;height:20px;margin-right:5px;border:1px solid var(--line);border-radius:4px;font-weight:700}.math-note{margin-top:10px!important;font-size:10px!important}
.preview-grid{display:grid;grid-template-columns:minmax(0,1.55fr) minmax(230px,.65fr);gap:18px;align-items:start}.app{--canvas:#F5F9FB;--surface:#FFFFFF;--ink:#14242B;--body:#2F4047;--muted:#4F6168;--outline:#778A92;--primary:#20576A;--hover:#16495A;--selected:#DEF3FB;--line:#D9E3E7;--subtle:#EEF3F5;--app-action-ink:#FFFFFF;overflow:hidden;color:var(--body);background:var(--surface);border:1px solid var(--line);border-radius:10px;box-shadow:0 1px 2px rgb(20 36 43 / 5%)}.app[data-theme="dark"]{--canvas:#080E11;--surface:#111A1D;--raised:#182428;--ink:#E8F0F3;--body:#CEDADF;--muted:#9AABB2;--outline:#687E87;--primary:#73C4E2;--hover:#94D6EF;--selected:#15323D;--line:#304147;--subtle:#182428;--app-action-ink:#080E11}
.app-top{display:flex;flex-wrap:wrap;justify-content:space-between;align-items:center;gap:10px;padding:11px 14px;border-bottom:1px solid var(--line)}.app-brand{display:flex;align-items:center;gap:9px;color:var(--ink);font-weight:700}.app-brand img{width:42px;height:25px;object-fit:contain;background:#FFFFFF;border-radius:3px}.context{display:flex;flex-wrap:wrap;gap:4px 12px;color:var(--muted);font-size:11px}.context b{color:var(--body)}
.app-body{display:grid;grid-template-columns:150px minmax(0,1fr)}nav{padding:12px 9px;border-right:1px solid var(--line)}.nav-group{margin-bottom:12px}.nav-heading{margin:0 0 4px 7px;color:var(--muted);font-size:9px;font-weight:750;letter-spacing:.08em;text-transform:uppercase}.nav-item{display:block;margin:2px 0;padding:6px 7px;color:var(--body);border-radius:5px;font-size:11px;text-decoration:none}.nav-item.active{color:var(--primary);background:var(--selected);font-weight:700}
.work{min-width:0;padding:17px clamp(12px,1.8vw,22px)}.work-title{display:flex;justify-content:space-between;gap:12px;align-items:center;margin-bottom:10px}.work-title h3{margin:0;font-size:20px;letter-spacing:-.02em}.note{margin:0;color:var(--muted);font-size:10px}
.toolbar{display:flex;justify-content:space-between;align-items:center;gap:10px;padding:7px 9px;background:var(--subtle,#F8FAFB);border:1px solid var(--line);border-bottom:0;border-radius:6px 6px 0 0}.toolbar label{display:flex;align-items:center;gap:7px;color:var(--muted);font-size:11px}.toolbar select{min-height:30px;padding:3px 22px 3px 6px;border:1px solid var(--outline);border-radius:4px;background:var(--surface)}.link-button{padding:3px;color:var(--primary);border:0;background:transparent;font-size:11px;font-weight:650;cursor:pointer}
.editor{display:block;width:100%;min-height:140px;padding:12px;resize:vertical;color:var(--body);background:var(--surface);border:1px solid var(--line);border-radius:0 0 6px 6px;font:11px/1.6 ui-monospace,SFMono-Regular,Menlo,monospace}
.actions{display:flex;flex-wrap:wrap;gap:8px;margin-top:10px}.button{display:inline-flex;align-items:center;justify-content:center;min-height:36px;padding:0 12px;color:var(--body);background:var(--surface);border:1px solid var(--outline);border-radius:5px;font-weight:650;cursor:pointer}.button.primary{color:var(--app-action-ink);background:var(--primary);border-color:var(--primary)}.button.primary:hover{background:var(--hover)}.button.quiet{border-color:transparent}
.disclosure{margin-top:12px;border:1px solid var(--line);border-radius:6px}.disclosure>summary{display:flex;justify-content:space-between;align-items:center;gap:8px;padding:9px 11px;color:var(--ink);cursor:pointer;list-style:none}.disclosure>summary::-webkit-details-marker{display:none}.summary-title{font-weight:650}.summary-value{color:var(--muted);font-size:10px;text-align:right}.disclosure-body{padding:11px;border-top:1px solid var(--line)}.fields{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px 14px}.field{display:grid;gap:4px;min-width:0}.field label,.field-label{color:var(--body);font-size:11px;font-weight:650}.field input[type=number],.field select{width:100%;max-width:100%;min-height:35px;padding:5px 8px;background:var(--surface);border:1px solid var(--outline);border-radius:4px}#page-size{width:8rem}#timeout{width:6.5rem}.query-field input[type=number],.query-field select{box-sizing:border-box;max-width:100%;min-height:35px;padding:5px 8px;background:var(--surface);border:1px solid var(--outline);border-radius:4px}#page-size{width:8rem}#timeout{width:6.5rem}#download-limit,#tuple-format{width:8rem;max-width:100%}.query-fields{display:flex;flex-wrap:wrap;align-items:center;gap:10px 14px}.query-field{display:flex;align-items:center;gap:7px;min-width:0}.query-field>label{color:var(--body);font-size:11px;font-weight:650;white-space:nowrap}.query-fields #page-size,.query-fields #timeout{flex:0 0 auto}.download-fields{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr) auto;align-items:end;gap:10px 14px}.check{display:flex;align-items:center;gap:7px;min-height:35px;font-size:11px}.check input{width:15px;height:15px;margin:0;accent-color:var(--primary)}.disclosure-actions{display:flex;justify-content:flex-end;align-items:center}
.results-title{margin:18px 0 7px;color:var(--ink);font-size:14px}.sample-table{width:100%;margin-top:10px;border-collapse:collapse}.sample-table th,.sample-table td{padding:7px;border-bottom:1px solid var(--line);text-align:left;font-size:10px}.sample-table th{color:var(--muted)}.sample-table td:first-child{color:var(--primary);overflow-wrap:anywhere}.sample-label{margin:7px 0 0;color:var(--muted);font-size:9px}
.prototype-note{padding:15px;background:var(--selected);border:1px solid var(--line);border-radius:8px}.prototype-note h3{margin:0 0 7px;font-size:14px}.prototype-note p,.prototype-note li{font-size:11px;line-height:1.55}.prototype-note p{margin:0}.prototype-note ul{margin:10px 0 0;padding-left:18px}.live{min-height:1.4em;margin:6px 0 0;color:var(--muted);font-size:10px}.preview-theme{display:flex;gap:5px}.swatch-set h3{margin:10px 0 6px;font-size:12px}
.gallery{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:13px}.board{min-width:0;overflow:hidden;background:var(--surface);border:1px solid var(--line);border-radius:9px;box-shadow:0 1px 2px rgb(20 36 43 / 5%)}.board-img{display:grid;place-items:center;min-height:135px;background:#EDF3F5;border-bottom:1px solid var(--line)}.board-image-wrap{width:100%}.board-img img{display:block;width:100%;height:auto;max-height:680px;object-fit:contain;background:white}.board-theme-picker{display:flex;gap:4px;padding:5px;background:var(--surface)}.board-theme-picker button,.preview-theme button{min-height:32px;padding:4px 10px;color:var(--body);background:var(--surface);border:1px solid var(--outline);border-radius:5px;cursor:pointer}.board-theme-picker button[aria-pressed="true"],.preview-theme button[aria-pressed="true"]{color:var(--primary);background:var(--selected);border-color:var(--primary)}.board-theme-picker button:disabled{color:var(--muted);border-style:dashed;cursor:not-allowed;opacity:.72}.pending{max-width:280px;padding:28px 18px;color:var(--muted);font-size:11px;text-align:center}.board-copy{padding:11px 13px}.board-meta{display:flex;flex-wrap:wrap;gap:4px 7px;margin-bottom:5px;color:var(--muted);font-size:9px}.board-id{color:var(--primary);font-weight:750}.board-copy h3{margin:0 0 6px;font-size:14px;line-height:1.35}.board-caption{margin:0 0 7px;color:var(--muted);font-size:10px;font-weight:650}.board-copy details{padding-top:7px;border-top:1px solid var(--line)}.board-copy summary{color:var(--primary);font-size:10px;font-weight:650;cursor:pointer}.board-copy details p{margin:6px 0 0;color:var(--muted);font-size:10px;line-height:1.5;white-space:pre-wrap}
footer{margin-top:24px;padding-top:16px;border-top:1px solid var(--line);color:var(--muted);font-size:10px}footer p{margin:0 0 7px}
@media(max-width:850px){.palette,.preview-grid{grid-template-columns:minmax(0,1fr)}.gallery{grid-template-columns:minmax(0,1fr)}}
@media(max-width:600px){.page{padding-top:20px}header{grid-template-columns:minmax(0,1fr);gap:10px}.chip{justify-self:start}.app-body{grid-template-columns:minmax(0,1fr)}nav{display:flex;flex-wrap:wrap;gap:2px;padding:7px 9px;border-right:0;border-bottom:1px solid var(--line)}.nav-group{display:contents}.nav-heading{align-self:center;margin:0 4px}.nav-item{margin:0;padding:5px}.fields{grid-template-columns:minmax(0,1fr)}.query-fields{flex-direction:column;align-items:stretch;gap:10px}.query-field{align-items:flex-start;flex-direction:column;gap:4px}.query-field input[type=number],.query-field select{min-height:44px}.download-fields{grid-template-columns:minmax(0,1fr)}.button,.disclosure>summary,.toolbar select,.field input[type=number],.field select,.check,.preview-theme button,.board-theme-picker button{min-height:44px}.head{display:block}.head .chip{margin-top:8px}.preview-theme button,.board-theme-picker button{min-height:44px}}
@media(max-width:360px){.swatches{grid-template-columns:repeat(2,minmax(0,1fr))}.disclosure>summary{align-items:flex-start;flex-wrap:wrap}.summary-value{flex-basis:100%;text-align:left}.contrast-table{font-size:10px}.contrast-table th,.contrast-table td{padding:6px 4px}.contrast-table th:last-child,.contrast-table td:last-child{white-space:normal}}
</style>
</head><body><div class="page">
<header><div class="brand"><img src="images/rdf4j-logo-reference.png" alt="RDF4J logo"><div><p class="eyebrow">Workbench redesign · visual study</p><h1>Design coverage catalog</h1><p class="lede">Thirty-two source-grounded visual targets for every Workbench page family, form, configuration state, and shared control. The mockups explore a calmer layout while preserving existing routes, values, and actions.</p></div></div><span class="chip">Design proposal · not implemented</span></header>
<main>
<section aria-labelledby="palette-title"><div class="head"><div><h2 id="palette-title">Calculated palette</h2><p>Cool slate, navy and complementary blue derived from the original logo. Orange appears only within the unchanged logo.</p></div></div>
<div class="panel palette"><div><div class="logo-box"><img src="images/rdf4j-logo-reference.png" alt="Original RDF4J logo, preserved unchanged"><p>Original 132 × 80 source PNG, preserved byte-for-byte. Orange remains only in the original logo.</p></div><div class="palette-sets" id="palette-sets"></div></div>
<div class="contrast"><h3>Contrast from final 8-bit sRGB colors</h3><p>Ratios use exact hex tokens and WCAG relative luminance. Text pairs meet 4.5:1; controls and focus indicators meet 3:1.</p><div id="contrast-sets"></div><p class="math-note">Opaque logo orange count-weighted circular mean hue: 44.20646663°. Complementary UI hue: 224.20646663°. Token lightness/chroma are design choices; the calculation does not prove an aesthetic choice.</p></div></div></section>
<section aria-labelledby="prototype-title"><div class="head"><div><h2 id="prototype-title">Interactive query-controls preview</h2><p>Exact token colors with native named disclosures. Inputs are local mock controls; this preview sends no form data or server requests.</p></div></div>
<div class="preview-grid"><div class="app" data-theme="light"><div class="app-top"><div class="app-brand"><img src="images/rdf4j-logo-reference.png" alt=""><span>Workbench</span></div><div class="context"><span><b>Server</b> Local server · Change</span><span><b>Repository</b> Research · Change</span></div><div class="preview-theme" role="group" aria-label="Preview theme"><span>Preview theme</span><button type="button" data-preview-theme="light" aria-pressed="true">Light</button><button type="button" data-preview-theme="dark" aria-pressed="false">Dark</button></div></div>
<div class="app-body"><nav aria-label="Workbench navigation"><div class="nav-group"><p class="nav-heading">Explore</p><a class="nav-item" href="#prototype-title">Summary</a><a class="nav-item" href="#prototype-title">Namespaces</a><a class="nav-item" href="#prototype-title">Contexts</a><a class="nav-item" href="#prototype-title">Types</a><a class="nav-item" href="#prototype-title">Explore</a><a class="nav-item active" href="#prototype-title" aria-current="page">Query</a><a class="nav-item" href="#prototype-title">Saved Queries</a><a class="nav-item" href="#prototype-title">Export</a></div><div class="nav-group"><p class="nav-heading">Modify</p><a class="nav-item" href="#prototype-title">Update · Add · Remove · Clear</a></div><div class="nav-group"><a class="nav-item" href="#prototype-title">Information</a></div></nav>
<div class="work"><div class="work-title"><h3>Query</h3><p class="note">Illustrative sample · fresh editors start empty</p></div>
<div class="toolbar"><label for="query-language">Query language <select id="query-language"><option selected>SPARQL</option></select></label><button class="link-button" type="button" data-action>Save query</button></div>
<label for="query-editor" style="position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0)">Query text</label><textarea class="editor" id="query-editor" spellcheck="false">PREFIX ex: &lt;https://example.org/&gt;
SELECT ?person ?name
WHERE {
  ?person a ex:Person ;
          ex:name ?name .
}
LIMIT 100</textarea><p class="note" style="margin-top:5px">Sample query for illustration; not a Workbench default.</p>
<div class="actions"><button class="button primary" type="button" data-action>Run query</button><button class="button quiet" type="button" data-action>Explain</button></div>
<details class="disclosure"><summary><span class="summary-title">Query options</span><span class="summary-value" id="query-summary">100 rows · 60 s · Inferred on</span></summary><div class="disclosure-body"><div class="query-fields">
<div class="query-field"><label for="page-size">Results per page</label><select id="page-size"><option>All</option><option>10</option><option>50</option><option selected>100</option><option>200</option></select></div>
<div class="query-field"><label for="timeout">Query timeout (seconds)</label><input id="timeout" type="number" min="0" value="60"></div>
<label class="check" for="inferred"><input id="inferred" type="checkbox" checked><span>Include inferred statements</span></label><div class="disclosure-actions"><button class="button" type="button" data-action>Clear</button></div>
</div></div></details>
<h4 class="results-title">Results</h4>
<details class="disclosure"><summary><span class="summary-title">Display</span><span class="summary-value" id="display-summary">Auto · Wrap values · Show datatypes</span></summary><div class="disclosure-body"><div class="fields">
<div class="field"><label for="layout">Layout</label><select id="layout"><option selected>Auto</option><option>Table</option><option>Records</option></select></div><label class="check" for="wrap"><input id="wrap" type="checkbox" checked><span>Wrap values</span></label><label class="check" for="datatypes"><input id="datatypes" type="checkbox" checked><span>Show datatypes</span></label>
</div></div></details>
<details class="disclosure"><summary><span class="summary-title">Download</span><span class="summary-value" id="download-summary">All · CSV</span></summary><div class="disclosure-body"><div class="fields download-fields">
<div class="field"><label for="tuple-format">Download format</label><select id="tuple-format"><option value="text/csv" selected>CSV</option></select></div>
<div class="field"><label for="download-limit">Download limit</label><select id="download-limit"><option value="0" selected>All</option><option value="10">10</option><option value="50">50</option><option value="100">100</option><option value="200">200</option></select></div>
<div class="disclosure-actions"><button class="button" type="button" data-action>Download</button></div>
</div><p class="note" style="margin:8px 0 0">CSV is a valid tuple-results format; available formats come from installed writers.</p></div></details>
<table class="sample-table"><thead><tr><th>Person</th><th>Name</th></tr></thead><tbody><tr><td>ex:person/1</td><td>Ada</td></tr><tr><td>ex:person/2</td><td>Sam</td></tr><tr><td>ex:person/3</td><td>Rin</td></tr></tbody></table><p class="sample-label">Sample response values only · not database defaults.</p><p class="live" id="live" aria-live="polite"></p>
</div></div></div>
<aside class="prototype-note"><h3>Disclosure behavior</h3><p>Each named group starts closed. Open it to inspect controls; summaries keep chosen values visible. Changes update the summary. Browser-native keyboard and focus behavior remains available.</p><ul><li>Query options: 100 rows, 60 seconds, inferred statements on.</li><li>Display: Auto layout, Wrap values and Show datatypes on.</li><li>Download: All results by default, CSV as the displayed valid tuple format example.</li><li>Routes, sample query and rows are illustrative; source-backed defaults and server-provided choices remain authoritative.</li></ul></aside>
</div></section>
<section aria-labelledby="boards-title"><div class="head"><div><h2 id="boards-title">Visual target boards</h2><p>Every distinct page family and configuration state has its own legible target. Written specifications retain exact source meaning.</p></div><span class="chip" id="board-count">Loading target boards</span></div><div class="gallery" id="gallery"></div></section>
</main>
<footer><p>Generated raster mockups approximate text, logo rendering and color. The unchanged logo file and exact CSS swatches/contrast pairs above are authoritative.</p><p>References: <a href="https://www.w3.org/TR/css-color-4/#ok-lab">CSS Color 4 OKLab/OKLCH</a> · <a href="https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html">WCAG 2.2 text contrast</a> · <a href="https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html">WCAG 2.2 non-text contrast</a> · <a href="DESIGN-COVERAGE.md">Coverage brief</a> · <a href="board-specs.json">Board specifications</a> · <a href="final-board-manifest.json">Sanitized final board manifest</a></p></footer>
</div>
<script>
const boards=@@BOARDS@@, palette=@@PALETTE@@;
const tokenNames={canvas:"Canvas",surface:"Surface",raisedSurface:"Raised surface",ink:"Heading / ink",body:"Body text",muted:"Muted text",controlOutline:"Control outline",primary:"Primary action / focus",primaryHover:"Primary hover",selectedSurface:"Selected surface",focusRing:"Focus ring",primaryText:"Primary button text"};
function renderSwatches(title,tokens){
  const section=document.createElement("section");section.className="swatch-set";
  const heading=document.createElement("h3");heading.textContent=title;section.append(heading);
  const grid=document.createElement("div");grid.className="swatches";
  Object.entries(tokens).filter(([key])=>key!=="focusRing").forEach(([key,value])=>{
    const swatch=document.createElement("div");swatch.className="swatch";
    swatch.innerHTML='<div class="swatch-color"></div><div class="swatch-info"><span class="swatch-name"></span><span class="swatch-hex"></span></div>';
    swatch.querySelector(".swatch-color").style.background=value.hex;
    swatch.querySelector(".swatch-name").textContent=tokenNames[key]||key;
    swatch.querySelector(".swatch-hex").textContent=value.hex;
    grid.append(swatch);
  });
  section.append(grid);document.getElementById("palette-sets").append(section);
}
renderSwatches("Light palette",palette.tokens);
renderSwatches("Dark palette",palette.darkTokens);
const color=(tokens,ref)=>ref.startsWith("#")?ref:tokens[ref].hex;
const lightPairs=[
  ["bodyOnWhite","Body text on white","body","#FFFFFF",4.5],["bodyOnCanvas","Body text on canvas","body","canvas",4.5],
  ["mutedOnWhite","Muted text on white","muted","#FFFFFF",4.5],["mutedOnCanvas","Muted text on canvas","muted","canvas",4.5],
  ["controlOutlineAgainstWhite","Control outline on white","controlOutline","#FFFFFF",3],["controlOutlineAgainstCanvas","Control outline on canvas","controlOutline","canvas",3],
  ["controlOutlineAgainstSelectedSurface","Control outline on selected","controlOutline","selectedSurface",3],
  ["focusRingAgainstWhite","Focus ring on white","focusRing","#FFFFFF",3],["focusRingAgainstCanvas","Focus ring on canvas","focusRing","canvas",3],
  ["whiteTextOnPrimary","White button text","#FFFFFF","primary",4.5],["whiteTextOnPrimaryHover","White button text on hover","#FFFFFF","primaryHover",4.5],
  ["inkOnSelectedSurface","Ink on selected surface","ink","selectedSurface",4.5]
];
const darkPairs=[
  ["bodyOnSurface","Body text on surface","body","surface",4.5],["bodyOnCanvas","Body text on canvas","body","canvas",4.5],
  ["mutedOnSurface","Muted text on surface","muted","surface",4.5],["mutedOnCanvas","Muted text on canvas","muted","canvas",4.5],
  ["mutedOnRaisedSurface","Muted text on raised surface","muted","raisedSurface",4.5],
  ["controlOutlineAgainstSurface","Control outline on surface","controlOutline","surface",3],
  ["controlOutlineAgainstCanvas","Control outline on canvas","controlOutline","canvas",3],
  ["controlOutlineAgainstRaisedSurface","Control outline on raised surface","controlOutline","raisedSurface",3],
  ["controlOutlineAgainstSelectedSurface","Control outline on selected","controlOutline","selectedSurface",3],
  ["focusRingAgainstSurface","Focus ring on surface","focusRing","surface",3],["focusRingAgainstCanvas","Focus ring on canvas","focusRing","canvas",3],
  ["primaryTextOnPrimary","Primary button text","primaryText","primary",4.5],["primaryTextOnHover","Primary button text on hover","primaryText","primaryHover",4.5],
  ["inkOnSelectedSurface","Ink on selected surface","ink","selectedSurface",4.5]
];
function renderContrast(title,data,tokens,pairs){
  const section=document.createElement("section");section.className="contrast-set";
  const heading=document.createElement("h4");heading.textContent=title;section.append(heading);
  const scroll=document.createElement("div");scroll.className="contrast-scroll";
  const table=document.createElement("table");table.className="contrast-table";
  table.innerHTML='<thead><tr><th>Foreground / element</th><th>Color pair</th><th>Ratio</th><th>Minimum</th></tr></thead><tbody></tbody>';
  const body=table.querySelector("tbody");
  for(const [key,label,fgRef,bgRef,minimum] of pairs){
    const fg=color(tokens,fgRef),bg=color(tokens,bgRef),result=data[key],row=document.createElement("tr");
    row.innerHTML='<td><span class="aa-sample">Aa</span><span class="pair-label"></span></td><td><code></code></td><td></td><td class="ratio-pass"></td>';
    const sample=row.querySelector(".aa-sample");sample.style.color=fg;sample.style.background=bg;
    row.querySelector(".pair-label").textContent=label;
    row.querySelector("code").textContent=fg+" / "+bg;
    row.children[2].textContent=result.ratio.toFixed(3)+":1";
    row.children[3].textContent=(result.pass?"Pass":"Fail")+" · "+minimum+":1";
    if(!result.pass)row.children[3].className="ratio-fail";
    body.append(row);
  }
  scroll.append(table);section.append(scroll);document.getElementById("contrast-sets").append(section);
}
renderContrast("Light mode",palette.contrast,palette.tokens,lightPairs);
renderContrast("Dark mode",palette.darkContrast,palette.darkTokens,darkPairs);
const gallery=document.getElementById("gallery");
boards.forEach(board=>{
  const article=document.createElement("article");article.className="board";
  const routes=board.routes.length?board.routes.join(" · "):"shared component patterns";
  const imageArea=document.createElement("div");imageArea.className="board-img";
  const light=board.images.find(item=>item.theme==="light"),initial=light||board.images[0];
  if(initial){
    const wrap=document.createElement("div");wrap.className="board-image-wrap";
    const img=document.createElement("img");img.src=initial.src;img.alt=board.id+" "+board.title+" "+initial.theme+" visual design target";img.loading="lazy";wrap.append(img);
    imageArea.append(wrap);
    const picker=document.createElement("div");picker.className="board-theme-picker";picker.setAttribute("role","group");picker.setAttribute("aria-label",board.id+" image theme");
    for(const theme of ["light","dark"]){
      const item=board.images.find(candidate=>candidate.theme===theme);
      const button=document.createElement("button");button.type="button";button.textContent=theme==="dark"?"Dark":"Light";button.disabled=!item;button.setAttribute("aria-pressed",String(theme===initial.theme));
      if(item)button.addEventListener("click",()=>{img.src=item.src;img.alt=board.id+" "+board.title+" "+item.theme+" visual design target";for(const sibling of picker.children)sibling.setAttribute("aria-pressed",String(sibling===button))});
      picker.append(button);
    }
    imageArea.append(picker);
  }else{
    const pending=document.createElement("div");pending.className="pending";pending.textContent="Board image pending. Source-backed coverage and intended content are specified below.";imageArea.append(pending);
  }
  const copy=document.createElement("div");copy.className="board-copy";
  copy.innerHTML='<div class="board-meta"><span class="board-id"></span><span class="board-group"></span><span>·</span><span class="board-routes"></span></div><h3></h3><p class="board-caption"></p><details><summary>Read source-backed content specification</summary><p></p></details>';
  copy.querySelector(".board-id").textContent=board.id;copy.querySelector(".board-group").textContent=board.group;copy.querySelector(".board-routes").textContent=routes;copy.querySelector("h3").textContent=board.title;
  copy.querySelector(".board-caption").textContent=board.caption;copy.querySelector(".board-caption").hidden=!board.caption;copy.querySelector("details p").textContent=board.prompt;
  article.append(imageArea,copy);gallery.append(article);
});
const generated=boards.filter(board=>board.images.length).length,bothThemes=boards.filter(board=>board.images.some(img=>img.theme==="light")&&board.images.some(img=>img.theme==="dark")).length;
document.getElementById("board-count").textContent=generated+" / "+boards.length+" targets · "+bothThemes+" / "+boards.length+" light+dark";
document.querySelectorAll("[data-preview-theme]").forEach(button=>button.addEventListener("click",()=>{
  document.querySelector(".app").dataset.theme=button.dataset.previewTheme;
  document.querySelectorAll("[data-preview-theme]").forEach(other=>other.setAttribute("aria-pressed",String(other===button)));
}));
const page=document.getElementById("page-size"),downloadLimit=document.getElementById("download-limit"),tupleFormat=document.getElementById("tuple-format"),downloadSummary=document.getElementById("download-summary"),timeout=document.getElementById("timeout"),infer=document.getElementById("inferred"),querySummary=document.getElementById("query-summary");
const queryUpdate=()=>{const rows=page.value==="All"?"All rows":page.value+" rows";querySummary.textContent=rows+" · "+(timeout.value===""?"empty":timeout.value+" s")+" · Inferred "+(infer.checked?"on":"off")};
[page,timeout,infer].forEach(el=>{el.addEventListener("input",queryUpdate);el.addEventListener("change",queryUpdate)});
const downloadUpdate=()=>{const limit=downloadLimit.value==="0"?"All":downloadLimit.value;downloadSummary.textContent=limit+" · "+tupleFormat.options[tupleFormat.selectedIndex].text};
[downloadLimit,tupleFormat].forEach(el=>el.addEventListener("change",downloadUpdate));
const layout=document.getElementById("layout"),wrap=document.getElementById("wrap"),types=document.getElementById("datatypes"),displaySummary=document.getElementById("display-summary");
const displayUpdate=()=>{displaySummary.textContent=layout.value+" · "+(wrap.checked?"Wrap values":"No wrapping")+" · "+(types.checked?"Show datatypes":"Hide datatypes")};
[layout,wrap,types].forEach(el=>{el.addEventListener("input",displayUpdate);el.addEventListener("change",displayUpdate)});
document.querySelectorAll("[data-action]").forEach(button=>button.addEventListener("click",()=>{document.getElementById("live").textContent="Visual prototype only · no server request was sent."}));
</script></body></html>'''
html = html.replace("@@BOARDS@@", json.dumps(boards, ensure_ascii=False))
html = html.replace("@@PALETTE@@", json.dumps(PALETTE, ensure_ascii=False))
(ROOT / "index.html").write_text(html)
print(f"wrote {ROOT / 'index.html'} with {sum(1 for board in boards if board['images'])}/{len(boards)} board images")
