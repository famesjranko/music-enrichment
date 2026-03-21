package com.landofoz.musicmeta.provider.wikipedia

import com.landofoz.musicmeta.EnrichmentData

/** Maps Wikipedia responses to EnrichmentData subclasses. */
object WikipediaMapper {

    fun toBiography(summary: WikipediaSummary): EnrichmentData.Biography =
        EnrichmentData.Biography(
            text = summary.extract,
            source = "Wikipedia",
            thumbnailUrl = summary.thumbnailUrl,
        )
}
