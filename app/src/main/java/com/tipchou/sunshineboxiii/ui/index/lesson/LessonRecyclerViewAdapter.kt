package com.tipchou.sunshineboxiii.ui.index.lesson

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.avos.avoscloud.AVAnalytics
import com.avos.avoscloud.AVObject
import com.avos.avoscloud.AVUser
import com.tipchou.sunshineboxiii.R
import com.tipchou.sunshineboxiii.entity.local.DownloadLocal
import com.tipchou.sunshineboxiii.entity.local.FavoriteLocal
import com.tipchou.sunshineboxiii.entity.local.LessonLocal
import com.tipchou.sunshineboxiii.entity.local.RoleLocal
import com.tipchou.sunshineboxiii.support.DaggerMagicBox
import com.tipchou.sunshineboxiii.support.Resource
import com.tipchou.sunshineboxiii.ui.course.CourseActivity
import com.tipchou.sunshineboxiii.ui.index.DownloadHolder
import com.tipchou.sunshineboxiii.ui.index.IndexActivity

/**
 * Created by 邵励治 on 2018/5/8.
 * Perfect Code
 */
class LessonRecyclerViewAdapter(private val activity: IndexActivity, private val fragment: LessonFragment) : RecyclerView.Adapter<LessonRecyclerViewAdapter.ViewHolder>() {
    class ItemData(val LessonLocal: LessonLocal, val editor: Boolean, val download: Boolean)

    private val netStatusLiveData: LiveData<Boolean>
    private val roleLiveData: LiveData<Resource<List<RoleLocal>>>
    private val lessonLiveData: LiveData<Resource<List<LessonLocal>>>
    private val favoriteLiveData: LiveData<Resource<List<FavoriteLocal>>>
    private val downloadedLessonLiveData: LiveData<List<DownloadLocal>>
    private val downloadQueueLiveData: LiveData<HashMap<DownloadHolder, String>>
    private val downloadLesson: (downloadHolder: DownloadHolder) -> Unit

    private val layoutInflater: LayoutInflater = LayoutInflater.from(activity)

    private val itemDataList = ArrayList<ItemData>()
    private val viewModel: LessonViewModel

    init {
        DaggerMagicBox.create().poke(this)
        viewModel = ViewModelProviders.of(activity).get(LessonViewModel::class.java)
        netStatusLiveData = viewModel.getNetStatus()
        netStatusLiveData.observe(activity, Observer { })
        downloadLesson = { viewModel.downloadLesson(it) }
        downloadedLessonLiveData = viewModel.getDownloadedLesson()
        downloadedLessonLiveData.observe(activity, Observer { })
        downloadQueueLiveData = viewModel.getDownloadQueue()
        roleLiveData = viewModel.getRole()
        roleLiveData.observe(activity, Observer { })
        lessonLiveData = viewModel.getLesson()
        lessonLiveData.observe(activity, Observer {
            itemDataList.clear()
            buildLessonList(it)
            notifyDataSetChanged()
            showNoDataHint(it)
        })
        favoriteLiveData = viewModel.getFavorite()
        favoriteLiveData.observe(activity, Observer {

        })
    }

    private fun showNoDataHint(it: Resource<List<LessonLocal>>?) {
        if (it?.status == Resource.Status.SUCCESS || it?.status == Resource.Status.ERROR) {
            if (itemDataList.size == 0) {
                fragment.showNoDataHint(true)
            } else {
                fragment.showNoDataHint(false)
            }
        }
    }

