package com.landofoz.musicmeta.provider.fanarttv

import com.landofoz.musicmeta.EnrichmentData

/** Maps Fanart.tv responses to EnrichmentData subclasses. */
object FanartTvMapper {

    fun toArtwork(url: String): EnrichmentData.Artwork =
        EnrichmentData.Artwork(url = url)
}
