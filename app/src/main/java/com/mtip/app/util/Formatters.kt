package com.mtip.app.util

import java.text.SimpleDateFormat
import java.util.*

object Formatters {
    private const val PICO = 1_000_000_000_000L

    fun xmr(atomic: Long): String =
        "%.8f XMR".format(atomic.toDouble() / PICO)

    fun truncAddr(a: String, n: Int = 8) =
        "${a.take(n)}…${a.takeLast(n)}"

    fun date(ms: Long): String =
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm", Locale.getDefault()
        ).format(Date(ms))
}