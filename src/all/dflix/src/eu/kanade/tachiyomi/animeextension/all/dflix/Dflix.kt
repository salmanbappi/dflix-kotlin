package eu.kanade.tachiyomi.animeextension.all.dflix

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

class Dflix : AnimeHttpSource() {

    override val name = "Dflix Kotlin"

    override val baseUrl = "https://dflix.discoveryftp.net"

    override val lang = "all"

    override val supportsLatest = true

    override val id: Long = 5181466391484419844L

    override val client: OkHttpClient = super.client.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .dispatcher(okhttp3.Dispatcher().apply {
            maxRequests = 100
            maxRequestsPerHost = 100
        })
        .build()

    private val cm by lazy { CookieManager(client) }

    override fun headersBuilder() = super.headersBuilder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .add("Accept", "*/*")
        .add("Cookie", cm.getCookiesHeaders())
        .add("Referer", "$baseUrl/")

    private fun fixUrl(url: String): String {
        if (url.isBlank()) return url
        val u = url.trim().replace(" ", "%20")
        return if (u.startsWith("http")) u else "$baseUrl$u"
    }

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage {
        return withContext(Dispatchers.IO) {
            val url = Filters.getUrl(query, filters)
            val response = client.newCall(GET(fixUrl(url), headers)).execute()
            popularAnimeParse(response)
        }
    }

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        return GET(fixUrl(Filters.getUrl(query, filters)), headers)
    }

    override fun searchAnimeParse(response: Response) = popularAnimeParse(response)

    override fun popularAnimeRequest(page: Int): Request {
        return GET("$baseUrl/m/recent/$page", headers)
    }

    override fun popularAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()
        val animeList = document.select("div.card, div.movie-item").map { element ->
            SAnime.create().apply {
                val titleEl = element.selectFirst("h5 a, h4 a, .title a")
                title = titleEl?.text() ?: ""
                url = titleEl?.attr("href") ?: ""
                thumbnail_url = element.selectFirst("img")?.attr("abs:src")?.replace(" ", "%20") ?: ""
            }
        }.filter { it.title.isNotEmpty() }
        
        val hasNextPage = document.selectFirst("a.page-link[rel=next], .pagination .next") != null
        return AnimesPage(animeList, hasNextPage)
    }

    override fun latestUpdatesRequest(page: Int) = popularAnimeRequest(page)
    override fun latestUpdatesParse(response: Response) = popularAnimeParse(response)

    override fun getFilterList() = Filters.getFilterList()

    override fun animeDetailsRequest(anime: SAnime): Request {
        return GET(fixUrl(anime.url), headers)
    }

    override fun animeDetailsParse(response: Response): SAnime {
        val document = response.asJsoup()
        val mediaType = getMediaType(document)
        
        return if (mediaType == "m") getMovieDetails(document) else getSeriesDetails(document)
    }

    private fun getMediaType(document: Document): String {
        val html = document.select("script").html()
        return if (html.contains("/m/lazyload/") || html.contains("/m/lazy_load/")) "m" else "s"
    }

    private fun getMovieDetails(document: Document) = SAnime.create().apply {
        status = SAnime.COMPLETED
        thumbnail_url = document.selectFirst("figure.movie-detail-banner img")?.attr("abs:src")?.replace(" ", "%20") ?: ""
        genre = document.select("div.ganre-wrapper a").joinToString { it.text().replace(",", "").trim() }
        description = document.selectFirst("p.storyline")?.text()?.trim() ?: ""
    }

    private fun getSeriesDetails(document: Document) = SAnime.create().apply {
        status = SAnime.ONGOING
        thumbnail_url = document.selectFirst("div.movie-detail-banner img")?.attr("abs:src")?.replace(" ", "%20") ?: ""
        genre = document.select("div.ganre-wrapper a").joinToString { it.text().replace(",", "").trim() }
        description = document.selectFirst("p.storyline")?.text()?.trim() ?: ""
    }

    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        return withContext(Dispatchers.IO) {
            val response = client.newCall(GET(fixUrl(anime.url), headers)).execute()
            val document = response.asJsoup()
            val mediaType = getMediaType(document)

            if (mediaType == "m") {
                getMovieMedia(document)
            } else {
                sortEpisodes(extractEpisodes(document))
            }
        }
    }

    private fun extractEpisodes(document: Document): List<EpisodeData> {
        return document.select("div.container > div > div.card").mapNotNull { element ->
            val titleElement = element.selectFirst("h5") ?: return@mapNotNull null
            val rawTitle = titleElement.ownText().trim()
            val seasonEpisode = rawTitle.split("&nbsp;").first().trim()
            val url = element.selectFirst("h5 a")?.attr("href")?.trim() ?: ""
            val qualityText = element.selectFirst("h5 .badge-fill")?.text() ?: ""
            val quality = sizeRegex.replace(qualityText, "$1").trim()
            val epName = element.selectFirst("h4")?.ownText()?.trim() ?: ""
            val size = element.selectFirst("h4 .badge-outline")?.text()?.trim() ?: ""
            
            if (seasonEpisode.isNotEmpty() && url.isNotEmpty()) {
                EpisodeData(seasonEpisode, url, quality, epName, size)
            } else null
        }
    }

    private fun getMovieMedia(document: Document): List<SEpisode> {
        val linkElement = document.select("div.col-md-12 a.btn").lastOrNull()
        val url = linkElement?.attr("href")?.let { it.replace(" ", "%20") } ?: ""
        val quality = document.select(".badge-wrapper .badge-fill").lastOrNull()?.text()?.replace("|", "•")?.trim() ?: ""
        
        return listOf(SEpisode.create().apply {
            this.url = url
            this.name = "Movie"
            this.episode_number = 1f
            this.scanlator = quality
        })
    }

    private fun sortEpisodes(list: List<EpisodeData>): List<SEpisode> {
        return list.mapIndexed { index, it ->
            SEpisode.create().apply {
                url = it.videoUrl
                name = "${it.seasonEpisode} - ${it.episodeName}".trim()
                episode_number = (list.size - index).toFloat()
                scanlator = "${it.quality}  •  ${it.size}"
            }
        }
    }

    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        return listOf(Video(episode.url, "Video", fixUrl(episode.url)))
    }

    override fun episodeListParse(response: Response): List<SEpisode> = throw Exception("Not used")
    override fun videoListParse(response: Response): List<Video> = throw Exception("Not used")

    data class EpisodeData(
        val seasonEpisode: String,
        val videoUrl: String,
        val quality: String,
        val episodeName: String,
        val size: String
    )

    class CookieManager(private val client: OkHttpClient) {
        private val cookieUrl = "https://dflix.discoveryftp.net/login/demo".toHttpUrl()
        @Volatile
        private var cookieMap = mutableMapOf<String, String>()
        private val lock = Any()

        fun getCookiesHeaders(): String {
            if (cookieMap.isEmpty()) {
                synchronized(lock) {
                    if (cookieMap.isEmpty()) {
                        fetchCookies()
                    }
                }
            }
            return cookieMap.map { "${it.key}=${it.value}" }.joinToString("; ")
        }

        private fun fetchCookies() {
            val req = Request.Builder()
                .url(cookieUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://dflix.discoveryftp.net/login")
                .header("X-Requested-With", "XMLHttpRequest")
                .build()
            try {
                val res = client.newBuilder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
                    .newCall(req)
                    .execute()
                
                val cookies = Cookie.parseAll(cookieUrl, res.headers)
                cookies.forEach { cookieMap[it.name] = it.value }
                res.close()
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    companion object {
        private val sizeRegex = Regex(""".*\s(\d+\.\d+\s+MB)$""")
    }
}
