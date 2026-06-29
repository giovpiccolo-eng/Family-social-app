import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../core/theme.dart';
import '../models/prism_item.dart';
import '../providers/app_provider.dart';

class AssessmentScreen extends StatefulWidget {
  const AssessmentScreen({super.key});

  @override
  State<AssessmentScreen> createState() => _AssessmentScreenState();
}

class _AssessmentScreenState extends State<AssessmentScreen> {
  int? _selectedOption;
  bool _answered = false;

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppProvider>();
    final session = app.session;

    if (session == null) return const SizedBox.shrink();

    if (session.isDone) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        app.finishAssessment();
      });
      return const _CompletingScreen();
    }

    final item = session.nextItem;
    if (item == null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        app.finishAssessment();
      });
      return const _CompletingScreen();
    }

    return Scaffold(
      backgroundColor: PrismColors.background,
      appBar: AppBar(
        backgroundColor: PrismColors.background,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.close, color: PrismColors.inkMuted, size: 20),
          onPressed: () => _confirmExit(context, app),
        ),
        title: Column(
          children: [
            Text('FLUID REASONING', style: labelCaps(color: PrismColors.fluid)),
            const SizedBox(height: 2),
            LinearProgressIndicator(
              value: session.progress,
              backgroundColor: PrismColors.border,
              valueColor: const AlwaysStoppedAnimation(PrismColors.fluid),
              minHeight: 2,
            ),
          ],
        ),
        centerTitle: true,
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 8),

              // Item content
              Expanded(
                child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 250),
                  child: _ItemCard(item: item, key: ValueKey(item.id)),
                ),
              ),

              const SizedBox(height: 20),

              // Answer options
              ...List.generate(item.options.length, (i) {
                final isSelected = _selectedOption == i;
                return Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: _OptionButton(
                    label: item.options[i].toString(),
                    index: i,
                    selected: isSelected,
                    answered: _answered,
                    correct: i == item.answer,
                    onTap: _answered ? null : () => _onSelect(i, item, app),
                  ),
                );
              }),

              const SizedBox(height: 8),
              Text(
                'Take your time. There is no time pressure on this item.',
                style: Theme.of(context).textTheme.bodySmall,
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _onSelect(int index, PrismItem item, AppProvider app) {
    setState(() {
      _selectedOption = index;
      _answered = true;
    });

    Future.delayed(const Duration(milliseconds: 600), () {
      if (!mounted) return;
      final correct = index == item.answer;
      app.recordResponse(item, correct);
      setState(() {
        _selectedOption = null;
        _answered = false;
      });
    });
  }

  void _confirmExit(BuildContext context, AppProvider app) {
    showDialog<void>(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: PrismColors.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(14),
          side: const BorderSide(color: PrismColors.border),
        ),
        title: const Text('Exit assessment?'),
        content: const Text(
          'Your progress will be lost. The result requires at least 5 items.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Continue'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              app.goHome();
            },
            child: Text('Exit', style: TextStyle(color: PrismColors.negative)),
          ),
        ],
      ),
    );
  }
}

class _ItemCard extends StatelessWidget {
  final PrismItem item;
  const _ItemCard({required this.item, super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: PrismColors.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: PrismColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            _paradigmLabel(item.paradigm),
            style: labelCaps(color: PrismColors.fluid),
          ),
          const SizedBox(height: 20),
          _buildStem(context, item),
        ],
      ),
    );
  }

  String _paradigmLabel(ItemParadigm p) => switch (p) {
        ItemParadigm.numberSeries => 'NUMBER SERIES',
        ItemParadigm.matrixCompletion => 'MATRIX COMPLETION',
        ItemParadigm.figuralSeries => 'PATTERN SERIES',
        ItemParadigm.oddOneOut => 'ODD ONE OUT',
        ItemParadigm.logicalDeduction => 'LOGICAL DEDUCTION',
        _ => 'REASONING',
      };

  Widget _buildStem(BuildContext context, PrismItem item) {
    switch (item.paradigm) {
      case ItemParadigm.numberSeries:
        return _NumberSeriesStem(stem: item.stem);
      case ItemParadigm.matrixCompletion:
        return _MatrixStem(stem: item.stem);
      case ItemParadigm.oddOneOut:
        return _OddOneOutStem(stem: item.stem);
      default:
        return _TextStem(text: item.stem.toString());
    }
  }
}

