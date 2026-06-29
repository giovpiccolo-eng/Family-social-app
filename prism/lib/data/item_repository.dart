import 'dart:convert';
import 'package:flutter/services.dart';
import '../models/prism_item.dart';

class ItemRepository {
  static List<PrismItem>? _fluid;

  static Future<List<PrismItem>> loadDomain(String domain) async {
    if (domain == 'fluid') {
      _fluid ??= await _load('assets/items/fluid_items.json');
      return _fluid!;
    }
    return [];
  }

  static Future<List<PrismItem>> _load(String path) async {
    final raw = await rootBundle.loadString(path);
    final list = jsonDecode(raw) as List;
    return list.map((j) => PrismItem.fromJson(j as Map<String, dynamic>)).toList();
  }
}
