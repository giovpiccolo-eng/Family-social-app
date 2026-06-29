import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../core/theme.dart';
import '../models/assessment_result.dart';
import '../providers/app_provider.dart';

class ResultsScreen extends StatelessWidget {
  final AssessmentResult result;
  const ResultsScreen({super.key, required this.result});

  @override
  Widget build(BuildContext context) {
    final fluid = result.scoreFor('fluid');
    final theme = Theme.of(context);

    return Scaffold(
      backgroundColor: PrismColors.background,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 16),

              // Header
              Text('YOUR RESULT', style: labelCaps(color: PrismColors.accent)),
              const SizedBox(height: 8),
              Text('Cognitive Ability Profile',
                  style: theme.textTheme.displaySmall),
              const SizedBox(height: 4),
              Text(
                'Fluid Reasoning Screener · ${result.date.day}/${result.date.month}/${result.date.year}',
                style: theme.textTheme.bodySmall,
              ),

              const SizedBox(height: 32),

              // Index band — the hero
              if (fluid != null) ...[
                _IndexBand(score: fluid),
                const SizedBox(height: 24),
                _DomainCard(score: fluid),
                const SizedBox(height: 24),
              ],

              // What it means
              _InterpretationSection(result: result),
              const SizedBox(height: 24),

              // Domain spectrum placeholder
              _DomainSpectrum(result: result),
              const SizedBox(height: 32),

              // Disclaimer box
              _DisclaimerBox(),
              const SizedBox(height: 24),

              // Actions
              ElevatedButton(
                onPressed: () => context.read<AppProvider>().startAssessment(),
                child: const Text('RETAKE ASSESSMENT'),
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: () => context.read<AppProvider>().goHome(),
                child: const Text('HOME'),
              ),
              const SizedBox(height: 32),
            ],
          ),
        ),
      ),
    );
  }
}

class _IndexBand extends StatelessWidget {
  final DomainScore score;
  const _IndexBand({required this.score});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            PrismColors.fluid.withOpacity(0.15),
            PrismColors.surface,
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: PrismColors.fluid.withOpacity(0.4)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('PRISM INDEX (FLUID)', style: labelCaps(color: PrismColors.fluid)),
          const SizedBox(height: 12),
          Row(
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text(
                score.index.round().toString(),
                style: monoStyle(size: 52, color: PrismColors.inkPrimary, weight: FontWeight.w700),
              ),
              const SizedBox(width: 8),
              Text(
                '± ${(score.ciHi - score.ciLo).round() ~/ 2}',
                style: monoStyle(size: 18, color: PrismColors.inkSecondary),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            '95% CI  ${score.ciLo.round()} – ${score.ciHi.round()}',
            style: monoStyle(size: 13, color: PrismColors.fluid),
          ),
          const SizedBox(height: 2),
          Text(
            '~${score.percentile.round()}th percentile  ·  ${score.itemsAdministered} items administered',
            style: monoStyle(size: 11),
          ),
          const SizedBox(height: 16),

          // CI visualisation bar
          _CIBar(score: score),
        ],
      ),
    );
  }
}

class _CIBar extends StatelessWidget {
  final DomainScore score;
  const _CIBar({required this.score});

  @override
  Widget build(BuildContext context) {
    const scaleMin = 55.0;
    const scaleMax = 145.0;
    const scaleRange = scaleMax - scaleMin;

    final loFrac = ((score.ciLo - scaleMin) / scaleRange).clamp(0.0, 1.0);
    final hiFrac = ((score.ciHi - scaleMin) / scaleRange).clamp(0.0, 1.0);
    final midFrac = ((score.index - scaleMin) / scaleRange).clamp(0.0, 1.0);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        LayoutBuilder(builder: (context, constraints) {
          final w = constraints.maxWidth;
          return Stack(
            children: [
              // Track
              Container(
                height: 8,
                decoration: BoxDecoration(
                  color: PrismColors.surfaceElevated,
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
              // CI band
              Positioned(
                left: w * loFrac,
                width: w * (hiFrac - loFrac),
                child: Container(
                  height: 8,
                  decoration: BoxDecoration(
                    color: PrismColors.fluid.withOpacity(0.3),
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
              ),
              // Point estimate
              Positioned(
                left: w * midFrac - 4,
                child: Container(
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
                    color: PrismColors.fluid,
                    shape: BoxShape.circle,
                  ),
                ),
              ),
            ],
          );
        }),
        const SizedBox(height: 4),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('55', style: monoStyle(size: 10)),
            Text('100', style: monoStyle(size: 10)),
            Text('145', style: monoStyle(size: 10)),
          ],
        ),
      ],
    );
  }
}

class _DomainCard extends StatelessWidget {
  final DomainScore score;
  const _DomainCard({required this.score});

  @override
  Widget build(BuildContext context) {
    final semCiWidth = (score.ciHi - score.ciLo).round();
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: PrismColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: PrismColors.border),
      ),
      child: Column(
        children: [
          _StatRow('Domain', 'Fluid Reasoning'),
          _StatRow('θ estimate', score.theta.toStringAsFixed(2)),
          _StatRow('SEM', score.sem.toStringAsFixed(2)),
          _StatRow('CI width (95%)', '$semCiWidth points'),
          _StatRow('Items', score.itemsAdministered.toString()),
          _StatRow('Calibration', 'Seed parameters (Phase 0)'),
        ],
      ),
    );
  }
}

