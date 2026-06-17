import { readdir, readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";

const root = process.cwd();
const distClient = join(root, "dist", "client");
const distServerAssets = join(root, "dist", "server", "assets");

const serverAssets = await readdir(distServerAssets);
const manifestFile = serverAssets.find((f) =>
  f.includes("tanstack-start-manifest"),
);

if (!manifestFile) {
  console.error("Could not find TanStack Start manifest in dist/server/assets");
  process.exit(1);
}

const manifestContent = await readFile(
  join(distServerAssets, manifestFile),
  "utf-8",
);

const entryMatch = manifestContent.match(/clientEntry:\s*"([^"]+)"/);
if (!entryMatch) {
  console.error("Could not extract clientEntry from manifest");
  process.exit(1);
}
const clientEntry = entryMatch[1];

const clientAssets = await readdir(join(distClient, "assets"));
const cssFile = clientAssets.find(
  (f) => f.startsWith("styles-") && f.endsWith(".css"),
);

const rootPreloadsMatch = manifestContent.match(
  /__root__.*?preloads:\s*\[([^\]]+)\]/,
);
let preloadTags = "";
if (rootPreloadsMatch) {
  const preloads = rootPreloadsMatch[1]
    .match(/"([^"]+)"/g)
    ?.map((s) => s.replace(/"/g, ""));
  if (preloads) {
    preloadTags = preloads
      .map((p) => `    <link rel="modulepreload" href="${p}" />`)
      .join("\n");
  }
}

const html = `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>EduLife — One clear path to learn, pass, and grow</title>
    <meta name="description" content="EduLife is a mobile-first learning platform for Moroccan learners. Discover courses, track progress, pass exams, and earn verified certificates." />
    <meta name="author" content="EduLife" />
    <meta property="og:title" content="EduLife — Structured learning for real progress" />
    <meta property="og:description" content="One guided path from course to certificate. Built for Moroccan learners in Darija, French, and English." />
    <meta property="og:type" content="website" />
    <meta name="twitter:card" content="summary" />
    <meta name="twitter:site" content="@EduLifeApp" />
${cssFile ? `    <link rel="stylesheet" href="/assets/${cssFile}" />` : ""}
${preloadTags}
    <script>try{if(localStorage.getItem('edulife-dark')==='true')document.documentElement.classList.add('dark')}catch(e){}</script>
  </head>
  <body>
    <script type="module" src="${clientEntry}"></script>
  </body>
</html>
`;

await writeFile(join(distClient, "index.html"), html);
console.log(`Generated dist/client/index.html (entry: ${clientEntry})`);
