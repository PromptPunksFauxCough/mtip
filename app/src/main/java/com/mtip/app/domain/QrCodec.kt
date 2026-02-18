package com.mtip.app.domain

import java.net.URLDecoder
import java.net.URLEncoder

object QrCodec {

    data class Payload(
        val address: String?,
        val seed: String,
        val restoreHeight: Long
    )

    fun encode(address: String, seed: String, restoreHeight: Long): String {
        val encodedSeed = URLEncoder.encode(
            seed.trim(), "UTF-8"
        )
        return "monero-wallet:$address?seed=$encodedSeed" +
            "&height=$restoreHeight"
    }

    fun decode(raw: String): Payload? {
        val input = raw.trim()
        if (!input.startsWith("monero-wallet:")) return null

        val qIdx = input.indexOf('?')
        if (qIdx < 0) return null

        val address = input.substring("monero-wallet:".length, qIdx)

        val query = input.substring(qIdx + 1)
        val params = parseQuery(query)

        val seed = params["seed"]
            ?.let { URLDecoder.decode(it, "UTF-8") }
            ?.trim()
            ?: return null

        if (seed.isBlank()) return null

        val height = params["height"]
            ?.toLongOrNull() ?: 0L

        return Payload(address, seed, height)
    }

    private fun parseQuery(
        query: String
    ): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (pair in query.split("&")) {
            val eq = pair.indexOf('=')
            if (eq > 0) {
                val key = pair.substring(0, eq)
                val value = pair.substring(eq + 1)
                map[key] = value
            }
        }
        return map
    }
}
