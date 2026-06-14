package com.silentport.silentport.firewall

/**
 * A temporary firewall unblock for a single package.
 *
 * Persisted in DataStore as the string `"packageName:expiryEpochMillis"`. This type centralizes
 * that encoding so the format lives in exactly one place instead of being parsed ad-hoc with
 * `split(":")` across the view model, controller and data source.
 */
data class TemporaryUnblock(
    val packageName: String,
    val expiresAt: Long
) {
    /** Whether this unblock is still in effect at [nowMillis]. */
    fun isActiveAt(nowMillis: Long): Boolean = expiresAt > nowMillis

    fun encode(): String = "$packageName:$expiresAt"

    companion object {
        /** Parses a persisted entry, or returns `null` if it is malformed. */
        fun decode(entry: String): TemporaryUnblock? {
            val parts = entry.split(":")
            if (parts.size != 2) return null
            val expiresAt = parts[1].toLongOrNull() ?: return null
            return TemporaryUnblock(parts[0], expiresAt)
        }

        /** Package names whose unblock is still active at [nowMillis]. */
        fun activePackages(entries: Set<String>, nowMillis: Long): Set<String> =
            entries.mapNotNull { entry ->
                decode(entry)?.takeIf { it.isActiveAt(nowMillis) }?.packageName
            }.toSet()
    }
}
