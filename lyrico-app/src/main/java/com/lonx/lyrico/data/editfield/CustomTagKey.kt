package com.lonx.lyrico.data.editfield

import java.util.Locale

/**
 * 自定义标签键名的归一化规则。扫描入库与用户手动添加都走这里，保证两边得到同一个字符串。
 */
object CustomTagKey {

    const val MAX_KEY_LENGTH = 64

    /** 去空白、转大写；不合法时返回 null。 */
    fun normalize(input: String): String? {
        val key = input.trim()

        return when {
            key.isBlank() -> null
            key.length > MAX_KEY_LENGTH -> null
            key.any { it == '\n' || it == '\r' } -> null
            else -> key.uppercase(Locale.ROOT)
        }
    }
}
