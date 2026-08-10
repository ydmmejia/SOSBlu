package com.bitchat.android.core.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val BitChatIcon: ImageVector
    get() {
        _BitChatIcon?.let { return it }

        return ImageVector.Builder(
            name = "SOSBluIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // Emergency Cross / Shield Symbol
                moveTo(10f, 3f)
                lineTo(14f, 3f)
                lineTo(14f, 10f)
                lineTo(21f, 10f)
                lineTo(21f, 14f)
                lineTo(14f, 14f)
                lineTo(14f, 21f)
                lineTo(10f, 21f)
                lineTo(10f, 14f)
                lineTo(3f, 14f)
                lineTo(3f, 10f)
                lineTo(10f, 10f)
                close()
            }
        }.build().also { _BitChatIcon = it }
    }

private var _BitChatIcon: ImageVector? = null
