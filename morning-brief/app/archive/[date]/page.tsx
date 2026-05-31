import Link from "next/link";
import { notFound } from "next/navigation";
import { readArchive } from "@/lib/archive";
import { markdownToHtml } from "@/lib/markdown";

export const dynamic = "force-dynamic";

export default async function BriefPage({ params }: { params: { date: string } }) {
  const entry = await readArchive(params.date);
  if (!entry) notFound();

  const html = markdownToHtml(entry.markdown);

  return (
    <>
      <p className="kicker">{formatDate(params.date)}</p>
      <p>
        <Link href="/archive">← Archive</Link>
      </p>
      <article className="essay" dangerouslySetInnerHTML={{ __html: html }} />
    </>
  );
}

function formatDate(date: string): string {
  const d = new Date(date + "T00:00:00");
  return new Intl.DateTimeFormat("en-GB", {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
  }).format(d);
}
