import '../models/content_item.dart';
import '../models/user_progress.dart';
import '../data/content_repository.dart';
import '../data/srs_engine.dart';

enum RoundType { warmUp, learn, apply, lab, crack, close }

class SessionRound {
  final RoundType type;
  final List<ContentItem> items;
  int currentIndex;
  int xpEarned;
  int correctCount;

  SessionRound({
    required this.type,
    required this.items,
    this.currentIndex = 0,
    this.xpEarned = 0,
    this.correctCount = 0,
  });

  bool get isComplete => currentIndex >= items.length;
  ContentItem? get current => isComplete ? null : items[currentIndex];

  String get typeName {
    switch (type) {
      case RoundType.warmUp:
        return 'WARM-UP';
      case RoundType.learn:
        return 'LEARN';
      case RoundType.apply:
        return 'APPLY';
      case RoundType.lab:
        return 'LAB';
      case RoundType.crack:
        return 'CRACK';
      case RoundType.close:
        return 'CLOSE';
    }
  }

  String get typeSubtitle {
    switch (type) {
      case RoundType.warmUp:
        return 'SRS — definitions & equations due today';
      case RoundType.learn:
        return 'Discover the concept';
      case RoundType.apply:
        return 'AO2 — data, graphs & calculations';
      case RoundType.lab:
        return 'AO3 — practical skills';
      case RoundType.crack:
        return 'Exam-style question';
      case RoundType.close:
        return 'Session complete';
    }
  }
}

class AnswerResult {
  final bool correct;
  final int marksEarned;
  final int marksAvailable;
  final int xpEarned;
  final List<String> missedKeywords;
  final String? explanation;

  const AnswerResult({
    required this.correct,
    required this.marksEarned,
    required this.marksAvailable,
    required this.xpEarned,
    this.missedKeywords = const [],
    this.explanation,
  });

  double get accuracy => marksAvailable > 0 ? marksEarned / marksAvailable : (correct ? 1.0 : 0.0);
}

class SessionController {
  final ContentRepository contentRepo;
  final UserProgress userProgress;
  final String primaryNodeId;

  List<SessionRound> rounds = [];
  int currentRoundIndex = 0;
  int totalXpEarned = 0;
  int totalCreditsEarned = 0;
  bool isComplete = false;

  SessionController({
    required this.contentRepo,
    required this.userProgress,
    required this.primaryNodeId,
  });

  Future<void> buildSession() async {
    await contentRepo.preloadAll();
    final allItems = await contentRepo.itemsForNode(primaryNodeId);

    // Round 1: WARM-UP — SRS recall items due today
    final srsItems = contentRepo.srsDueItems(userProgress.srsData);
    final warmUpItems = srsItems.isNotEmpty
        ? srsItems.take(5).toList()
        : allItems.where((i) => i.mode == QuestionMode.recall).take(5).toList();

    // Round 2: LEARN — learn mode items
    final learnItems = allItems.where((i) => i.mode == QuestionMode.learn).toList();

    // Round 3: APPLY — apply, calculate, command items
    final applyItems = allItems
        .where((i) => i.mode == QuestionMode.apply || i.mode == QuestionMode.calculate || i.mode == QuestionMode.command)
        .take(3)
        .toList();

    // Round 4: LAB — lab items (or more apply if no lab)
    var labItems = allItems.where((i) => i.mode == QuestionMode.lab).take(2).toList();
    if (labItems.isEmpty) {
      labItems = allItems.where((i) => i.mode == QuestionMode.apply).skip(2).take(2).toList();
    }

    // Round 5: CRACK — one exam boss question
    final crackItems = allItems.where((i) => i.mode == QuestionMode.crack).take(1).toList();

    rounds = [
      if (warmUpItems.isNotEmpty) SessionRound(type: RoundType.warmUp, items: warmUpItems),
      if (learnItems.isNotEmpty) SessionRound(type: RoundType.learn, items: learnItems),
      if (applyItems.isNotEmpty) SessionRound(type: RoundType.apply, items: applyItems),
      if (labItems.isNotEmpty) SessionRound(type: RoundType.lab, items: labItems),
      if (crackItems.isNotEmpty) SessionRound(type: RoundType.crack, items: crackItems),
    ];

    // Fallback: if we only have items for some rounds, still create at least one round
    if (rounds.isEmpty) {
      rounds = [SessionRound(type: RoundType.apply, items: allItems.take(5).toList())];
    }
  }

  SessionRound? get currentRound =>
      currentRoundIndex < rounds.length ? rounds[currentRoundIndex] : null;

  ContentItem? get currentItem => currentRound?.current;

  bool get hasMoreRounds => currentRoundIndex < rounds.length;

  int get totalRounds => rounds.length;
  int get completedRounds => currentRoundIndex;

  double get sessionProgress {
    if (rounds.isEmpty) return 0;
    double progress = currentRoundIndex / rounds.length;
    final round = currentRound;
    if (round != null && round.items.isNotEmpty) {
      progress += (round.currentIndex / round.items.length) / rounds.length;
    }
    return progress.clamp(0.0, 1.0);
  }

