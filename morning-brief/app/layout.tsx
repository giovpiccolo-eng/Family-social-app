import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "Morning Brief",
  description: "A daily news learning brief tailored to your interests.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <div className="container">
          <nav className="top">
            <Link href="/">Settings</Link>
            <Link href="/archive">Archive</Link>
          </nav>
          {children}
        </div>
      </body>
    </html>
  );
}
