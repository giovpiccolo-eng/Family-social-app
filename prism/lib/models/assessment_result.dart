import 'dart:math' as math;

class DomainScore {
  final String domain;
  final double theta;
  final double sem;
  final double index;
  final double ciLo;
  final double ciHi;
  final double percentile;
  final int itemsAdministered;

  const DomainScore({
    required this.domain,
    required this.theta,
    required this.sem,
    required this.index,
    required this.ciLo,
    required this.ciHi,
    required this.percentile,
    required this.itemsAdministered,
  });

  Map<String, dynamic> toJson() => {
        'domain': domain,
        'theta': theta,
        'sem': sem,
        'index': index,
        'ciLo': ciLo,
        'ciHi': ciHi,
        'percentile': percentile,
        'itemsAdministered': itemsAdministered,
      };

  factory DomainScore.fromJson(Map<String, dynamic> json) => DomainScore(
        domain: json['domain'] as String,
        theta: (json['theta'] as num).toDouble(),
        sem: (json['sem'] as num).toDouble(),
        index: (json['index'] as num).toDouble(),
        ciLo: (json['ciLo'] as num).toDouble(),
        ciHi: (json['ciHi'] as num).toDouble(),
        percentile: (json['percentile'] as num).toDouble(),
        itemsAdministered: json['itemsAdministered'] as int,
      );
}

class AssessmentResult {
  final DateTime date;
  final List<DomainScore> domainScores;
  final String assessmentType;

  const AssessmentResult({
    required this.date,
    required this.domainScores,
    required this.assessmentType,
  });

  DomainScore? scoreFor(String domain) =>
      domainScores.where((d) => d.domain == domain).firstOrNull;

  // Overall: pooled from available domains (harmonic mean of precisions)
  double get overallTheta {
    if (domainScores.isEmpty) return 0;
    return domainScores.map((d) => d.theta).reduce((a, b) => a + b) /
        domainScores.length;
  }

  double get overallSem {
    if (domainScores.isEmpty) return 1.0;
    final sumInvVar = domainScores
        .map((d) => 1 / (d.sem * d.sem))
        .reduce((a, b) => a + b);
    return 1 / math.sqrt(sumInvVar);
  }

  double get overallIndex => 100 + 15 * overallTheta;
  double get overallCiLo => overallIndex - 1.96 * 15 * overallSem;
  double get overallCiHi => overallIndex + 1.96 * 15 * overallSem;
  double get overallPercentile {
    // reuse fluid percentile from DomainScore
    return domainScores.isEmpty ? 50 : domainScores.first.percentile;
  }

  Map<String, dynamic> toJson() => {
        'date': date.toIso8601String(),
        'assessmentType': assessmentType,
        'domainScores': domainScores.map((d) => d.toJson()).toList(),
      };

  factory AssessmentResult.fromJson(Map<String, dynamic> json) =>
      AssessmentResult(
        date: DateTime.parse(json['date'] as String),
        assessmentType: json['assessmentType'] as String? ?? 'quick',
        domainScores: (json['domainScores'] as List)
            .map((d) => DomainScore.fromJson(d as Map<String, dynamic>))
            .toList(),
      );
}
