import 'dart:convert';

class NodeMastery {
  final String nodeId;
  int ao1Score; // 0–100
  int ao2Score;
  int ao3Score;
  int totalAnswered;
  int totalCorrect;
  bool bossCleared;

  NodeMastery({
    required this.nodeId,
    this.ao1Score = 0,
    this.ao2Score = 0,
    this.ao3Score = 0,
    this.totalAnswered = 0,
    this.totalCorrect = 0,
    this.bossCleared = false,
  });

  // Weighted average mastery %
  int get masteryPercent {
    const w1 = 0.3, w2 = 0.5, w3 = 0.2;
    return (ao1Score * w1 + ao2Score * w2 + ao3Score * w3).round();
  }

  bool get isMastered => masteryPercent >= 80;
  bool get isElite => masteryPercent >= 95;

  void recordAnswer({required String ao, required bool correct, required int marks, required int maxMarks}) {
    totalAnswered++;
    if (correct) totalCorrect++;

    final pct = maxMarks > 0 ? (marks / maxMarks * 100).round() : (correct ? 100 : 0);
    final weight = 0.15; // how much each answer shifts the score

    switch (ao) {
      case 'AO1':
        ao1Score = ((ao1Score * (1 - weight)) + (pct * weight)).round().clamp(0, 100);
      case 'AO2':
        ao2Score = ((ao2Score * (1 - weight)) + (pct * weight)).round().clamp(0, 100);
      case 'AO3':
        ao3Score = ((ao3Score * (1 - weight)) + (pct * weight)).round().clamp(0, 100);
    }
  }

  Map<String, dynamic> toJson() => {
        'nodeId': nodeId,
        'ao1': ao1Score,
        'ao2': ao2Score,
        'ao3': ao3Score,
        'answered': totalAnswered,
        'correct': totalCorrect,
        'bossCleared': bossCleared,
      };

  factory NodeMastery.fromJson(Map<String, dynamic> j) => NodeMastery(
        nodeId: j['nodeId'] as String,
        ao1Score: (j['ao1'] as num?)?.toInt() ?? 0,
        ao2Score: (j['ao2'] as num?)?.toInt() ?? 0,
        ao3Score: (j['ao3'] as num?)?.toInt() ?? 0,
        totalAnswered: (j['answered'] as num?)?.toInt() ?? 0,
        totalCorrect: (j['correct'] as num?)?.toInt() ?? 0,
        bossCleared: j['bossCleared'] as bool? ?? false,
      );
}

class UserProgress {
  int totalXp;
  int credits;
  int streakDays;
  DateTime? lastSessionDate;
  int streakFreezes;
  Map<String, NodeMastery> nodeMastery; // nodeId → mastery
  Set<String> unlockedNexuses; // nexus IDs
  Set<String> badges;
  Map<String, dynamic> srsData; // itemId → srs state

  UserProgress({
    this.totalXp = 0,
    this.credits = 0,
    this.streakDays = 0,
    this.lastSessionDate,
    this.streakFreezes = 2,
    Map<String, NodeMastery>? nodeMastery,
    Set<String>? unlockedNexuses,
    Set<String>? badges,
    Map<String, dynamic>? srsData,
  })  : nodeMastery = nodeMastery ?? {},
        unlockedNexuses = unlockedNexuses ?? {},
        badges = badges ?? {},
        srsData = srsData ?? {};

  NodeMastery masteryFor(String nodeId) {
    return nodeMastery.putIfAbsent(nodeId, () => NodeMastery(nodeId: nodeId));
  }

  void addXp(int amount) => totalXp += amount;
  void addCredits(int amount) => credits += amount;

  void updateStreak() {
    final today = DateTime.now();
    final todayDate = DateTime(today.year, today.month, today.day);
    if (lastSessionDate == null) {
      streakDays = 1;
    } else {
      final lastDate = DateTime(
        lastSessionDate!.year,
        lastSessionDate!.month,
        lastSessionDate!.day,
      );
      final diff = todayDate.difference(lastDate).inDays;
      if (diff == 1) {
        streakDays++;
      } else if (diff > 1) {
        if (streakFreezes > 0 && diff == 2) {
          streakFreezes--;
          streakDays++;
        } else {
          streakDays = 1;
        }
      }
      // diff == 0: same day, don't change streak
    }
    lastSessionDate = today;
  }

  bool get sessionDoneToday {
    if (lastSessionDate == null) return false;
    final now = DateTime.now();
    final l = lastSessionDate!;
    return l.year == now.year && l.month == now.month && l.day == now.day;
  }

  Map<String, dynamic> toJson() => {
        'totalXp': totalXp,
        'credits': credits,
        'streakDays': streakDays,
        'lastSessionDate': lastSessionDate?.toIso8601String(),
        'streakFreezes': streakFreezes,
        'nodeMastery': {for (final e in nodeMastery.entries) e.key: e.value.toJson()},
        'unlockedNexuses': unlockedNexuses.toList(),
        'badges': badges.toList(),
        'srsData': srsData,
      };

  factory UserProgress.fromJson(Map<String, dynamic> j) {
    final nm = (j['nodeMastery'] as Map<String, dynamic>? ?? {}).map(
      (k, v) => MapEntry(k, NodeMastery.fromJson(v as Map<String, dynamic>)),
    );
    return UserProgress(
      totalXp: (j['totalXp'] as num?)?.toInt() ?? 0,
      credits: (j['credits'] as num?)?.toInt() ?? 0,
      streakDays: (j['streakDays'] as num?)?.toInt() ?? 0,
      lastSessionDate: j['lastSessionDate'] != null
          ? DateTime.tryParse(j['lastSessionDate'] as String)
          : null,
      streakFreezes: (j['streakFreezes'] as num?)?.toInt() ?? 2,
      nodeMastery: nm,
      unlockedNexuses: Set<String>.from((j['unlockedNexuses'] as List? ?? [])),
      badges: Set<String>.from((j['badges'] as List? ?? [])),
      srsData: (j['srsData'] as Map<String, dynamic>?) ?? {},
    );
  }

  static UserProgress empty() => UserProgress();
}
