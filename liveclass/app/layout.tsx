import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "LiveClass — Teacher Console",
  description: "An AI co-pilot that listens to a teacher's lesson and surfaces vetted material in real time.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="min-h-screen font-sans">{children}</body>
    </html>
  );
}
