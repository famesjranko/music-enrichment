package com.landofoz.musicmeta.provider.lastfm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LastFmMapperGenreTest {

    @Test
    fun `toGenre populates genreTags with 0_3f confidence and lastfm source`() {
        // Given
        val tags = listOf("indie", "rock", "alternative")

        // When
        val metadata = LastFmMapper.toGenre(tags)

        // Then
        val genreTags = metadata.genreTags
        assertTrue(genreTags != null)
        assertEquals(3, genreTags!!.size)
        assertEquals("indie", genreTags[0].name)
        assertEquals(0.3f, genreTags[0].confidence)
        assertEquals(listOf("lastfm"), genreTags[0].sources)
        assertEquals("rock", genreTags[1].name)
        assertEquals(0.3f, genreTags[1].confidence)
        assertEquals(listOf("lastfm"), genreTags[1].sources)
        assertEquals("alternative", genreTags[2].name)
        assertEquals(0.3f, genreTags[2].confidence)
    }

    @Test
    fun `toGenre still populates genres for backward compatibility`() {
        // Given
        val tags = listOf("jazz", "soul")

        // When
        val metadata = LastFmMapper.toGenre(tags)

        // Then
        assertEquals(listOf("jazz", "soul"), metadata.genres)
    }

    @Test
    fun `toGenre returns null genreTags for empty tag list`() {
        // Given
        val tags = emptyList<String>()

        // When
        val metadata = LastFmMapper.toGenre(tags)

        // Then
        assertNull(metadata.genreTags)
    }

    @Test
    fun `toGenre returns empty genres for empty tag list`() {
        // Given
        val tags = emptyList<String>()

        // When
        val metadata = LastFmMapper.toGenre(tags)

        // Then
        // genres was previously `tags` directly, so empty list maps to empty genres
        // The existing contract: toGenre(emptyList()) returns Metadata(genres = emptyList())
        // but that's non-null empty list. Let's check what the original did:
        // fun toGenre(tags: List<String>): EnrichmentData.Metadata = EnrichmentData.Metadata(genres = tags)
        // So with emptyList(), genres = emptyList() (not null).
        assertEquals(emptyList<String>(), metadata.genres)
    }

    @Test
    fun `toGenre with single tag produces one genreTag`() {
        // Given
        val tags = listOf("classical")

        // When
        val metadata = LastFmMapper.toGenre(tags)

        // Then
        val genreTags = metadata.genreTags
        assertTrue(genreTags != null)
        assertEquals(1, genreTags!!.size)
        assertEquals("classical", genreTags[0].name)
        assertEquals(0.3f, genreTags[0].confidence)
        assertEquals(listOf("lastfm"), genreTags[0].sources)
    }
}
