import Link from "next/link";
import { listArchive } from "@/lib/archive";

export const dynamic = "force-dynamic";

export default async function ArchivePage() {
  const entries = await listArchive();

  return (
    <>
      <p className="kicker">Morning Brief</p>
      <h1>Archive</h1>
      {entries.length === 0 ? (
        <p className="hint">
          No briefs yet. Generate one from <Link href="/">Settings</Link> with “Send a brief now”.
        </p>
      ) : (
        <ul className="archive">
          {entries.map((e) => (
            <li key={e.date}>
              <div className="date">{formatDate(e.date)}</div>
              <Link href={`/archive/${e.date}`}>{e.title}</Link>
            </li>
          ))}
        </ul>
      )}
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
