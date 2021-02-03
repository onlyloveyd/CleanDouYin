package tech.kicky.cleandouyin.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import tech.kicky.cleandouyin.data.DVideo
import tech.kicky.cleandouyin.network.Retrofitance
import java.io.*


/**
 * Main ViewModel
 * author: yidong
 * 2021/1/29
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val videoPrefix = "https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids="
    private val _video = MutableLiveData<DVideo>()
    val video: LiveData<DVideo> = _video

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _toast = MutableLiveData<String>()
    val toast: LiveData<String> = _toast

    private val _showDownload = MutableLiveData(false)
    val showDownload: LiveData<Boolean> = _showDownload

    private val _downloadSuccessfully = MutableLiveData(false)
    val downloadSuccessfully: LiveData<Boolean> = _downloadSuccessfully

    fun parseVideoUrl(clipUrl: String) {
        viewModelScope.launch {
            _loading.value = true
            _showDownload.value = false
            withContext(Dispatchers.IO) {
                try {
                    val videoUrl = parseVideoUrlFromClipBoard(clipUrl)
                    val con = Jsoup.connect(videoUrl)
                    con.header(
                        "User-Agent",
                        "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1"
                    )
                    val resp = con.method(Connection.Method.GET).execute()
                    val strUrl = resp.url().toString()
                    val itemId = strUrl.substring(strUrl.indexOf("video/"), strUrl.lastIndexOf("/"))
                        .replace("video/", "")
                    val waterMarkUrl: String = videoPrefix + itemId
                    val waterMarkJson =
                        Jsoup.connect(waterMarkUrl).ignoreContentType(true).execute().body()
                    val json = JSONObject(waterMarkJson)

                    // 1.获取Video标签
                    val videoJson =
                        json.getJSONArray("item_list").getJSONObject(0).getJSONObject("video")
                    // 2.获取封面
                    val cover =
                        videoJson.getJSONObject("cover").getJSONArray("url_list")[0].toString()
                    // 3.获取标题
                    val title =
                        json.getJSONArray("item_list").getJSONObject(0).getString("desc")
                    // 4.获取无水印视频链接,与宽高
                    val waterMarkVideoUrl =
                        videoJson.getJSONObject("play_addr").getJSONArray("url_list")[0].toString()
                    val originVideoUrl = waterMarkVideoUrl.replace("playwm", "play")

                    val dVideo = DVideo(cover, title, originVideoUrl)
                    _video.postValue(dVideo)
                    _loading.postValue(false)
                    _showDownload.postValue(true)
                } catch (exception: IOException) {
                    exception.printStackTrace()
                    _loading.postValue(false)
                }
            }
        }
    }

    /**
     * 从抖音“复制链接”后解析网址
     */
    private fun parseVideoUrlFromClipBoard(clipUrl: String): String {
        val start: Int = clipUrl.indexOf("http")
        val end: Int = clipUrl.lastIndexOf("/")
        return clipUrl.substring(start, end)
    }

    fun doDownload() {
        viewModelScope.launch {
            _loading.value = true
            withContext(Dispatchers.IO) {
                _video.value?.let {
                    try {
                        val response = Retrofitance.downloadApi.download(it.url)
                        downloadVideo(response, it.title)
                        _toast.postValue("视频保存成功")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _toast.postValue("视频下载失败")
                    } finally {
                        _loading.postValue(false)
                    }
                }
            }
        }
    }

    private fun downloadVideo(body: ResponseBody, title: String) {
        val resolver = getApplication<Application>().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.TITLE, title)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/DouYin")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        )

        uri?.let { contentUri ->
            val cr: ContentResolver = getApplication<Application>().contentResolver
            val fd = cr.openFileDescriptor(contentUri, "w")

            val outStream = FileOutputStream(fd?.fileDescriptor) //写
            val inStream: InputStream = body.byteStream()

            var byteRead: Int
            try {
                val buffer = ByteArray(1024)
                while (inStream.read(buffer).also { byteRead = it } != -1) {
                    outStream.write(buffer, 0, byteRead)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                inStream.close()
                outStream.close()
            }

            values.clear()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            resolver.update(uri, values, null, null)
        }

    }

    private fun insertVideo(title: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.TITLE, title)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //this one
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/DouYin")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        return getApplication<Application>().contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        )
    }
}