// Learner ranks (XP thresholds)
const List<Map<String, dynamic>> kRanks = [
  {'name': 'Cadet', 'minXp': 0, 'icon': '⬡'},
  {'name': 'Lab Tech', 'minXp': 500, 'icon': '⬡⬡'},
  {'name': 'Analyst', 'minXp': 1500, 'icon': '⬡⬡⬡'},
  {'name': 'Investigator', 'minXp': 3500, 'icon': '◈'},
  {'name': 'Lead Scientist', 'minXp': 7000, 'icon': '◈◈'},
  {'name': 'Laureate', 'minXp': 14000, 'icon': '★'},
];

String rankForXp(int xp) {
  for (int i = kRanks.length - 1; i >= 0; i--) {
    if (xp >= (kRanks[i]['minXp'] as int)) return kRanks[i]['name'] as String;
  }
  return 'Cadet';
}

int nextRankXp(int xp) {
  for (final r in kRanks) {
    if ((r['minXp'] as int) > xp) return r['minXp'] as int;
  }
  return (kRanks.last['minXp'] as int) + 5000;
}

// IGCSE command words with depth guidance
const Map<String, String> kCommandWords = {
  'state': 'Name or give a fact, no explanation needed.',
  'define': 'Give the precise meaning of a term.',
  'describe': 'Use data/observations; say what happens.',
  'explain': 'Say WHY it happens — mechanism required.',
  'calculate': 'Show equation, substitution, and unit.',
  'suggest': 'Apply knowledge to an unfamiliar context.',
  'predict': 'Use a trend or rule to say what happens next.',
  'compare': 'Identify both similarities AND differences.',
  'evaluate': 'Assess evidence, give a judgement.',
  'plan': 'Describe a method including variables and controls.',
};

// Assessment objectives
const Map<String, String> kAOLabels = {
  'AO1': 'Recall',
  'AO2': 'Applying',
  'AO3': 'Practical',
};

// Realms
const List<Map<String, dynamic>> kRealms = [
  {
    'id': 'life',
    'name': 'LIFE',
    'subtitle': 'Biology',
    'description': 'Cells, enzymes, photosynthesis, transport, disease. The living machinery.',
  },
  {
    'id': 'matter',
    'name': 'MATTER',
    'subtitle': 'Chemistry',
    'description': 'Particles, atoms, bonding, reactions, acids. What everything is made of.',
  },
  {
    'id': 'forces',
    'name': 'FORCES',
    'subtitle': 'Physics',
    'description': 'Motion, forces, energy, heat, waves. How it all moves and transfers.',
  },
];

// Nexuses
const List<Map<String, dynamic>> kNexuses = [
  {
    'id': 'N1_particles',
    'name': 'PARTICLES',
    'nodes': ['L3', 'M1', 'F4'],
    'tagline': 'One particle model, three lenses.',
    'description':
        'Diffusion in cells (Bio) × kinetic particle theory (Chem) × Brownian motion & gas pressure (Phys).',
  },
  {
    'id': 'N2_energy',
    'name': 'ENERGY',
    'nodes': ['L6', 'M5', 'F3'],
    'tagline': 'Energy is conserved everywhere.',
    'description':
        'Respiration & photosynthesis (Bio) × exo/endothermic change (Chem) × energy stores & transfers (Phys).',
  },
  {
    'id': 'N3_transport',
    'name': 'TRANSPORT',
    'nodes': ['L3', 'M6', 'F2'],
    'tagline': 'Gradients & pressure.',
    'description': 'Osmosis (Bio) × solutions & concentration (Chem) × pressure (Phys).',
  },
];

// Mastery thresholds
const int kMasteryThreshold = 80; // % to consider a node mastered
const int kEliteThreshold = 95;

// Session round durations (seconds)
const int kWarmUpSecs = 300;
const int kLearnSecs = 360;
const int kApplySecs = 420;
const int kLabSecs = 360;
const int kCrackSecs = 240;

// SRS intervals (days) — simplified SM-2
const List<int> kSRSIntervals = [1, 3, 7, 14, 30, 60, 120];
