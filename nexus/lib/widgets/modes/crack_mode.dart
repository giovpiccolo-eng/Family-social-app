import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../../core/theme.dart';
import '../../models/content_item.dart';

class CrackMode extends StatefulWidget {
  final ContentItem item;
  final void Function(String answer) onSubmit;

  const CrackMode({super.key, required this.item, required this.onSubmit});

  @override
  State<CrackMode> createState() => _CrackModeState();
}

class _CrackModeState extends State<CrackMode> {
  final _controller = TextEditingController();
  bool _submitted = false;
  int _charCount = 0;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final item = widget.item;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Exam boss header
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [NexusColors.nexusLight, NexusColors.surface],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: NexusColors.nexus.withOpacity(0.4)),
            ),
            child: Row(
              children: [
                const Text('⚡', style: TextStyle(fontSize: 24)),
                const SizedBox(width: 12),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('CRACK · EXAM BOSS', style: labelCaps(color: NexusColors.nexus)),
                    const SizedBox(height: 2),
                    Text(
                      '${item.marks} marks · ${item.command.toUpperCase()}',
                      style: theme.textTheme.bodySmall,
                    ),
                  ],
                ),
              ],
            ),
          ).animate().fadeIn().slideY(begin: -0.05),

          const SizedBox(height: 20),

          // Question
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
                Text(item.stem, style: theme.textTheme.bodyLarge?.copyWith(height: 1.75)),
                const SizedBox(height: 12),
                // Mark allocation indicator
                Row(
                  children: [
                    ...List.generate(
                      item.marks,
                      (i) => Padding(
                        padding: const EdgeInsets.only(right: 4),
                        child: Container(
                          width: 14,
                          height: 14,
                          decoration: BoxDecoration(
                            color: NexusColors.xpGold.withOpacity(0.2),
                            borderRadius: BorderRadius.circular(3),
                            border: Border.all(color: NexusColors.xpGold.withOpacity(0.4)),
                          ),
                          child: const Center(
                            child: Text('✦', style: TextStyle(fontSize: 8, color: NexusColors.xpGold)),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text('[${item.marks} marks]', style: theme.textTheme.bodySmall),
                  ],
                ),
              ],
            ),
          ).animate().fadeIn(delay: 100.ms),

          const SizedBox(height: 16),

          // Examiner tip
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: NexusColors.matterLight,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: NexusColors.matter.withOpacity(0.3)),
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('💡', style: TextStyle(fontSize: 16)),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Aim for ${item.marks} distinct points. Use the command word "${item.command}" — that tells you the depth needed.',
                    style: monoStyle(size: 12, color: NexusColors.matter),
                  ),
                ),
              ],
            ),
          ).animate().fadeIn(delay: 150.ms),

          const SizedBox(height: 16),

          // Answer area
          Stack(
            children: [
              TextField(
                controller: _controller,
                maxLines: 8,
                textCapitalization: TextCapitalization.sentences,
                style: Theme.of(context).textTheme.bodyLarge,
                decoration: InputDecoration(
                  hintText: 'Write your full answer here…\n\nTarget: ${item.marks} distinct points.',
                  alignLabelWithHint: true,
                ),
                onChanged: (v) => setState(() => _charCount = v.length),
              ),
              Positioned(
                bottom: 10,
                right: 14,
                child: Text(
                  '$_charCount chars',
                  style: monoStyle(size: 10, color: NexusColors.inkMuted),
                ),
              ),
            ],
          ).animate().fadeIn(delay: 200.ms),

          const SizedBox(height: 20),

          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: _submitted ? null : _submit,
              style: ElevatedButton.styleFrom(
                backgroundColor: NexusColors.nexus,
                padding: const EdgeInsets.symmetric(vertical: 18),
              ),
              child: const Text('SUBMIT TO MARK SCHEME'),
            ),
          ).animate().fadeIn(delay: 250.ms),
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
