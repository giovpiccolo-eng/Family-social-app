// Campaign node definitions — the "levels" of the skill tree

class NodeDef {
  final String id; // e.g. "F1", "L3", "M1"
  final String realm; // life | matter | forces
  final String title;
  final String examFocus;
  final String primaryAO;
  final List<String> nexusLinks; // e.g. ["N1_particles"]
  final List<String> prerequisites; // node IDs that must be mastered first
  final int orderIndex;

  const NodeDef({
    required this.id,
    required this.realm,
    required this.title,
    required this.examFocus,
    required this.primaryAO,
    this.nexusLinks = const [],
    this.prerequisites = const [],
    required this.orderIndex,
  });
}

const List<NodeDef> kAllNodes = [
  // ── Realm I · LIFE ──────────────────────────────────────────────────────────
  NodeDef(
    id: 'L1', realm: 'life', orderIndex: 0,
    title: 'Characteristics & classification of living organisms',
    examFocus: 'define, classify', primaryAO: 'AO1',
  ),
  NodeDef(
    id: 'L2', realm: 'life', orderIndex: 1,
    title: 'Cells & organisation',
    examFocus: 'magnification calculations', primaryAO: 'AO2',
    prerequisites: ['L1'],
  ),
  NodeDef(
    id: 'L3', realm: 'life', orderIndex: 2,
    title: 'Movement in & out of cells',
    examFocus: 'water potential, osmosis', primaryAO: 'AO2',
    prerequisites: ['L2'],
    nexusLinks: ['N1_particles', 'N3_transport'],
  ),
  NodeDef(
    id: 'L4', realm: 'life', orderIndex: 3,
    title: 'Biological molecules & food tests',
    examFocus: 'practical — Benedict\'s, biuret, iodine', primaryAO: 'AO3',
    prerequisites: ['L2'],
  ),
  NodeDef(
    id: 'L5', realm: 'life', orderIndex: 4,
    title: 'Enzymes',
    examFocus: 'graphs — temperature & pH', primaryAO: 'AO2',
    prerequisites: ['L4'],
  ),
  NodeDef(
    id: 'L6', realm: 'life', orderIndex: 5,
    title: 'Plant nutrition — photosynthesis',
    examFocus: 'explain limiting factors', primaryAO: 'AO2',
    prerequisites: ['L3', 'L5'],
    nexusLinks: ['N2_energy'],
  ),
  NodeDef(
    id: 'L7', realm: 'life', orderIndex: 6,
    title: 'Human nutrition — digestion & absorption',
    examFocus: 'describe', primaryAO: 'AO1',
    prerequisites: ['L4'],
  ),
  NodeDef(
    id: 'L8', realm: 'life', orderIndex: 7,
    title: 'Transport in plants',
    examFocus: 'data — transpiration', primaryAO: 'AO2',
    prerequisites: ['L3', 'L6'],
  ),
  NodeDef(
    id: 'L9', realm: 'life', orderIndex: 8,
    title: 'Transport in animals',
    examFocus: 'label/describe circulatory system', primaryAO: 'AO1',
    prerequisites: ['L7'],
  ),
  NodeDef(
    id: 'L10', realm: 'life', orderIndex: 9,
    title: 'Diseases & immunity',
    examFocus: 'explain antibodies & vaccination', primaryAO: 'AO1',
    prerequisites: ['L9'],
  ),

  // ── Realm II · MATTER ───────────────────────────────────────────────────────
  NodeDef(
    id: 'M1', realm: 'matter', orderIndex: 0,
    title: 'Kinetic particle theory & states of matter',
    examFocus: 'explain changes of state', primaryAO: 'AO2',
    nexusLinks: ['N1_particles'],
  ),
  NodeDef(
    id: 'M2', realm: 'matter', orderIndex: 1,
    title: 'Atoms, elements & compounds',
    examFocus: 'atomic structure', primaryAO: 'AO1',
    prerequisites: ['M1'],
  ),
  NodeDef(
    id: 'M3', realm: 'matter', orderIndex: 2,
    title: 'The Periodic Table',
    examFocus: 'predict trends', primaryAO: 'AO2',
    prerequisites: ['M2'],
  ),
  NodeDef(
    id: 'M4', realm: 'matter', orderIndex: 3,
    title: 'Chemical bonding',
    examFocus: 'explain dot-and-cross', primaryAO: 'AO2',
    prerequisites: ['M2'],
  ),
  NodeDef(
    id: 'M5', realm: 'matter', orderIndex: 4,
    title: 'Stoichiometry & the mole',
    examFocus: 'calculations', primaryAO: 'AO2',
    prerequisites: ['M4'],
    nexusLinks: ['N2_energy'],
  ),
  NodeDef(
    id: 'M6', realm: 'matter', orderIndex: 5,
    title: 'Acids, bases & salts',
    examFocus: 'practical — salt preparation', primaryAO: 'AO3',
    prerequisites: ['M5'],
    nexusLinks: ['N3_transport'],
  ),

  // ── Realm III · FORCES ──────────────────────────────────────────────────────
  NodeDef(
    id: 'F1', realm: 'forces', orderIndex: 0,
    title: 'Motion',
    examFocus: 'calculations & graph interpretation', primaryAO: 'AO2',
  ),
  NodeDef(
    id: 'F2', realm: 'forces', orderIndex: 1,
    title: 'Forces',
    examFocus: 'Newton\'s laws, moments, pressure', primaryAO: 'AO2',
    prerequisites: ['F1'],
    nexusLinks: ['N3_transport'],
  ),
  NodeDef(
    id: 'F3', realm: 'forces', orderIndex: 2,
    title: 'Energy',
    examFocus: 'KE, GPE, efficiency calculations', primaryAO: 'AO2',
    prerequisites: ['F2'],
    nexusLinks: ['N2_energy'],
  ),
  NodeDef(
    id: 'F4', realm: 'forces', orderIndex: 3,
    title: 'Thermal physics',
    examFocus: 'explain in particle terms', primaryAO: 'AO2',
    prerequisites: ['F3'],
    nexusLinks: ['N1_particles'],
  ),
  NodeDef(
    id: 'F5', realm: 'forces', orderIndex: 4,
    title: 'Waves',
    examFocus: 'v = fλ calculations', primaryAO: 'AO2',
    prerequisites: ['F4'],
  ),
];

NodeDef? nodeById(String id) {
  try {
    return kAllNodes.firstWhere((n) => n.id == id);
  } catch (_) {
    return null;
  }
}

List<NodeDef> nodesForRealm(String realm) =>
    kAllNodes.where((n) => n.realm == realm).toList()
      ..sort((a, b) => a.orderIndex.compareTo(b.orderIndex));
