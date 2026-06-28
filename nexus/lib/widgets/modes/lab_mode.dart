import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../../core/theme.dart';
import '../../models/content_item.dart';

class LabMode extends StatefulWidget {
  final ContentItem item;
  final void Function(String answer) onSubmit;

  const LabMode({super.key, required this.item, required this.onSubmit});

  @override
  State<LabMode> createState() => _LabModeState();
}

class _LabModeState extends State<LabMode> {
  final _extraController = TextEditingController();
  bool _submitted = false;

  @override
  void dispose() {
    _extraController.dispose();
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
          // Badge
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: NexusColors.lifeLight,
              borderRadius: BorderRadius.circular(6),
              border: Border.all(color: NexusColors.life.withOpacity(0.3)),
            ),
            child: Text('LAB · AO3 PRACTICAL', style: labelCaps(color: NexusColors.life)),
          ),

          const SizedBox(height: 20),

          // Scenario
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
                Text('EXPERIMENTAL DESIGN', style: labelCaps()),
                const SizedBox(height: 10),
                Text(item.stem, style: theme.textTheme.bodyLarge?.copyWith(height: 1.7)),
              ],
            ),
          ).animate().fadeIn(),

          const SizedBox(height: 20),

          // Mark scheme targets as fill-in boxes
          Text('IDENTIFY THE VARIABLES', style: labelCaps()),
          const SizedBox(height: 12),

          _VariableBox(
            label: 'Independent variable',
            hint: 'What you change',
            color: NexusColors.life,
            controller: TextEditingController(),
          ),
          const SizedBox(height: 10),
          _VariableBox(
            label: 'Dependent variable',
            hint: 'What you measure',
            color: NexusColors.forces,
            controller: TextEditingController(),
          ),
          const SizedBox(height: 10),
          _VariableBox(
            label: 'Control variable 1',
            hint: 'What you keep the same',
            color: NexusColors.matter,
            controller: TextEditingController(),
          ),
          const SizedBox(height: 10),
          _VariableBox(
            label: 'Control variable 2',
            hint: 'Another variable to keep constant',
            color: NexusColors.matter,
            controller: TextEditingController(),
          ),

          const SizedBox(height: 20),

          // Open answer for anomalies, reliability etc.
          Text('ADDITIONAL NOTES', style: labelCaps()),
          const SizedBox(height: 8),
          TextField(
            controller: _extraController,
            maxLines: 3,
            textCapitalization: TextCapitalization.sentences,
            style: theme.textTheme.bodyMedium,
            decoration: const InputDecoration(
              hintText: 'Reliability, anomalies, equipment, safety…',
            ),
          ),

          const SizedBox(height: 20),

          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: _submitted ? null : _submit,
              style: ElevatedButton.styleFrom(backgroundColor: NexusColors.life),
              child: const Text('SUBMIT'),
            ),
          ).animate().fadeIn(delay: 200.ms),
        ],
      ),
    );
  }

  void _submit() {
    setState(() => _submitted = true);
    widget.onSubmit(_extraController.text);
  }
}

class _VariableBox extends StatelessWidget {
  final String label;
  final String hint;
  final Color color;
  final TextEditingController controller;

  const _VariableBox({
    required this.label,
    required this.hint,
    required this.color,
    required this.controller,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 6,
          height: 48,
          decoration: BoxDecoration(
            color: color,
            borderRadius: BorderRadius.circular(3),
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: labelCaps(color: color, size: 10)),
              const SizedBox(height: 4),
              TextField(
                controller: controller,
                style: Theme.of(context).textTheme.bodyMedium,
                decoration: InputDecoration(hintText: hint),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
