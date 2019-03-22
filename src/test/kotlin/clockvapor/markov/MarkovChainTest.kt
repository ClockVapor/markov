package clockvapor.markov

import org.junit.Assert
import org.junit.Test

class MarkovChainTest {
    @Test
    fun testAddMarkovChain() {
        val markovChain = MarkovChain(mutableMapOf())
        val markovChain2 = MarkovChain(mutableMapOf(
            "foo" to mutableMapOf(
                "bar" to 4,
                "baz" to 2
            ),
            "hello" to mutableMapOf(
                "world" to 1,
                "friends" to 1
            )
        ))
        markovChain.add(markovChain2)
        Assert.assertEquals(
            mutableMapOf(
                "foo" to mutableMapOf(
                    "bar" to 4,
                    "baz" to 2
                ),
                "hello" to mutableMapOf(
                    "world" to 1,
                    "friends" to 1
                )
            ),
            markovChain.data
        )
    }

    @Test
    fun testRemoveMarkovChain() {
        val markovChain = MarkovChain(mutableMapOf(
            "foo" to mutableMapOf(
                "bar" to 5,
                "baz" to 3
            ),
            "hello" to mutableMapOf(
                "world" to 2,
                "friends" to 1
            )
        ))
        val markovChain2 = MarkovChain(mutableMapOf(
            "foo" to mutableMapOf(
                "bar" to 4,
                "baz" to 2
            ),
            "hello" to mutableMapOf(
                "world" to 1,
                "friends" to 1
            )
        ))
        markovChain.remove(markovChain2)
        Assert.assertEquals(
            mutableMapOf(
                "foo" to mutableMapOf(
                    "bar" to 1,
                    "baz" to 1
                ),
                "hello" to mutableMapOf(
                    "world" to 1
                )
            ),
            markovChain.data
        )
    }
}
