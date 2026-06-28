import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';
import '../core/theme.dart';
import '../core/constants.dart';
import '../providers/app_provider.dart';
import '../session/session_controller.dart';

class CloseScreen extends StatelessWidget {
  final SessionController session;

  const CloseScreen({super.key, required this.session});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final app = context.read<AppProvider>();
    final progress = app.progress;

    return Scaffold(
      backgroundColor: NexusColors.background,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              const Spacer(),

              // Session complete icon
              const Text('⚡', style: TextStyle(fontSize: 64))
                  .animate()
                  .scale(duration: 600.ms, curve: Curves.elasticOut),

              const SizedBox(height: 16),

              Text(
                'MISSION COMPLETE',
                style: theme.textTheme.displaySmall?.copyWith(color: NexusColors.nexus),
                textAlign: TextAlign.center,
              ).animate().fadeIn(delay: 200.ms),

              const SizedBox(height: 8),

              Text(
                'Session saved. Streak +1.',
                style: theme.textTheme.bodyMedium?.copyWith(color: NexusColors.inkSecondary),
              ).animate().fadeIn(delay: 350.ms),

              const SizedBox(height: 40),

              // Stats row
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  _StatChip(
                    value: '+${session.totalXpEarned}',
                    label: 'XP EARNED',
                    color: NexusColors.xpGold,
                    delay: 400,
                  ),
                  _StatChip(
                    value: '+${session.totalCreditsEarned}',
                    label: 'CREDITS',
                    color: NexusColors.nexus,
                    delay: 500,
                  ),
                  _StatChip(
                    value: '${progress.streakDays}',
                    label: 'DAY STREAK',
                    color: NexusColors.life,
                    delay: 600,
                  ),
                ],
              ),

              const SizedBox(height: 32),

              // Rank
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: NexusColors.surface,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: NexusColors.border),
                ),
                child: Column(
                  children: [
                    Text('RANK', style: labelCaps()),
                    const SizedBox(height: 6),
                    Text(
                      rankForXp(progress.totalXp),
                      style: theme.textTheme.headlineLarge?.copyWith(color: NexusColors.xpGold),
                    ),
                    const SizedBox(height: 8),
                    // XP progress to next rank
                    Builder(builder: (context) {
                      final next = nextRankXp(progress.totalXp);
                      final current = progress.totalXp;
                      // Find current rank threshold
                      int currentMin = 0;
                      for (final r in kRanks) {
                        if ((r['minXp'] as int) <= current) currentMin = r['minXp'] as int;
                      }
                      final pct = ((current - currentMin) / (next - currentMin)).clamp(0.0, 1.0);
                      return Column(
                        children: [
                          ClipRRect(
                            borderRadius: BorderRadius.circular(4),
                            child: LinearProgressIndicator(
                              value: pct,
                              minHeight: 8,
                              backgroundColor: NexusColors.border,
                              valueColor: const AlwaysStoppedAnimation(NexusColors.xpGold),
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            '${current} / ${next} XP',
                            style: monoStyle(size: 11, color: NexusColors.inkMuted),
                          ),
                        ],
                      );
                    }),
                  ],
                ),
              ).animate().fadeIn(delay: 700.ms),

              const Spacer(),

              // "One more?" hook
              Text(
                '→ one more question?',
                style: theme.textTheme.bodySmall?.copyWith(
                  color: NexusColors.inkMuted,
                  decoration: TextDecoration.underline,
                ),
              ).animate().fadeIn(delay: 900.ms),

              const SizedBox(height: 12),

              // Home button
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () => context.read<AppProvider>().endSession(),
                  child: const Text('BACK TO BASE'),
                ),
              ).animate().fadeIn(delay: 800.ms),
            ],
          ),
        ),
      ),
    );
  }
}

class _StatChip extends StatelessWidget {
  final String value;
  final String label;
  final Color color;
  final int delay;

  const _StatChip({
    required this.value,
    required this.label,
    required this.color,
    required this.delay,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          width: 90,
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: BoxDecoration(
            color: color.withOpacity(0.1),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: color.withOpacity(0.4)),
          ),
          child: Column(
            children: [
              Text(
                value,
                style: TextStyle(
                  color: color,
                  fontSize: 22,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 2),
              Text(label, style: labelCaps(color: color, size: 9)),
            ],
          ),
        ),
      ],
    ).animate().fadeIn(delay: Duration(milliseconds: delay)).slideY(begin: 0.1);
  }
}
