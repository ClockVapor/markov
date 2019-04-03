package clockvapor.markov

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.util.*
import kotlin.collections.HashSet

// Needs to remain public for JSON writing
@Suppress("MemberVisibilityCanBePrivate")
open class MarkovChain(val data: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()) {
    /** A [Random] instance used to generate random content. */
    @JsonIgnore
    var random = Random()

    /** Generates a list of words starting with a random seed. */
    open fun generate(): List<String> = getNextWord(EMPTY)?.let { seed ->
        val result = generateWithSeed(seed)
        when (result) {
            is GenerateWithSeedResult.Success -> result.message
            is GenerateWithSeedResult.NoSuchSeed -> null
        }
    }.orEmpty()

    /** Generates a list of words starting with the given seed. */
    open fun generateWithSeed(seed: String): GenerateWithSeedResult =
        if (data.contains(seed)) {
            val result = mutableListOf<String>()
            var word: String? = seed
            while (word != null && word != EMPTY) {
                result += word
                word = getNextWord(word)
            }
            GenerateWithSeedResult.Success(result)
        } else {
            GenerateWithSeedResult.NoSuchSeed()
        }

    /** Generates a list of words starting with the given (case-insensitive) seed. */
    open fun generateWithCaseInsensitiveSeed(seed: String): GenerateWithSeedResult =
        data.filterKeys { it.equals(seed, ignoreCase = true) }
            .mapValues { it.value.values.sum() }
            .getWeightedRandomKey(random)?.let(::generateWithSeed) ?: GenerateWithSeedResult.NoSuchSeed()

    /**
     * Adds a list of words to the Markov chain. A single count will be given or added to each pair of
     * consecutive words in the list.
     * @see [addPair]
     */
    open fun add(words: List<String>) {
        if (words.isNotEmpty()) {
            addPair(EMPTY, words.first())
            for (i in 0 until words.size - 1) {
                addPair(words[i], words[i + 1])
            }
            addPair(words.last(), EMPTY)
        }
    }

    /**
     * Adds the data contained in a [MarkovChain] to this Markov chain.
     * @see [add]
     * @see [addPair]
     */
    open fun add(markovChain: MarkovChain) {
        for (word in HashSet(markovChain.data.keys)) {
            val dataMap = markovChain.data[word]!!
            for (secondWord in HashSet(dataMap.keys)) {
                addPair(word, secondWord, dataMap[secondWord] ?: 0)
            }
        }
    }

    /**
     * Removes a list of words from the Markov chain. A single count will be removed from each pair of
     * consecutive words in the list.
     * @see [removePair]
     */
    open fun remove(words: List<String>) {
        if (words.isNotEmpty()) {
            removePair(EMPTY, words.first())
            for (i in 0 until words.size - 1) {
                removePair(words[i], words[i + 1])
            }
            removePair(words.last(), EMPTY)
        }
    }

    /**
     * Removes the data contained in a [MarkovChain] from this Markov chain.
     * @see [remove]
     * @see [removePair]
     */
    open fun remove(markovChain: MarkovChain) {
        for (word in HashSet(markovChain.data.keys)) {
            val dataMap = markovChain.data[word]!!
            for (secondWord in HashSet(dataMap.keys)) {
                removePair(word, secondWord, dataMap[secondWord] ?: 0)
            }
        }
    }

    /** Clears all data from the Markov chain. */
    open fun clear(): Unit = data.clear()

    /** Adds a given count to a pair of words in the Markov chain. Returns the new count for the pair. */
    protected open fun addPair(a: String, b: String, amount: Int = 1): Int =
        if (amount < 1) 0
        else data.getOrPut(a) { mutableMapOf() }.compute(b) { _, c -> c?.plus(amount) ?: amount }!!

    /**
     * Removes a given count from a pair of words in the Markov chain. Returns the new count for the pair, or null
     * if the pair does not exist in the Markov chain. If the returned count is less than 1, the pair has been removed
     * from the Markov chain.
     */
    protected open fun removePair(a: String, b: String, amount: Int = 1): Int? =
        if (amount < 1) null
        else
            data[a]?.let { wordMap ->
                wordMap.computeIfPresent(b) { _, count -> count - amount }.also {
                    if (it != null && it <= 0) {
                        wordMap -= b
                        if (wordMap.isEmpty()) {
                            data -= a
                        }
                    }
                }
            }

    /** Gets a random word following the given word. */
    open fun getNextWord(word: String): String? = data[word]?.getWeightedRandomKey(random)

    /** Writes the Markov chain as a JSON file to the given path. */
    open fun write(path: String) {
        ObjectMapper().writeValue(File(path), this)
    }

    /** Parent type of all results produced when attempting to generate a message from some seed value. */
    sealed class GenerateWithSeedResult {
        /** Result produced when a message is successfully generated. */
        class Success(val message: List<String>) : GenerateWithSeedResult()

        /** Result produced when the given seed is not found in the Markov chain. */
        class NoSuchSeed : GenerateWithSeedResult()
    }

    companion object {
        const val EMPTY = ""

        /** Reads the Markov chain JSON file at the given path. */
        @Suppress("UNCHECKED_CAST")
        fun read(path: String): MarkovChain =
            ObjectMapper().readValue<MarkovChain>(File(path), MarkovChain::class.java)
    }
}
