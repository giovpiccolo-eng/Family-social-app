import PDFDocument from "pdfkit";

interface Segment {
  text: string;
  bold?: boolean;
  italic?: boolean;
  link?: string;
}

/** Tokenise inline Markdown (**bold**, *italic*, [text](url)) into runs. */
function parseInline(line: string): Segment[] {
  const segments: Segment[] = [];
  const re = /(\[([^\]]+)\]\(([^)]+)\))|(\*\*([^*]+)\*\*)|(\*([^*]+)\*)/g;
  let last = 0;
  let m: RegExpExecArray | null;
  while ((m = re.exec(line)) !== null) {
    if (m.index > last) segments.push({ text: line.slice(last, m.index) });
    if (m[1]) segments.push({ text: m[2], link: m[3] });
    else if (m[4]) segments.push({ text: m[5], bold: true });
    else if (m[6]) segments.push({ text: m[7], italic: true });
    last = re.lastIndex;
  }
  if (last < line.length) segments.push({ text: line.slice(last) });
  return segments.length ? segments : [{ text: line }];
}

function fontFor(seg: Segment): string {
  if (seg.bold) return "Times-Bold";
  if (seg.italic || seg.link) return "Times-Italic";
  return "Times-Roman";
}

export interface PdfMeta {
  title: string;
  dateLabel: string;
  greeting?: string;
}

/**
 * Render a brief's Markdown to a print-quality PDF.
 * Returns the finished file as a Buffer.
 */
export function renderPdf(markdown: string, meta: PdfMeta): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({
      size: "A4",
      margins: { top: 64, bottom: 64, left: 64, right: 64 },
      info: { Title: meta.title, Author: "Morning Brief" },
    });

    const chunks: Buffer[] = [];
    doc.on("data", (c) => chunks.push(c as Buffer));
    doc.on("end", () => resolve(Buffer.concat(chunks)));
    doc.on("error", reject);

    const ink = "#1c1c1c";
    const muted = "#6b6b6b";
    const gold = "#8a7d5a";
    const accent = "#1a5276";
    const line = "#e6e2d6";
    const contentWidth = doc.page.width - 128;

    // Masthead
    doc
      .font("Helvetica-Bold")
      .fontSize(10)
      .fillColor(gold)
      .text(`MORNING BRIEF  ·  ${meta.dateLabel.toUpperCase()}`, { characterSpacing: 1.5 });
    doc.moveDown(0.4);

    const lines = markdown.split("\n");
    let firstH1Done = false;

    const renderParagraph = (segs: Segment[], opts: { size: number; color: string; gap: number; italicWhole?: boolean }) => {
      doc.fontSize(opts.size).fillColor(opts.color);
      segs.forEach((seg, i) => {
        const isLast = i === segs.length - 1;
        const font = opts.italicWhole ? "Times-Italic" : fontFor(seg);
        doc.font(font);
        if (seg.link) {
          doc.fillColor(accent).text(seg.text, { continued: !isLast, underline: true, link: seg.link });
          doc.fillColor(opts.color);
        } else {
          doc.text(seg.text, { continued: !isLast, underline: false, link: undefined });
        }
      });
      doc.moveDown(opts.gap);
    };

    for (let raw of lines) {
      const lineText = raw.replace(/\s+$/, "");
      if (!lineText.trim()) continue;

      if (lineText.startsWith("# ")) {
        const t = lineText.slice(2).trim();
        doc.moveDown(0.3);
        doc.font("Times-Bold").fontSize(26).fillColor(ink).text(t, { lineGap: 2 });
        doc.moveDown(0.4);
        firstH1Done = true;
        if (meta.greeting) {
          doc.font("Times-Roman").fontSize(12).fillColor(ink).text(meta.greeting);
          doc.moveDown(0.4);
        }
        continue;
      }

      if (lineText.startsWith("## ")) {
        const t = lineText.slice(3).trim();
        doc.moveDown(0.6);
        // section rule
        const y = doc.y;
        doc.font("Times-Bold").fontSize(16).fillColor(ink).text(t);
        doc
          .moveTo(64, doc.y + 2)
          .lineTo(64 + contentWidth, doc.y + 2)
          .lineWidth(0.5)
          .strokeColor(line)
          .stroke();
        doc.moveDown(0.5);
        continue;
      }

      if (lineText.startsWith("- ")) {
        const segs = parseInline(lineText.slice(2).trim());
        doc.fontSize(10.5).fillColor(ink);
        doc.font("Times-Roman").text("•  ", { continued: true });
        segs.forEach((seg, i) => {
          const isLast = i === segs.length - 1;
          doc.font(fontFor(seg));
          if (seg.link) {
            doc.fillColor(accent).text(seg.text, { continued: !isLast, underline: true, link: seg.link });
            doc.fillColor(ink);
          } else {
            doc.text(seg.text, { continued: !isLast, link: undefined });
          }
        });
        doc.moveDown(0.25);
        continue;
      }

      // Standfirst: a whole-line italic right after the title.
      const isStandfirst = firstH1Done && /^\*[^*].*\*$/.test(lineText.trim());
      const segs = parseInline(lineText.trim());
      renderParagraph(segs, {
        size: isStandfirst ? 12.5 : 11.5,
        color: isStandfirst ? muted : ink,
        gap: 0.7,
        italicWhole: isStandfirst,
      });
    }

    doc.end();
  });
}
