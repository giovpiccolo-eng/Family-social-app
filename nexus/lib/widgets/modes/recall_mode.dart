import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../../core/theme.dart';
import '../../models/content_item.dart';

class RecallMode extends StatefulWidget {
  final ContentItem item;
  final void Function(String answer) onSubmit;

  const RecallMode({super.key, required this.item, required this.onSubmit});

  @override
  State<RecallMode> createState() => _RecallModeState();
}

class _RecallModeState extends State<RecallMode> {
  final _controller = TextEditingController();
  final _focus = FocusNode();
  bool _submitted = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _focus.requestFocus());
  }

  @override
  void dispose() {
    _controller.dispose();
    _focus.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Mode badge
          _modeBadge('RECALL · SRS'),

          const SizedBox(height: 20),

          // Term / front
          if (widget.item.front != null) ...[
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: NexusColors.surfaceVariant,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: NexusColors.border),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('DEFINE', style: labelCaps()),
                  const SizedBox(height: 8),
                  Text(
                    widget.item.front!,
                    style: theme.textTheme.headlineMedium,
                  ),
                ],
              ),
            ).animate().fadeIn(duration: 300.ms).slideY(begin: 0.05),
            const SizedBox(height: 16),
          ],

          // Stem question
          Text(
            widget.item.stem,
            style: theme.textTheme.bodyLarge,
          ).animate().fadeIn(duration: 300.ms, delay: 100.ms),

          const SizedBox(height: 20),

          // Keyword hints (live highlighting)
          _KeywordHighlighter(
            answer: _controller.text,
            keywords: widget.item.keywords,
          ),

          const SizedBox(height: 12),

          // Text input
          TextField(
            controller: _controller,
            focusNode: _focus,
            maxLines: 4,
            textCapitalization: TextCapitalization.sentences,
            style: theme.textTheme.bodyLarge,
            decoration: const InputDecoration(
              hintText: 'Type your definition…',
            ),
            onChanged: (_) => setState(() {}),
            onSubmitted: _submitted ? null : (_) => _submit(),
          ).animate().fadeIn(duration: 300.ms, delay: 150.ms),

          const SizedBox(height: 16),

          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: _submitted ? null : _submit,
              child: const Text('SUBMIT'),
            ),
          ).animate().fadeIn(duration: 300.ms, delay: 200.ms),
        ],
      ),
    );
  }

  void _submit() {
    if (_controller.text.trim().isEmpty) return;
    setState(() => _submitted = true);
    widget.onSubmit(_controller.text.trim());
  }
}

class _KeywordHighlighter extends StatelessWidget {
  final String answer;
  final List<String> keywords;

  const _KeywordHighlighter({required this.answer, required this.keywords});

  @override
  Widget build(BuildContext context) {
    if (keywords.isEmpty) return const SizedBox.shrink();
    return Wrap(
      spacing: 8,
      runSpacing: 6,
      children: keywords.map((kw) {
        final hit = answer.toLowerCase().contains(kw.toLowerCase());
        return AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          decoration: BoxDecoration(
            color: hit ? NexusColors.correct.withOpacity(0.15) : NexusColors.surfaceVariant,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(
              color: hit ? NexusColors.correct : NexusColors.border,
              width: 1,
            ),
          ),
          child: Text(
            hit ? '✓ $kw' : kw,
            style: monoStyle(
              size: 12,
              color: hit ? NexusColors.correct : NexusColors.inkMuted,
            ),
          ),
        );
      }).toList(),
    );
  }
}

Widget _modeBadge(String label) {
  return Container(
    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
    decoration: BoxDecoration(
      color: NexusColors.forcesLight,
      borderRadius: BorderRadius.circular(6),
      border: Border.all(color: NexusColors.forces.withOpacity(0.3)),
    ),
    child: Text(label, style: labelCaps(color: NexusColors.forces)),
  );
}
