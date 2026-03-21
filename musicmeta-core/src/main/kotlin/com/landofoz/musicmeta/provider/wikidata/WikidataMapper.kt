package com.landofoz.musicmeta.provider.wikidata

import com.landofoz.musicmeta.EnrichmentData

/** Maps Wikidata responses to EnrichmentData subclasses. */
object WikidataMapper {

    fun toArtwork(imageUrl: String): EnrichmentData.Artwork =
        EnrichmentData.Artwork(url = imageUrl)
}
