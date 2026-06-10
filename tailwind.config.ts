import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: ["class"],
  content: [
    "./app/**/*.{ts,tsx}",
    "./components/**/*.{ts,tsx}",
    "./lib/**/*.{ts,tsx}",
  ],
  theme: {
    container: {
      center: true,
      padding: "1.5rem",
      screens: { "2xl": "1400px" },
    },
    extend: {
      colors: {
        // Observer.AI palette — dark navy + amber accent.
        navy: {
          950: "#0A0F1C",
          900: "#0D1424",
          800: "#121B2F",
          700: "#1A2440",
          600: "#243056",
          500: "#2F3D6B",
        },
        amber: {
          DEFAULT: "#F59E0B",
          400: "#FBBF24",
          500: "#F59E0B",
          600: "#D97706",
        },
        ink: {
          DEFAULT: "#E6EAF2",
          muted: "#8A93A8",
          faint: "#5A6378",
        },
        rule: "#1F2A44",
        // shadcn tokens mapped onto the Observer palette.
        background: "#0A0F1C",
        foreground: "#E6EAF2",
        card: "#0D1424",
        "card-foreground": "#E6EAF2",
        popover: "#0D1424",
        "popover-foreground": "#E6EAF2",
        primary: "#F59E0B",
        "primary-foreground": "#0A0F1C",
        secondary: "#1A2440",
        "secondary-foreground": "#E6EAF2",
        muted: "#121B2F",
        "muted-foreground": "#8A93A8",
        accent: "#1A2440",
        "accent-foreground": "#E6EAF2",
        destructive: "#DC2626",
        "destructive-foreground": "#E6EAF2",
        border: "#1F2A44",
        input: "#1F2A44",
        ring: "#F59E0B",
      },
      fontFamily: {
        // Typography per brief §5.
        sans: ["var(--font-dm-sans)", "system-ui", "sans-serif"],
        serif: ["var(--font-fraunces)", "Georgia", "serif"],
        mono: ["var(--font-plex-mono)", "ui-monospace", "monospace"],
      },
      borderRadius: {
        lg: "0.75rem",
        md: "0.5rem",
        sm: "0.375rem",
      },
      keyframes: {
        "accordion-down": {
          from: { height: "0" },
          to: { height: "var(--radix-accordion-content-height)" },
        },
        "accordion-up": {
          from: { height: "var(--radix-accordion-content-height)" },
          to: { height: "0" },
        },
        pulse: {
          "0%, 100%": { opacity: "1" },
          "50%": { opacity: "0.5" },
        },
      },
      animation: {
        "accordion-down": "accordion-down 0.2s ease-out",
        "accordion-up": "accordion-up 0.2s ease-out",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
};

export default config;
