# Generate Mermaid Diagrams for Report

The report references mermaid diagrams that need to be generated as PNG files for inclusion in the LaTeX PDF.

## Mermaid Files Created

Located in `diagrams/`:
- `web-architecture.mmd` - Web app internal architecture
- `unified-platform-architecture.mmd` - Web + Mobile + Backend unified view
- `data-synchronization.mmd` - Cross-platform data sync example
- `web-tech-stack.mmd` - Complete web technology stack

## Generation Methods

### Option 1: Online Mermaid Editor (Fastest)

1. Go to https://mermaid.live
2. Copy content from each `.mmd` file
3. Paste into editor
4. Click "Download SVG" or "Download PNG"
5. Save as `diagrams/[filename].png`

### Option 2: Mermaid CLI (Local)

Install mermaid-cli globally:
```bash
npm install -g @mermaid-js/mermaid-cli
```

Then generate diagrams:
```bash
cd diagrams

mmdc -i web-architecture.mmd -o web-architecture.png
mmdc -i unified-platform-architecture.mmd -o unified-platform-architecture.png
mmdc -i data-synchronization.mmd -o data-synchronization.png
mmdc -i web-tech-stack.mmd -o web-tech-stack.png
```

### Option 3: Docker

```bash
docker run --rm -v $(pwd)/diagrams:/data minlag/mermaid-cli:latest \
  -i /data/web-architecture.mmd \
  -o /data/web-architecture.png
```

(Repeat for each .mmd file)

### Option 4: VS Code Extension

1. Install "Markdown Preview Mermaid Support" extension
2. Open each `.mmd` file in VS Code
3. Right-click preview → "Export SVG" or "Export PNG"

## Current Status

LaTeX report references these diagrams in:
- Chapter 6 (Architecture web) - `web-architecture.png`
- Chapter 9 (Architecture backend unifiée) - `unified-platform-architecture.png`
- Chapter 9, section 2 (Synchronisation) - `data-synchronization.png`
- Chapter 8 (Technologies web) - `web-tech-stack.png`

Once PNG files are generated and placed in `diagrams/` folder, run:
```bash
cd "rapport PFA"
pdflatex -interaction=nonstopmode untitled-1.tex
pdflatex untitled-1.tex  # Run twice for TOC
```

## File Sizes & Quality

- Default resolution: 1920x1080 (good for PDF)
- For higher quality: use `--width 2400 --height 1440` in mermaid-cli
- SVG format better for scaling, PNG better for PDF embedding

## Example: Full Batch Generation

Bash script to generate all at once:
```bash
#!/bin/bash
cd diagrams

for file in *.mmd; do
  outfile="${file%.mmd}.png"
  mmdc -i "$file" -o "$outfile" -w 1920 -H 1080
  echo "Generated $outfile"
done

echo "All diagrams generated!"
```

Save as `generate-diagrams.sh` and run:
```bash
chmod +x generate-diagrams.sh
./generate-diagrams.sh
```
