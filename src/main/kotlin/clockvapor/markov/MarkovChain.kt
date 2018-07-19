package clockvapor.markov

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.util.*

// Needs to remain public for JSON writing
@Suppress("MemberVisibilityCanBePrivate")
open class MarkovChain(val data: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()) {
    @JsonIgnore
    var random = Random()

    /** Generates a list of words with the "empty" starting seed. */
    fun generate(): List<String> = getWeightedRandomWord(EMPTY)?.let { seed ->
        val result = generateWithSeed(seed)
        when (result) {
            is GenerateWithSeedResult.Success -> result.message
            is GenerateWithSeedResult.NoSuchSeed -> null
        }
    }.orEmpty()

    /** Generates a list of words starting with the given seed. */
    fun generateWithSeed(seed: String): GenerateWithSeedResult =
        if (data.contains(seed)) {
            val result = mutableListOf<String>()
            var word: String? = seed
            while (word != null && word != EMPTY) {
                result += word
                word = getWeightedRandomWord(word)
            }
            GenerateWithSeedResult.Success(result)
        } else {
            GenerateWithSeedResult.NoSuchSeed()
        }

    /** Generates a list of words starting with the given (case-insensitive) seed. */
    fun generateWithCaseInsensitiveSeed(seed: String): GenerateWithSeedResult =
        data.filterKeys { it.equals(seed, ignoreCase = true) }
            .mapValues { it.value.values.sum() }
            .getWeightedRandomKey(random)?.let(::generateWithSeed) ?: GenerateWithSeedResult.NoSuchSeed()

    /** Adds a list of words to the Markov chain. */
    fun add(words: List<String>) {
        if (words.isNotEmpty()) {
            addPair(EMPTY, words.first())
            for (i in 0 until words.size - 1) {
                addPair(words[i], words[i + 1])
            }
            addPair(words.last(), EMPTY)
        }
    }

    /** Removes a list of words from the Markov chain. */
    fun remove(words: List<String>) {
        if (words.isNotEmpty()) {
            removePair(EMPTY, words.first())
            for (i in 0 until words.size - 1) {
                removePair(words[i], words[i + 1])
            }
            removePair(words.last(), EMPTY)
        }
    }

    /** Clears all data from the Markov chain. */
    fun clear() = data.clear()

    /** Adds a single count to a pair of words in the Markov chain. */
    private fun addPair(a: String, b: String) {
        data.getOrPut(a) { mutableMapOf() }.compute(b) { _, c -> c?.plus(1) ?: 1 }
    }

    /** Removes a single count from a pair of words in the Markov chain. */
    private fun removePair(a: String, b: String) {
        data[a]?.let { wordMap ->
            wordMap.computeIfPresent(b) { _, count -> count - 1 }
            val c = wordMap[b]
            if (c != null && c <= 0) {
                wordMap -= b
                if (wordMap.isEmpty()) {
                    data -= a
                }
            }
        }
    }

    /** Gets a random word following the given word. */
    private fun getWeightedRandomWord(word: String): String? = data[word]?.getWeightedRandomKey(random)

    /** Writes the Markov chain as a JSON file to the given path. */
    fun write(path: String) {
        ObjectMapper().writeValue(File(path), this)
    }

    /** Parent type of all results produced when attempting to generate a message from some seed value. */
    sealed class GenerateWithSeedResult {
        /** Result produced when the given seed is not found in the Markov chain. */
        class NoSuchSeed : GenerateWithSeedResult()

        /** Result produced when a message is successfully generated. */
        class Success(val message: List<String>) : GenerateWithSeedResult()
    }

    companion object {
        const val EMPTY = ""

        /** Reads the Markov chain JSON file at the given path. */
        @Suppress("UNCHECKED_CAST")
        fun read(path: String): MarkovChain =
            ObjectMapper().readValue<MarkovChain>(File(path), MarkovChain::class.java)
    }
}
