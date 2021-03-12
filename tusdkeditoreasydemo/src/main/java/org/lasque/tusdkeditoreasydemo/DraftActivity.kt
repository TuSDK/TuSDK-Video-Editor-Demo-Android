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
import org.lasque.tusdkeditoreasydemo.base.OnItemDeleteClickListener

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

            draftAdapter.setOnItemDeleteClickListener(object : OnItemDeleteClickListener<DraftItem,DraftAdapter.DraftViewHolder>{
                override fun onItemDelete(pos: Int, holder: DraftAdapter.DraftViewHolder, item: DraftItem) {
                    draftList.removeAt(pos)
                    draftAdapter.updateDraftList(draftList)


                    if (draftList.isEmpty()){
                        lsq_particle_list_panel.visibility = View.GONE
                        lsq_draft_null_slogan.visibility = View.VISIBLE
                        sp.edit().remove(DraftItem.Draft_List_Key).apply()

                    } else{
                        val draftString = gson.toJson(draftList)
                        sp.edit().putString(DraftItem.Draft_List_Key,draftString).apply()
                    }
                }

            })

            val layoutManager = LinearLayoutManager(this)
            layoutManager.orientation = LinearLayoutManager.VERTICAL
            lsq_draft_list.layoutManager = layoutManager
            lsq_draft_list.adapter = draftAdapter

            mDraftAdapter = draftAdapter
        }

        lsq_title.setText(FunctionType.DraftList.mTitleId)
        lsq_output_video.text = "删除全部草稿"
        lsq_output_video.setOnClickListener {
            sp.edit().remove(DraftItem.Draft_List_Key).apply()
            lsq_particle_list_panel.visibility = View.GONE
            lsq_draft_null_slogan.visibility = View.VISIBLE
        }
        lsq_output_video.visibility = View.VISIBLE
        lsq_back.setOnClickListener {
            finish()
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