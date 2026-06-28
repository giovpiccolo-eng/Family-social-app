import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';
import '../core/theme.dart';
import '../core/constants.dart';
import '../models/node_def.dart';
import '../providers/app_provider.dart';
import 'world_map_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppProvider>();
    final progress = app.progress;
    final theme = Theme.of(context);

    return Scaffold(
      backgroundColor: NexusColors.background,
      body: SafeArea(
        child: SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Top bar: logo + rank
                Row(
                  children: [
                    Text(
                      'NEXUS',
                      style: theme.textTheme.headlineLarge?.copyWith(
                        color: NexusColors.nexus,
                        letterSpacing: 4,
                      ),
                    ),
                    const Spacer(),
                    _RankBadge(xp: progress.totalXp),
                  ],
                ).animate().fadeIn(duration: 400.ms),

                const SizedBox(height: 4),

                Text(
                  'Coordinated Sciences, connected.',
                  style: theme.textTheme.bodySmall,
                ).animate().fadeIn(delay: 100.ms),

                const SizedBox(height: 28),

                // Streak + XP bar
                _StreakXpRow(
                  streak: progress.streakDays,
                  xp: progress.totalXp,
                  freezes: progress.streakFreezes,
                ).animate().fadeIn(delay: 150.ms),

                const SizedBox(height: 32),

                // START TODAY'S MISSION — the dominant action
                _MissionButton(
                  nodeId: app.recommendedNodeId,
                  onTap: () => app.startSession(app.recommendedNodeId),
                ).animate().fadeIn(delay: 200.ms).scale(begin: const Offset(0.95, 0.95)),

                const SizedBox(height: 28),

                // Quick node picker
                Text('CHOOSE A NODE', style: labelCaps()).animate().fadeIn(delay: 300.ms),

                const SizedBox(height: 10),

                _NodePicker(
                  onNodeSelected: (nodeId) => app.startSession(nodeId),
                ).animate().fadeIn(delay: 350.ms),

                const SizedBox(height: 28),

                // World Map preview
                _WorldMapPreview(
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => const WorldMapScreen()),
                  ),
                ).animate().fadeIn(delay: 400.ms),

                const SizedBox(height: 28),

                // Realm mastery cards
                Text('REALM PROGRESS', style: labelCaps()).animate().fadeIn(delay: 450.ms),

                const SizedBox(height: 10),

                ...['life', 'matter', 'forces'].asMap().entries.map((e) {
                  final realmId = e.value;
                  final nodes = nodesForRealm(realmId);
                  final mastered = nodes.where((n) {
                    final m = progress.nodeMastery[n.id];
                    return m != null && m.isMastered;
                  }).length;

                  return Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: _RealmCard(
                      realmId: realmId,
                      masteredCount: mastered,
                      totalCount: nodes.length,
                    ).animate().fadeIn(delay: Duration(milliseconds: 500 + e.key * 80)),
                  );
                }),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _MissionButton extends StatelessWidget {
  final String nodeId;
  final VoidCallback onTap;

  const _MissionButton({required this.nodeId, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final node = nodeById(nodeId);

    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            colors: [Color(0xFF1A103A), Color(0xFF0D1A3A)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: NexusColors.nexus.withOpacity(0.6), width: 1.5),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Text('⚡', style: TextStyle(fontSize: 24)),
                const SizedBox(width: 8),
                Text('TODAY\'S MISSION', style: labelCaps(color: NexusColors.nexus)),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              'START TODAY\'S MISSION',
              style: theme.textTheme.headlineLarge?.copyWith(
                color: Colors.white,
                height: 1.2,
              ),
            ),
            if (node != null) ...[
              const SizedBox(height: 6),
              Text(
                '${node.id} · ${node.title}',
                style: theme.textTheme.bodySmall?.copyWith(color: NexusColors.nexus.withOpacity(0.8)),
              ),
            ],
            const SizedBox(height: 20),
            Row(
              children: [
                _MiniRound(label: 'WARM-UP', color: NexusColors.life),
                const SizedBox(width: 6),
                _MiniRound(label: 'LEARN', color: NexusColors.matter),
                const SizedBox(width: 6),
                _MiniRound(label: 'APPLY', color: NexusColors.forces),
                const SizedBox(width: 6),
                _MiniRound(label: 'LAB', color: NexusColors.life),
                const SizedBox(width: 6),
                _MiniRound(label: 'CRACK', color: NexusColors.nexus),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _MiniRound extends StatelessWidget {
  final String label;
  final Color color;
  const _MiniRound({required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
      decoration: BoxDecoration(
        color: color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(5),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Text(label, style: labelCaps(color: color, size: 9)),
    );
  }
}

class _StreakXpRow extends StatelessWidget {
  final int streak;
  final int xp;
  final int freezes;

  const _StreakXpRow({required this.streak, required this.xp, required this.freezes});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final rank = rankForXp(xp);

    return Row(
      children: [
        // Streak
        _InfoChip(
          icon: '🔥',
          value: '$streak',
          label: 'day streak',
          color: NexusColors.matter,
        ),
        const SizedBox(width: 10),
        // XP
        _InfoChip(
          icon: '⚡',
          value: '$xp',
          label: 'total XP',
          color: NexusColors.xpGold,
        ),
        const SizedBox(width: 10),
        // Freezes
        if (freezes > 0)
          _InfoChip(
            icon: '🧊',
            value: '$freezes',
            label: 'freezes',
            color: NexusColors.forces,
          ),
      ],
    );
  }
}

class _InfoChip extends StatelessWidget {
  final String icon;
  final String value;
  final String label;
  final Color color;

  const _InfoChip({
    required this.icon,
    required this.value,
    required this.label,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: color.withOpacity(0.08),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Row(
        children: [
          Text(icon, style: const TextStyle(fontSize: 16)),
          const SizedBox(width: 6),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(value, style: monoStyle(size: 16, color: color, weight: FontWeight.w700)),
              Text(label, style: labelCaps(size: 9, color: color)),
            ],
          ),
        ],
      ),
    );
  }
}

class _NodePicker extends StatelessWidget {
  final void Function(String nodeId) onNodeSelected;

  const _NodePicker({required this.onNodeSelected});

  @override
  Widget build(BuildContext context) {
    const available = ['F1', 'L3', 'M1'];
    return SizedBox(
      height: 50,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: available.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (context, i) {
          final nodeId = available[i];
          final node = nodeById(nodeId);
          if (node == null) return const SizedBox.shrink();
          final color = realmColor(node.realm);

          return GestureDetector(
            onTap: () => onNodeSelected(nodeId),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              decoration: BoxDecoration(
                color: realmSurface(node.realm),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: color.withOpacity(0.4)),
              ),
              child: Row(
                children: [
                  Text(
                    nodeId,
                    style: monoStyle(size: 13, color: color, weight: FontWeight.w700),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    node.title.split(' ').take(2).join(' '),
                    style: TextStyle(color: color.withOpacity(0.8), fontSize: 12),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

class _WorldMapPreview extends StatelessWidget {
  final VoidCallback onTap;
  const _WorldMapPreview({required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: NexusColors.surface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: NexusColors.border),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('WORLD MODEL', style: labelCaps()),
                  const SizedBox(height: 4),
                  Text(
                    'Three realms · 21 nodes · 3 Nexuses',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
            Row(
              children: [
                _RealmDot(color: NexusColors.life),
                const SizedBox(width: 4),
                _RealmDot(color: NexusColors.matter),
                const SizedBox(width: 4),
                _RealmDot(color: NexusColors.forces),
                const SizedBox(width: 4),
                _RealmDot(color: NexusColors.nexus),
              ],
            ),
            const SizedBox(width: 8),
            const Icon(Icons.chevron_right, color: NexusColors.inkMuted),
          ],
        ),
      ),
    );
  }
}

class _RealmDot extends StatelessWidget {
  final Color color;
  const _RealmDot({required this.color});
  @override
  Widget build(BuildContext context) => Container(
        width: 12,
        height: 12,
        decoration: BoxDecoration(color: color, shape: BoxShape.circle),
      );
}

class _RankBadge extends StatelessWidget {
  final int xp;
  const _RankBadge({required this.xp});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: NexusColors.xpGold.withOpacity(0.1),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: NexusColors.xpGold.withOpacity(0.4)),
      ),
      child: Text(
        rankForXp(xp),
        style: monoStyle(size: 12, color: NexusColors.xpGold, weight: FontWeight.w700),
      ),
    );
  }
}

class _RealmCard extends StatelessWidget {
  final String realmId;
  final int masteredCount;
  final int totalCount;

  const _RealmCard({
    required this.realmId,
    required this.masteredCount,
    required this.totalCount,
  });

  @override
  Widget build(BuildContext context) {
    final color = realmColor(realmId);
    final pct = totalCount > 0 ? masteredCount / totalCount : 0.0;
    final realmData = kRealms.firstWhere((r) => r['id'] == realmId);

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: NexusColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: NexusColors.border),
      ),
      child: Row(
        children: [
          Container(
            width: 4,
            height: 48,
            decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(2)),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(realmData['name'] as String, style: labelCaps(color: color)),
                const SizedBox(height: 2),
                Text(realmData['subtitle'] as String,
                    style: Theme.of(context).textTheme.bodySmall),
              ],
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                '$masteredCount/$totalCount',
                style: monoStyle(size: 13, color: color),
              ),
              const SizedBox(height: 4),
              SizedBox(
                width: 80,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(3),
                  child: LinearProgressIndicator(
                    value: pct,
                    backgroundColor: NexusColors.border,
                    valueColor: AlwaysStoppedAnimation(color),
                    minHeight: 6,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
