import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/theme.dart';
import 'providers/app_provider.dart';
import 'screens/home_screen.dart';
import 'screens/assessment_screen.dart';
import 'screens/results_screen.dart';

class PrismApp extends StatelessWidget {
  const PrismApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'PRISM',
      theme: PrismTheme.dark,
      darkTheme: PrismTheme.dark,
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
      case AppState.home:
        return const HomeScreen();
      case AppState.assessing:
        return const AssessmentScreen();
      case AppState.results:
        return ResultsScreen(result: app.lastResult!);
    }
  }
}

class _SplashScreen extends StatelessWidget {
  const _SplashScreen();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: PrismColors.background,
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'PRISM',
              style: Theme.of(context).textTheme.displayLarge?.copyWith(
                    color: PrismColors.accent,
                    letterSpacing: 12,
                    fontWeight: FontWeight.w300,
                  ),
            ),
            const SizedBox(height: 8),
            Text(
              'Cognitive ability, refracted.',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: PrismColors.inkSecondary,
                  ),
            ),
            const SizedBox(height: 48),
            SizedBox(
              width: 20,
              height: 20,
              child: CircularProgressIndicator(
                strokeWidth: 1.5,
                color: PrismColors.accent.withOpacity(0.6),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
