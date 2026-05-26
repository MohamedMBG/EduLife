#!/usr/bin/env node

/**
 * Generate mermaid diagrams as PNG files
 * Run: node generate-diagrams.js
 */

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const diagrams = [
  'diagrams/web-architecture.mmd',
  'diagrams/unified-platform-architecture.mmd',
  'diagrams/data-synchronization.mmd',
  'diagrams/web-tech-stack.mmd',
];

console.log('🎨 Generating mermaid diagrams...\n');

diagrams.forEach((diagramPath) => {
  const baseName = path.basename(diagramPath, '.mmd');
  const outputPath = `diagrams/${baseName}.png`;

  if (!fs.existsSync(diagramPath)) {
    console.error(`❌ File not found: ${diagramPath}`);
    return;
  }

  try {
    console.log(`⏳ Generating ${baseName}.png...`);

    // Using mermaid-cli via npx
    execSync(
      `npx -y @mermaid-js/mermaid-cli@latest -i ${diagramPath} -o ${outputPath} -w 1920 -H 1080`,
      { stdio: 'inherit' }
    );

    if (fs.existsSync(outputPath)) {
      const stats = fs.statSync(outputPath);
      console.log(`✅ Generated: ${outputPath} (${Math.round(stats.size / 1024)}KB)\n`);
    } else {
      console.error(`❌ Failed to generate ${outputPath}\n`);
    }
  } catch (error) {
    console.error(`❌ Error generating ${baseName}:`);
    console.error(error.message);
    console.log('');
  }
});

console.log('\n📊 Diagram generation complete!');
console.log('Next steps:');
console.log('  1. Verify PNG files exist in diagrams/');
console.log('  2. Run: cd "rapport PFA" && pdflatex -interaction=nonstopmode untitled-1.tex');
console.log('  3. Run pdflatex again for table of contents');
