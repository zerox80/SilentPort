package com.silentport.silentport.firewall

/**
 * Single owner of the temporary-unblock wire format.
 *
 * Each entry is encoded as `"<packageName>:<expiryEpochMillis>"`. Centralising the
 * encoding/decoding here keeps the `split(":")` parsing out of the ViewModel and the
 * firewall controller, where it used to be duplicated in several places.
 */
object TemporaryUnblocks {

    private const val SEPARATOR = ":"

    /** Encodes a temporary unblock entry for persistence. */
    fun encode(packageName: String, expiryMillis: Long): String =
        "$packageName$SEPARATOR$expiryMillis"

    /** True if [entry] belongs to [packageName] regardless of its expiry. */
    fun matches(entry: String, packageName: String): Boolean =
        entry.startsWith("$packageName$SEPARATOR")

    /** Package names whose temporary unblock is still valid at [now]. */
    fun validPackages(entries: Set<String>, now: Long): Set<String> =
        entries.mapNotNullTo(mutableSetOf()) { entry ->
            val (packageName, expiry) = decode(entry) ?: return@mapNotNullTo null
            packageName.takeIf { expiry > now }
        }

    /** Earliest future expiry across [entries], or null when none is pending. */
    fun nextExpiry(entries: Set<String>, now: Long): Long? =
        entries.asSequence()
            .mapNotNull { decode(it)?.second }
            .filter { it > now }
            .minOrNull()

    private fun decode(entry: String): Pair<String, Long>? {
        val parts = entry.split(SEPARATOR)
        if (parts.size != 2) return null
        val expiry = parts[1].toLongOrNull() ?: return null
        return parts[0] to expiry
    }
}
