package com.penandpaper.domain.model
data class Folder(
    val id: Long,
    val parentId: Long?,
    val name: String,
    val color: Long?,
    val sortIndex: Int = 0,
    val layout: String = "list",
    val iconSize: String = "medium",
    val contentMode: String = "auto", // auto | light | dark | themeOnPrimary | themeOnSurface
    val preferLight: Boolean = false
)
