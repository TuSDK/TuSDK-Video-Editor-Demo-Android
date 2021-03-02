/**
 *  TuSDK
 *  PulseDemo$
 *  org.lasque.tusdkeditoreasydemo$
 *  @author  H.ys
 *  @Date    2020/12/14$ 18:02$
 *  @Copyright    (c) 2019 tusdk.com. All rights reserved.
 *
 *
 */
package org.lasque.tusdkeditoreasydemo

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.draft_activity.*
import kotlinx.android.synthetic.main.include_title_layer.*
import org.jetbrains.anko.startActivity
import org.lasque.tusdkpulse.core.utils.TLog
import org.lasque.tusdkeditoreasydemo.base.BaseActivity
import org.lasque.tusdkeditoreasydemo.base.DraftItem
import org.lasque.tusdkeditoreasydemo.base.OnItemClickListener

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/12/14  18:02
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
class DraftActivity : BaseActivity() {

    private var mDraftAdapter: DraftAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.draft_activity)

        lsq_title.setText(FunctionType.DraftList.mTitleId)
        lsq_output_video.visibility = View.GONE
        lsq_back.setOnClickListener {
            finish()
        }

        val sp = getSharedPreferences("Tu-Draft-list", Context.MODE_PRIVATE)
        val gson = Gson()
        val draftListJson = sp.getString(DraftItem.Draft_List_Key,"")

        val type = object : TypeToken<MutableList<DraftItem>>() {}.type
        if (TextUtils.isEmpty(draftListJson)){
            lsq_particle_list_panel.visibility = View.GONE
            lsq_draft_null_slogan.visibility = View.VISIBLE

        } else {
            lsq_draft_null_slogan.visibility = View.GONE
            lsq_particle_list_panel.visibility = View.VISIBLE
            var draftList : MutableList<DraftItem> = gson.fromJson(draftListJson,type)

            val draftAdapter = DraftAdapter(draftList,this)
            draftAdapter.setOnItemClickListener(object : OnItemClickListener<DraftItem, DraftAdapter.DraftViewHolder> {
                override fun onItemClick(pos: Int, holder: DraftAdapter.DraftViewHolder, item: DraftItem) {
                    when(val functionType = FunctionType.values()[item.funcType]){
                        FunctionType.ParticleEffect ->{
                            startActivity<ParticleActivity>(DraftItem.Draft_Path_Key to item.draftPath)
                        }
                        FunctionType.PictureInPicture->{
                            startActivity<ImageStickerActivity>("FunctionType" to FunctionType.PictureInPicture,DraftItem.Draft_Path_Key to item.draftPath)

                        }
                        FunctionType.Text ->{
                            startActivity<TextStickerActivity>(DraftItem.Draft_Path_Key to item.draftPath)

                        }
                        else -> {
                            startActivity<ApiActivity>("function" to functionType,DraftItem.Draft_Path_Key to item.draftPath)

                        }
                    }
                }
            })

            val layoutManager = LinearLayoutManager(this)
            layoutManager.orientation = LinearLayoutManager.VERTICAL
            lsq_draft_list.layoutManager = layoutManager
            lsq_draft_list.adapter = draftAdapter

            mDraftAdapter = draftAdapter
        }

    }

    override fun onResume() {
        super.onResume()
        refreshDraftList()
    }

    private fun refreshDraftList() {
        val sp = getSharedPreferences("Tu-Draft-list", Context.MODE_PRIVATE)
        val gson = Gson()
        val draftListJson = sp.getString(DraftItem.Draft_List_Key, "")
        if (!draftListJson.isNullOrBlank()){
            val type = object : TypeToken<MutableList<DraftItem>>() {}.type
            var draftList: MutableList<DraftItem> = gson.fromJson(draftListJson, type)

            mDraftAdapter?.updateDraftList(draftList)
        }
    }
}