  // Mark keyword answer (recall/crack)
  AnswerResult markKeywordAnswer(ContentItem item, String userAnswer) {
    final answer = userAnswer.toLowerCase();
    final keywords = item.keywords.map((k) => k.toLowerCase()).toList();

    // Check which keywords are present in the answer
    final hitKeywords = keywords.where((kw) => answer.contains(kw)).toList();
    final missedKeywords = item.keywords
        .where((kw) => !answer.contains(kw.toLowerCase()))
        .toList();

    final marksEarned = (hitKeywords.length / keywords.length * item.marks).round();
    final correct = marksEarned >= (item.marks * 0.7).round();

    final xp = (item.xpReward * marksEarned / item.marks.clamp(1, 100)).round();

    // Update SRS
    _updateSRS(item, correct ? 2 : 0);

    return AnswerResult(
      correct: correct,
      marksEarned: marksEarned,
      marksAvailable: item.marks,
      xpEarned: xp,
      missedKeywords: missedKeywords,
    );
  }

  // Mark a calculation answer
  AnswerResult markCalculationAnswer(ContentItem item, String userValue, String userUnit) {
    final expected = item.answer?['value'];
    final expectedUnit = item.answer?['unit'] as String?;

    if (expected == null) {
      return const AnswerResult(correct: false, marksEarned: 0, marksAvailable: 3, xpEarned: 0);
    }

    final parsedUser = double.tryParse(userValue.trim());
    final expectedDouble = (expected as num).toDouble();
    final valueCorrect = parsedUser != null && (parsedUser - expectedDouble).abs() < expectedDouble * 0.02;
    final unitCorrect = expectedUnit != null &&
        userUnit.trim().toLowerCase().replaceAll(' ', '') ==
            expectedUnit.toLowerCase().replaceAll(' ', '');

    int marks = 0;
    final missed = <String>[];

    // Mark scheme: equation (we credit if attempt was made), substitution, answer+unit
    // For simplicity: 1 for attempt, 1 for value, 1 for unit
    marks++; // method mark (they attempted a calculation)
    if (valueCorrect) marks++;
    else missed.add('correct value ${item.answer!['value']} ${item.answer!['unit']}');
    if (unitCorrect) marks++;
    else missed.add('correct unit: ${expectedUnit ?? ""}');

    final totalMarks = item.marks.clamp(1, 10);
    final xp = (item.xpReward * marks / totalMarks).round();

    return AnswerResult(
      correct: valueCorrect && unitCorrect,
      marksEarned: marks.clamp(0, totalMarks),
      marksAvailable: totalMarks,
      xpEarned: xp,
      missedKeywords: missed,
    );
  }

  // Mark MCQ / apply answer
  AnswerResult markApplyAnswer(ContentItem item, List<String> selectedKeywords) {
    final markScheme = item.markScheme;
    int marks = 0;
    final missed = <String>[];

    for (final point in markScheme) {
      final hit = selectedKeywords.any(
        (kw) => point.toLowerCase().contains(kw.toLowerCase()),
      );
      if (hit) marks++;
      else missed.add(point);
    }

    final correct = marks >= (markScheme.length * 0.6).round();
    final xp = markScheme.isNotEmpty
        ? (item.xpReward * marks / markScheme.length).round()
        : (correct ? item.xpReward : 0);

    return AnswerResult(
      correct: correct,
      marksEarned: marks,
      marksAvailable: item.marks,
      xpEarned: xp,
      missedKeywords: missed,
    );
  }

  void applyResult(AnswerResult result, ContentItem item) {
    totalXpEarned += result.xpEarned;
    totalCreditsEarned += result.correct ? item.creditsReward : 0;
    userProgress.addXp(result.xpEarned);
    if (result.correct) userProgress.addCredits(item.creditsReward);

    // Update node mastery
    final mastery = userProgress.masteryFor(item.node);
    mastery.recordAnswer(
      ao: item.ao,
      correct: result.correct,
      marks: result.marksEarned,
      maxMarks: item.marks,
    );

    currentRound?.xpEarned += result.xpEarned;
    if (result.correct) currentRound?.correctCount++;
  }

  void advanceItem() {
    currentRound?.currentIndex++;
    if (currentRound?.isComplete == true) {
      currentRoundIndex++;
      if (currentRoundIndex >= rounds.length) {
        isComplete = true;
        _finalise();
      }
    }
  }

  void _finalise() {
    userProgress.updateStreak();
  }

  void _updateSRS(ContentItem item, int quality) {
    final existing = userProgress.srsData[item.id];
    final current = existing != null
        ? SrsState.fromJson(existing as Map<String, dynamic>)
        : null;
    final updated = SrsEngine.update(current, quality);
    userProgress.srsData[item.id] = updated.toJson();
  }
}
