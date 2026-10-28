package dev.couchtv.ontv.ui.search

import dev.couchtv.ontv.ui.lucide

/** The few extra Lucide glyphs search needs, drawn the same way as the nav icons. */
object SearchIcons {
    val Backspace = lucide("delete",
        "M20 5H9l-7 7 7 7h11a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2z M18 9l-6 6 M12 9l6 6")
    val History = lucide("history",
        "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8 M3 3v5h5 M12 7v5l4 2")
    val Sparkle = lucide("sparkles",
        "M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z")
    val Arrow = lucide("arrow-up-left", "M7 17V7h10 M17 17 7 7")
}
