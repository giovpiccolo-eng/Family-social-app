import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/theme.dart';
import 'providers/app_provider.dart';
import 'screens/home_screen.dart';
import 'screens/session_screen.dart';

class NexusApp extends StatelessWidget {
  const NexusApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NEXUS',
      theme: NexusTheme.dark,
      darkTheme: NexusTheme.dark,
      themeMode: ThemeMode.dark,
      debugShowCheckedModeBanner: false,
      home: const _AppRoot(),
    );
  }
}

class _AppRoot extends StatefulWidget {
  const _AppRoot();

  @override
  State<_AppRoot> createState() => _AppRootState();
}

class _AppRootState extends State<_AppRoot> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<AppProvider>().init();
    });
  }

  @override
  Widget build(BuildContext context) {
    final app = context.watch<AppProvider>();

    switch (app.state) {
      case AppState.loading:
        return const _SplashScreen();
      case AppState.inSession:
        return const SessionScreen();
      case AppState.ready:
        return const HomeScreen();
    }
  }
}

class _SplashScreen extends StatelessWidget {
  const _SplashScreen();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NexusColors.background,
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'NEXUS',
              style: Theme.of(context).textTheme.displayMedium?.copyWith(
                    color: NexusColors.nexus,
                    letterSpacing: 8,
                  ),
            ).animate(onPlay: (c) => c.repeat()).shimmer(
                  duration: 1500.ms,
                  color: NexusColors.nexus.withOpacity(0.5),
                ),
            const SizedBox(height: 8),
            Text(
              'Coordinated Sciences, connected.',
              style: Theme.of(context).textTheme.bodySmall,
            ).animate().fadeIn(delay: 300.ms),
            const SizedBox(height: 40),
            SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: NexusColors.nexus.withOpacity(0.6),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
