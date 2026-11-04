import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = {
  title: "AURICO · Acompanhamento profissional",
  description:
    "Planos definidos por profissionais. Acompanhamento com consentimento.",
};
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <body>{children}</body>
    </html>
  );
}
