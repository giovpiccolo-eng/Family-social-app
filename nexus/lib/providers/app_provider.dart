import 'package:flutter/foundation.dart';
import '../models/user_progress.dart';
import '../data/content_repository.dart';
import '../data/progress_repository.dart';
import '../session/session_controller.dart';

enum AppState { loading, ready, inSession }

class AppProvider extends ChangeNotifier {
  final ContentRepository _contentRepo = ContentRepository();
  final ProgressRepository _progressRepo = ProgressRepository();

  AppState state = AppState.loading;
  UserProgress progress = UserProgress.empty();
  SessionController? session;

  Future<void> init() async {
    progress = await _progressRepo.load();
    await _contentRepo.preloadAll();
    state = AppState.ready;
    notifyListeners();
  }

  Future<void> startSession(String nodeId) async {
    session = SessionController(
      contentRepo: _contentRepo,
      userProgress: progress,
      primaryNodeId: nodeId,
    );
    await session!.buildSession();
    state = AppState.inSession;
    notifyListeners();
  }

  Future<void> endSession() async {
    await _progressRepo.save(progress);
    session = null;
    state = AppState.ready;
    notifyListeners();
  }

  Future<void> saveProgress() async {
    await _progressRepo.save(progress);
  }

  void notifyProgressChanged() {
    notifyListeners();
  }

  ContentRepository get contentRepo => _contentRepo;

  // Returns the "best" node to start today (first unmastered in F1 for MVP)
  String get recommendedNodeId {
    // Priority: first unmastered node in order F1 → L3 → M1 …
    const ordered = ['F1', 'L3', 'M1', 'F2', 'L5', 'M3'];
    for (final id in ordered) {
      final m = progress.nodeMastery[id];
      if (m == null || !m.isMastered) return id;
    }
    return 'F1';
  }
}
