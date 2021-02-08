package tech.kicky.cleandouyin.viewmodel

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


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
                } catch (exception: Exception) {
                    exception.printStackTrace()
                    _loading.postValue(false)
                    _toast.postValue("请输入正确的抖音视频地址")
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
                        if (downloadVideo(response, it.title)) {
                            _toast.postValue("视频保存成功")
                        } else {
                            _toast.postValue("视频保存失败")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _toast.postValue("视频保存失败")
                    } finally {
                        _loading.postValue(false)
                    }
                }
            }
        }
    }

    private fun getUriFromName(videoName: String): Uri? {
        val contentResolver = getApplication<Application>().contentResolver
        val uri: Uri?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.TITLE, "$videoName.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_MOVIES + "/DouYin"
                    )
//                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            uri = contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values
            )
        } else {
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            if (!path.exists()) {
                path.mkdirs()
            }
            val pathStr = path.absolutePath + "/DouYin"
            val file = File(pathStr)
            if (!file.exists()) {
                file.mkdirs()
            }
            val videoPath = pathStr + File.separator + videoName + ".mp4"
            val values = ContentValues()
            values.put(MediaStore.Video.Media.DISPLAY_NAME, videoName)
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            values.put(MediaStore.Video.Media.DATA, videoPath)
            values.put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        }
        return uri
    }

    private fun downloadVideo(body: ResponseBody, title: String): Boolean {
        val contentResolver = getApplication<Application>().contentResolver
        val uri = getUriFromName(title)
        uri?.let { localUri ->
            val fileDescriptor: ParcelFileDescriptor? =
                contentResolver.openFileDescriptor(localUri, "w")
            val inStream: InputStream = body.byteStream()
            val outStream = FileOutputStream(fileDescriptor?.fileDescriptor)
            try {
                outStream.use { outPut ->
                    var read: Int
                    val buffer = ByteArray(2048)
                    while (inStream.read(buffer).also { read = it } != -1) {
                        outPut.write(buffer, 0, read)
                    }
                }
                return true
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                inStream.close()
                outStream.close()
            }
            return false
        }
        return false
    }
}