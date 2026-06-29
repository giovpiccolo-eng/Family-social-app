enum ItemDomain {
  fluid,
  quantitative,
  verbal,
  visualSpatial,
  workingMemory,
  speed,
}

enum ItemParadigm {
  numberSeries,
  matrixCompletion,
  figuralSeries,
  oddOneOut,
  logicalDeduction,
  analogy,
}

class IrtParams {
  final double a;
  final double b;
  final double c;

  const IrtParams({required this.a, required this.b, required this.c});

  factory IrtParams.fromJson(Map<String, dynamic> json) => IrtParams(
        a: (json['a'] as num).toDouble(),
        b: (json['b'] as num).toDouble(),
        c: (json['c'] as num).toDouble(),
      );
}

class PrismItem {
  final String id;
  final ItemDomain domain;
  final ItemParadigm paradigm;
  final IrtParams irt;
  final dynamic stem;
  final List<dynamic> options;
  final int answer;
  final bool timed;
  final int? softLimitS;
  final String rationale;
  final bool languageLoaded;
  final String calibrationStatus;

  const PrismItem({
    required this.id,
    required this.domain,
    required this.paradigm,
    required this.irt,
    required this.stem,
    required this.options,
    required this.answer,
    this.timed = false,
    this.softLimitS,
    this.rationale = '',
    this.languageLoaded = false,
    this.calibrationStatus = 'seed',
  });

  double get a => irt.a;
  double get b => irt.b;
  double get c => irt.c;

  factory PrismItem.fromJson(Map<String, dynamic> json) => PrismItem(
        id: json['id'] as String,
        domain: _parseDomain(json['domain'] as String),
        paradigm: _parseParadigm(json['paradigm'] as String),
        irt: IrtParams.fromJson(json['irt'] as Map<String, dynamic>),
        stem: json['stem'],
        options: List<dynamic>.from(json['options'] as List),
        answer: json['answer'] as int,
        timed: json['timed'] as bool? ?? false,
        softLimitS: json['soft_limit_s'] as int?,
        rationale: json['rationale'] as String? ?? '',
        languageLoaded: json['language_loaded'] as bool? ?? false,
        calibrationStatus:
            (json['calibration'] as Map<String, dynamic>?)?['status']
                    as String? ??
                'seed',
      );

  static ItemDomain _parseDomain(String s) => switch (s) {
        'fluid' => ItemDomain.fluid,
        'quantitative' => ItemDomain.quantitative,
        'verbal' => ItemDomain.verbal,
        'visual_spatial' => ItemDomain.visualSpatial,
        'working_memory' => ItemDomain.workingMemory,
        'speed' => ItemDomain.speed,
        _ => ItemDomain.fluid,
      };

  static ItemParadigm _parseParadigm(String s) => switch (s) {
        'number_series' => ItemParadigm.numberSeries,
        'matrix_completion' => ItemParadigm.matrixCompletion,
        'figural_series' => ItemParadigm.figuralSeries,
        'odd_one_out' => ItemParadigm.oddOneOut,
        'logical_deduction' => ItemParadigm.logicalDeduction,
        'analogy' => ItemParadigm.analogy,
        _ => ItemParadigm.numberSeries,
      };
}
