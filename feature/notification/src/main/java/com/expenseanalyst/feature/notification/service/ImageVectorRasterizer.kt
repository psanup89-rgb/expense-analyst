package com.expenseanalyst.feature.notification.service

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath

/**
 * Draws a Compose [ImageVector] onto a plain Android [Canvas], so a notification (built outside
 * any Composition) can show exactly the glyph the app shows for a category.
 *
 * This replaced a separate set of hand-drawn `ic_cat_*` drawables keyed by the 16 seeded icon
 * names. That second copy drifted from the app: a category whose icon the user changed through
 * the picker (Leisure → `hotel`, Vehicle, Joey, Split Payments) had no drawable, so its
 * notification fell back to a letter badge while the app showed the real icon. Rendering the same
 * `categoryIconVector()` output makes a mismatch impossible for any icon the picker offers.
 *
 * Handles what Material icons use: nested groups with their transforms, clip paths, fill type
 * and fill alpha. Strokes are not drawn — Material Filled icons have none.
 */
internal object ImageVectorRasterizer {

    fun draw(canvas: Canvas, vector: ImageVector, left: Float, top: Float, sizePx: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(sizePx / vector.viewportWidth, sizePx / vector.viewportHeight)
        drawGroup(canvas, vector.root, paint, Color.alpha(color))
        canvas.restore()
    }

    private fun drawGroup(canvas: Canvas, group: VectorGroup, paint: Paint, baseAlpha: Int) {
        canvas.save()
        canvas.translate(group.translationX + group.pivotX, group.translationY + group.pivotY)
        canvas.rotate(group.rotation)
        canvas.scale(group.scaleX, group.scaleY)
        canvas.translate(-group.pivotX, -group.pivotY)
        if (group.clipPathData.isNotEmpty()) {
            canvas.clipPath(group.clipPathData.toPath().asAndroidPath())
        }
        for (node in group) {
            when (node) {
                is VectorGroup -> drawGroup(canvas, node, paint, baseAlpha)
                is VectorPath -> {
                    if (node.fill == null) continue
                    val path = node.pathData.toPath().asAndroidPath().apply {
                        fillType = if (node.pathFillType == PathFillType.EvenOdd) {
                            Path.FillType.EVEN_ODD
                        } else {
                            Path.FillType.WINDING
                        }
                    }
                    paint.alpha = (baseAlpha * node.fillAlpha).toInt().coerceIn(0, 255)
                    canvas.drawPath(path, paint)
                }
            }
        }
        canvas.restore()
    }

    private object Color {
        fun alpha(color: Int): Int = (color ushr 24) and 0xFF
    }
}
