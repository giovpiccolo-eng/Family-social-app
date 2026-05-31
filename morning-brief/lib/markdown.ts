import { marked } from "marked";

marked.setOptions({ gfm: true, breaks: false });

export function markdownToHtml(markdown: string): string {
  return marked.parse(markdown, { async: false }) as string;
}

/**
 * Wrap rendered essay HTML in a clean, email-client-friendly document.
 * Inline styles only — email clients strip <style> in many cases, but a
 * scoped <style> in the head works in most modern webmail and is a good
 * baseline. Kept deliberately simple and readable.
 */
export function renderEmailHtml(opts: {
  title: string;
  bodyHtml: string;
  dateLabel: string;
  greeting?: string;
}): string {
  const { title, bodyHtml, dateLabel, greeting } = opts;
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>${escapeHtml(title)}</title>
<style>
  body { margin: 0; background: #f6f5f1; }
  .wrap { max-width: 680px; margin: 0 auto; padding: 32px 20px 64px;
    font-family: Georgia, 'Times New Roman', serif; color: #1c1c1c;
    line-height: 1.65; font-size: 18px; }
  .kicker { font-family: -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif;
    text-transform: uppercase; letter-spacing: .12em; font-size: 12px;
    color: #8a7d5a; margin: 0 0 4px; }
  h1 { font-size: 30px; line-height: 1.2; margin: 8px 0 16px; }
  h2 { font-size: 22px; margin: 36px 0 8px; border-bottom: 1px solid #e6e2d6; padding-bottom: 6px; }
  h3 { font-size: 19px; margin: 24px 0 6px; }
  p { margin: 0 0 16px; }
  em { color: #555; }
  a { color: #1a5276; }
  blockquote { border-left: 3px solid #d9d2bf; margin: 16px 0; padding: 4px 16px; color: #444; }
  ul, ol { margin: 0 0 16px; padding-left: 24px; }
  hr { border: 0; border-top: 1px solid #e6e2d6; margin: 32px 0; }
  .footer { font-family: -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif;
    font-size: 13px; color: #999; margin-top: 48px; }
</style>
</head>
<body>
  <div class="wrap">
    <p class="kicker">Morning Brief · ${escapeHtml(dateLabel)}</p>
    ${greeting ? `<p>${escapeHtml(greeting)}</p>` : ""}
    ${bodyHtml}
    <p class="footer">Generated for you by Morning Brief. Reply to adjust your interests.</p>
  </div>
</body>
</html>`;
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}
