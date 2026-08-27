package com.opxl.sleepslide.data.repository


import com.opxl.sleepslide.domain.model.Domain
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MixSerializer @Inject constructor() {

    fun serialize(mix: Domain.SoundMix): String = JSONObject().apply {
        put("masterVolume", mix.masterVolume)
        put("fadeInDurationMs", mix.fadeInDurationMs)
        put("layers", JSONArray().also { arr ->
            mix.layers.forEach { layer ->
                arr.put(JSONObject().apply {
                    put("position", layer.position)
                    put("volume", layer.volume)
                    put("isMuted", layer.isMuted)
                    put("sound", JSONObject().apply {
                        put("id", layer.sound.id)
                        put("title", layer.sound.title)
                        put("category", layer.sound.category.name)
                        put("assetPath", layer.sound.assetPath)
                        put("isPremium", layer.sound.isPremium)
                        put("isBundled", layer.sound.isBundled)
                    })
                })
            }
        })
    }.toString()

    fun deserialize(json: String): Domain.SoundMix {
        val obj = JSONObject(json)
        val layersArr = obj.getJSONArray("layers")
        val layers = (0 until layersArr.length()).map { i ->
            val layerObj = layersArr.getJSONObject(i)
            val soundObj = layerObj.getJSONObject("sound")
            Domain.SoundLayer(
                position = layerObj.getInt("position"),
                volume = layerObj.getDouble("volume").toFloat(),
                isMuted = layerObj.getBoolean("isMuted"),
                sound = Domain.Sound(
                    id = soundObj.getString("id"),
                    title = soundObj.getString("title"),
                    category = Domain.SoundCategory.valueOf(soundObj.getString("category")),
                    assetPath = soundObj.getString("assetPath"),
                    isPremium = soundObj.getBoolean("isPremium"),
                    isBundled = soundObj.getBoolean("isBundled"),
                ),
            )
        }
        return Domain.SoundMix(
            layers = layers,
            masterVolume = obj.getDouble("masterVolume").toFloat(),
            fadeInDurationMs = obj.getLong("fadeInDurationMs"),
        )
    }
}
