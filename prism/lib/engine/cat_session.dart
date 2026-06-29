import '../models/prism_item.dart';
import 'irt_engine.dart';

class CatSession {
  final String domain;
  final List<PrismItem> bank;
  final double semTarget;
  final int maxItems;
  final int minItems;

  double theta = 0.0;
  double sem = 1.0; // starts at prior SD
  final List<PrismItem> administered = [];
  final List<bool> responses = [];

  CatSession({
    required this.domain,
    required this.bank,
    this.semTarget = 0.45,
    this.maxItems = 20,
    this.minItems = 5,
  });

  bool get isDone {
    final remaining = bank.length - administered.length;
    return remaining == 0 ||
        administered.length >= maxItems ||
        (sem <= semTarget && administered.length >= minItems);
  }

  PrismItem? get nextItem => IrtEngine.selectItem(
        bank,
        administered.map((i) => i.id).toSet(),
        theta,
      );

  void recordResponse(PrismItem item, bool correct) {
    administered.add(item);
    responses.add(correct);
    final result = IrtEngine.eapEstimate(administered, responses);
    theta = result.theta;
    sem = result.sem;
  }

  double get progress =>
      administered.isEmpty ? 0 : administered.length / maxItems;
}
