import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../../core/theme.dart';
import '../../models/content_item.dart';

class CalculateMode extends StatefulWidget {
  final ContentItem item;
  final void Function(String value, String unit) onSubmit;

  const CalculateMode({super.key, required this.item, required this.onSubmit});

  @override
  State<CalculateMode> createState() => _CalculateModeState();
}

class _CalculateModeState extends State<CalculateMode> {
  final _valueController = TextEditingController();
  final _unitController = TextEditingController();
  bool _submitted = false;
  bool _showEquation = false;

  @override
  void dispose() {
    _valueController.dispose();
    _unitController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Badge
          _calcBadge('CALCULATE · AO2', widget.item.marks),

          const SizedBox(height: 20),

          // Question stem
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: NexusColors.surface,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: NexusColors.border),
            ),
            child: Text(
              widget.item.stem,
              style: theme.textTheme.bodyLarge?.copyWith(height: 1.7),
            ),
          ).animate().fadeIn(),

          const SizedBox(height: 16),

          // Given quantities
          if (widget.item.given != null) ...[
            Text('GIVEN', style: labelCaps()),
            const SizedBox(height: 8),
            Wrap(
              spacing: 10,
              runSpacing: 8,
              children: widget.item.given!.entries.map((e) {
                return Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(
                    color: NexusColors.forcesLight,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: NexusColors.forces.withOpacity(0.4)),
                  ),
                  child: Text(
                    '${e.key} = ${e.value}',
                    style: monoStyle(size: 14, color: NexusColors.forces),
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: 16),
          ],

          // Equation hint (toggle)
          if (widget.item.equation != null) ...[
            GestureDetector(
              onTap: () => setState(() => _showEquation = !_showEquation),
              child: Row(
                children: [
                  Icon(
                    _showEquation ? Icons.expand_less : Icons.expand_more,
                    color: NexusColors.inkMuted,
                    size: 18,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    _showEquation ? 'Hide equation' : 'Show equation hint',
                    style: theme.textTheme.labelMedium,
                  ),
                ],
              ),
            ),
            if (_showEquation) ...[
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: NexusColors.nexusLight,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: NexusColors.nexus.withOpacity(0.3)),
                ),
                child: Text(
                  widget.item.equation!,
                  style: monoStyle(size: 16, color: NexusColors.nexus),
                ),
              ),
            ],
            const SizedBox(height: 16),
          ],

          // Mark scheme reminder
          Text('SHOW YOUR WORKING', style: labelCaps()),
          const SizedBox(height: 6),
          Text(
            'Method marks are credited even if your final answer is wrong.',
            style: theme.textTheme.bodySmall,
          ),

          const SizedBox(height: 20),

          // Answer inputs
          Row(
            children: [
              Expanded(
                flex: 3,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('ANSWER', style: labelCaps()),
                    const SizedBox(height: 6),
                    TextField(
                      controller: _valueController,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true, signed: true),
                      style: monoStyle(size: 18),
                      decoration: const InputDecoration(hintText: '0.0'),
                      onChanged: (_) => setState(() {}),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                flex: 2,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('UNIT', style: labelCaps()),
                    const SizedBox(height: 6),
                    TextField(
                      controller: _unitController,
                      style: monoStyle(size: 18),
                      decoration: const InputDecoration(hintText: 'm/s'),
                      onChanged: (_) => setState(() {}),
                    ),
                  ],
                ),
              ),
            ],
          ).animate().fadeIn(delay: 150.ms),

          const SizedBox(height: 20),

          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: _submitted ? null : _submit,
              child: const Text('CHECK ANSWER'),
            ),
          ),
        ],
      ),
    );
  }

  void _submit() {
    if (_valueController.text.trim().isEmpty) return;
    setState(() => _submitted = true);
    widget.onSubmit(_valueController.text.trim(), _unitController.text.trim());
  }
}

Widget _calcBadge(String label, int marks) {
  return Row(
    children: [
      Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(
          color: NexusColors.forcesLight,
          borderRadius: BorderRadius.circular(6),
          border: Border.all(color: NexusColors.forces.withOpacity(0.3)),
        ),
        child: Text(label, style: labelCaps(color: NexusColors.forces)),
      ),
      const Spacer(),
      Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(
          color: NexusColors.surfaceVariant,
          borderRadius: BorderRadius.circular(6),
        ),
        child: Text('[$marks marks]', style: monoStyle(size: 12, color: NexusColors.inkSecondary)),
      ),
    ],
  );
}
