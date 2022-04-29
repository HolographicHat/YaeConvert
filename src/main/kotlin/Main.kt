import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

import data.*
import kotlinx.coroutines.awaitAll
import okhttp3.HttpUrl.Companion.toHttpUrl
import utils.*
import java.util.Stack
import kotlin.io.path.*

private const val packageInfo = "pkg_version"

private data class Diffs(
    val add: List<ResVersionItem>,
    val delete: List<ResVersionItem>,
    val modify: List<ResVersionItem>
)

private infix fun List<ResVersionItem>.diff(other: List<ResVersionItem>): Diffs {
    val add = other.map { it.name }.let { filterNot { i -> i.name in it } }
    val delete = map { it.name }.let { other.filterNot { i -> i.name in it } }
    val modify = minus(add.toSet()).minus(other.toSet())
    return Diffs(add, delete, modify)
}

private suspend fun getPackageInfo(info: PackageInfo): String {
    val hostStr = info.scatteredUrl.toHttpUrl().host
    return mirrorNow("GetPackage:${hostStr}") {
        buildHttpRequest {
            url = "${info.scatteredUrl}/$packageInfo"
        }.execute().body!!.byteStream().use {
            it.readBytes().decodeToString()
        }
    }
}

private val cache by lazy { Path("${System.getProperty("user.dir")}/cache").createDirectories() }

fun main(vararg args: String) = runBlocking {
    println("----------------------------------------------------")
    println(" YaeConvert - 国服/国际服转换工具")
    println(" https://github.com/HolographicHat/YaeConvert")
    println("----------------------------------------------------")
    val verbose = args.contains("--verbose")
    val gamePath = cache.resolve("GI-PATH").let { pf ->
        if (pf.exists()) {
            Path(pf.readText())
        } else {
            println("原神(YuanShen.exe/GenshinImpact.exe)所在路径: ")
            val unverifiedPath = Path(readln())
            check(unverifiedPath.resolve("UnityPlayer.dll").exists() && unverifiedPath.resolve("pkg_version").exists())
            unverifiedPath.also {
                pf.writeText(it.absolutePathString())
            }
        }
    }
    val isOversea = gamePath.resolve("GenshinImpact.exe").exists()
    info(arrayOf("CN", "OS").let { if (isOversea) it.reversedArray() else it }.joinToString(" => "))
    info("获取版本信息")
    val (sInfo, dInfo) = request<FullPackageInfo>("$bucketUrl/public/scattered_urls.json").run {
        if (isOversea) os to cn else cn to os
    }
    info("获取文件信息")
    val (sourceFiles, destFiles) = coroutineScope {
        listOf(
            async {
                getPackageInfo(sInfo)
            },
            async {
                getPackageInfo(dInfo)
            }
        ).awaitAll()
    }
    val sFileList = sourceFiles.replace(sInfo.name, dInfo.name).lines().mapNotNull {
        if (it.isNotBlank()) it.toDataClass<ResVersionItem>() else null
    }
    val dFileList = destFiles.lines().mapNotNull {
        if (it.isNotBlank()) it.toDataClass<ResVersionItem>() else null
    }
    val diffs = dFileList diff sFileList
    info("添加 ${diffs.add.size} 文件, 移除 ${diffs.delete.size} 文件, 修改 ${diffs.modify.size} 文件")
    if (verbose) {
        diffs.modify.takeIf { it.isNotEmpty() }?.forEach {
            debug("\u001B[38;2;104;151;187m${it.name}\u001B[0m")
        }
        diffs.delete.takeIf { it.isNotEmpty() }?.forEach {
            debug("\u001B[38;2;108;108;108m${it.name}\u001B[0m")
        }
        diffs.add.takeIf { it.isNotEmpty() }?.forEach {
            debug("\u001B[38;2;98;151;85m${it.name}\u001B[0m")
        }
    }
    info("检查缓存资源")
    mirrorNow("Backup") {
        sFileList.filter { f -> f.name in diffs.delete.map { it.name } || f.name in diffs.modify.map { it.name } }
            .forEach {
                gamePath.resolve(it.name.replace(dInfo.name, sInfo.name)).takeIf { f -> f.exists() }
                    ?.moveTo(cache.resolve(it.md5), true)
            }
    }
    val files = diffs.modify + diffs.add
    val downloadFiles = Stack<ResVersionItem>().also { stack ->
        val l = files.filter { !cache.resolve(it.md5).exists() }.distinctBy { it.md5 }.sortedBy { it.size }
        stack.addAll(l)
    }
    List(5) {
        async {
            while (!downloadFiles.empty()) {
                val item = downloadFiles.pop()
                info("下载 ${item.name.takeLastWhile { it != '/' }.padEnd(32)} ${bytesToReadableSize(item.size)}")
                retry(3) {
                    val temp = cache.resolve("temp_${item.md5}")
                    downloadAndDigest(
                        url = "${dInfo.scatteredUrl}/${item.name}",
                        file = temp,
                        client = defaultClient,
                        algorithm = Algorithm.MD5
                    ) { hash ->
                        check(hash == item.md5) {
                            temp.deleteIfExists()
                            "Hash check fail: ${temp.name}"
                        }
                        temp.moveTo(cache.resolve(item.md5), true)
                    }
                }
            }
        }
    }.awaitAll()
    info("更新游戏资源")
    mirrorNow("UpdateResource") {
        gamePath.resolve("${sInfo.name}_Data").moveTo(gamePath.resolve("${dInfo.name}_Data"), true)
        gamePath.resolve(packageInfo).writeText(destFiles)
        gamePath.listDirectoryEntries("Audio_*_$packageInfo").forEach {
            it.writeText(it.readText().replace(sInfo.name, dInfo.name))
        }
        files.forEach {
            cache.resolve(it.md5).copyTo(gamePath.resolve(it.name), true)
        }
    }
    if (verbose) {
        Mirror.report()
    }
    info("完成")
}
