package eu.kanade.tachiyomi.animeextension.all.dflix

object FilterData {
    val MODES = arrayOf(
        "Select Mode...",
        "4K",
        "Genre",
        "Type & Year",
        "Dual Audio",
        "Language"
    )

    val GENRES = arrayOf(
        "Action", "Adventure", "Comedy", "Crime", "Documentary", "Drama", "Family",
        "Fantasy", "History", "Horror", "Musical", "Mystery", "Romance", "Sci-Fi",
        "Thriller", "Tv Movie", "War", "Western"
    )

    val CATEGORIES = arrayOf(
        "Animation", "Bangla", "English", "Hindi", "Others", "Tamil"
    )

    val YEARS = (2025 downTo 2000).map { it.toString() }.toTypedArray()
}