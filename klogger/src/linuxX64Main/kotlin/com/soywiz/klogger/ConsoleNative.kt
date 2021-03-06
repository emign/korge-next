package com.soywiz.klogger

actual inline fun Console.log(vararg msg: Any?) {
    println(msg.joinToString(", "))
}

actual inline fun Console.warn(vararg msg: Any?) {
    Console.log("WARNING:", *msg)
}

actual inline fun Console.error(vararg msg: Any?) {
    Console.log("ERROR:", *msg)
}

