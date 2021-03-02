/**
 * TuSDKVideoDemo
 * Constants.java
 *
 * @author Bonan
 * @Date: 2017-5-8 上午10:42:48
 * @Copyright: (c) 2017 tusdk.com. All rights reserved.
 */
package org.lasque.tusdkeditoreasydemo.utils;

import com.tusdk.pulse.editor.ClipLayer;
import com.tusdk.pulse.editor.ClipLayer.Transition;

import org.lasque.tusdkpulse.core.seles.tusdk.FilterGroup;
import org.lasque.tusdkpulse.core.seles.tusdk.FilterLocalPackage;
import org.lasque.tusdkpulse.core.seles.tusdk.FilterOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.tusdk.pulse.editor.ClipLayer.Transition.*;

/**
 *
 */
public class Constants {
    /**
     * 最大录制时长 (单位：秒)
     */
    public static final int MAX_RECORDING_TIME = 15;

    /**
     * 最小录制时长 (单位：秒)
     */
    public static final int MIN_RECORDING_TIME = 3;

    /** 最大合成数 (单位：个) */
    public static final int MAX_EDITOR_SELECT_MUN = 9;


    public static String TTF_KEY = "TTF-Path";


    /** 配音code列表 **/
    public static String[] AUDIO_EFFECTS_CODES = new String[]{"none","record","lively", "oldmovie", "relieve"};

    /**
     * 漫画滤镜 filterCode 列表
     */
    public static String[] COMICSFILTERS = {"None","CHComics_Video","USComics_Video","JPComics_Video","Lightcolor_Video","Ink_Video","Monochrome_Video"};
    /**
     * 滤镜 filterCode 列表
     */
    public static String[] VIDEOFILTERS = {"None", "SkinNature10","SkinPink10", "SkinJelly10", "SkinNoir10", "SkinRuddy10",
            "SkinSugar10", "SkinPowder10", "SkinWheat10","SkinSoft10","SkinPure10","SkinMoving10","SkinPast10","SkinCookies10",
            "SkinRose10"};


    /**
     * @param hasCantoon
     * @return
     */
    public static List<FilterGroup> getCameraFilters(boolean hasCantoon){
        List<FilterGroup> filterGroup = FilterLocalPackage.shared().getGroups();
        List<FilterGroup> result = new ArrayList<>();

        for (FilterGroup group : filterGroup){
            if (group.groupFiltersType == 0){
//                Collections.sort(group.filters, new Comparator<FilterOption>() {
//                    @Override
//                    public int compare(FilterOption o1, FilterOption o2) {
//                        return Long.compare(o1.id, o2.id);
//                    }
//                });
                result.add(group);
            }
        }

        Collections.sort(result, new Comparator<FilterGroup>() {
            @Override
            public int compare(FilterGroup o1, FilterGroup o2) {
                return Long.compare(o1.groupId, o2.groupId);
            }
        });

        if (hasCantoon){
            FilterGroup cartoon = FilterLocalPackage.shared().getFilterGroup(252);
            result.add(cartoon);
        }
        return result;
    }

    public static List<FilterOption> getEditorFilters(){
        List<FilterGroup> filterGroup = FilterLocalPackage.shared().getGroups();
        List<FilterGroup> result = new ArrayList<>();

        for (FilterGroup group : filterGroup){
            if (group.groupFiltersType == 0){
//                Collections.sort(group.filters, new Comparator<FilterOption>() {
//                    @Override
//                    public int compare(FilterOption o1, FilterOption o2) {
//                        return Long.compare(o1.id, o2.id);
//                    }
//                });
                result.add(group);
            }
        }

        Collections.sort(result, new Comparator<FilterGroup>() {
            @Override
            public int compare(FilterGroup o1, FilterGroup o2) {
                return Long.compare(o1.groupId, o2.groupId);
            }
        });
        List<FilterOption> options = new ArrayList<>();
        for (FilterGroup group : result){
            options.addAll(FilterLocalPackage.shared().getGroupFilters(group));
        }
        return options;
    }

    /** -----------注意事项：视频录制使用人像美颜滤镜(带有磨皮、大眼、瘦脸)，编辑组件尽量不要使用人像美颜滤镜，会造成视频处理过度，效果更不好，建议使用纯色偏滤镜 ----------------*/
    /**
     * 编辑滤镜 filterCode 列表
     */
    public static String[] EDITORFILTERS = {"None", "Olympus_1", "Leica_1", "Gold_1", "Cheerful_1",
            "White_1", "s1950_1", "Blurred_1", "Newborn_1", "Fade_1", "NewYork_1"};


    /**
     * 场景特效code 列表
     */
    public static String[] SCENE_EFFECT_CODES = {"LiveShake01", "LiveMegrim01", "EdgeMagic01", "LiveFancy01_1", "LiveSoulOut01",
            "LiveSignal01", "LiveLightning01", "LiveXRay01", "LiveHeartbeat01", "LiveMirrorImage01", "LiveSlosh01", "LiveOldTV01"};

    /**
     * 时间特效Code列表
     * @since V3.0.0
     */
    public static String[] TIME_EFFECT_CODES = {"node", "repeated", "slowmotion", "reverse"};

    /**
     * 魔法 Code 列表
     */
    public static String[] PARTICLE_CODES = {"snow01", "Music", "Star", "Love", "Bubbles", "Surprise", "Fireball", "Flower",
            "Magic", "Money", "Burning"};

    public static String[] TRANSITIONS_CODES = {"",
            NAME_Fade,NAME_FadeColor,NAME_WipeLeft,NAME_WipeRight,NAME_WipeUp,NAME_WipeDown,NAME_PullLeft,
            NAME_PullRight,NAME_PullUp,NAME_PullDown,NAME_Swap,NAME_Doorway,NAME_CrossZoom,NAME_CrossWarp,
            NAME_PinWheel,NAME_Radial,NAME_SimpleZoom,NAME_DreamyZoom,NAME_Perlin,NAME_Circle,NAME_CircleClose,NAME_CircleOpen,
            NAME_LinearBlur,NAME_Heart
    };
}