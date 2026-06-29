import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class PrismColors {
  PrismColors._();

  static const Color background = Color(0xFF0C0E14);
  static const Color surface = Color(0xFF131620);
  static const Color surfaceElevated = Color(0xFF1A1E2C);
  static const Color accent = Color(0xFF6366F1);
  static const Color accentLight = Color(0xFF1A1B3A);
  static const Color border = Color(0xFF252838);
  static const Color inkPrimary = Color(0xFFEEF0F5);
  static const Color inkSecondary = Color(0xFF9399B0);
  static const Color inkMuted = Color(0xFF555C78);
  static const Color positive = Color(0xFF10B981);
  static const Color negative = Color(0xFFEF4444);

  // Domain spectrum — the prism refraction
  static const Color fluid = Color(0xFF8B5CF6);
  static const Color quantitative = Color(0xFFF59E0B);
  static const Color verbal = Color(0xFF10B981);
  static const Color visualSpatial = Color(0xFF06B6D4);
  static const Color workingMemory = Color(0xFFF97316);
  static const Color speed = Color(0xFFEF4444);
}

class PrismTheme {
  PrismTheme._();

  static ThemeData get dark {
    final base = ThemeData.dark(useMaterial3: true);
    return base.copyWith(
      scaffoldBackgroundColor: PrismColors.background,
      colorScheme: const ColorScheme.dark(
        primary: PrismColors.accent,
        secondary: PrismColors.accent,
        surface: PrismColors.surface,
        onPrimary: Colors.white,
        onSurface: PrismColors.inkPrimary,
      ),
      textTheme: GoogleFonts.interTextTheme(base.textTheme).copyWith(
        displayLarge: GoogleFonts.spaceGrotesk(
          fontSize: 40, fontWeight: FontWeight.w300,
          color: PrismColors.inkPrimary, letterSpacing: -1,
        ),
        displayMedium: GoogleFonts.spaceGrotesk(
          fontSize: 32, fontWeight: FontWeight.w400,
          color: PrismColors.inkPrimary, letterSpacing: -0.5,
        ),
        displaySmall: GoogleFonts.spaceGrotesk(
          fontSize: 24, fontWeight: FontWeight.w500,
          color: PrismColors.inkPrimary,
        ),
        headlineLarge: GoogleFonts.spaceGrotesk(
          fontSize: 20, fontWeight: FontWeight.w600,
          color: PrismColors.inkPrimary,
        ),
        headlineMedium: GoogleFonts.spaceGrotesk(
          fontSize: 18, fontWeight: FontWeight.w500,
          color: PrismColors.inkPrimary,
        ),
        titleLarge: GoogleFonts.inter(
          fontSize: 16, fontWeight: FontWeight.w600,
          color: PrismColors.inkPrimary,
        ),
        titleMedium: GoogleFonts.inter(
          fontSize: 14, fontWeight: FontWeight.w500,
          color: PrismColors.inkPrimary,
        ),
        bodyLarge: GoogleFonts.inter(
          fontSize: 15, height: 1.6, color: PrismColors.inkPrimary,
        ),
        bodyMedium: GoogleFonts.inter(
          fontSize: 13, height: 1.5, color: PrismColors.inkSecondary,
        ),
        bodySmall: GoogleFonts.inter(
          fontSize: 12, color: PrismColors.inkSecondary,
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: PrismColors.accent,
          foregroundColor: Colors.white,
          minimumSize: const Size(double.infinity, 52),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          textStyle: GoogleFonts.inter(
              fontSize: 14, fontWeight: FontWeight.w600, letterSpacing: 0.5),
          elevation: 0,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: PrismColors.inkSecondary,
          side: const BorderSide(color: PrismColors.border),
          minimumSize: const Size(double.infinity, 52),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          textStyle: GoogleFonts.inter(fontSize: 14, fontWeight: FontWeight.w500),
          elevation: 0,
        ),
      ),
      cardTheme: CardTheme(
        color: PrismColors.surface,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: const BorderSide(color: PrismColors.border),
        ),
      ),
      dividerTheme: const DividerThemeData(color: PrismColors.border, thickness: 1),
    );
  }
}

TextStyle monoStyle({
  double size = 12,
  Color color = PrismColors.inkSecondary,
  FontWeight weight = FontWeight.w400,
}) =>
    GoogleFonts.jetBrainsMono(fontSize: size, color: color, fontWeight: weight);

TextStyle labelCaps({double size = 10, Color color = PrismColors.inkMuted}) =>
    GoogleFonts.inter(
      fontSize: size,
      letterSpacing: 1.5,
      fontWeight: FontWeight.w600,
      color: color,
    );

Color domainColor(String domain) {
  switch (domain) {
    case 'fluid':          return PrismColors.fluid;
    case 'quantitative':   return PrismColors.quantitative;
    case 'verbal':         return PrismColors.verbal;
    case 'visual_spatial': return PrismColors.visualSpatial;
    case 'working_memory': return PrismColors.workingMemory;
    case 'speed':          return PrismColors.speed;
    default:               return PrismColors.accent;
  }
}
