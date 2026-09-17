package com.giglister.app.util

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/** Only routes supported public links; never accepts arbitrary navigation destinations. */
fun incomingRoute(url: String?, eventId: String? = null): String? {
    eventId?.toLongOrNull()?.takeIf { it > 0 }?.let { return "event/$it" }
    val uri = runCatching { URI(url ?: return null) }.getOrNull() ?: return null
    if (uri.scheme != "https" || uri.host != "sandbox.fotosvorju.de") return null
    val segments = uri.path.orEmpty().trim('/').split('/')
    if (segments.size == 2) {
        val id = segments[1].toLongOrNull()?.takeIf { it > 0 } ?: return null
        return when (segments[0]) { "konzerte" -> "event/$id"; "bands" -> "bands/$id"; "orte" -> "locations/$id"; "festivals" -> "festivals/$id" + if (uri.rawQuery.orEmpty().split('&').contains("filter=saved")) "?saved=true" else ""; else -> null }
    }
    val target = when (uri.path) { "/email-bestaetigen" -> "verify"; "/passwort-zuruecksetzen" -> "reset"; else -> return null }
    val token = runCatching { uri.rawQuery.orEmpty().split('&').map { it.split('=', limit = 2) }.firstOrNull { it[0] == "token" }?.getOrNull(1)?.let { URLDecoder.decode(it, "UTF-8") } }.getOrNull()
    if (token.isNullOrBlank()) return null
    return "$target?token=${URLEncoder.encode(token, "UTF-8").replace("+", "%20") }"
}
