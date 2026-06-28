import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

// Realm accent colours + Nexus violet
class NexusColors {
  static const background = Color(0xFF0B0D12);
  static const surface = Color(0xFF13161E);
  static const surfaceVariant = Color(0xFF1C2030);
  static const border = Color(0xFF2A2F40);

  static const inkPrimary = Color(0xFFE8ECF4);
  static const inkSecondary = Color(0xFF8A93A8);
  static const inkMuted = Color(0xFF4A5168);

  // Realm: LIFE — Biology
  static const life = Color(0xFF3DD68C);
  static const lifeLight = Color(0xFF1A3D2A);

  // Realm: MATTER — Chemistry
  static const matter = Color(0xFFFFB547);
  static const matterLight = Color(0xFF3D2D0A);

  // Realm: FORCES — Physics
  static const forces = Color(0xFF4A9FFF);
  static const forcesLight = Color(0xFF0A1F3D);

  // Nexus cross-science
  static const nexus = Color(0xFFA855F7);
  static const nexusLight = Color(0xFF2A1040);

  static const correct = Color(0xFF3DD68C);
  static const wrong = Color(0xFFFF5A5A);
  static const warning = Color(0xFFFFB547);

  static const xpGold = Color(0xFFFFD700);
}

Color realmColor(String realm) {
  switch (realm.toLowerCase()) {
    case 'life':
      return NexusColors.life;
    case 'matter':
      return NexusColors.matter;
    case 'forces':
      return NexusColors.forces;
    default:
      return NexusColors.nexus;
  }
}

Color realmSurface(String realm) {
  switch (realm.toLowerCase()) {
    case 'life':
      return NexusColors.lifeLight;
    case 'matter':
      return NexusColors.matterLight;
    case 'forces':
      return NexusColors.forcesLight;
    default:
      return NexusColors.nexusLight;
  }
}

class NexusTheme {
  static ThemeData get dark {
    final base = ThemeData.dark(useMaterial3: true);
    return base.copyWith(
      scaffoldBackgroundColor: NexusColors.background,
      colorScheme: const ColorScheme.dark(
        primary: NexusColors.nexus,
        secondary: NexusColors.forces,
        surface: NexusColors.surface,
        onPrimary: NexusColors.inkPrimary,
        onSurface: NexusColors.inkPrimary,
        outline: NexusColors.border,
      ),
      textTheme: _textTheme,
      cardTheme: CardTheme(
        color: NexusColors.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: const BorderSide(color: NexusColors.border, width: 1),
        ),
        elevation: 0,
        margin: EdgeInsets.zero,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: NexusColors.nexus,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
          textStyle: _textTheme.labelLarge,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: NexusColors.surfaceVariant,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: NexusColors.border),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: NexusColors.border),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: NexusColors.nexus, width: 1.5),
        ),
        hintStyle: const TextStyle(color: NexusColors.inkMuted),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      ),
      dividerTheme: const DividerThemeData(color: NexusColors.border, thickness: 1),
    );
  }

  static TextTheme get _textTheme {
    return TextTheme(
      // Display: large headings — geometric sans
      displayLarge: GoogleFonts.spaceGrotesk(
        fontSize: 48, fontWeight: FontWeight.w700, color: NexusColors.inkPrimary, letterSpacing: -1.5,
      ),
      displayMedium: GoogleFonts.spaceGrotesk(
        fontSize: 36, fontWeight: FontWeight.w700, color: NexusColors.inkPrimary, letterSpacing: -1,
      ),
      displaySmall: GoogleFonts.spaceGrotesk(
        fontSize: 28, fontWeight: FontWeight.w600, color: NexusColors.inkPrimary,
      ),
      // Headlines
      headlineLarge: GoogleFonts.spaceGrotesk(
        fontSize: 24, fontWeight: FontWeight.w700, color: NexusColors.inkPrimary,
      ),
      headlineMedium: GoogleFonts.spaceGrotesk(
        fontSize: 20, fontWeight: FontWeight.w600, color: NexusColors.inkPrimary,
      ),
      headlineSmall: GoogleFonts.spaceGrotesk(
        fontSize: 18, fontWeight: FontWeight.w600, color: NexusColors.inkPrimary,
      ),
      // Body — clean sans
      bodyLarge: GoogleFonts.inter(
        fontSize: 16, fontWeight: FontWeight.w400, color: NexusColors.inkPrimary, height: 1.6,
      ),
      bodyMedium: GoogleFonts.inter(
        fontSize: 14, fontWeight: FontWeight.w400, color: NexusColors.inkPrimary, height: 1.5,
      ),
      bodySmall: GoogleFonts.inter(
        fontSize: 12, fontWeight: FontWeight.w400, color: NexusColors.inkSecondary, height: 1.4,
      ),
      // Labels
      labelLarge: GoogleFonts.spaceGrotesk(
        fontSize: 15, fontWeight: FontWeight.w600, color: NexusColors.inkPrimary, letterSpacing: 0.5,
      ),
      labelMedium: GoogleFonts.spaceGrotesk(
        fontSize: 13, fontWeight: FontWeight.w500, color: NexusColors.inkSecondary, letterSpacing: 0.4,
      ),
      labelSmall: GoogleFonts.spaceGrotesk(
        fontSize: 11, fontWeight: FontWeight.w500, color: NexusColors.inkMuted, letterSpacing: 0.8,
      ),
      // Title (monospace for data)
      titleLarge: GoogleFonts.jetBrainsMono(
        fontSize: 18, fontWeight: FontWeight.w600, color: NexusColors.inkPrimary,
      ),
      titleMedium: GoogleFonts.jetBrainsMono(
        fontSize: 15, fontWeight: FontWeight.w500, color: NexusColors.inkPrimary,
      ),
      titleSmall: GoogleFonts.jetBrainsMono(
        fontSize: 13, fontWeight: FontWeight.w400, color: NexusColors.inkSecondary,
      ),
    );
  }
}

// Convenience: mono text style for equations/data
TextStyle monoStyle({double size = 14, Color? color, FontWeight weight = FontWeight.w500}) {
  return GoogleFonts.jetBrainsMono(
    fontSize: size,
    fontWeight: weight,
    color: color ?? NexusColors.inkPrimary,
  );
}

TextStyle labelCaps({double size = 11, Color? color}) {
  return GoogleFonts.spaceGrotesk(
    fontSize: size,
    fontWeight: FontWeight.w600,
    color: color ?? NexusColors.inkMuted,
    letterSpacing: 1.2,
  );
}
