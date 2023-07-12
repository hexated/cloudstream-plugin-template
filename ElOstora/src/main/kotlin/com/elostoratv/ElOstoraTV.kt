package com.elostoratv

import android.icu.util.Calendar
import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.nicehttp.requestCreator

class ElOstoraTV : MainAPI() {
    override var lang = "ar"

    override var name = "El Ostora TV"
    override val usesWebView = false
    override val hasMainPage = true
    override val hasDownloadSupport = false
    override val supportedTypes =
        setOf(
            TvType.Live
        )
    private val APIurl = "https://z420572.radwan.shop/api/v4_8.php"

    override val mainPage = generateHomePage()

    private fun generateHomePage() : List<MainPageData> {

        val homepage =  mutableListOf<MainPageData>()
        val data = mapOf(
            "main_id" to "1",
            "id" to "",
            "sub_id" to "0"
        )
        val decodedbody = getDecoded(data)
        //Log.d("King", "decodedbody:$decodedbody")

        parseJson<Categories>(decodedbody).results?.map { element ->
            homepage.add(mainPage(name = element.category_name, url = element.cid))
        } ?: throw ErrorLoadingException("Invalid Json response")

        return homepage
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val items = mutableListOf<HomePageList>()
        if (page <= 1) {
            //Log.d("King", "request:$request")
            val data = mapOf(
                "id" to "",
                "cat_id" to request.data
            )
            val decodedbody = getDecoded(data)
            //Log.d("King", "decodedbody:$decodedbody")
            val list = parseJson<Results>(decodedbody).results?.map { element ->
                element.toSearchResponse(request)
            } ?: throw ErrorLoadingException("Invalid Json response")
            if (list.isNotEmpty()) items.add(HomePageList(request.name, list, true))
        }
        return newHomePageResponse(items)
    }

    private fun Channel.toSearchResponse(request: MainPageRequest): SearchResponse {
        return LiveSearchResponse(
            name = this.channel_title,
            url = Data(id = id, channel_title = this.channel_title, channel_thumbnail = this.channel_thumbnail, category = request.name, channel_url = this.channel_url).toJson(),
            this@ElOstoraTV.name,
            TvType.Live,
            this.channel_thumbnail,
        )
    }

    override suspend fun load(url: String): LoadResponse {
        //Log.d("King", "Load:$url")
        val data = parseJson<Data>(url)

        return LiveStreamLoadResponse(
            name = data.channel_title,
            url = Data(id = data.id, channel_title = data.channel_title, channel_thumbnail = data.channel_thumbnail, category = data.category, channel_url = urlfix(data.channel_url)).toJson(),
            dataUrl = Data(id = data.id, channel_title = data.channel_title, channel_thumbnail = data.channel_thumbnail, category = data.category, channel_url = urlfix(data.channel_url)).toJson(),
            apiName = name,
            posterUrl = data.channel_thumbnail,
            type = TvType.Live,
        )
    }
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        //Log.d("King", "loadLinks:$data")
        val data = parseJson<Data>(data)
            callback.invoke(
                ExtractorLink(
                    source = data.channel_title,
                    name = data.channel_title,
                    url = data.channel_url,
                    referer = "",
                    quality = Qualities.Unknown.value,
                    isM3u8 = true,
                )
            )
        return true
    }

    private fun urlfix(url: String): String {
        return url.replace("https://raw.githubusercontent.com/ostoratv/m3u8/master/update.m3u8?<F>302<F>","")
    }
    private fun getDecoded(payload: Map<String, String>): String {

        val t = Calendar.getInstance().timeInMillis.toString()

        val client = app.baseClient.newBuilder()
            .build()

        val request = requestCreator(
            method = "POST",
            url = APIurl,
            headers = mapOf(
                "user-agent" to "Mozilla/5.0 (Linux; U; Android 10; en; YAL-L41 Api/HUAWEIYAL-L41) AppleWebKit/534.30 (KHTML, like Gecko) Version/5.0 Mobile Safari/534.30",
                "Time" to t,
            ),
            data = payload,
        )
        val req = client.newCall(request).execute()
        val decryptedBody = decrypt(req.body.string(), t.toCharArray())
        //Log.d("King", "decryptedBody:" + decryptedBody)
        return decryptedBody
    }

    private fun decrypt(str: String, key: CharArray): String {
        val sb = java.lang.StringBuilder()
        for (i in str.indices) {
            val charAt = str[i]
            val cArr: CharArray = key
            sb.append((charAt.code xor cArr[i % cArr.size].code).toChar())
        }
        return sb.toString()
    }

    data class Channel(
        @JsonProperty("id") val id: String,
        @JsonProperty("channel_title") val channel_title: String,
        @JsonProperty("channel_thumbnail") val channel_thumbnail: String,
        @JsonProperty("channel_url") val channel_url: String,
        @JsonProperty("download_url") val download_url: String,
        @JsonProperty("agent") val agent: String,
        @JsonProperty("video_player") val video_player: String,
    )
    data class Results(
        @JsonProperty("data") val results: ArrayList<Channel>? = arrayListOf(),
    )

    data class Categories(
        @JsonProperty("data") val results: ArrayList<Category>? = arrayListOf(),
    )
    data class Category(
        @JsonProperty("cid") val cid: String,
        @JsonProperty("cat") val cat: String,
        @JsonProperty("category_name") val category_name: String,
        @JsonProperty("image") val image: String,
        @JsonProperty("adp") val adp: String,
    )

    data class Data(
        val id: String? = null,
        val channel_title: String,
        val channel_thumbnail: String,
        val category: String,
        val channel_url: String,
    )
}