import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../data/item_repository.dart';
import '../engine/cat_session.dart';
import '../engine/irt_engine.dart';
import '../models/assessment_result.dart';
import '../models/prism_item.dart';

enum AppState { loading, home, assessing, results }

class AppProvider extends ChangeNotifier {
  AppState _state = AppState.loading;
  AppState get state => _state;

  CatSession? _session;
  CatSession? get session => _session;

  AssessmentResult? _lastResult;
  AssessmentResult? get lastResult => _lastResult;

  List<AssessmentResult> history = [];
  bool disclaimerAccepted = false;

  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    disclaimerAccepted = prefs.getBool('disclaimer_accepted') ?? false;

    final raw = prefs.getString('assessment_history');
    if (raw != null) {
      final list = jsonDecode(raw) as List;
      history = list
          .map((j) => AssessmentResult.fromJson(j as Map<String, dynamic>))
          .toList();
      if (history.isNotEmpty) _lastResult = history.last;
    }

    _state = AppState.home;
    notifyListeners();
  }

  Future<void> acceptDisclaimer() async {
    disclaimerAccepted = true;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('disclaimer_accepted', true);
    notifyListeners();
  }

  Future<void> startAssessment() async {
    final bank = await ItemRepository.loadDomain('fluid');
    _session = CatSession(
      domain: 'fluid',
      bank: bank,
      semTarget: 0.45,
      maxItems: 20,
      minItems: 5,
    );
    _state = AppState.assessing;
    notifyListeners();
  }

  void recordResponse(PrismItem item, bool correct) {
    _session?.recordResponse(item, correct);
    notifyListeners();
  }

  Future<void> finishAssessment() async {
    final s = _session;
    if (s == null) return;

    final (ciLo, ciHi) = IrtEngine.indexCI(s.theta, s.sem);
    final score = DomainScore(
      domain: 'fluid',
      theta: s.theta,
      sem: s.sem,
      index: IrtEngine.thetaToIndex(s.theta),
      ciLo: ciLo,
      ciHi: ciHi,
      percentile: IrtEngine.percentile(s.theta),
      itemsAdministered: s.administered.length,
    );

    _lastResult = AssessmentResult(
      date: DateTime.now(),
      domainScores: [score],
      assessmentType: 'screener',
    );
    history.add(_lastResult!);

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(
      'assessment_history',
      jsonEncode(history.map((r) => r.toJson()).toList()),
    );

    _session = null;
    _state = AppState.results;
    notifyListeners();
  }

  void goHome() {
    _state = AppState.home;
    notifyListeners();
  }
}