class _StatRow extends StatelessWidget {
  final String label;
  final String value;
  const _StatRow(this.label, this.value);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        children: [
          Expanded(child: Text(label, style: Theme.of(context).textTheme.bodySmall)),
          Text(value, style: monoStyle(size: 12, color: PrismColors.inkPrimary)),
        ],
      ),
    );
  }
}

class _InterpretationSection extends StatelessWidget {
  final AssessmentResult result;
  const _InterpretationSection({required this.result});

  @override
  Widget build(BuildContext context) {
    final fluid = result.scoreFor('fluid');
    if (fluid == null) return const SizedBox.shrink();

    final idx = fluid.index;
    String range, framing;

    if (idx < 80) {
      range = 'Below average range';
      framing = 'Fluid reasoning is trainable. Regular practice with novel problems and strategy work can lead to meaningful improvement over time.';
    } else if (idx < 90) {
      range = 'Low-average range';
      framing = 'This profile suggests some relative difficulty with novel pattern-finding tasks. Structured practice in logical reasoning can help.';
    } else if (idx < 110) {
      range = 'Average range';
      framing = 'A solid fluid reasoning profile. Most people fall in this range, and performance varies by rest, focus, and familiarity with this type of task.';
    } else if (idx < 120) {
      range = 'High-average range';
      framing = 'Above-average facility with novel reasoning tasks. This reflects efficient working memory and pattern-detection.';
    } else if (idx < 130) {
      range = 'Superior range';
      framing = 'Strong fluid reasoning ability, placing you well above most people on novel problem-solving tasks.';
    } else {
      range = 'Very superior range';
      framing = 'Exceptionally high fluid reasoning score — though always read this alongside the confidence interval.';
    }

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: PrismColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: PrismColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('INTERPRETATION', style: labelCaps()),
          const SizedBox(height: 10),
          Text(range,
              style: Theme.of(context)
                  .textTheme
                  .titleMedium
                  ?.copyWith(color: PrismColors.fluid)),
          const SizedBox(height: 8),
          Text(framing, style: Theme.of(context).textTheme.bodyMedium),
          const SizedBox(height: 12),
          Text(
            'Note: This result covers fluid reasoning only. A full six-domain profile '
            'requires completing all domain modules. Confidence intervals are wide at this '
            'early calibration stage.',
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ),
    );
  }
}

class _DomainSpectrum extends StatelessWidget {
  final AssessmentResult result;
  const _DomainSpectrum({required this.result});

  @override
  Widget build(BuildContext context) {
    final domains = [
      ('fluid', 'Fluid', PrismColors.fluid),
      ('quantitative', 'Quant.', PrismColors.quantitative),
      ('verbal', 'Verbal', PrismColors.verbal),
      ('visual_spatial', 'Spatial', PrismColors.visualSpatial),
      ('working_memory', 'Memory', PrismColors.workingMemory),
      ('speed', 'Speed', PrismColors.speed),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('DOMAIN SPECTRUM', style: labelCaps()),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: PrismColors.surface,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: PrismColors.border),
          ),
          child: Column(
            children: domains.map((d) {
              final (key, label, color) = d;
              final score = result.scoreFor(key);
              final hasMeasure = score != null;
              final barFrac = hasMeasure
                  ? ((score.index - 55) / 90).clamp(0.0, 1.0)
                  : 0.0;

              return Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: Row(
                  children: [
                    SizedBox(
                      width: 56,
                      child: Text(label, style: labelCaps(size: 9, color: color)),
                    ),
                    Expanded(
                      child: Stack(
                        children: [
                          Container(
                            height: 16,
                            decoration: BoxDecoration(
                              color: PrismColors.surfaceElevated,
                              borderRadius: BorderRadius.circular(4),
                            ),
                          ),
                          if (hasMeasure)
                            FractionallySizedBox(
                              widthFactor: barFrac,
                              child: Container(
                                height: 16,
                                decoration: BoxDecoration(
                                  color: color.withOpacity(0.5),
                                  borderRadius: BorderRadius.circular(4),
                                ),
                              ),
                            ),
                          if (!hasMeasure)
                            Container(
                              height: 16,
                              alignment: Alignment.center,
                              child: Text('not measured',
                                  style: monoStyle(size: 9, color: PrismColors.inkMuted)),
                            ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    SizedBox(
                      width: 36,
                      child: Text(
                        hasMeasure ? score.index.round().toString() : '–',
                        style: monoStyle(
                          size: 12,
                          color: hasMeasure ? PrismColors.inkPrimary : PrismColors.inkMuted,
                        ),
                        textAlign: TextAlign.right,
                      ),
                    ),
                  ],
                ),
              );
            }).toList(),
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Unmeasured domains will fill in once those modules are complete.',
          style: Theme.of(context).textTheme.bodySmall,
        ),
      ],
    );
  }
}

class _DisclaimerBox extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: PrismColors.accentLight,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: PrismColors.accent.withOpacity(0.2)),
      ),
      child: Text(
        'PRISM is an adaptive estimate for self-insight and curiosity — not a clinical '
        'or diagnostic IQ test. For formal assessment (gifted placement, clinical '
        'diagnosis, educational decisions), please consult a qualified psychologist '
        'and a validated instrument such as WAIS or WISC.',
        style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: PrismColors.inkSecondary,
              height: 1.5,
            ),
      ),
    );
  }
}
