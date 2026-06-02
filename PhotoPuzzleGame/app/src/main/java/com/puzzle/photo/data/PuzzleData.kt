package com.puzzle.photo.data

data class PuzzleStep(
    val stepNumber: Int,
    val clue: String,
    val hint: String,
    val acceptableLabels: List<String>,
    val successMessage: String
)

object PuzzleData {
    val steps = listOf(
        PuzzleStep(
            stepNumber = 1,
            clue = "Every great mind needs fuel for thought.\n\nBlack, hot, and bitter — or sweet if you prefer. Find the vessel that holds the scholar's morning ritual.",
            hint = "Something warm you drink in the morning... aromatic and energising.",
            acceptableLabels = listOf(
                "coffee", "cup", "mug", "drinkware", "coffee cup", "espresso",
                "cappuccino", "latte", "teacup", "drink", "beverage", "tableware"
            ),
            successMessage = "The Scholar drinks deep! Energy for the quest ahead."
        ),
        PuzzleStep(
            stepNumber = 2,
            clue = "My mentor always said: look to the ancients.\n\nThey stored their wisdom between covers, page after page. Find what holds a thousand stories.",
            hint = "Something you read, with pages and a spine.",
            acceptableLabels = listOf(
                "book", "publication", "hardcover", "paperback", "textbook",
                "novel", "magazine", "newspaper", "comic book", "notebook"
            ),
            successMessage = "Knowledge acquired! The path grows clearer."
        ),
        PuzzleStep(
            stepNumber = 3,
            clue = "The scholar's most precious resource — it governs all study, all work, all rest.\n\nFind what marks the passing hours.",
            hint = "Something that tells you what time it is.",
            acceptableLabels = listOf(
                "clock", "watch", "analog watch", "digital clock", "alarm clock",
                "wristwatch", "timer", "stopwatch", "wall clock"
            ),
            successMessage = "Time noted! Every second of this quest counts."
        ),
        PuzzleStep(
            stepNumber = 4,
            clue = "Nature holds the deepest secrets.\n\nLife itself pushes upward toward the light. Find something alive, rooted, and green.",
            hint = "A living thing that grows from soil and reaches for sunlight.",
            acceptableLabels = listOf(
                "plant", "flower", "houseplant", "tree", "leaf", "vegetation",
                "grass", "herb", "shrub", "orchid", "rose", "succulent",
                "cactus", "fern", "foliage", "branch", "petal"
            ),
            successMessage = "Life discovered! One final truth remains."
        ),
        PuzzleStep(
            stepNumber = 5,
            clue = "All truth returns to the essential.\n\nClear, pure, vital — no living thing survives without it. Find the vessel that holds life's most fundamental substance.",
            hint = "Something that contains the clear liquid all living things need.",
            acceptableLabels = listOf(
                "bottle", "water bottle", "drink", "drinkware", "glass",
                "water", "beverage", "liquid", "plastic bottle", "jar"
            ),
            successMessage = "The Scholar's Quest is complete! You have found all five truths."
        )
    )
}
