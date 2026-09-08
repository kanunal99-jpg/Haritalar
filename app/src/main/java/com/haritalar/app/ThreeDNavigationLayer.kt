package com.haritalar.app

import android.graphics.Color
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.PropertyFactory.fillExtrusionBase
import org.maplibre.android.style.layers.PropertyFactory.fillExtrusionColor
import org.maplibre.android.style.layers.PropertyFactory.fillExtrusionHeight
import org.maplibre.android.style.layers.PropertyFactory.fillExtrusionOpacity
import org.maplibre.android.style.sources.TileSet
import org.maplibre.android.style.sources.VectorSource

/** First-stage 3D building renderer for active navigation. */
object ThreeDNavigationLayer {
    const val SOURCE_ID = "haritalar-3d-openfreemap"
    const val BUILDING_LAYER_ID = "haritalar-3d-buildings"
    const val SOURCE_LAYER = "building"
    private const val TILE_URL = "https://tiles.openfreemap.org/planet/{z}/{x}/{y}.pbf"

    fun install(style: Style) {
        if (style.getLayer(BUILDING_LAYER_ID) != null) return
        if (style.getSource(SOURCE_ID) == null) {
            style.addSource(VectorSource(SOURCE_ID, TileSet("haritalar-openfreemap", TILE_URL)))
        }

        val layer = FillExtrusionLayer(BUILDING_LAYER_ID, SOURCE_ID).apply {
            sourceLayer = SOURCE_LAYER
            minZoom = 14f
            setProperties(
                fillExtrusionColor(Color.LTGRAY),
                fillExtrusionHeight(
                    Expression.interpolate(
                        Expression.linear(),
                        Expression.zoom(),
                        Expression.stop(14f, 0f),
                        Expression.stop(16f, Expression.get("render_height")),
                    ),
                ),
                fillExtrusionBase(Expression.get("render_min_height")),
                fillExtrusionOpacity(0.82f),
            )
        }
        style.addLayer(layer)
    }
}
