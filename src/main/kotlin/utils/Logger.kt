package utils

import java.text.SimpleDateFormat
import java.util.*

fun debug(msg: String) {
    printLog(0, msg)
}

fun info(msg: String) {
    printLog(1, msg)
}

fun warn(msg: String) {
    printLog(2, msg)
}

fun error(msg: String) {
    printLog(3, msg)
}

fun error(msg: String, throwable: Throwable? = null) {
    printLog(3, msg, throwable)
}

private val fmt by lazy {
    SimpleDateFormat("HH:mm:ss.SSS")
}
private val time get() = fmt.format(Date())

private fun printLog(level: Int, msg: String, throwable: Throwable? = null) {
    val (color, ls) = when (level) {
        0 -> "38;5;43" to "DEBUG"
        1 -> "94" to "INFO"
        2 -> "93" to "WARN"
        3 -> "1;31" to "ERROR"
        else -> "39" to "UNK"
    }
    val lStr = ls.padStart(5, ' ')
    val header = "\u001B[${color}m${lStr}\u001B[0m"
    println("$time $header \u001B[37m->\u001B[0m $msg")
    throwable?.printStackTrace()
}
