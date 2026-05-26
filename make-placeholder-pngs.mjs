import { deflateSync } from 'zlib';
import { writeFileSync } from 'fs';
import path from 'path';

const LOGOS_DIR = 'rapport PFA/edulife-logos';

function crc32(buf) {
  let crc = 0xFFFFFFFF;
  for (const byte of buf) {
    crc ^= byte;
    for (let i = 0; i < 8; i++) crc = (crc >>> 1) ^ (crc & 1 ? 0xEDB88320 : 0);
  }
  return (crc ^ 0xFFFFFFFF) >>> 0;
}

function chunk(type, data) {
  const t = Buffer.from(type);
  const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
  const crcVal = Buffer.alloc(4); crcVal.writeUInt32BE(crc32(Buffer.concat([t, data])));
  return Buffer.concat([len, t, data, crcVal]);
}

function makePNG(size, r, g, b) {
  const sig = Buffer.from([0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A]);
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(size, 0); ihdr.writeUInt32BE(size, 4);
  ihdr[8]=8; ihdr[9]=2; // 8-bit RGB
  const rows = [];
  for (let y = 0; y < size; y++) {
    rows.push(0x00); // filter none
    for (let x = 0; x < size; x++) rows.push(r, g, b);
  }
  return Buffer.concat([sig, chunk('IHDR',ihdr), chunk('IDAT', deflateSync(Buffer.from(rows))), chunk('IEND', Buffer.alloc(0))]);
}

// brand colors
const logos = [
  ['react.png',      0x61, 0xDA, 0xFB], // React cyan
  ['typescript.png', 0x31, 0x78, 0xC6], // TS blue
  ['tailwind.png',   0x06, 0xB6, 0xD4], // Tailwind cyan
  ['vite.png',       0x64, 0x6C, 0xFF], // Vite purple
  ['cloudflare.png', 0xF3, 0x80, 0x20], // CF orange
  ['zod.png',        0x30, 0x68, 0xB7], // Zod blue
];

for (const [name, r, g, b] of logos) {
  const out = path.join(LOGOS_DIR, name);
  const png = makePNG(64, r, g, b);
  writeFileSync(out, png);
  console.log(`  ok   ${name} (${png.length} bytes, RGB #${r.toString(16).padStart(2,'0')}${g.toString(16).padStart(2,'0')}${b.toString(16).padStart(2,'0')})`);
}
console.log('Done');
