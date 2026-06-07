import type { LessonKit } from "../types";

// A small, hand-curated whitelist of approved sources for the v0 lesson.
// In production this would be loaded from a teacher-built kit; for the silent
// partner v0 it lives in code so the demo runs without any kit-builder UI.

export const FRENCH_REVOLUTION_KIT: LessonKit = {
  title: "The French Revolution — opening lesson (Year 11)",
  objectives: [
    "Explain the structural causes of the Revolution (estates, fiscal crisis, Enlightenment).",
    "Place the events of 1789 in sequence: Estates-General, Tennis Court Oath, Bastille, August decrees.",
    "Read a primary source with attention to its author, audience, and silences.",
  ],
  outline: [
    "Hook: a single image (Houël's Bastille) and the question 'why this day?'",
    "Causes: the three estates, the deficit, Enlightenment ideas.",
    "Events of 1789, in sequence.",
    "Close reading: Declaration of the Rights of Man, articles 1–6.",
    "Extension: who is left out of 'Man'?",
  ],
  sources: [
    {
      id: "houel-bastille-1789",
      kind: "image",
      title: "Storming of the Bastille",
      citation: "Jean-Pierre Houël, gouache, 1789. Bibliothèque nationale de France.",
      year: "1789",
      blurb:
        "Eyewitness gouache of 14 July 1789. The earliest visual record by an artist who was in Paris that week.",
    },
    {
      id: "declaration-rights-of-man-1789",
      kind: "primary",
      title: "Declaration of the Rights of Man and of the Citizen",
      citation: "National Constituent Assembly, 26 August 1789.",
      year: "1789",
      blurb:
        "Seventeen articles. Asserts liberty, property, security, and resistance to oppression as natural rights.",
    },
    {
      id: "tennis-court-oath-david",
      kind: "image",
      title: "The Tennis Court Oath",
      citation: "Jacques-Louis David, drawing, 1791 (unfinished painting).",
      year: "1791",
      blurb:
        "The Third Estate swears not to disperse until a constitution is written. Composed long after the event — propaganda, not reportage.",
    },
    {
      id: "cahiers-de-doleances-third-estate",
      kind: "primary",
      title: "Cahiers de doléances — Third Estate of Paris",
      citation: "Compiled spring 1789, presented to the Estates-General.",
      year: "1789",
      blurb:
        "Grievance books from across France. Reveal what ordinary subjects actually wanted from the king before the Revolution radicalised.",
    },
    {
      id: "necker-financial-report-1781",
      kind: "dataset",
      title: "Compte rendu au Roi",
      citation: "Jacques Necker, financial accounts to Louis XVI, 1781.",
      year: "1781",
      blurb:
        "Necker's published royal accounts. The first time the French public saw the deficit in numbers — and it broke trust in the crown's solvency.",
    },
    {
      id: "olympe-de-gouges-1791",
      kind: "primary",
      title: "Declaration of the Rights of Woman and of the Female Citizen",
      citation: "Olympe de Gouges, pamphlet, 1791.",
      year: "1791",
      blurb:
        "Rewrites the 1789 Declaration to include women. Useful for the 'who is left out of \"Man\"?' question; de Gouges was guillotined in 1793.",
    },
    {
      id: "three-estates-cartoon",
      kind: "image",
      title: "'À faut espérer q'eu s'jeu là finira bentôt'",
      citation: "Anonymous engraving, 1789. Musée Carnavalet.",
      year: "1789",
      blurb:
        "Famous cartoon: a peasant carries a clergyman and a noble on his back. The single clearest visual of the estates system.",
    },
    {
      id: "lefebvre-coming-of-french-revolution",
      kind: "article",
      title: "The Coming of the French Revolution — chapter on the deficit",
      citation: "Georges Lefebvre, 1939 (English translation, Princeton, 1947).",
      year: "1939",
      blurb:
        "Classic short account of how the fiscal crisis forced the calling of the Estates-General. Accessible for Year 11 with light scaffolding.",
    },
    {
      id: "estates-general-population-data",
      kind: "dataset",
      title: "Population and representation by estate, 1789",
      citation: "Compiled from Le Roy Ladurie, *The Ancien Régime*, Table 2.1.",
      year: "1789",
      blurb:
        "Three estates, ~27 million people. First Estate ~0.5%, Second Estate ~1.5%, Third Estate ~98%. Each estate had one vote.",
    },
    {
      id: "marseillaise-1792-recording",
      kind: "audio",
      title: "La Marseillaise — period instruments",
      citation: "Composed by Rouget de Lisle, 1792. Recording: Orchestre Révolutionnaire et Romantique, 1989.",
      year: "1792",
      blurb:
        "War song of the Marseille federates. Useful for the radicalisation arc — what 1789 became by 1792.",
    },
    {
      id: "sieyes-what-is-third-estate",
      kind: "primary",
      title: "What is the Third Estate?",
      citation: "Emmanuel-Joseph Sieyès, pamphlet, January 1789.",
      year: "1789",
      blurb:
        "Opens: 'What is the Third Estate? Everything. What has it been until now in the political order? Nothing.' The argument that reframed the spring of 1789.",
    },
    {
      id: "map-paris-1789",
      kind: "image",
      title: "Paris in 1789 — fortifications and faubourgs",
      citation: "Esnauts and Rapilly, map of Paris, 1789.",
      year: "1789",
      blurb:
        "Period map. Shows the Bastille's position on the eastern edge — useful for explaining why a working-class crowd reached it first.",
    },
  ],
};
