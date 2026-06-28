import 'dart:convert';
import 'package:flutter/services.dart';
import '../models/content_item.dart';

class ContentRepository {
  final Map<String, List<ContentItem>> _cache = {};

  // All known content files
  static const List<String> _contentFiles = [
    'assets/content/f1_motion.json',
    'assets/content/l3_movement.json',
    'assets/content/m1_particles.json',
  ];

  Future<void> preloadAll() async {
    for (final file in _contentFiles) {
      await _loadFile(file);
    }
  }

  Future<List<ContentItem>> itemsForNode(String nodeId) async {
    if (_cache.containsKey(nodeId)) return _cache[nodeId]!;

    // Try to find the right file
    for (final file in _contentFiles) {
      final items = await _loadFile(file);
      if (_cache.containsKey(nodeId)) return _cache[nodeId]!;
      // check if any item matches
      if (items.any((i) => i.node == nodeId)) {
        _cache[nodeId] = items.where((i) => i.node == nodeId).toList();
        return _cache[nodeId]!;
      }
    }
    return [];
  }

  Future<List<ContentItem>> _loadFile(String assetPath) async {
    try {
      final raw = await rootBundle.loadString(assetPath);
      final decoded = jsonDecode(raw) as Map<String, dynamic>;
      final nodeId = decoded['node'] as String;
      final itemsJson = decoded['items'] as List;
      final items = itemsJson
          .map((j) => ContentItem.fromJson(j as Map<String, dynamic>))
          .toList();
      _cache[nodeId] = items;
      return items;
    } catch (e) {
      return [];
    }
  }

  Future<List<ContentItem>> itemsByMode(String nodeId, QuestionMode mode) async {
    final all = await itemsForNode(nodeId);
    return all.where((i) => i.mode == mode).toList();
  }

  Future<ContentItem?> itemById(String id) async {
    for (final file in _contentFiles) {
      await _loadFile(file);
    }
    for (final items in _cache.values) {
      try {
        return items.firstWhere((i) => i.id == id);
      } catch (_) {}
    }
    return null;
  }

  // Items due for SRS review today across all loaded nodes
  List<ContentItem> srsDueItems(Map<String, dynamic> srsData) {
    final all = _cache.values.expand((l) => l).toList();
    final due = <ContentItem>[];
    for (final item in all) {
      if (item.mode != QuestionMode.recall) continue;
      final state = srsData[item.id];
      if (state == null) {
        due.add(item);
        continue;
      }
      final nextDue = state['nextDue'] as String?;
      if (nextDue == null) {
        due.add(item);
        continue;
      }
      final dueDate = DateTime.tryParse(nextDue);
      if (dueDate == null) {
        due.add(item);
        continue;
      }
      final now = DateTime.now();
      if (!dueDate.isAfter(DateTime(now.year, now.month, now.day))) {
        due.add(item);
      }
    }
    return due;
  }
}