    private fun buildLessonList(it: Resource<List<LessonLocal>>?) {
        if (it?.status == Resource.Status.SUCCESS || it?.status == Resource.Status.ERROR || it?.status == Resource.Status.LOADING) {
            val roleList: List<RoleLocal>? = roleLiveData.value?.data
            val downloadedLessonList: List<DownloadLocal>? = downloadedLessonLiveData.value
            val lessonList = it.data
            if (roleList == null) {
                //should not be here
            } else {
                if (downloadedLessonList == null) {
                    //should not be here
                } else {
                    val isEditor = isUserEditor(roleList)
                    if (isEditor) {
                        if (lessonList == null) {
                            //should not be here
                        } else {
                            for (lesson in lessonList) {
                                if (lesson.areChecked == 1) {
                                    var download = false
                                    for (downloadedLesson in downloadedLessonList) {
                                        if (downloadedLesson.objectId == lesson.objectId) {
                                            if (downloadedLesson.stagingUrl != null) {
                                                download = true
                                            }
                                        }
                                    }
                                    itemDataList.add(ItemData(lesson, true, download))
                                }
                            }
                            for (lesson in lessonList) {
                                if (lesson.isPublish == true) {
                                    var download = false
                                    for (downloadedLesson in downloadedLessonList) {
                                        if (downloadedLesson.objectId == lesson.objectId) {
                                            if (downloadedLesson.publishedUrl != null) {
                                                download = true
                                            }
                                        }
                                    }
                                    itemDataList.add(ItemData(lesson, false, download))
                                }
                            }
                        }
                    } else {
                        if (lessonList == null) {
                            //should not be here
                        } else {
                            for (lesson in lessonList) {
                                if (lesson.isPublish == true) {
                                    var download = false
                                    for (downloadedLesson in downloadedLessonList) {
                                        if (downloadedLesson.objectId == lesson.objectId) {
                                            if (downloadedLesson.publishedUrl != null) {
                                                download = true
                                            }
                                        }
                                    }
                                    itemDataList.add(ItemData(lesson, false, download))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isUserEditor(roleList: List<RoleLocal>): Boolean {
        var isEditor = false
        for (role in roleList) {
            when (role.name) {
                "admin" -> isEditor = true
                "admin1" -> isEditor = true
                "admin2" -> isEditor = true
            }
        }
        return isEditor
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(layoutInflater.inflate(R.layout.item_index_recyclerview, parent, false))

    override fun getItemCount(): Int = itemDataList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(itemData = itemDataList[position])

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        private val favoriteImageView: ImageView
        private val backgroundImageView: ImageView
        private val isAuditedImageView: ImageView
        private val fileDownloadImageView: ImageView
        private val downloadStatusTextView: TextView
        private val lessonNameTextView: TextView

        //useful data
        private var lesson: LessonLocal? = null
        private var editor: Boolean? = null
        private var download: Boolean? = null

        init {
            itemView.setOnClickListener(this)
            favoriteImageView = itemView.findViewById(R.id.index_rcv_imageview4)
            backgroundImageView = itemView.findViewById(R.id.index_rcv_imageview1)
            isAuditedImageView = itemView.findViewById(R.id.index_rcv_imageview2)
            downloadStatusTextView = itemView.findViewById(R.id.index_rcv_textview1)
            lessonNameTextView = itemView.findViewById(R.id.index_rcv_textview2)
            fileDownloadImageView = itemView.findViewById(R.id.index_rcv_imageview3)
        }

        fun bind(itemData: ItemData?) {
            getUsefulData(itemData)
            val (editor, lesson, download) = getValueNotNull()
            setUpLessonName()
            setUpEditorTip(editor)
            setUpBackground(download, lesson)
            setUpFavorite(lesson)
            observerDownload(editor, lesson)
        }

        private fun setUpFavorite(lesson: LessonLocal) {
            favoriteImageView.visibility = View.GONE
            for (favorite in favoriteLiveData.value?.data!!) {
                if (favorite.lessonId == lesson.objectId) {
                    when (favorite.action) {
                        true -> favoriteImageView.visibility = View.VISIBLE
                        false -> favoriteImageView.visibility = View.GONE
                    }
                    break
                }
            }
        }

        fun bind(lesson: LessonLocal, editor: Boolean, download: Boolean) {
            this.lesson = lesson
            this.editor = editor
            this.download = download
            setUpLessonName()
            setUpEditorTip(editor)
            setUpBackground(download, lesson)
            setUpFavorite(lesson)
            observerDownload(editor, lesson)
        }

        private fun observerDownload(editor: Boolean, lesson: LessonLocal) {
            val (isDownloading, myDownloadHolder: DownloadHolder?) = isDownloading(editor, lesson)
            if (isDownloading && myDownloadHolder != null) {
                downloadQueueLiveData.observe(activity, object : Observer<HashMap<DownloadHolder, String>> {
                    override fun onChanged(t: HashMap<DownloadHolder, String>?) {
                        if (t != null) {
                            var isMyHolderInMap = false
                            for (downloadHolder in t.keys) {
                                if (downloadHolder == myDownloadHolder) {
                                    isMyHolderInMap = true
                                    break
                                }
                            }
                            if (isMyHolderInMap) {
                                downloadStatusTextView.text = t[myDownloadHolder]
                            } else {
                                downloadQueueLiveData.removeObserver(this)
                                bind(lesson, editor, true)
                            }
                        } else {
                            //should not be here!!!
                        }
                    }
                })
            }
        }

        private fun isDownloading(editor: Boolean, lesson: LessonLocal): Pair<Boolean, DownloadHolder?> {
            val downloadHolderMap = downloadQueueLiveData.value
            var isDownloading = false
            var myDownloadHolder: DownloadHolder? = null
            if (downloadHolderMap != null) {
                for (downloadHolder in downloadHolderMap.keys) {
                    if (downloadHolder.editor == editor && downloadHolder.lessonObjectId == lesson.objectId) {
                        isDownloading = true
                        myDownloadHolder = downloadHolder
                        break
                    }
                }
            } else {
                //should not be here
            }
            return Pair(isDownloading, myDownloadHolder)
        }

        private fun setUpBackground(download: Boolean, lesson: LessonLocal) {
            if (download) {
                fileDownloadImageView.visibility = View.GONE
                downloadStatusTextView.text = ""
                when (lesson.subject) {
                    "NURSERY" -> backgroundImageView.setBackgroundResource(R.drawable.nursery)
                    "MUSIC" -> backgroundImageView.setBackgroundResource(R.drawable.music)
                    "READING" -> backgroundImageView.setBackgroundResource(R.drawable.reading)
                    "GAME" -> backgroundImageView.setBackgroundResource(R.drawable.game)
                }
            } else {
                fileDownloadImageView.visibility = View.VISIBLE
                downloadStatusTextView.text = "点击下载"
                when (lesson.subject) {
                    "NURSERY" -> backgroundImageView.setBackgroundResource(R.drawable.nursery_gray)
                    "MUSIC" -> backgroundImageView.setBackgroundResource(R.drawable.music_gray)
                    "READING" -> backgroundImageView.setBackgroundResource(R.drawable.reading_gray)
                    "GAME" -> backgroundImageView.setBackgroundResource(R.drawable.game_gray)
                }
            }
        }

        private fun getValueNotNull(): Triple<Boolean, LessonLocal, Boolean> {
            val editor = this.editor
                    ?: throw Exception("LessonRecyclerViewAdapter's bind() editor is null")
            val lesson = this.lesson
                    ?: throw Exception("LessonRecyclerViewAdapter's bind() lesson is null")
            val download = this.download
                    ?: throw Exception("LessonRecyclerViewAdapter's bind() download is null")
            return Triple(editor, lesson, download)
        }

        private fun setUpEditorTip(editor: Boolean) {
            when (editor) {
                true -> {
                    isAuditedImageView.visibility = View.VISIBLE
                }
                false -> {
                    isAuditedImageView.visibility = View.GONE
                }
            }
        }

        private fun getUsefulData(itemData: ItemData?) {
            lesson = itemData?.LessonLocal
            editor = itemData?.editor
            download = itemData?.download
        }

        private fun setUpLessonName() {
            lessonNameTextView.text = lesson?.name
        }

        override fun onClick(v: View?) {
            if (netStatusLiveData.value!!) {
                //当前网络可用
                if (download!!) {
                    //如果已下载
                    when (editor) {
                        true -> {
                            for (downloadedLesson in downloadedLessonLiveData.value!!) {
                                if (downloadedLesson.objectId == lesson?.objectId) {
                                    if (downloadedLesson.stagingUrl != null) {
                                        AVAnalytics.onEvent(activity, "单个课程被打开次数", lesson?.name)
                                        AVAnalytics.onEvent(activity, "打开课程总数")
                                        AVAnalytics.onEvent(activity, "用户打开课程数", AVUser.getCurrentUser().username + ":" + AVUser.getCurrentUser().mobilePhoneNumber)
                                        AVAnalytics.onEvent(activity, "类目下课程打开数", lesson?.subject)
                                        val accessRecord = AVObject("UserAction")
                                        accessRecord.put("userId", AVUser.getCurrentUser().objectId)
                                        accessRecord.put("lessonId", lesson?.objectId)
                                        accessRecord.put("behaviorType", "openLesson")
                                        accessRecord.put("equipment", "androidApp")
                                        accessRecord.saveInBackground()
                                        activity.startActivity(CourseActivity.newIntent(activity, lesson?.objectId!!, downloadedLesson.stagingUrl!!))
                                    } else {
                                        throw Exception("FUCK")
                                    }
                                }
                            }

                        }
                        false -> {
                            for (downloadedLesson in downloadedLessonLiveData.value!!) {
                                if (downloadedLesson.objectId == lesson?.objectId) {
                                    if (downloadedLesson.publishedUrl != null) {
                                        AVAnalytics.onEvent(activity, "单个课程被打开次数", lesson?.name)
                                        AVAnalytics.onEvent(activity, "打开课程总数")
                                        AVAnalytics.onEvent(activity, "用户打开课程数", AVUser.getCurrentUser().username + ":" + AVUser.getCurrentUser().mobilePhoneNumber)
                                        AVAnalytics.onEvent(activity, "类目下课程打开数", lesson?.subject)
                                        activity.startActivity(CourseActivity.newIntent(activity, lesson?.objectId!!, downloadedLesson.publishedUrl!!))
                                    } else {
                                        throw Exception("FUCK")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val (isDownloading, myDownloadHolder: DownloadHolder?) = isDownloading(editor!!, lesson!!)
                    if (isDownloading && myDownloadHolder != null) {
                        activity.showSnackBar("该课程正在下载队列中，请稍等片刻")
                    } else {
                        //如果没下载
                        fileDownloadImageView.visibility = View.GONE
                        downloadLesson(DownloadHolder(lessonObjectId = lesson!!.objectId, editor = editor!!, downloadUrl = if (editor!!) {
                            lesson!!.stagingPackageUrl!!
                        } else {
                            lesson!!.packageUrl!!
                        }))
                        observerDownload(editor!!, lesson!!)
                    }
                }
            } else {
                if (download!!) {
                    //如果已下载
                    when (editor) {
                        true -> {
                            for (downloadedLesson in downloadedLessonLiveData.value!!) {
                                if (downloadedLesson.objectId == lesson?.objectId) {
                                    if (downloadedLesson.stagingUrl != null) {
                                        AVAnalytics.onEvent(activity, "单个课程被打开次数", lesson?.name)
                                        AVAnalytics.onEvent(activity, "打开课程总数")
                                        AVAnalytics.onEvent(activity, "用户打开课程数", AVUser.getCurrentUser().username + ":" + AVUser.getCurrentUser().mobilePhoneNumber)
                                        AVAnalytics.onEvent(activity, "类目下课程打开数", lesson?.subject)

                                        activity.startActivity(CourseActivity.newIntent(activity, lesson?.objectId!!, downloadedLesson.stagingUrl!!))
                                    } else {
                                        throw Exception("FUCK")
                                    }
                                }
                            }

                        }
                        false -> {
                            for (downloadedLesson in downloadedLessonLiveData.value!!) {
                                if (downloadedLesson.objectId == lesson?.objectId) {
                                    if (downloadedLesson.publishedUrl != null) {
                                        AVAnalytics.onEvent(activity, "单个课程被打开次数", lesson?.name)
                                        AVAnalytics.onEvent(activity, "打开课程总数")
                                        AVAnalytics.onEvent(activity, "用户打开课程数", AVUser.getCurrentUser().username + ":" + AVUser.getCurrentUser().mobilePhoneNumber)
                                        AVAnalytics.onEvent(activity, "类目下课程打开数", lesson?.subject)

                                        activity.startActivity(CourseActivity.newIntent(activity, lesson?.objectId!!, downloadedLesson.publishedUrl!!))
                                    } else {
                                        throw Exception("FUCK")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    activity.showSnackBar("当前网络不可用，请连接网络后下载")
                }
            }
        }
    }
}
