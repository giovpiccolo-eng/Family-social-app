import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:provider/provider.dart';
import '../core/theme.dart';
import '../core/constants.dart';
import '../models/node_def.dart';
import '../providers/app_provider.dart';

class WorldMapScreen extends StatefulWidget {
  const WorldMapScreen({super.key});

  @override
  State<WorldMapScreen> createState() => _WorldMapScreenState();
}

class _WorldMapScreenState extends State<WorldMapScreen> with SingleTickerProviderStateMixin {
  late TabController _tabs;

  @override
  void initState() {
    super.initState();
    _tabs = TabController(length: 3, vsync: this);
  }

  @override
  void dispose() {
    _tabs.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      backgroundColor: NexusColors.background,
      appBar: AppBar(
        backgroundColor: NexusColors.background,
        elevation: 0,
        title: Text('WORLD MODEL', style: labelCaps(color: NexusColors.inkSecondary)),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: NexusColors.inkSecondary),
          onPressed: () => Navigator.pop(context),
        ),
        bottom: TabBar(
          controller: _tabs,
          indicatorColor: NexusColors.nexus,
          labelStyle: labelCaps(size: 11),
          unselectedLabelStyle: labelCaps(size: 11, color: NexusColors.inkMuted),
          tabs: const [
            Tab(text: 'LIFE'),
            Tab(text: 'MATTER'),
            Tab(text: 'FORCES'),
          ],
        ),
      ),
      body: Column(
        children: [
          // Nexus section at top
          _NexusBar(),

          // Realm tabs
          Expanded(
            child: TabBarView(
              controller: _tabs,
              children: [
                _RealmMap(realm: 'life'),
                _RealmMap(realm: 'matter'),
                _RealmMap(realm: 'forces'),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _NexusBar extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final progress = context.watch<AppProvider>().progress;

    return Container(
      height: 80,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: NexusColors.border)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('NEXUS CONNECTIONS', style: labelCaps(color: NexusColors.nexus, size: 9)),
          const SizedBox(height: 6),
          SizedBox(
            height: 40,
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: kNexuses.length,
              separatorBuilder: (_, __) => const SizedBox(width: 8),
              itemBuilder: (context, i) {
                final nexus = kNexuses[i];
                final unlocked = progress.unlockedNexuses.contains(nexus['id']);
                return _NexusChip(nexus: nexus, unlocked: unlocked);
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _NexusChip extends StatelessWidget {
  final Map<String, dynamic> nexus;
  final bool unlocked;

  const _NexusChip({required this.nexus, required this.unlocked});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: unlocked ? NexusColors.nexusLight : NexusColors.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: unlocked ? NexusColors.nexus : NexusColors.border,
        ),
      ),
      child: Row(
        children: [
          Text(
            unlocked ? '◈' : '○',
            style: TextStyle(
              color: unlocked ? NexusColors.nexus : NexusColors.inkMuted,
              fontSize: 14,
            ),
          ),
          const SizedBox(width: 6),
          Text(
            nexus['name'] as String,
            style: labelCaps(
              color: unlocked ? NexusColors.nexus : NexusColors.inkMuted,
              size: 10,
            ),
          ),
        ],
      ),
    );
  }
}

class _RealmMap extends StatelessWidget {
  final String realm;
  const _RealmMap({required this.realm});

  @override
  Widget build(BuildContext context) {
    final progress = context.watch<AppProvider>().progress;
    final nodes = nodesForRealm(realm);
    final color = realmColor(realm);
    final app = context.read<AppProvider>();

    return ListView.builder(
      padding: const EdgeInsets.all(20),
      itemCount: nodes.length,
      itemBuilder: (context, i) {
        final node = nodes[i];
        final mastery = progress.nodeMastery[node.id];
        final pct = mastery?.masteryPercent ?? 0;
        final isMastered = mastery?.isMastered ?? false;
        final isElite = mastery?.isElite ?? false;

        // Check if prerequisites are met
        final prereqsMet = node.prerequisites.isEmpty ||
            node.prerequisites.every((p) {
              final m = progress.nodeMastery[p];
              return m != null && m.isMastered;
            });

        return Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Row(
            children: [
              // Connector line
              if (i > 0)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: Column(
                    children: [
                      Container(width: 2, height: 20, color: color.withOpacity(0.3)),
                    ],
                  ),
                )
              else
                const SizedBox(width: 10),

              // Node card
              Expanded(
                child: GestureDetector(
                  onTap: prereqsMet ? () => app.startSession(node.id) : null,
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: isMastered
                          ? realmSurface(realm)
                          : prereqsMet
                              ? NexusColors.surface
                              : NexusColors.background,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: isMastered
                            ? color
                            : prereqsMet
                                ? NexusColors.border
                                : NexusColors.border.withOpacity(0.4),
                        width: isMastered ? 1.5 : 1,
                      ),
                    ),
                    child: Row(
                      children: [
                        // Node ID + status
                        SizedBox(
                          width: 44,
                          child: Column(
                            children: [
                              Text(
                                node.id,
                                style: monoStyle(
                                  size: 14,
                                  color: isMastered
                                      ? color
                                      : prereqsMet
                                          ? NexusColors.inkPrimary
                                          : NexusColors.inkMuted,
                                  weight: FontWeight.w700,
                                ),
                              ),
                              if (isElite)
                                Text('ELITE', style: labelCaps(color: NexusColors.xpGold, size: 8))
                              else if (isMastered)
                                Text('✓', style: TextStyle(color: color, fontSize: 14)),
                            ],
                          ),
                        ),

                        const SizedBox(width: 10),

                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                node.title,
                                style: TextStyle(
                                  color: prereqsMet
                                      ? NexusColors.inkPrimary
                                      : NexusColors.inkMuted,
                                  fontSize: 13,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                node.examFocus,
                                style: monoStyle(size: 11, color: NexusColors.inkMuted),
                              ),
                              if (prereqsMet && pct > 0) ...[
                                const SizedBox(height: 6),
                                ClipRRect(
                                  borderRadius: BorderRadius.circular(3),
                                  child: LinearProgressIndicator(
                                    value: pct / 100,
                                    backgroundColor: NexusColors.border,
                                    valueColor: AlwaysStoppedAnimation(color),
                                    minHeight: 4,
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ),

                        // Nexus link indicator
                        if (node.nexusLinks.isNotEmpty)
                          Padding(
                            padding: const EdgeInsets.only(left: 8),
                            child: Text(
                              '◈',
                              style: TextStyle(
                                color: NexusColors.nexus.withOpacity(0.6),
                                fontSize: 16,
                              ),
                            ),
                          ),

                        // Lock icon
                        if (!prereqsMet)
                          const Padding(
                            padding: EdgeInsets.only(left: 8),
                            child: Icon(Icons.lock_outline, color: NexusColors.inkMuted, size: 16),
                          ),
                      ],
                    ),
                  ).animate().fadeIn(delay: Duration(milliseconds: i * 60)),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
