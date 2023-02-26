package option

import arrow.core.*
import arrow.core.raise.Raise
import arrow.core.raise.option
import kotlinx.benchmark.*
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@State(Scope.Benchmark)
class OptionBenchmark {
  @Param("1000", "10000", "100000")
  var iterations = 0

  @Benchmark
  fun nestedOptions(blackhole: Blackhole) {
    for (i in 0..iterations) {
      val option = Some(i)
      val nestedOption = option.map(::Some)
      blackhole.consume(nestedOption)
    }
  }

  @Benchmark
  fun nestedOptionsBoxed(blackhole: Blackhole) {
    buildList(iterations) {
      for (i in 0..iterations) {
        add(Some(i))
      }
    }.map { it.map(::Some) }.forEach(blackhole::consume)
  }

  @Benchmark
  fun someComprehension(blackhole: Blackhole) = runBlocking {
    for (i in 0..iterations)
      blackhole.consume(option {
        val option = Some(i)
        val stringified = option.map { intToString(it) }
        // stringified has to be preserved through suspension calls
        val original = stringified.map { stringToInt(it) }
        blackhole.consume(original)
        stringified
      })
  }

  @Benchmark
  fun noneComprehension(blackhole: Blackhole) = runBlocking {
    for (i in 0..iterations)
      blackhole.consume(option {
        val option = Some(i)
        val stringified = option.map { intToInvalidString(it) }
        // stringified has to be preserved through suspension calls
        val original = stringified.map { stringToInt(it) }
        blackhole.consume(original)
        stringified
      })
  }

  @Benchmark
  fun someComprehensionBlocking(blackhole: Blackhole){
    for (i in 0..iterations)
      blackhole.consume(option {
        val option = Some(i)
        val stringified = option.map { intToStringBlocking(it) }
        // stringified has to be preserved through suspension calls
        val original = stringified.map { stringToIntBlocking(it) }
        blackhole.consume(original)
        stringified
      })
  }

  @Benchmark
  fun noneComprehensionBlocking(blackhole: Blackhole){
    for (i in 0..iterations)
      blackhole.consume(option {
        val option = Some(i)
        val stringified = option.map { intToInvalidStringBlocking(it) }
        val original = stringified.map { stringToIntBlocking(it) }
        blackhole.consume(original)
        stringified
      })
  }



  suspend fun intToString(int: Int): String = suspendCoroutine {
    it.resume(int.toString())
  }

  suspend fun intToInvalidString(int: Int): String = suspendCoroutine {
    it.resume("$int.0")
  }
  context(Raise<Option<Nothing>>)
  suspend fun stringToInt(string: String): Int = suspendCoroutine {
    it.resume(string.toIntOrNull() ?: raise(None))
  }

  fun intToStringBlocking(int: Int): String = int.toString()

  fun intToInvalidStringBlocking(int: Int): String = "$int.0"

  context(Raise<Option<Nothing>>)
  fun stringToIntBlocking(string: String): Int = string.toIntOrNull() ?: raise(None)
}
