import 'dart:math' as math;
import '../models/prism_item.dart';

class IrtEngine {
  static const double _D = 1.7;

  // 3PL probability P(correct | θ)
  static double probability(double theta, double a, double b, double c) {
    final z = _D * a * (theta - b);
    return c + (1 - c) / (1 + math.exp(-z));
  }

  // Fisher information for one item at theta
  static double fisherInfo(double theta, double a, double b, double c) {
    final p = probability(theta, a, b, c);
    final q = 1 - p;
    final pStar = p - c;
    if (pStar <= 1e-10 || p <= 1e-10 || q <= 1e-10) return 0;
    return _D * _D * a * a * pStar * pStar * q / ((1 - c) * (1 - c) * p);
  }

  // EAP theta estimate + SEM via Gauss–Hermite quadrature (41-point grid)
  static ({double theta, double sem}) eapEstimate(
    List<PrismItem> items,
    List<bool> responses,
  ) {
    const int n = 41;
    const double lo = -4.0;
    const double hi = 4.0;
    const double step = (hi - lo) / (n - 1);

    final thetas = List.generate(n, (i) => lo + i * step);

    // Log-posterior: prior N(0,1) + log-likelihood
    final logPost = <double>[];
    for (final t in thetas) {
      double lp = _normalLogPdf(t);
      for (int i = 0; i < items.length; i++) {
        final p = probability(t, items[i].a, items[i].b, items[i].c);
        lp += responses[i]
            ? math.log(p.clamp(1e-12, 1.0))
            : math.log((1 - p).clamp(1e-12, 1.0));
      }
      logPost.add(lp);
    }

    // Log-sum-exp normalisation
    final maxLp = logPost.reduce(math.max);
    final weights = logPost.map((l) => math.exp(l - maxLp)).toList();
    final totalW = weights.reduce((a, b) => a + b);

    double eap = 0;
    for (int i = 0; i < n; i++) {
      eap += thetas[i] * weights[i] / totalW;
    }

    double variance = 0;
    for (int i = 0; i < n; i++) {
      final d = thetas[i] - eap;
      variance += d * d * weights[i] / totalW;
    }

    return (theta: eap, sem: math.sqrt(variance));
  }

  // Select item with maximum Fisher info at current theta (excluding administered)
  static PrismItem? selectItem(
    List<PrismItem> bank,
    Set<String> administered,
    double theta,
  ) {
    PrismItem? best;
    double bestInfo = -1;
    for (final item in bank) {
      if (administered.contains(item.id)) continue;
      final info = fisherInfo(theta, item.a, item.b, item.c);
      if (info > bestInfo) {
        bestInfo = info;
        best = item;
      }
    }
    return best;
  }

  // θ → PRISM index (M=100, SD=15)
  static double thetaToIndex(double theta) => 100 + 15 * theta;

  // 95% CI on index scale
  static (double lo, double hi) indexCI(double theta, double sem) =>
      (thetaToIndex(theta - 1.96 * sem), thetaToIndex(theta + 1.96 * sem));

  // Percentile via normal CDF approximation
  static double percentile(double theta) => _normalCdf(theta) * 100;

  static double _normalCdf(double x) {
    final t = 1.0 / (1.0 + 0.2316419 * x.abs());
    final poly = t *
        (0.319381530 +
            t *
                (-0.356563782 +
                    t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))));
    final pdfX =
        math.exp(-0.5 * x * x) / math.sqrt(2 * math.pi);
    final p = 1.0 - pdfX * poly;
    return x >= 0 ? p : 1.0 - p;
  }

  static double _normalLogPdf(double x) =>
      -0.5 * x * x - 0.5 * math.log(2 * math.pi);
}
