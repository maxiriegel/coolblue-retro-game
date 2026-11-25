package com.maxriegel.coolblueglimlachrun.data

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxriegel.coolblueglimlachrun.game.CharacterType
import com.maxriegel.coolblueglimlachrun.game.ScoreEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class ScoreRepositoryTest {

    private lateinit var repository: SharedPreferencesScoreRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        repository = SharedPreferencesScoreRepository(context)
        repository.clear()
    }

    @Test
    fun startsEmptyWithNoFakeData() {
        assertTrue(repository.getTopScores().isEmpty())
    }

    @Test
    fun persistsAndSortsMaxRemiAndVeroScoresDescending() {
        repository.saveScore(ScoreEntry(CharacterType.MAX, 2400, 18, 2, 54, 1000L))
        repository.saveScore(ScoreEntry(CharacterType.VERO, 3650, 28, 2, 45, 2500L))
        repository.saveScore(ScoreEntry(CharacterType.REMI, 3150, 24, 2, 48, 2000L))

        val top = repository.getTopScores(5)
        assertEquals(3, top.size)
        assertEquals(CharacterType.VERO, top[0].character)
        assertEquals(3650, top[0].score)
        assertEquals(CharacterType.REMI, top[1].character)
        assertEquals(3150, top[1].score)
        assertEquals(CharacterType.MAX, top[2].character)
    }
}
