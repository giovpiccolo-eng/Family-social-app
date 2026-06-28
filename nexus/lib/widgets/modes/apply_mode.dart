import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../../core/theme.dart';
import '../../models/content_item.dart';

class ApplyMode extends StatefulWidget {
  final ContentItem item;
  final void Function(String answer) onSubmit;

  const ApplyMode({super.key, required this.item, required this.onSubmit});

  @override
  State<ApplyMode> createState() => _ApplyModeState();
}

class _ApplyModeState extends State<ApplyMode> {
  final _controller = TextEditingController();
  bool _submitted = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final item = widget.item;
    final color = realmColor(item.realm);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Badge row
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: realmSurface(item.realm),
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(color: color.withOpacity(0.3)),
                ),
                child: Text(
                  'APPLY · ${item.ao}',
                  style: labelCaps(color: color),
                ),
              ),
              const Spacer(),
              // Command word chip
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: NexusColors.nexusLight,
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(color: NexusColors.nexus.withOpacity(0.3)),
                ),
                child: Text(
                  item.command.toUpperCase(),
                  style: labelCaps(color: NexusColors.nexus),
                ),
              ),
            ],
          ),

          const SizedBox(height: 20),

          // Graph / dataset
          if (item.dataset != null) ...[
            _DatasetWidget(dataset: item.dataset!),
            const SizedBox(height: 16),
          ],

          // Question stem
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: NexusColors.surface,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: NexusColors.border),
            ),
            child: Text(item.stem, style: theme.textTheme.bodyLarge?.copyWith(height: 1.7)),
          ).animate().fadeIn(),

          const SizedBox(height: 12),

          // Command word guidance
          _CommandGuidance(command: item.command),

          const SizedBox(height: 16),

          // Answer input
          TextField(
            controller: _controller,
            maxLines: 5,
            textCapitalization: TextCapitalization.sentences,
            style: theme.textTheme.bodyLarge,
            decoration: InputDecoration(
              hintText: _hintFor(item.command),
            ),
            onChanged: (_) => setState(() {}),
          ).animate().fadeIn(delay: 100.ms),

          const SizedBox(height: 16),

          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: _submitted ? null : _submit,
              child: const Text('SUBMIT ANSWER'),
            ),
          ),
        ],
      ),
    );
  }

  String _hintFor(String command) {
    switch (command) {
      case 'describe':
        return 'Describe what you see in the data…';
      case 'explain':
        return 'Explain the reason why…';
      case 'suggest':
        return 'Suggest what might happen…';
      default:
        return 'Type your answer…';
    }
  }

  void _submit() {
    if (_controller.text.trim().isEmpty) return;
    setState(() => _submitted = true);
    widget.onSubmit(_controller.text.trim());
  }
}

class _CommandGuidance extends StatelessWidget {
  final String command;
  const _CommandGuidance({required this.command});

  @override
  Widget build(BuildContext context) {
    const guidance = {
      'describe': 'DESCRIBE: State what happens — use data/observations.',
      'explain': 'EXPLAIN: State what happens AND give the reason WHY.',
      'suggest': 'SUGGEST: Apply your knowledge to this unfamiliar context.',
      'state': 'STATE: Give the fact — no explanation needed.',
      'compare': 'COMPARE: Give both similarities AND differences.',
      'calculate': 'CALCULATE: Show equation, substitution, and unit.',
    };
    final text = guidance[command];
    if (text == null) return const SizedBox.shrink();

    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: NexusColors.nexusLight,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: NexusColors.nexus.withOpacity(0.25)),
      ),
      child: Text(text, style: monoStyle(size: 12, color: NexusColors.nexus)),
    );
  }
}

class _DatasetWidget extends StatelessWidget {
  final Dataset dataset;
  const _DatasetWidget({required this.dataset});

  @override
  Widget build(BuildContext context) {
    if (dataset.type == 'line_chart') {
      return _LineChart(dataset: dataset);
    }
    if (dataset.type == 'table') {
      return _TableWidget(dataset: dataset);
    }
    return const SizedBox.shrink();
  }
}

class _LineChart extends StatelessWidget {
  final Dataset dataset;
  const _LineChart({required this.dataset});

