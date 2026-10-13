package com.lonx.lyrico.domain.poster

object PosterFileEditNames {
    /** Rename the stem only; changing an extension must never disguise the file's contents. */
    fun rename(original: String, input: String, otherNames: List<String>): String? {
        val stem = input.trim()
        if (stem.isEmpty() || stem == "." || stem == ".." || stem.any { it == '/' || it == '\\' || it.isISOControl() }) return null
        val extension = original.substringAfterLast('.', "")
        val name = if (extension.isEmpty()) stem else "$stem.$extension"
        return name.takeUnless { otherNames.any { other -> other.equals(name, ignoreCase = true) } }
    }
}
