package com.landofoz.musicmeta.provider.fanarttv

data class FanartTvImage(
    val url: String,
    val id: String? = null,
    val likes: Int = 0,
)

data class FanartTvArtistImages(
    val thumbnails: List<FanartTvImage>,
    val backgrounds: List<FanartTvImage>,
    val logos: List<FanartTvImage>,
    val banners: List<FanartTvImage>,
    val albumCovers: List<FanartTvImage> = emptyList(),
    val cdArt: List<FanartTvImage> = emptyList(),
)

/** Album-specific images from the Fanart.tv album endpoint (/v3/music/albums/{releaseGroupMbid}). */
data class FanartTvAlbumImages(
    val albumCovers: List<FanartTvImage>,
    val cdArt: List<FanartTvImage>,
)
