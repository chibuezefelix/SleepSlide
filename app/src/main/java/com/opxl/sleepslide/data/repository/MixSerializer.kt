package com.opxl.sleepslide.data.repository


import com.opxl.sleepslide.domain.model.Domain
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JSON for mixes stored outside Room (last-played ephemeral mix, play-history snapshots).
 *
 * The sound fields here are a snapshot, not the truth: isPremium is no longer written, and
 * anything read back must go through [com.opxl.sleepslide.domain.repository.SoundRepository.resolve]
 * before it is played. Older JSON that still carries isPremium stays readable.
 */
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
                    // Ignored on read: resolve() replaces it with the catalogue's current value
                    isPremium = soundObj.optBoolean("isPremium", false),
                    isBundled = soundObj.optBoolean("isBundled", true),
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
