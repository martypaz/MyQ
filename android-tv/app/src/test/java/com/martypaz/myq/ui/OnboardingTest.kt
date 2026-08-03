package com.martypaz.myq.ui

import com.martypaz.myq.data.prefs.Profile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {

    private fun state(
        firstName: String? = null,
        networkId: Int? = null,
        isProfileLoaded: Boolean = true,
        nameSkipped: Boolean = false,
        regionSkipped: Boolean = false,
    ) = HomeUiState(
        profile = Profile(firstName = firstName, networkId = networkId),
        isProfileLoaded = isProfileLoaded,
        nameSkipped = nameSkipped,
        regionSkipped = regionSkipped,
    )

    @Test
    fun `asks for whichever answer is missing`() {
        val fresh = state()
        assertTrue(fresh.needsName)
        assertTrue(fresh.needsRegion)

        val named = state(firstName = "Marty")
        assertFalse(named.needsName)
        assertTrue(named.needsRegion)

        val located = state(networkId = 64416)
        assertTrue(located.needsName)
        assertFalse(located.needsRegion)
    }

    @Test
    fun `asks for nothing once both are answered`() {
        val known = state(firstName = "Marty", networkId = 64416)
        assertFalse(known.needsName)
        assertFalse(known.needsRegion)
    }

    @Test
    fun `asks nothing until the profile has actually been read`() {
        // Null means "none saved", which before the profile loads is
        // indistinguishable from "not looked yet" — acting on it opened the
        // keyboard at someone the app already knew.
        val loading = state(isProfileLoaded = false)
        assertFalse(loading.needsName)
        assertFalse(loading.needsRegion)
    }

    @Test
    fun `a declined question is not asked again this session`() {
        assertFalse(state(nameSkipped = true).needsName)
        assertFalse(state(regionSkipped = true).needsRegion)
    }

    @Test
    fun `skipping one question still leaves the other asked`() {
        val skippedName = state(nameSkipped = true)
        assertFalse(skippedName.needsName)
        assertTrue(skippedName.needsRegion)
    }
}
