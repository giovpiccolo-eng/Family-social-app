// Content item schema — every question/card in the app.
// Faithfully mirrors the JSON schema from the NEXUS brief.

enum QuestionMode { recall, learn, apply, calculate, lab, command, crack, frontier }

class ContentItem {
  final String id;
  final QuestionMode mode;
  final String realm;
  final String node;
  final String ao; // AO1 | AO2 | AO3
  final String command; // state | describe | explain | calculate …
  final int marks;
  final int difficulty; // 1–5
  final String stem;

  // Mode-specific fields
  final String? front; // recall: term shown
  final String? back; // recall: definition (exact)
  final List<String> keywords; // recall/crack: mark-scheme keywords
  final Map<String, String>? given; // calculate: given quantities
  final String? equation;
  final Map<String, dynamic>? answer; // {value, unit} for calculate
  final List<String> markScheme;
  final Dataset? dataset; // apply/lab: graph/table data
  final String? learnContent; // learn mode: explanation text
  final List<String>? learnSteps; // learn mode: discovery steps

  final String? nexusLink; // e.g. "N1_particles"
  final String? frontier; // deep-dive text
  final int xpReward;
  final int creditsReward;

  // SRS tracking (mutated during play, not from JSON)
  int srsInterval; // days until next review
  DateTime? srsNextDue;
  int srsRepetitions;
  double srsEasiness;

  ContentItem({
    required this.id,
    required this.mode,
    required this.realm,
    required this.node,
    required this.ao,
    required this.command,
    required this.marks,
    required this.difficulty,
    required this.stem,
    this.front,
    this.back,
    this.keywords = const [],
    this.given,
    this.equation,
    this.answer,
    this.markScheme = const [],
    this.dataset,
    this.learnContent,
    this.learnSteps,
    this.nexusLink,
    this.frontier,
    this.xpReward = 20,
    this.creditsReward = 4,
    this.srsInterval = 1,
    this.srsNextDue,
    this.srsRepetitions = 0,
    this.srsEasiness = 2.5,
  });

  factory ContentItem.fromJson(Map<String, dynamic> j) {
    final modeStr = j['mode'] as String? ?? 'recall';
    final mode = QuestionMode.values.firstWhere(
      (m) => m.name == modeStr,
      orElse: () => QuestionMode.recall,
    );

    Dataset? dataset;
    if (j['dataset'] != null) {
      dataset = Dataset.fromJson(j['dataset'] as Map<String, dynamic>);
    }

    final reward = j['reward'] as Map<String, dynamic>? ?? {};
    final msRaw = j['mark_scheme'];
    final List<String> ms = msRaw is List ? msRaw.cast<String>() : [];
    final kwRaw = j['keywords'];
    final List<String> kw = kwRaw is List ? kwRaw.cast<String>() : ms;

    return ContentItem(
      id: j['id'] as String,
      mode: mode,
      realm: j['realm'] as String? ?? 'forces',
      node: j['node'] as String? ?? '',
      ao: j['ao'] as String? ?? 'AO1',
      command: j['command'] as String? ?? 'state',
      marks: (j['marks'] as num?)?.toInt() ?? 1,
      difficulty: (j['difficulty'] as num?)?.toInt() ?? 2,
      stem: j['stem'] as String? ?? '',
      front: j['front'] as String?,
      back: j['back'] as String?,
      keywords: kw,
      given: j['given'] != null ? Map<String, String>.from(j['given'] as Map) : null,
      equation: j['equation'] as String?,
      answer: j['answer'] as Map<String, dynamic>?,
      markScheme: ms,
      dataset: dataset,
      learnContent: j['learn_content'] as String?,
      learnSteps: (j['learn_steps'] as List?)?.cast<String>(),
      nexusLink: j['nexus'] as String?,
      frontier: j['frontier'] as String?,
      xpReward: (reward['xp'] as num?)?.toInt() ?? 20,
      creditsReward: (reward['credits'] as num?)?.toInt() ?? 4,
    );
  }

  // Whether this SRS card is due today
  bool get srsDueToday {
    if (srsNextDue == null) return true;
    final now = DateTime.now();
    return !srsNextDue!.isAfter(DateTime(now.year, now.month, now.day));
  }
}

// Dataset: attached to apply/lab items for graph or table rendering
class Dataset {
  final String type; // 'line_chart' | 'table' | 'bar_chart'
  final String xLabel;
  final String yLabel;
  final List<DataPoint> points;
  final List<List<dynamic>>? tableRows;
  final List<String>? tableHeaders;
  final String? note;

  Dataset({
    required this.type,
    this.xLabel = '',
    this.yLabel = '',
    this.points = const [],
    this.tableRows,
    this.tableHeaders,
    this.note,
  });

  factory Dataset.fromJson(Map<String, dynamic> j) {
    final pts = (j['points'] as List? ?? [])
        .map((p) => DataPoint(
              x: (p['x'] as num).toDouble(),
              y: (p['y'] as num).toDouble(),
              label: p['label'] as String?,
            ))
        .toList();

    return Dataset(
      type: j['type'] as String? ?? 'line_chart',
      xLabel: j['x_label'] as String? ?? '',
      yLabel: j['y_label'] as String? ?? '',
      points: pts,
      tableHeaders: (j['headers'] as List?)?.cast<String>(),
      tableRows: (j['rows'] as List?)?.map((r) => r as List<dynamic>).toList(),
      note: j['note'] as String?,
    );
  }
}

class DataPoint {
  final double x;
  final double y;
  final String? label;
  DataPoint({required this.x, required this.y, this.label});
}
