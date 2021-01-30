package tech.kicky.cleandouyin

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cn.jzvd.Jzvd
import coil.load
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import tech.kicky.cleandouyin.databinding.ActivityMainBinding
import tech.kicky.cleandouyin.viewmodel.MainViewModel

@InternalCoroutinesApi
@FlowPreview
@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {

    private val mBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val mainViewModel by viewModels<MainViewModel>()

    private fun TextView.textWatcherFlow(): Flow<String> = callbackFlow<String> {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                Log.d("Flow", "afterTextChanged s =  ${s.toString()}")
                offer(s.toString())
            }
        }
        addTextChangedListener(textWatcher)
        awaitClose { removeTextChangedListener(textWatcher) }
    }.buffer(Channel.CONFLATED)
        .debounce(300L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        mainViewModel.video.observe(this, {
            mBinding.video.setUp(it.url, it.title)
            mBinding.video.posterImageView.scaleType = ImageView.ScaleType.FIT_CENTER
            mBinding.video.posterImageView.load(it.cover)
            mBinding.video.fullscreenButton.visibility = View.GONE
            mBinding.video.startVideo()
        })

        mainViewModel.loading.observe(this, {
            if (it) {
                mBinding.spinKit.visibility = View.VISIBLE
                mBinding.parse.isClickable = false
            } else {
                mBinding.spinKit.visibility = View.INVISIBLE
                mBinding.parse.isClickable = true
            }
        })

        mainViewModel.toast.observe(this, {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        })

        mainViewModel.showDownload.observe(this, {
            if (it) {
                mBinding.download.visibility = View.VISIBLE
            } else {
                mBinding.download.visibility = View.INVISIBLE
            }
        })

        lifecycleScope.launchWhenStarted {
            mBinding.etUrl.textWatcherFlow().collect {
                if (it.isEmpty()) {
                    mBinding.clear.visibility = View.GONE
                } else {
                    mBinding.clear.visibility = View.VISIBLE
                }
            }
        }
    }

    fun doParse(view: View) {
        hideKeyBoard(view)
        val clipUrl = mBinding.etUrl.text.toString()
        if (clipUrl.isEmpty()) {
            Toast.makeText(this, "请输入正确的抖音链接", Toast.LENGTH_SHORT).show()
        } else {
            mainViewModel.parseVideoUrl(clipUrl)
        }
    }

    private fun hideKeyBoard(v: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }

    fun doDownload(view: View) {
        mainViewModel.doDownload()
    }

    override fun onPause() {
        super.onPause()
        Jzvd.releaseAllVideos()
    }

    fun doClear(view: View) {
        mBinding.etUrl.setText("")
    }
}