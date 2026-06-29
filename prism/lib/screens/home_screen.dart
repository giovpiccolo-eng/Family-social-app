import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../core/theme.dart';
import '../providers/app_provider.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppProvider>();
    final theme = Theme.of(context);

    return Scaffold(
      backgroundColor: PrismColors.background,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 24),

              // Logo
              Row(
                children: [
                  _PrismMark(),
                  const SizedBox(width: 12),
                  Text('PRISM', style: monoStyle(size: 18, weight: FontWeight.w700, color: PrismColors.inkPrimary)),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                'Cognitive ability, refracted.',
                style: theme.textTheme.bodyMedium,
              ),

              const SizedBox(height: 40),

              // Previous result chip
              if (app.lastResult != null) ...[
                _LastResultCard(result: app.lastResult!),
                const SizedBox(height: 32),
              ],

              // Domain preview
              Text('WHAT THIS MEASURES', style: labelCaps()),
              const SizedBox(height: 12),
              _DomainRow(),
              const SizedBox(height: 32),

              // What PRISM is / is not
              _HonestyBox(),
              const SizedBox(height: 40),

              // CTA
              ElevatedButton(
                onPressed: () => _onStart(context, app),
                child: const Text('START FLUID REASONING SCREENER'),
              ),
              const SizedBox(height: 12),
              Text(
                '~10 min · adaptive · fluid domain · result with 95% CI',
                style: theme.textTheme.bodySmall,
                textAlign: TextAlign.center,
              ),

              const SizedBox(height: 40),
            ],
          ),
        ),
      ),
    );
  }

  void _onStart(BuildContext context, AppProvider app) {
    if (!app.disclaimerAccepted) {
      showDialog<void>(
        context: context,
        barrierDismissible: false,
        builder: (_) => _DisclaimerDialog(onAccept: () {
          app.acceptDisclaimer();
          app.startAssessment();
        }),
      );
    } else {
      app.startAssessment();
    }
  }
}

class _PrismMark extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 28,
      height: 28,
      child: CustomPaint(painter: _PrismPainter()),
    );
  }
}

class _PrismPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final colors = [
      PrismColors.fluid,
      PrismColors.quantitative,
      PrismColors.verbal,
      PrismColors.visualSpatial,
      PrismColors.workingMemory,
      PrismColors.speed,
    ];
    final w = size.width;
    final h = size.height;
    final paint = Paint()..style = PaintingStyle.fill;

    // Draw 6 thin refraction beams fanning right from centre-left
    final cx = w * 0.3;
    final cy = h * 0.5;
    for (int i = 0; i < colors.length; i++) {
      final frac = (i + 0.5) / colors.length;
      final endY = h * frac;
      paint.color = colors[i];
      paint.strokeWidth = 2;
      paint.style = PaintingStyle.stroke;
      canvas.drawLine(Offset(cx, cy), Offset(w, endY), paint);
    }

    // Prism triangle
    paint.style = PaintingStyle.fill;
    paint.color = PrismColors.inkPrimary.withOpacity(0.15);
    final path = Path()
      ..moveTo(cx - 6, 0)
      ..lineTo(cx + 8, cy)
      ..lineTo(cx - 6, h)
      ..close();
    canvas.drawPath(path, paint);
    paint.style = PaintingStyle.stroke;
    paint.color = PrismColors.inkMuted;
    paint.strokeWidth = 1;
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(_) => false;
}

class _DomainRow extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final domains = [
      ('Fluid', PrismColors.fluid),
      ('Quant.', PrismColors.quantitative),
      ('Verbal', PrismColors.verbal),
      ('Spatial', PrismColors.visualSpatial),
      ('Memory', PrismColors.workingMemory),
      ('Speed', PrismColors.speed),
    ];

    return Row(
      children: domains.map((d) {
        final (label, color) = d;
        return Expanded(
          child: Container(
            margin: const EdgeInsets.only(right: 4),
            padding: const EdgeInsets.symmetric(vertical: 6),
            decoration: BoxDecoration(
              color: color.withOpacity(0.08),
              borderRadius: BorderRadius.circular(6),
              border: Border.all(color: color.withOpacity(0.25)),
            ),
            child: Text(
              label,
              style: labelCaps(size: 9, color: color),
              textAlign: TextAlign.center,
            ),
          ),
        );
      }).toList(),
    );
  }
}

class _HonestyBox extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: PrismColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: PrismColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('WHAT PRISM IS', style: labelCaps(color: PrismColors.positive)),
          const SizedBox(height: 8),
          _Bullet('An adaptive estimate of reasoning ability across six domains'),
          _Bullet('Always reported with a 95% confidence interval'),
          _Bullet('Built on real IRT psychometrics and calibrated items'),
          const SizedBox(height: 12),
          Text('WHAT PRISM IS NOT', style: labelCaps(color: PrismColors.negative)),
          const SizedBox(height: 8),
          _Bullet('Not a clinical or diagnostic IQ test'),
          _Bullet('Not a basis for high-stakes decisions'),
          _Bullet('Not population-normed on a representative sample'),
        ],
      ),
    );
  }
}

class _Bullet extends StatelessWidget {
  final String text;
  const _Bullet(this.text);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('· ', style: Theme.of(context).textTheme.bodySmall),
          Expanded(
            child: Text(text, style: Theme.of(context).textTheme.bodySmall),
          ),
        ],
      ),
    );
  }
}

class _LastResultCard extends StatelessWidget {
  final dynamic result;
  const _LastResultCard({required this.result});

  @override
  Widget build(BuildContext context) {
    final score = result.domainScores.isNotEmpty ? result.domainScores.first : null;
    if (score == null) return const SizedBox.shrink();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: PrismColors.accentLight,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: PrismColors.accent.withOpacity(0.3)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('LAST RESULT', style: labelCaps(color: PrismColors.accent)),
                const SizedBox(height: 4),
                Text(
                  '${score.index.round()} (95% CI ${score.ciLo.round()}–${score.ciHi.round()})',
                  style: monoStyle(size: 16, color: PrismColors.inkPrimary, weight: FontWeight.w600),
                ),
                Text(
                  '${score.percentile.round()}th percentile · ${score.itemsAdministered} items',
                  style: monoStyle(size: 11),
                ),
              ],
            ),
          ),
          const Icon(Icons.chevron_right, color: PrismColors.inkMuted, size: 20),
        ],
      ),
    );
  }
}

class _DisclaimerDialog extends StatelessWidget {
  final VoidCallback onAccept;
  const _DisclaimerDialog({required this.onAccept});

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: PrismColors.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: const BorderSide(color: PrismColors.border),
      ),
      title: Text('Before you begin',
          style: Theme.of(context).textTheme.titleLarge),
      content: Text(
        'PRISM is an adaptive estimate of reasoning ability — not a clinical or '
        'diagnostic test. Results include a confidence interval that reflects '
        'measurement uncertainty.\n\n'
        'For formal assessment (gifted placement, clinical diagnosis, educational '
        'decisions), consult a qualified psychologist and a validated instrument '
        'such as WAIS or WISC.\n\n'
        'PRISM informs curiosity. It never adjudicates a life decision.',
        style: Theme.of(context).textTheme.bodyMedium,
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text('Cancel',
              style: TextStyle(color: PrismColors.inkSecondary)),
        ),
        ElevatedButton(
          onPressed: () {
            Navigator.pop(context);
            onAccept();
          },
          style: ElevatedButton.styleFrom(minimumSize: Size.zero, padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10)),
          child: const Text('I understand — begin'),
        ),
      ],
    );
  }
}
