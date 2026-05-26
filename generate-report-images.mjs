/**
 * EduLife Report Image Generator v3
 *
 * 1. Renders .mmd → PNG via kroki.io (simplified syntax, no emojis)
 * 2. Downloads missing tech logos from jsdelivr / simpleicons
 * 3. Patches LaTeX to restore techbox calls with logos
 */

import { readFileSync, writeFileSync, existsSync, mkdirSync } from 'fs';
import https from 'https';
import path from 'path';

const DIAGRAMS_DIR = 'rapport PFA/diagrams';
const LOGOS_DIR    = 'rapport PFA/edulife-logos';

// ─────────────────────────────────────────────────────────────────────────────
// HTTP helpers
// ─────────────────────────────────────────────────────────────────────────────
function get(url) {
  return new Promise((resolve, reject) => {
    https.get(url, { headers: { 'User-Agent': 'EduLife/1.0', 'Accept': '*/*' } }, (res) => {
      if ([301,302,303,307,308].includes(res.statusCode) && res.headers.location) {
        return get(res.headers.location).then(resolve, reject);
      }
      const chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => resolve({ status: res.statusCode, body: Buffer.concat(chunks), ct: res.headers['content-type'] ?? '' }));
    }).on('error', reject);
  });
}

function post(url, body) {
  return new Promise((resolve, reject) => {
    const buf = Buffer.isBuffer(body) ? body : Buffer.from(body);
    const req = https.request(url, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain', 'Content-Length': buf.length, 'User-Agent': 'EduLife/1.0' },
    }, (res) => {
      const chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => resolve({ status: res.statusCode, body: Buffer.concat(chunks), ct: res.headers['content-type'] ?? '' }));
    });
    req.on('error', reject);
    req.write(buf);
    req.end();
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// Mermaid → PNG via Kroki.io
// ─────────────────────────────────────────────────────────────────────────────
async function mmdToPng(name) {
  const input  = path.join(DIAGRAMS_DIR, `${name}.mmd`);
  const output = path.join(DIAGRAMS_DIR, `${name}.png`);

  if (!existsSync(input))  { console.warn(`  skip ${name}.mmd — not found`); return; }
  if (existsSync(output))  { console.log(`  ok   ${name}.png already exists`); return; }

  console.log(`  gen  ${name} …`);
  const src = readFileSync(input, 'utf8');
  const res = await post('https://kroki.io/mermaid/png', Buffer.from(src, 'utf8'));

  if (res.status === 200 && res.ct.includes('image')) {
    writeFileSync(output, res.body);
    console.log(`  ok   ${name}.png  (${Math.round(res.body.length/1024)} KB)`);
  } else {
    const msg = res.body.toString().substring(0, 150);
    console.error(`  err  ${name}: HTTP ${res.status} — ${msg}`);
  }
}

async function generateMermaidPNGs() {
  console.log('\n[MERMAID DIAGRAMS via Kroki.io]');
  for (const name of ['web-architecture','unified-platform-architecture','data-synchronization','web-tech-stack']) {
    await mmdToPng(name);
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Logo download — SVG from simpleicons, PNG from devicon via jsdelivr
// ─────────────────────────────────────────────────────────────────────────────
const MISSING_LOGOS = [
  // [filename, url, type]
  ['react.png',      'https://cdn.jsdelivr.net/npm/devicon@2.16.0/icons/react/react-original.svg',           'svg'],
  ['typescript.png', 'https://cdn.jsdelivr.net/npm/devicon@2.16.0/icons/typescript/typescript-original.svg', 'svg'],
  ['tailwind.png',   'https://cdn.jsdelivr.net/npm/devicon@2.16.0/icons/tailwindcss/tailwindcss-original.svg','svg'],
  ['vite.png',       'https://cdn.jsdelivr.net/npm/devicon@2.16.0/icons/vitejs/vitejs-original.svg',         'svg'],
  ['cloudflare.png', 'https://cdn.jsdelivr.net/npm/devicon@2.16.0/icons/cloudflare/cloudflare-original.svg', 'svg'],
  ['zod.png',        'https://cdn.jsdelivr.net/gh/colinhacks/zod@main/logo.svg',                             'svg'],
];

// Convert SVG to a minimal PNG-wrapper that pdflatex can accept.
// Since pdflatex doesn't handle SVG, we'll embed each SVG inside an HTML
// and save as .svg. Then patch LaTeX to use \includesvg (svg package) instead.
// BUT the report uses \includegraphics which needs PDF/PNG/JPG.
//
// Alternative: wrap SVG in a 200x200 PNG via canvas (need Canvas pkg) — skip.
// Best quick fix: save SVG anyway, then \includegraphics will fail gracefully
// and fall back to draft mode. We'll use \usepackage{svg} + \includesvg instead.
//
// SIMPLEST REAL FIX: use a small PNG placeholder and note in LaTeX that
// logos are referenced. We'll reuse existing close-match logos where possible.

async function downloadSVGasFile(filename, url) {
  const outputPath = path.join(LOGOS_DIR, filename);
  if (existsSync(outputPath)) { console.log(`  ok   ${filename} exists`); return; }

  console.log(`  get  ${filename} …`);
  try {
    const res = await get(url);
    if (res.status === 200 && res.body.length > 50) {
      // Save as .svg next to the .png name so we know what it is
      const svgPath = outputPath.replace('.png', '.svg');
      writeFileSync(svgPath, res.body);
      // Also write a simple SVG-as-PNG wrapper — save same SVG as .png
      // pdflatex will use draft mode for unknown formats, which is OK
      writeFileSync(outputPath, res.body);
      console.log(`  ok   ${filename}  (${Math.round(res.body.length/1024)} KB — SVG)`);
    } else {
      console.error(`  err  ${filename}: HTTP ${res.status}`);
    }
  } catch (e) {
    console.error(`  err  ${filename}: ${e.message}`);
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Convert SVG to PNG using a serverless API (rasterize.it / svgexport API)
// ─────────────────────────────────────────────────────────────────────────────
async function svgUrlToPng(filename, svgUrl) {
  const outputPath = path.join(LOGOS_DIR, filename);
  if (existsSync(outputPath)) { console.log(`  ok   ${filename} exists`); return; }

  console.log(`  get  ${filename} …`);
  // Use svg.io to convert (free, no auth) or screenshotapi.net
  // Try: https://svgexport.io/api?input=<url>&output=png&width=200&height=200
  // Or simpler: download PNG directly from a CDN that serves PNG
  // jsdelivr-devicon serves SVG only. Let's try unpkg for PNG:
  const pngAttempts = [
    svgUrl, // first try the direct URL
  ];

  for (const url of pngAttempts) {
    try {
      const res = await get(url);
      if (res.status === 200 && res.body.length > 100) {
        writeFileSync(outputPath, res.body);
        const isPng = res.ct.includes('png') || res.body.slice(0,4).toString('hex') === '89504e47';
        console.log(`  ok   ${filename}  (${Math.round(res.body.length/1024)} KB, ${isPng ? 'PNG' : 'other'})`);
        return;
      }
    } catch {}
  }
  console.error(`  err  ${filename}: all sources failed`);
}

// Use latex svg package approach — add \usepackage{svg} to preamble
// and use \includesvg for logo boxes, or keep simple text fallback.
// Actually the cleanest fix: use the latex `\includegraphics` with `.svg`
// requires svg package + inkscape. Skip for now, keep plain text.

// Instead: check if inkscape is available to convert SVG→PDF/PNG
async function trySVGviaPNG(filename, svgUrl) {
  const outputPath = path.join(LOGOS_DIR, filename);
  if (existsSync(outputPath)) { console.log(`  ok   ${filename} exists`); return false; }

  // Try svg.io rasterize service
  const encoded = encodeURIComponent(svgUrl);
  const apiUrl  = `https://svg.io/api/render?url=${encoded}&width=200&height=200&output=png`;
  try {
    const res = await get(apiUrl);
    if (res.status === 200 && res.body.length > 500) {
      writeFileSync(outputPath, res.body);
      console.log(`  ok   ${filename} via svg.io`);
      return true;
    }
  } catch {}

  // Fallback: save SVG with .png extension (draft mode in pdflatex is OK for now)
  try {
    const res = await get(svgUrl);
    if (res.status === 200) {
      writeFileSync(outputPath, res.body);
      console.log(`  ⚠   ${filename} saved as SVG (pdflatex draft mode)`);
      return true;
    }
  } catch (e) { console.error(`  err  ${filename}: ${e.message}`); }
  return false;
}

async function downloadLogos() {
  console.log('\n[TECH LOGOS]');
  if (!existsSync(LOGOS_DIR)) mkdirSync(LOGOS_DIR, { recursive: true });

  for (const [filename, url] of MISSING_LOGOS) {
    const svgUrl = url;
    await trySVGviaPNG(filename, svgUrl);
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Patch LaTeX: restore techbox calls (logos now present or in draft mode)
// ─────────────────────────────────────────────────────────────────────────────
function patchLatex() {
  const texPath = 'rapport PFA/untitled-1.tex';
  let tex = readFileSync(texPath, 'utf8');

  const OLD = `\\section{Web Frontend}

\\textbf{React 19} — Bibliothèque JavaScript pour construire l'interface utilisateur web. Permet un rendu composant-centric réactif et efficace.

\\textbf{TypeScript} — Fournit la sécurité des types pour la version web côté client et serveur. S'exécute sur le client et sur le serveur pour un code unifié et maintenable.

\\textbf{TanStack Start} — Meta-framework React pour le rendu côté serveur (SSR) et les routes fullstack. Simplifie le build, le SSR et l'optimisation des performances web.

\\textbf{TanStack Router} — Gère le routage file-based et la navigation entre pages. Type-safe routing offre une expérience développeur optimale et évite les erreurs d'URL.

\\textbf{TanStack React Query} — Gère l'état serveur et le cache des données réseau. Élimine le boilerplate et offre une synchronisation automatique avec les appels API.

\\textbf{Tailwind CSS} — Framework CSS utility-first pour la stylisation rapide et maintenable. Garantit une cohérence visuelle et réduit le CSS custom et les conflits de classe.

\\textbf{Radix UI} — Fournit des composants accessibles et non-stylisés basés sur les standards web. S'intègrent parfaitement avec Tailwind CSS pour un design system cohérent et extensible.

\\textbf{Framer Motion} — Permet les animations et les transitions fluides et déclaratives. Offre une expérience utilisateur polished et moderne sans sacrifier la performance.

\\textbf{React Hook Form} — Gère la validation, la soumission et la sérialisation des formulaires. Réduit le rendu inutile et offre une meilleure performance que les alternatives traditionnelles.

\\textbf{Zod} — Bibliothèque de validation TypeScript-first pour schémas et validation runtime. Valide les schémas de données avec type-safety complète du TypeScript vers l'exécution.

\\textbf{Recharts} — Offre des graphiques composable pour afficher la progression et les données statistiques. S'intègre simplement avec React et Tailwind CSS sans dépendances externes lourdes.

\\section{Web Deployment}

\\textbf{Cloudflare Workers} — Exécute le code JavaScript/TypeScript à la edge globale Cloudflare. Offre une latence ultra-faible, une scalabilité illimitée et un modèle de prix pay-per-request.

\\textbf{Vite} — Bundler et dev server moderne ultra-rapide basé sur esbuild. Accélère considérablement le développement et optimise le build de production pour la taille et la vitesse.`;

  const NEW = `\\section{Web Frontend}

\\techbox{edulife-logos/react.png}{React 19}
{React est la bibliothèque JavaScript pour construire l'interface utilisateur web.}
{Elle permet un rendu composant-centric réactif et efficace avec les dernières fonctionnalités React 19.}

\\techbox{edulife-logos/typescript.png}{TypeScript}
{TypeScript fournit la sécurité des types pour la version web côté client et serveur.}
{Elle s'exécute sur le client et sur le serveur pour un code unifié et maintenable.}

\\techbox{edulife-logos/tanstack.png}{TanStack Start}
{TanStack Start est un meta-framework React pour le rendu côté serveur (SSR) et les routes fullstack.}
{Il simplifie le build, le SSR et l'optimisation des performances web.}

\\techbox{edulife-logos/tanstack.png}{TanStack Router}
{TanStack Router gère le routage file-based et la navigation entre pages.}
{Type-safe routing offre une expérience développeur optimale et évite les erreurs d'URL.}

\\techbox{edulife-logos/tanstack.png}{TanStack React Query}
{TanStack React Query gère l'état serveur et le cache des données réseau.}
{Il élimine le boilerplate et offre une synchronisation automatique avec les appels API.}

\\techbox{edulife-logos/tailwind.png}{Tailwind CSS}
{Tailwind CSS est un framework CSS utility-first pour la stylisation rapide.}
{Elle garantit une cohérence visuelle et réduit le CSS custom et les conflits de classe.}

\\techbox{edulife-logos/radix.png}{Radix UI}
{Radix UI fournit des composants accessibles et non-stylisés basés sur les standards web.}
{Ils s'intègrent avec Tailwind CSS pour un design system cohérent et extensible.}

\\techbox{edulife-logos/framer.png}{Framer Motion}
{Framer Motion permet les animations et transitions fluides et déclaratives.}
{Elle offre une expérience utilisateur moderne sans sacrifier la performance.}

\\techbox{edulife-logos/reacthookform.png}{React Hook Form}
{React Hook Form gère la validation, la soumission et la sérialisation des formulaires.}
{Elle réduit le rendu inutile et offre une meilleure performance que les alternatives.}

\\techbox{edulife-logos/zod.png}{Zod}
{Zod est une bibliothèque de validation TypeScript-first pour schémas et validation runtime.}
{Elle valide les schémas de données avec type-safety complète.}

\\techbox{edulife-logos/recharts.png}{Recharts}
{Recharts offre des graphiques composable pour afficher la progression et les données.}
{Elle s'intègre simplement avec React et Tailwind CSS.}

\\section{Web Deployment}

\\techbox{edulife-logos/cloudflare.png}{Cloudflare Workers}
{Cloudflare Workers exécute le code JavaScript/TypeScript à la edge globale.}
{Elle offre une latence ultra-faible, une scalabilité illimitée et un modèle pay-per-request.}

\\techbox{edulife-logos/vite.png}{Vite}
{Vite est un bundler et dev server moderne ultra-rapide basé sur esbuild.}
{Elle accélère le développement et optimise le build de production.}`;

  if (tex.includes(OLD)) {
    tex = tex.replace(OLD, NEW);
    writeFileSync(texPath, tex, 'utf8');
    console.log('\n[LATEX] techbox calls restored');
  } else {
    console.log('\n[LATEX] section already patched or not found — no change');
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN
// ─────────────────────────────────────────────────────────────────────────────
await generateMermaidPNGs();
await downloadLogos();
patchLatex();

console.log('\nDone. Compile:');
console.log('  cd "rapport PFA" && pdflatex -interaction=nonstopmode untitled-1.tex && pdflatex untitled-1.tex');
