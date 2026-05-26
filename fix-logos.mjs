import { writeFileSync, existsSync } from 'fs';
import https from 'https';
import path from 'path';

const LOGOS_DIR = 'rapport PFA/edulife-logos';

function get(url) {
  return new Promise((resolve, reject) => {
    https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0 EduLife/1.0', 'Accept': 'image/*,*/*' } }, (res) => {
      if ([301,302,303,307,308].includes(res.statusCode) && res.headers.location) {
        return get(res.headers.location).then(resolve, reject);
      }
      const chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => resolve({ status: res.statusCode, body: Buffer.concat(chunks), ct: res.headers['content-type'] ?? '' }));
    }).on('error', reject);
  });
}

function isPNG(buf) { return buf.length > 4 && buf[0] === 0x89 && buf[1] === 0x50 && buf[2] === 0x4E && buf[3] === 0x47; }
function isJPEG(buf) { return buf.length > 3 && buf[0] === 0xFF && buf[1] === 0xD8 && buf[2] === 0xFF; }

// Wikipedia serves real PNG thumbnails of SVG at these URLs
const TARGETS = [
  ['react.png', [
    'https://upload.wikimedia.org/wikipedia/commons/thumb/a/a7/React-icon.svg/200px-React-icon.svg.png',
    'https://www.iconpacks.net/icons/2/free-react-logo-icon-1693-thumb.png',
  ]],
  ['typescript.png', [
    'https://upload.wikimedia.org/wikipedia/commons/thumb/4/4c/Typescript_logo_2020.svg/200px-Typescript_logo_2020.svg.png',
  ]],
  ['tailwind.png', [
    'https://upload.wikimedia.org/wikipedia/commons/thumb/d/d5/Tailwind_CSS_Logo.svg/200px-Tailwind_CSS_Logo.svg.png',
  ]],
  ['vite.png', [
    'https://upload.wikimedia.org/wikipedia/commons/thumb/f/f1/Vitejs-logo.svg/200px-Vitejs-logo.svg.png',
  ]],
  ['cloudflare.png', [
    'https://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/Cloudflare_Logo.svg/200px-Cloudflare_Logo.svg.png',
  ]],
  ['zod.png', [
    'https://upload.wikimedia.org/wikipedia/commons/thumb/9/9e/Zod_Logo.svg/200px-Zod_Logo.svg.png',
    // fallback: reuse typescript logo as placeholder
    'https://upload.wikimedia.org/wikipedia/commons/thumb/4/4c/Typescript_logo_2020.svg/200px-Typescript_logo_2020.svg.png',
  ]],
];

for (const [filename, urls] of TARGETS) {
  const out = path.join(LOGOS_DIR, filename);
  let done = false;
  for (const url of urls) {
    try {
      console.log(`  try  ${filename} <- ${url}`);
      const res = await get(url);
      if (res.status === 200 && (isPNG(res.body) || isJPEG(res.body))) {
        writeFileSync(out, res.body);
        console.log(`  ok   ${filename} (${Math.round(res.body.length/1024)} KB)`);
        done = true;
        break;
      } else {
        console.log(`  skip HTTP ${res.status} ct=${res.ct} size=${res.body.length} sig=${res.body.slice(0,4).toString('hex')}`);
      }
    } catch (e) { console.log(`  err  ${e.message}`); }
  }
  if (!done) console.error(`  FAIL ${filename} — all sources failed`);
}
console.log('\nDone');
