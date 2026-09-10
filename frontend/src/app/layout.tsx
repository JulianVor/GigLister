import type { Metadata } from "next";
import { Archivo_Black, Barlow_Condensed, Inter } from "next/font/google";
import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import "./globals.css";

const displayFont = Archivo_Black({
  variable: "--font-display-family",
  subsets: ["latin"],
  weight: "400",
});

const metaFont = Barlow_Condensed({
  variable: "--font-meta-family",
  subsets: ["latin"],
  weight: ["500", "600"],
});

const sansFont = Inter({
  variable: "--font-sans-family",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "GigLister – Konzerte in deiner Nähe",
  description: "Der lokale Konzertführer: Wo, wann und wer spielt in deiner Stadt.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="de" className={`${displayFont.variable} ${metaFont.variable} ${sansFont.variable} h-full`}>
      <body className="min-h-full flex flex-col font-sans antialiased">
        <Header />
        <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-8">{children}</main>
        <Footer />
      </body>
    </html>
  );
}
