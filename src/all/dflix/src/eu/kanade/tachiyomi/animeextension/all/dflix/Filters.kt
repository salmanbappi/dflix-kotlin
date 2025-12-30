package eu.kanade.tachiyomi.animeextension.all.dflix

import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList

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

    fun getUrl(query: String, filters: AnimeFilterList): String {
        if (query.isNotEmpty()) {
            return "https://dflix.discoveryftp.net/search/$query"
        }

        val modeIndex = (filters[1] as DflixSelect).state
        return when (modeIndex) {
            0 -> "https://dflix.discoveryftp.net/m/recent/1"
            1 -> {
                val catIndex = (filters[4] as DflixSelect).state
                val category = FilterData.CATEGORIES[catIndex]
                "https://dflix.discoveryftp.net/m/uhd/$category"
            }
            2 -> {
                val genreIndex = (filters[5] as DflixSelect).state
                val genre = FilterData.GENRES[genreIndex]
                "https://dflix.discoveryftp.net/m/genre/$genre"
            }
            3 -> {
                val catIndex = (filters[6] as DflixSelect).state
                val category = FilterData.CATEGORIES[catIndex]
                val yearIndex = (filters[7] as DflixSelect).state
                val year = FilterData.YEARS[yearIndex]
                "https://dflix.discoveryftp.net/m/type/$category/$year"
            }
            4 -> "https://dflix.discoveryftp.net/m/dual/Hindi"
            5 -> "https://dflix.discoveryftp.net/m/lan/English"
            else -> "https://dflix.discoveryftp.net/m/recent/1"
        }
    }
}
