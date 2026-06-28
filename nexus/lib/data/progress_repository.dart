import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/user_progress.dart';

class ProgressRepository {
  static const _key = 'nexus_progress_v1';

  Future<UserProgress> load() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_key);
      if (raw == null) return UserProgress.empty();
      return UserProgress.fromJson(jsonDecode(raw) as Map<String, dynamic>);
    } catch (_) {
      return UserProgress.empty();
    }
  }

  Future<void> save(UserProgress progress) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_key, jsonEncode(progress.toJson()));
  }

  Future<void> reset() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_key);
  }
}
