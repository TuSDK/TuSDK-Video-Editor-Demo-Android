package org.lasque.tusdkeditoreasydemo

/**
 * TuSDK
 * org.lasque.tusdkeditoreasydemo.base
 * PulseDemo
 *
 * @author        H.ys
 * @Date        2020/10/22  11:52
 * @Copyright    (c) 2020 tusdk.com. All rights reserved.
 *
 */
enum class FunctionType(titleId : Int) {

    Null(-1),
    VideoStitching(R.string.lsq_function_video_stitcing),
    VideoSegmentation(R.string.lsq_function_video_segmentation),
    VideoImageStitching(R.string.lsq_function_video_image_stitcing),
    ImageStitching(R.string.lsq_function_image_stitcing),
    VideoAudioMix(R.string.lsq_function_video_audio_mix),
    AudioMix(R.string.lsq_function_audio_mix),
    FilterEffect(R.string.lsq_function_video_filter_effect),
    MVEffect(R.string.lsq_function_video_mv_effect),
    SceneEffect(R.string.lsq_function_video_scene_effect),
    ReverseEffect(R.string.lsq_function_video_reverse_effect),
    SlowEffect(R.string.lsq_function_video_slow_effect),
    RepeatEffect(R.string.lsq_function_video_repeat_effect),
    TransitionsEffect(R.string.lsq_function_video_transitions_effect),
    VideoRatio(R.string.lsq_function_video_ratio),
    ParticleEffect(R.string.lsq_function_particle_effect),
    PictureInPicture(R.string.lsq_function_picture_in_picture),
    VideoWithImage(R.string.lsq_function_video_with_image),
    VideoWithVideo(R.string.lsq_function_video_with_video),
    ImageWithImage(R.string.lsq_function_image_with_image),
    ImageWithVideo(R.string.lsq_function_image_with_video),
    Text(R.string.lsq_function_text),
    Bubble(R.string.lsq_function_bubble),
    Cover(R.string.lsq_function_cover),
    Speed(R.string.lsq_function_speed),
    MoiveCut(R.string.lsq_function_movie_cuter),
    ColorAdjust(R.string.lsq_function_color_adjust),
    Crop(R.string.lsq_function_crop),
    Transform(R.string.lsq_function_transform),
    CanvasBackgroundType(R.string.lsq_function_canvas_background),
    DraftList(R.string.lsq_function_draft_list),
    AudioPitch(R.string.lsq_function_audio_patch),
    Graffiti(R.string.lsq_function_graffiti),
    Freeze(R.string.lsq_function_freeze),
    Mosaic(R.string.lsq_function_mosaic)
    ;

    val mTitleId : Int = titleId

}