  @override
  Widget build(BuildContext context) {
    final points = dataset.points;
    if (points.isEmpty) return const SizedBox.shrink();

    final spots = points.map((p) => FlSpot(p.x, p.y)).toList();
    final minX = points.map((p) => p.x).reduce((a, b) => a < b ? a : b);
    final maxX = points.map((p) => p.x).reduce((a, b) => a > b ? a : b);
    final minY = points.map((p) => p.y).reduce((a, b) => a < b ? a : b);
    final maxY = points.map((p) => p.y).reduce((a, b) => a > b ? a : b);

    return Container(
      height: 200,
      padding: const EdgeInsets.fromLTRB(8, 16, 16, 8),
      decoration: BoxDecoration(
        color: NexusColors.surfaceVariant,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: NexusColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (dataset.note != null)
            Padding(
              padding: const EdgeInsets.only(left: 8, bottom: 8),
              child: Text(dataset.note!, style: monoStyle(size: 10, color: NexusColors.inkMuted)),
            ),
          Expanded(
            child: LineChart(
              LineChartData(
                gridData: FlGridData(
                  show: true,
                  getDrawingHorizontalLine: (_) => FlLine(color: NexusColors.border, strokeWidth: 1),
                  getDrawingVerticalLine: (_) => FlLine(color: NexusColors.border, strokeWidth: 1),
                ),
                titlesData: FlTitlesData(
                  leftTitles: AxisTitles(
                    axisNameWidget: RotatedBox(
                      quarterTurns: 3,
                      child: Text(dataset.yLabel, style: monoStyle(size: 10, color: NexusColors.inkSecondary)),
                    ),
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 40,
                      getTitlesWidget: (v, _) => Text(
                        v.toInt().toString(),
                        style: monoStyle(size: 10, color: NexusColors.inkSecondary),
                      ),
                    ),
                  ),
                  bottomTitles: AxisTitles(
                    axisNameWidget: Text(dataset.xLabel, style: monoStyle(size: 10, color: NexusColors.inkSecondary)),
                    sideTitles: SideTitles(
                      showTitles: true,
                      getTitlesWidget: (v, _) => Text(
                        v.toInt().toString(),
                        style: monoStyle(size: 10, color: NexusColors.inkSecondary),
                      ),
                    ),
                  ),
                  topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                  rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                ),
                borderData: FlBorderData(
                  show: true,
                  border: Border.all(color: NexusColors.border),
                ),
                minX: minX,
                maxX: maxX,
                minY: minY < 0 ? minY * 1.1 : 0,
                maxY: maxY * 1.1,
                lineBarsData: [
                  LineChartBarData(
                    spots: spots,
                    isCurved: false,
                    color: NexusColors.forces,
                    barWidth: 2,
                    dotData: FlDotData(
                      show: true,
                      getDotPainter: (_, __, ___, ____) => FlDotCirclePainter(
                        radius: 4,
                        color: NexusColors.forces,
                        strokeWidth: 0,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    ).animate().fadeIn(duration: 400.ms);
  }
}

class _TableWidget extends StatelessWidget {
  final Dataset dataset;
  const _TableWidget({required this.dataset});

  @override
  Widget build(BuildContext context) {
    final headers = dataset.tableHeaders ?? [];
    final rows = dataset.tableRows ?? [];
    if (headers.isEmpty && rows.isEmpty) return const SizedBox.shrink();

    return Container(
      decoration: BoxDecoration(
        color: NexusColors.surfaceVariant,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: NexusColors.border),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(12),
        child: Table(
          border: TableBorder.all(color: NexusColors.border, width: 1),
          children: [
            if (headers.isNotEmpty)
              TableRow(
                decoration: const BoxDecoration(color: NexusColors.surface),
                children: headers
                    .map((h) => Padding(
                          padding: const EdgeInsets.all(10),
                          child: Text(h, style: monoStyle(size: 12, color: NexusColors.inkSecondary)),
                        ))
                    .toList(),
              ),
            ...rows.map((row) => TableRow(
                  children: row
                      .map((cell) => Padding(
                            padding: const EdgeInsets.all(10),
                            child: Text(
                              cell.toString(),
                              style: monoStyle(size: 13),
                            ),
                          ))
                      .toList(),
                )),
          ],
        ),
      ),
    );
  }
}
