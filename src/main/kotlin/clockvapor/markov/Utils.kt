package clockvapor.markov

import java.util.*

/**
 * Gets a weighted random key, where the map is formatted with each key mapping to its "weight" value.
 * Returns null if the map is empty.
 */
fun <T> Map<T, Int>.getWeightedRandomKey(random: Random): T? {
    val total = values.sum()
    if (total > 0) {
        val x = random.nextInt(total)
        var current = 0
        for ((key, weight) in this) {
            current += weight
            if (x < current) {
                return key
            }
        }
    }
    return null
}
