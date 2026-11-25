package com.maxriegel.coolblueglimlachrun.data

import android.content.Context
import android.content.SharedPreferences
import com.maxriegel.coolblueglimlachrun.game.CharacterType
import com.maxriegel.coolblueglimlachrun.game.ScoreEntry

interface ScoreRepository {
    fun getTopScores(limit: Int = 5): List<ScoreEntry>
    fun saveScore(entry: ScoreEntry)
    fun clear()
}

class InMemoryScoreRepository : ScoreRepository {
    private val entries = mutableListOf<ScoreEntry>()

    override fun getTopScores(limit: Int): List<ScoreEntry> {
        return entries
            .sortedWith(compareByDescending<ScoreEntry> { it.score }.thenByDescending { it.smiles })
            .take(limit)
    }

    override fun saveScore(entry: ScoreEntry) {
        entries.add(entry)
    }

    override fun clear() {
        entries.clear()
    }
}

class SharedPreferencesScoreRepository(
    private val prefs: SharedPreferences
) : ScoreRepository {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    override fun getTopScores(limit: Int): List<ScoreEntry> {
        val raw = prefs.getString(KEY_SCORES, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split("\n")
            .mapNotNull { parseLine(it) }
            .sortedWith(compareByDescending<ScoreEntry> { it.score }.thenByDescending { it.smiles })
            .take(limit)
    }

    override fun saveScore(entry: ScoreEntry) {
        val updated = (getTopScores(MAX_STORED) + entry)
            .sortedWith(compareByDescending<ScoreEntry> { it.score }.thenByDescending { it.smiles })
            .take(MAX_STORED)
        val serialized = updated.joinToString("\n") { formatLine(it) }
        prefs.edit().putString(KEY_SCORES, serialized).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_SCORES).apply()
    }

    private fun formatLine(entry: ScoreEntry): String {
        return listOf(
            entry.character.name,
            entry.score.toString(),
            entry.smiles.toString(),
            entry.levelReached.toString(),
            entry.timeSeconds.toString(),
            entry.timestampMs.toString()
        ).joinToString("|")
    }

    private fun parseLine(line: String): ScoreEntry? {
        val parts = line.trim().split("|")
        if (parts.size != 6) return null
        return try {
            ScoreEntry(
                character = CharacterType.valueOf(parts[0]),
                score = parts[1].toInt(),
                smiles = parts[2].toInt(),
                levelReached = parts[3].toInt(),
                timeSeconds = parts[4].toInt(),
                timestampMs = parts[5].toLong()
            )
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_NAME = "coolblue_glimlach_scores"
        private const val KEY_SCORES = "top_scores_v1"
        private const val MAX_STORED = 10
    }
}
