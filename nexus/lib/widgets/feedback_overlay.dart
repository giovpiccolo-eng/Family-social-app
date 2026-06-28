import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../core/theme.dart';
import '../session/session_controller.dart';
import '../models/content_item.dart';

class FeedbackOverlay extends StatelessWidget {
  final AnswerResult result;
  final ContentItem item;
  final VoidCallback onNext;
  final bool isLastItem;

  const FeedbackOverlay({
    super.key,
    required this.result,
    required this.item,
    required this.onNext,
    this.isLastItem = false,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final correct = result.correct;
    final color = correct ? NexusColors.correct : NexusColors.wrong;

    return Container(
      color: Colors.black.withOpacity(0.85),
      child: SafeArea(
        child: Column(
          children: [
            const Spacer(),
            Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  // Result banner
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: color.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: color.withOpacity(0.5), width: 1.5),
                    ),
                    child: Column(
                      children: [
                        Text(
                          correct ? '✓ CORRECT' : '✗ REVIEW',
                          style: theme.textTheme.headlineMedium?.copyWith(color: color),
                        ).animate().scale(duration: 300.ms, curve: Curves.elasticOut),

                        const SizedBox(height: 8),

                        // Marks earned
                        Text(
                          '${result.marksEarned} / ${result.marksAvailable} marks  ·  +${result.xpEarned} XP',
                          style: monoStyle(size: 14, color: color),
                        ),
                      ],
                    ),
                  ).animate().fadeIn(duration: 200.ms).slideY(begin: 0.1),

                  const SizedBox(height: 16),

                  // Mark scheme (always shown)
                  if (item.markScheme.isNotEmpty) ...[
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: NexusColors.surface,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: NexusColors.border),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('MARK SCHEME', style: labelCaps(color: NexusColors.inkMuted)),
                          const SizedBox(height: 10),
                          ...item.markScheme.asMap().entries.map((e) {
                            final isKey = result.missedKeywords.any(
                              (mk) => e.value.toLowerCase().contains(mk.toLowerCase()),
                            );
                            return Padding(
                              padding: const EdgeInsets.only(bottom: 6),
                              child: Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    '${e.key + 1}.',
                                    style: monoStyle(size: 13, color: NexusColors.inkMuted),
                                  ),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Text(
                                      e.value,
                                      style: monoStyle(
                                        size: 13,
                                        color: isKey ? NexusColors.wrong : NexusColors.correct,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            );
                          }),
                        ],
                      ),
                    ).animate().fadeIn(delay: 100.ms),

                    const SizedBox(height: 12),
                  ],

                  // Missed keywords highlight
                  if (result.missedKeywords.isNotEmpty && !correct) ...[
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: NexusColors.wrong.withOpacity(0.08),
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(color: NexusColors.wrong.withOpacity(0.3)),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('KEYWORDS YOU MISSED', style: labelCaps(color: NexusColors.wrong)),
                          const SizedBox(height: 8),
                          Wrap(
                            spacing: 6,
                            runSpacing: 4,
                            children: result.missedKeywords
                                .take(6)
                                .map((kw) => Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                      decoration: BoxDecoration(
                                        color: NexusColors.wrong.withOpacity(0.15),
                                        borderRadius: BorderRadius.circular(6),
                                      ),
                                      child: Text(kw, style: monoStyle(size: 12, color: NexusColors.wrong)),
                                    ))
                                .toList(),
                          ),
                        ],
                      ),
                    ).animate().fadeIn(delay: 150.ms),

                    const SizedBox(height: 12),
                  ],

                  // Frontier fact
                  if (item.frontier != null) ...[
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: NexusColors.nexusLight,
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(color: NexusColors.nexus.withOpacity(0.3)),
                      ),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('🔭 ', style: TextStyle(fontSize: 16)),
                          Expanded(
                            child: Text(
                              item.frontier!,
                              style: monoStyle(size: 12, color: NexusColors.nexus),
                            ),
                          ),
                        ],
                      ),
                    ).animate().fadeIn(delay: 200.ms),
                    const SizedBox(height: 12),
                  ],

                  // Continue button
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton(
                      onPressed: onNext,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: correct ? NexusColors.correct : NexusColors.forces,
                        padding: const EdgeInsets.symmetric(vertical: 18),
                      ),
                      child: Text(isLastItem ? 'FINISH ROUND →' : 'NEXT →'),
                    ),
                  ).animate().fadeIn(delay: 250.ms),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
