package com.glowplay.player

import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import com.glowplay.player.enhance.FilmLook
import org.junit.Test

class FilmLookTest {

    @Test
    fun `none keeps the grade and clears film`() {
        val recipe = FilmLook.NONE.recipe()
        assertThat(recipe.grade).isNull()
        assertThat(recipe.film.isIdentity).isTrue()
    }

    @Test
    fun `noir is a high-contrast desaturated look with heavy grain`() {
        val recipe = FilmLook.NOIR.recipe()
        val grade = requireNotNull(recipe.grade)
        assertThat(grade.enabled).isTrue()
        assertThat(grade.contrast).isGreaterThan(0.2f)
        assertThat(grade.saturation).isLessThan(-0.5f)
        assertThat(recipe.film.vignette).isGreaterThan(0.3f)
        assertThat(recipe.film.grain).isGreaterThan(0.3f)
    }

    @Test
    fun `teal pushes the grade toward cool tones`() {
        val grade = requireNotNull(FilmLook.TEAL.recipe().grade)
        assertThat(grade.warmth).isLessThan(0f)
    }

    @Test
    fun `fade lifts blacks by lowering contrast`() {
        val grade = requireNotNull(FilmLook.FADE.recipe().grade)
        assertThat(grade.contrast).isLessThan(0f)
        assertThat(grade.saturation).isLessThan(0f)
    }

    @Test
    fun `vintage warms the grade`() {
        val grade = requireNotNull(FilmLook.VINTAGE.recipe().grade)
        assertThat(grade.warmth).isGreaterThan(0.2f)
    }

    @Test
    fun `every look recipe stays within slider bounds after clamping`() {
        FilmLook.entries.forEach { look ->
            val recipe = look.recipe()
            recipe.grade?.let { grade ->
                val clamped = grade.clamped()
                assertThat(clamped.brightness).isIn(Range.closed(-1f, 1f))
                assertThat(clamped.contrast).isIn(Range.closed(-1f, 1f))
                assertThat(clamped.saturation).isIn(Range.closed(-1f, 1f))
                assertThat(clamped.warmth).isIn(Range.closed(-1f, 1f))
                assertThat(clamped.glow).isIn(Range.closed(0f, 1f))
            }
            val film = recipe.film
            assertThat(film.sharpen).isIn(Range.closed(0f, 1f))
            assertThat(film.vignette).isIn(Range.closed(0f, 1f))
            assertThat(film.grain).isIn(Range.closed(0f, 1f))
        }
    }

    @Test
    fun `fromKey round-trips and falls back to none`() {
        FilmLook.entries.forEach { look ->
            assertThat(FilmLook.fromKey(look.storageKey)).isEqualTo(look)
        }
        assertThat(FilmLook.fromKey("does-not-exist")).isEqualTo(FilmLook.NONE)
        assertThat(FilmLook.fromKey(null)).isEqualTo(FilmLook.NONE)
    }

    @Test
    fun `filmFx matches the recipe film`() {
        FilmLook.entries.forEach { look ->
            assertThat(look.filmFx()).isEqualTo(look.recipe().film)
        }
    }
}