class _NumberSeriesStem extends StatelessWidget {
  final dynamic stem;
  const _NumberSeriesStem({required this.stem});

  @override
  Widget build(BuildContext context) {
    final values = (stem as List).map((e) => e.toString()).toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('What comes next in the sequence?',
            style: Theme.of(context).textTheme.bodyMedium),
        const SizedBox(height: 20),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            ...values.map((v) => _NumberChip(value: v)),
            _NumberChip(value: '?', highlight: true),
          ],
        ),
      ],
    );
  }
}

class _NumberChip extends StatelessWidget {
  final String value;
  final bool highlight;
  const _NumberChip({required this.value, this.highlight = false});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: highlight ? PrismColors.fluid.withOpacity(0.15) : PrismColors.surfaceElevated,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: highlight ? PrismColors.fluid : PrismColors.border,
          width: highlight ? 1.5 : 1,
        ),
      ),
      child: Text(
        value,
        style: monoStyle(
          size: 18,
          color: highlight ? PrismColors.fluid : PrismColors.inkPrimary,
          weight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _MatrixStem extends StatelessWidget {
  final dynamic stem;
  const _MatrixStem({required this.stem});

  @override
  Widget build(BuildContext context) {
    final rows = stem as List;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Find the value that completes the pattern.',
            style: Theme.of(context).textTheme.bodyMedium),
        const SizedBox(height: 20),
        Table(
          border: TableBorder.all(color: PrismColors.border, width: 1),
          children: rows.map<TableRow>((row) {
            final cells = row as List;
            return TableRow(
              children: cells.map<Widget>((cell) {
                final isQ = cell.toString() == '?';
                return Container(
                  height: 52,
                  color: isQ ? PrismColors.fluid.withOpacity(0.1) : Colors.transparent,
                  alignment: Alignment.center,
                  child: Text(
                    cell.toString(),
                    style: monoStyle(
                      size: isQ ? 22 : 18,
                      color: isQ ? PrismColors.fluid : PrismColors.inkPrimary,
                      weight: FontWeight.w700,
                    ),
                  ),
                );
              }).toList(),
            );
          }).toList(),
        ),
      ],
    );
  }
}

class _OddOneOutStem extends StatelessWidget {
  final dynamic stem;
  const _OddOneOutStem({required this.stem});

  @override
  Widget build(BuildContext context) {
    final values = (stem as List).map((e) => e.toString()).toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Which one does NOT belong with the others?',
            style: Theme.of(context).textTheme.bodyMedium),
        const SizedBox(height: 16),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: values
              .map((v) => _NumberChip(value: v))
              .toList(),
        ),
      ],
    );
  }
}

class _TextStem extends StatelessWidget {
  final String text;
  const _TextStem({required this.text});

  @override
  Widget build(BuildContext context) {
    return Text(text, style: Theme.of(context).textTheme.bodyLarge);
  }
}

class _OptionButton extends StatelessWidget {
  final String label;
  final int index;
  final bool selected;
  final bool answered;
  final bool correct;
  final VoidCallback? onTap;

  const _OptionButton({
    required this.label,
    required this.index,
    required this.selected,
    required this.answered,
    required this.correct,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    Color borderColor = PrismColors.border;
    Color bgColor = PrismColors.surface;
    Color textColor = PrismColors.inkPrimary;

    if (answered && selected) {
      borderColor = correct ? PrismColors.positive : PrismColors.negative;
      bgColor = correct
          ? PrismColors.positive.withOpacity(0.1)
          : PrismColors.negative.withOpacity(0.1);
      textColor = correct ? PrismColors.positive : PrismColors.negative;
    } else if (selected) {
      borderColor = PrismColors.accent;
      bgColor = PrismColors.accentLight;
    }

    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: borderColor),
        ),
        child: Text(
          label,
          style: monoStyle(size: 16, color: textColor, weight: FontWeight.w600),
          textAlign: TextAlign.center,
        ),
      ),
    );
  }
}

class _CompletingScreen extends StatelessWidget {
  const _CompletingScreen();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: PrismColors.background,
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: PrismColors.fluid,
              ),
            ),
            const SizedBox(height: 16),
            Text('Computing your profile…',
                style: Theme.of(context).textTheme.bodyMedium),
          ],
        ),
      ),
    );
  }
}
