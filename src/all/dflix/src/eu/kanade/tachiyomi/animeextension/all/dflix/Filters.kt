package eu.kanade.tachiyomi.animeextension.all.dflix

import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList

import java.net.URLEncoder

object Filters {
    fun getFilterList(): AnimeFilterList {
        return AnimeFilterList(
            AnimeFilter.Header("Search Mode"),
            DflixSelect("Mode", FilterData.MODES),
            AnimeFilter.Separator(),
            AnimeFilter.Header("Filters (Apply based on Mode)"),
            DflixSelect("4K Category", FilterData.CATEGORIES),
            DflixSelect("Genre", FilterData.GENRES),
            DflixSelect("Type", FilterData.CATEGORIES),
            DflixSelect("Year", FilterData.YEARS)
        )
    }

    fun getUrl(query: String, filters: AnimeFilterList, page: Int): String {
        if (query.isNotEmpty()) {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            return "${Dflix.BASE_URL}/search/$encodedQuery"
        }

        val modeIndex = if (filters.size > 1) (filters[1] as DflixSelect).state else 0
        return when (modeIndex) {
            0 -> "${Dflix.BASE_URL}/m/recent/$page"
            1 -> {
                val catIndex = if (filters.size > 4) (filters[4] as DflixSelect).state else 0
                val category = FilterData.CATEGORIES.getOrElse(catIndex) { "Hindi" }
                "${Dflix.BASE_URL}/m/uhd/$category/$page"
            }
            2 -> {
                val genreIndex = if (filters.size > 5) (filters[5] as DflixSelect).state else 0
                val genre = FilterData.GENRES.getOrElse(genreIndex) { "Action" }
                "${Dflix.BASE_URL}/m/genre/$genre/$page"
            }
            3 -> {
                val catIndex = if (filters.size > 6) (filters[6] as DflixSelect).state else 0
                val category = FilterData.CATEGORIES.getOrElse(catIndex) { "Hindi" }
                val yearIndex = if (filters.size > 7) (filters[7] as DflixSelect).state else 0
                val year = FilterData.YEARS.getOrElse(yearIndex) { "2025" }
                "${Dflix.BASE_URL}/m/type/$category/$year/$page"
            }
            4 -> "${Dflix.BASE_URL}/m/dual/Hindi/$page"
            5 -> "${Dflix.BASE_URL}/m/lan/English/$page"
            else -> "${Dflix.BASE_URL}/m/recent/$page"
        }
    }
}
