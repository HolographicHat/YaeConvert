package utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.math.BigInteger
import java.nio.file.Path
import java.security.MessageDigest

class RequestBuilderScope {
    
    private val builder = Request.Builder()
    
    fun build() = builder.build()
    
    var url = ""
        set(value) {
            check(value.isNotBlank()) { "Invalid url." }
            builder.url(value)
        }
    
    fun head() = builder.head()
    
}

@PublishedApi
internal val defaultClient = OkHttpClient()

suspend fun Request.execute(client: OkHttpClient = defaultClient) = withContext(Dispatchers.IO) {
    client.newCall(this@execute).execute()
}

inline fun buildHttpRequest(crossinline func: RequestBuilderScope.() -> Unit): Request {
    val scope = RequestBuilderScope()
    func(scope)
    return scope.build()
}

suspend inline fun <reified T: Any> request(
    url: String,
    client: OkHttpClient = defaultClient,
    crossinline builder: RequestBuilderScope.() -> Unit = {}
) = requestString(url, client, builder).toDataClass<T>()

suspend inline fun requestString(
    url: String,
    client: OkHttpClient = defaultClient,
    crossinline builder: RequestBuilderScope.() -> Unit = {}
) = buildHttpRequest {
    this.url = url
    builder()
}.execute(client).use {
    check(it.isSuccessful) { it.message }
    checkNotNull(it.body).string()
}

@Suppress("ControlFlowWithEmptyBody")
suspend fun downloadAndDigest(
    url: String,
    file: Path,
    client: OkHttpClient,
    algorithm: Algorithm,
    onFinish: (hash: String) -> Unit = {}
) = withContext(Dispatchers.IO) {
    val di = MessageDigest.getInstance(algorithm.s)
    client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
        file.sink().buffer().use { localBuf ->
            resp.body?.source()?.use { remoteBuf ->
                var length: Int
                val middleBuffer = ByteArray(2048)
                while (remoteBuf.read(middleBuffer, 0, 2048).also { i -> length = i } != -1) {
                    di.update(middleBuffer, 0, length)
                    localBuf.write(middleBuffer, 0, length)
                }
            }
        }
    }
    onFinish(BigInteger(1, di.digest()).toString(16).padStart(algorithm.length, '0'))
}

fun bytesToReadableSize(bytes: Long) = bytes.toFloat().let {
    when {
        it >= 1 shl 30 -> "%.1f GB".format(it / (1 shl 30))
        it >= 1 shl 20 -> "%.1f MB".format(it / (1 shl 20))
        it >= 1 shl 10 -> "%.1f KB".format(it / (1 shl 10))
        else -> "$it B"
    }
}
