import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';
import '../core/theme.dart';
import '../models/content_item.dart';
import '../providers/app_provider.dart';
import '../session/session_controller.dart';
import '../widgets/modes/recall_mode.dart';
import '../widgets/modes/calculate_mode.dart';
import '../widgets/modes/apply_mode.dart';
import '../widgets/modes/crack_mode.dart';
import '../widgets/modes/learn_mode.dart';
import '../widgets/modes/lab_mode.dart';
import '../widgets/feedback_overlay.dart';
import 'close_screen.dart';

class SessionScreen extends StatefulWidget {
  const SessionScreen({super.key});

  @override
  State<SessionScreen> createState() => _SessionScreenState();
}

class _SessionScreenState extends State<SessionScreen> {
  AnswerResult? _pendingResult;
  bool _showFeedback = false;

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppProvider>();
    final session = app.session;
    if (session == null) return const SizedBox.shrink();

    if (session.isComplete) {
      return CloseScreen(session: session);
    }

    final round = session.currentRound;
    if (round == null) return const SizedBox.shrink();

    final item = round.current;
    if (item == null) return const SizedBox.shrink();

    final realmCol = realmColor(item.realm);

    return Scaffold(
      backgroundColor: NexusColors.background,
      body: SafeArea(
        child: Stack(
          children: [
            Column(
              children: [
                // Header: round info + progress
                _SessionHeader(
                  session: session,
                  round: round,
                  realmColor: realmCol,
                  onExit: () => _confirmExit(context),
                ),

                // Question content
                Expanded(
                  child: AnimatedSwitcher(
                    duration: const Duration(milliseconds: 250),
                    switchInCurve: Curves.easeOut,
                    child: _buildQuestionWidget(item, session, round),
                  ),
                ),
              ],
            ),

            // Feedback overlay
            if (_showFeedback && _pendingResult != null)
              Positioned.fill(
                child: FeedbackOverlay(
                  result: _pendingResult!,
                  item: item,
                  isLastItem: round.currentIndex >= round.items.length - 1,
                  onNext: _advance,
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildQuestionWidget(ContentItem item, SessionController session, SessionRound round) {
    final key = ValueKey('${item.id}_${round.currentIndex}');

    switch (item.mode) {
      case QuestionMode.recall:
        return RecallMode(
          key: key,
          item: item,
          onSubmit: (answer) => _onKeywordAnswer(item, answer, session),
        );
      case QuestionMode.calculate:
        return CalculateMode(
          key: key,
          item: item,
          onSubmit: (val, unit) => _onCalcAnswer(item, val, unit, session),
        );
      case QuestionMode.apply:
      case QuestionMode.command:
        return ApplyMode(
          key: key,
          item: item,
          onSubmit: (answer) => _onKeywordAnswer(item, answer, session),
        );
      case QuestionMode.crack:
        return CrackMode(
          key: key,
          item: item,
          onSubmit: (answer) => _onKeywordAnswer(item, answer, session),
        );
      case QuestionMode.learn:
        return LearnMode(
          key: key,
          item: item,
          onComplete: () {
            final result = AnswerResult(
              correct: true,
              marksEarned: 0,
              marksAvailable: 0,
              xpEarned: item.xpReward,
            );
            _applyAndShow(result, item, session);
          },
        );
      case QuestionMode.lab:
        return LabMode(
          key: key,
          item: item,
          onSubmit: (answer) => _onKeywordAnswer(item, answer, session),
        );
      case QuestionMode.frontier:
        // Treat frontier as apply
        return ApplyMode(
          key: key,
          item: item,
          onSubmit: (answer) => _onKeywordAnswer(item, answer, session),
        );
    }
  }

  void _onKeywordAnswer(ContentItem item, String answer, SessionController session) {
    final result = session.markKeywordAnswer(item, answer);
    _applyAndShow(result, item, session);
  }

  void _onCalcAnswer(ContentItem item, String value, String unit, SessionController session) {
    final result = session.markCalculationAnswer(item, value, unit);
    _applyAndShow(result, item, session);
  }

  void _applyAndShow(AnswerResult result, ContentItem item, SessionController session) {
    session.applyResult(result, item);
    if (mounted) {
      setState(() {
        _pendingResult = result;
        _showFeedback = true;
      });
    }
  }

  void _advance() {
    final app = context.read<AppProvider>();
    final session = app.session!;
    session.advanceItem();
    setState(() {
      _showFeedback = false;
      _pendingResult = null;
    });
    if (session.isComplete) {
      app.endSession();
    } else {
      app.notifyProgressChanged();
    }
  }

  Future<void> _confirmExit(BuildContext context) async {
    final leave = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: NexusColors.surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Text('Leave session?', style: Theme.of(context).textTheme.headlineSmall),
        content: Text(
          'Progress so far will be saved.',
          style: Theme.of(context).textTheme.bodyMedium,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('STAY'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('LEAVE'),
          ),
        ],
      ),
    );

    if (leave == true && mounted) {
      await context.read<AppProvider>().endSession();
    }
  }
}

class _SessionHeader extends StatelessWidget {
  final SessionController session;
  final SessionRound round;
  final Color realmColor;
  final VoidCallback onExit;

  const _SessionHeader({
    required this.session,
    required this.round,
    required this.realmColor,
    required this.onExit,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final progress = session.sessionProgress;

    return Column(
      children: [
        // Progress bar
        LinearProgressIndicator(
          value: progress,
          backgroundColor: NexusColors.border,
          valueColor: AlwaysStoppedAnimation<Color>(realmColor),
          minHeight: 3,
        ),

        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: Row(
            children: [
              // Exit button
              GestureDetector(
                onTap: onExit,
                child: const Icon(Icons.close, color: NexusColors.inkMuted, size: 22),
              ),

              const SizedBox(width: 12),

              // Round info
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(round.typeName, style: labelCaps(color: realmColor)),
                    Text(round.typeSubtitle, style: theme.textTheme.bodySmall),
                  ],
                ),
              ),

              // Item counter
              Text(
                '${round.currentIndex + 1} / ${round.items.length}',
                style: monoStyle(size: 13, color: NexusColors.inkSecondary),
              ),

              const SizedBox(width: 8),

              // Round dots
              Row(
                children: List.generate(session.totalRounds, (i) {
                  final done = i < session.completedRounds;
                  final current = i == session.currentRoundIndex;
                  return Container(
                    width: 8,
                    height: 8,
                    margin: const EdgeInsets.only(left: 4),
                    decoration: BoxDecoration(
                      color: done
                          ? NexusColors.correct
                          : current
                              ? realmColor
                              : NexusColors.border,
                      shape: BoxShape.circle,
                    ),
                  );
                }),
              ),
            ],
          ),
        ),

        const Divider(height: 1),
      ],
    );
  }
}
