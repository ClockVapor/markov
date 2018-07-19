package clockvapor.markov

import java.util.*

/**
 * Gets a weighted random key, where the [Map] is formatted with each key mapping to its "weight" value.
 * Returns null if the [Map] is empty.
 */
fun <T> Map<T, Int>.getWeightedRandomKey(random: Random): T? {
    val x = random.nextInt(values.sum())
    var current = 0
    for ((w, count) in this) {
        current += count
        if (x < current) {
            return w
        }
    }
    return null
}
