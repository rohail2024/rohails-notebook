
package com.penandpaper.app.crypto
object KeyHolder {
    @Volatile private var dataKey: ByteArray? = null
    fun setKey(k: ByteArray) { dataKey = k }
    fun getKey(): ByteArray? = dataKey
    fun isUnlocked(): Boolean = dataKey != null
    fun clear() { dataKey = null }
}
