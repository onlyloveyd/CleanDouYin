package tech.kicky.cleandouyin.viewmodel

import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
class MainViewModel : ViewModel() {

    private val videoPrefix = "https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids="
    private val _video = MutableLiveData<DVideo>()
    val video: LiveData<DVideo> = _video

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _toast = MutableLiveData<String>()
    val toast: LiveData<String> = _toast

    private val _showDownload = MutableLiveData(false)
    val showDownload: LiveData<Boolean> = _showDownload

    fun parseVideoUrl(clipUrl: String) {
        viewModelScope.launch {
            _loading.value = true
            _showDownload.value = false
            withContext(Dispatchers.IO) {
                val videoUrl = parseVideoUrlFromClipBoard(clipUrl)
                try {
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
        val inStream: InputStream = body.byteStream()
        var byteRead: Int
        try {
            //封装一个保存文件的路径对象
            val fileSavePath = File(
                Environment.getExternalStorageDirectory().absolutePath.toString() + "/douyin/" + title + ".mp4"
            )
            val fileParent = fileSavePath.parentFile
            if (!fileParent?.exists()!!) {
                fileParent.mkdirs()
            }
            if (fileSavePath.exists()) { //如果文件存在，则删除原来的文件
                fileSavePath.delete()
            }
            //写入文件
            val fs = FileOutputStream(fileSavePath)
            val buffer = ByteArray(1024)
            while (inStream.read(buffer).also { byteRead = it } != -1) {
                fs.write(buffer, 0, byteRead)
            }
            inStream.close()
            fs.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}