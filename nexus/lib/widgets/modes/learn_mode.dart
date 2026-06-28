import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../../core/theme.dart';
import '../../models/content_item.dart';

class LearnMode extends StatefulWidget {
  final ContentItem item;
  final VoidCallback onComplete;

  const LearnMode({super.key, required this.item, required this.onComplete});

  @override
  State<LearnMode> createState() => _LearnModeState();
}

class _LearnModeState extends State<LearnMode> {
  int _step = 0; // -1 = intro, 0..n = discovery steps, n+1 = reveal

  List<String> get steps => widget.item.learnSteps ?? [];
  @override
  void initState() {
    super.initState();
    _step = 0;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final item = widget.item;
    final color = realmColor(item.realm);

    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Badge
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: realmSurface(item.realm),
              borderRadius: BorderRadius.circular(6),
              border: Border.all(color: color.withOpacity(0.3)),
            ),
            child: Text('LEARN · DISCOVER', style: labelCaps(color: color)),
          ),

          const SizedBox(height: 20),

          // Progress dots
          if (steps.isNotEmpty) ...[
            Row(
              children: List.generate(steps.length + 1, (i) {
                final done = i < _step;
                final current = i == _step - 1;
                return Expanded(
                  child: Container(
                    height: 4,
                    margin: const EdgeInsets.symmetric(horizontal: 2),
                    decoration: BoxDecoration(
                      color: done || current ? color : NexusColors.border,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                );
              }),
            ),
            const SizedBox(height: 20),
          ],

          // Discovery steps
          if (_step < steps.length) ...[
            AnimatedSwitcher(
              duration: const Duration(milliseconds: 300),
              child: Container(
                key: ValueKey(_step),
                width: double.infinity,
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: NexusColors.surface,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: NexusColors.border),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'THINK ABOUT IT…',
                      style: labelCaps(color: NexusColors.inkMuted),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      steps[_step],
                      style: theme.textTheme.headlineSmall?.copyWith(height: 1.5),
                    ),
                  ],
                ),
              ),
            ),

            const Spacer(),

            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () => setState(() => _step++),
                style: ElevatedButton.styleFrom(backgroundColor: color),
                child: Text(_step == steps.length - 1 ? 'SEE THE ANSWER →' : 'NEXT →'),
              ),
            ),
          ]

          // Reveal: the concept named
          else ...[
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [realmSurface(item.realm), NexusColors.surface],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: color.withOpacity(0.5)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('NOW IT HAS A NAME', style: labelCaps(color: color)),
                  const SizedBox(height: 12),
                  _renderMarkdown(context, item.learnContent ?? item.stem),
                ],
              ),
            ).animate().fadeIn(duration: 400.ms).scale(begin: const Offset(0.97, 0.97)),

            const SizedBox(height: 16),

            if (item.nexusLink != null) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: NexusColors.nexusLight,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: NexusColors.nexus.withOpacity(0.4)),
                ),
                child: Row(
                  children: [
                    const Text('◈', style: TextStyle(color: NexusColors.nexus, fontSize: 18)),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        'NEXUS LINK: This concept connects across all three sciences.',
                        style: monoStyle(size: 12, color: NexusColors.nexus),
                      ),
                    ),
                  ],
                ),
              ).animate().fadeIn(delay: 300.ms),
              const SizedBox(height: 16),
            ],

            const Spacer(),

            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: widget.onComplete,
                style: ElevatedButton.styleFrom(backgroundColor: color),
                child: const Text('GOT IT →'),
              ),
            ).animate().fadeIn(delay: 400.ms),
          ],
        ],
      ),
    );
  }

  Widget _renderMarkdown(BuildContext context, String text) {
    // Simple bold rendering: **text** → bold
    final spans = <InlineSpan>[];
    final regex = RegExp(r'\*\*(.+?)\*\*');
    int last = 0;
    final style = Theme.of(context).textTheme.bodyLarge?.copyWith(height: 1.7);

    for (final match in regex.allMatches(text)) {
      if (match.start > last) {
        spans.add(TextSpan(text: text.substring(last, match.start), style: style));
      }
      spans.add(TextSpan(
        text: match.group(1),
        style: style?.copyWith(fontWeight: FontWeight.w700, color: NexusColors.inkPrimary),
      ));
      last = match.end;
    }
    if (last < text.length) {
      spans.add(TextSpan(text: text.substring(last), style: style));
    }

    return RichText(text: TextSpan(children: spans));
  }
}
