package utils

val Throwable.simpleMessage get() = "${this::class.java.name}: $message"

@Suppress("SameParameterValue")
suspend fun <C, R> C.retry(times: Int, func: suspend C.() -> R) {
    var retryTimes = 0
    var lastError: Throwable? = null
    while (retryTimes < times) {
        runCatching {
            func()
            return
        }.onFailure {
            error("${it.simpleMessage} (${retryTimes}/${times})")
            retryTimes += 1
            lastError = it
        }
    }
    lastError?.let { throw it }
}
