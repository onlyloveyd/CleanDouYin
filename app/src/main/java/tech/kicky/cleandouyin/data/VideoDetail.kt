package tech.kicky.cleandouyin.data

/**
 * Video 详细信息
 * author: yidong
 * 2021/1/30
 */
class VideoDetail : ArrayList<VideoDetailItem>()

data class VideoDetailItem(
    val author: Author,
    val author_user_id: Long,
    val aweme_id: String,
    val aweme_type: Int,
    val cha_list: List<Cha>,
    val comment_list: Any,
    val create_time: Int,
    val desc: String,
    val duration: Int,
    val forward_id: String,
    val geofencing: Any,
    val group_id: Long,
    val image_infos: Any,
    val images: Any,
    val is_live_replay: Boolean,
    val is_preview: Int,
    val label_top_text: Any,
    val long_video: Any,
    val music: Music,
    val promotions: Any,
    val risk_infos: RiskInfos,
    val share_info: ShareInfo,
    val share_url: String,
    val statistics: Statistics,
    val text_extra: List<TextExtra>,
    val video: Video,
    val video_labels: Any,
    val video_text: Any
)

data class Author(
    val avatar_larger: AvatarLarger,
    val avatar_medium: AvatarMedium,
    val avatar_thumb: AvatarThumb,
    val followers_detail: Any,
    val geofencing: Any,
    val nickname: String,
    val platform_sync_info: Any,
    val policy_version: Any,
    val short_id: String,
    val signature: String,
    val type_label: Any,
    val uid: String,
    val unique_id: String
)

data class Cha(
    val cha_name: String,
    val cid: String,
    val connect_music: Any,
    val desc: String,
    val hash_tag_profile: String,
    val is_commerce: Boolean,
    val type: Int,
    val user_count: Int,
    val view_count: Int
)

data class Music(
    val author: String,
    val cover_hd: CoverHd,
    val cover_large: CoverLarge,
    val cover_medium: CoverMedium,
    val cover_thumb: CoverThumb,
    val duration: Int,
    val id: Long,
    val mid: String,
    val play_url: PlayUrl,
    val position: Any,
    val status: Int,
    val title: String
)

data class RiskInfos(
    val content: String,
    val type: Int,
    val warn: Boolean
)

data class ShareInfo(
    val share_desc: String,
    val share_title: String,
    val share_weibo_desc: String
)

data class Statistics(
    val aweme_id: String,
    val comment_count: Int,
    val digg_count: Int,
    val play_count: Int
)

data class TextExtra(
    val end: Int,
    val hashtag_id: Long,
    val hashtag_name: String,
    val start: Int,
    val type: Int
)

data class Video(
    val bit_rate: Any,
    val cover: Cover,
    val duration: Int,
    val dynamic_cover: DynamicCover,
    val has_watermark: Boolean,
    val height: Int,
    val origin_cover: OriginCover,
    val play_addr: PlayAddr,
    val ratio: String,
    val vid: String,
    val width: Int
)

data class AvatarLarger(
    val uri: String,
    val url_list: List<String>
)

data class AvatarMedium(
    val uri: String,
    val url_list: List<String>
)

data class AvatarThumb(
    val uri: String,
    val url_list: List<String>
)

data class CoverHd(
    val uri: String,
    val url_list: List<String>
)

data class CoverLarge(
    val uri: String,
    val url_list: List<String>
)

data class CoverMedium(
    val uri: String,
    val url_list: List<String>
)

data class CoverThumb(
    val uri: String,
    val url_list: List<String>
)

data class PlayUrl(
    val uri: String,
    val url_list: List<String>
)

data class Cover(
    val uri: String,
    val url_list: List<String>
)

data class DynamicCover(
    val uri: String,
    val url_list: List<String>
)

data class OriginCover(
    val uri: String,
    val url_list: List<String>
)

data class PlayAddr(
    val uri: String,
    val url_list: List<String>
)