// SM-2 spaced repetition algorithm
// quality: 0 = wrong, 1 = correct but hard, 2 = correct easy

class SrsState {
  final int interval; // days until next review
  final int repetitions;
  final double easiness;
  final DateTime nextDue;

  const SrsState({
    required this.interval,
    required this.repetitions,
    required this.easiness,
    required this.nextDue,
  });

  Map<String, dynamic> toJson() => {
        'interval': interval,
        'repetitions': repetitions,
        'easiness': easiness,
        'nextDue': nextDue.toIso8601String(),
      };

  factory SrsState.fromJson(Map<String, dynamic> j) => SrsState(
        interval: (j['interval'] as num?)?.toInt() ?? 1,
        repetitions: (j['repetitions'] as num?)?.toInt() ?? 0,
        easiness: (j['easiness'] as num?)?.toDouble() ?? 2.5,
        nextDue: j['nextDue'] != null
            ? DateTime.tryParse(j['nextDue'] as String) ?? DateTime.now()
            : DateTime.now(),
      );

  factory SrsState.initial() => SrsState(
        interval: 1,
        repetitions: 0,
        easiness: 2.5,
        nextDue: DateTime.now(),
      );
}

class SrsEngine {
  // Returns the updated state after an answer
  static SrsState update(SrsState? current, int quality) {
    final state = current ?? SrsState.initial();

    // quality: 0 wrong, 1 correct/hard, 2 correct/easy
    int newInterval;
    int newRepetitions;
    double newEasiness;

    if (quality == 0) {
      // Wrong: restart
      newInterval = 1;
      newRepetitions = 0;
      newEasiness = (state.easiness - 0.3).clamp(1.3, 5.0);
    } else {
      newRepetitions = state.repetitions + 1;
      newEasiness = (state.easiness + 0.1 - (2 - quality) * (0.08 + (2 - quality) * 0.02))
          .clamp(1.3, 5.0);

      if (state.repetitions == 0) {
        newInterval = 1;
      } else if (state.repetitions == 1) {
        newInterval = 3;
      } else {
        newInterval = (state.interval * newEasiness).round();
      }
    }

    final nextDue = DateTime.now().add(Duration(days: newInterval));

    return SrsState(
      interval: newInterval,
      repetitions: newRepetitions,
      easiness: newEasiness,
      nextDue: DateTime(nextDue.year, nextDue.month, nextDue.day),
    );
  }

  static bool isDue(SrsState? state) {
    if (state == null) return true;
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    return !state.nextDue.isAfter(today);
  }
}
