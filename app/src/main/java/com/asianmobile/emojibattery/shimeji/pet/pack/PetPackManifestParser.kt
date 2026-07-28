package com.asianmobile.emojibattery.shimeji.pet.pack

import com.asianmobile.emojibattery.shimeji.pet.engine.PetAction
import com.asianmobile.emojibattery.shimeji.pet.engine.PetVector
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class PetPackManifestParser {
    fun parse(json: String): PetPackManifest = try {
        val root = JSONObject(json)
        val canvasJson = root.requiredObject("canvas")
        val anchorJson = root.requiredObject("anchor")
        val speechAnchorJson = root.optJSONObject("speechAnchor")
        val interactionJson = root.optJSONObject("interaction") ?: JSONObject()
        val clipList = root.requiredArray("clips").objects("clips").map { clipJson ->
            val action = clipJson.requiredAction("action")
            val nextAction = clipJson.optString("nextAction")
                .takeIf(String::isNotBlank)
                ?.toPetAction("nextAction")
            val frames = clipJson.requiredArray("frames").objects("frames").map { frameJson ->
                val rectJson = frameJson.requiredObject("rect")
                val velocityJson = frameJson.optJSONObject("velocity")
                PetPackFrame(
                    file = frameJson.requiredString("file"),
                    rect = PetPackFrameRect(
                        x = rectJson.requiredInt("x"),
                        y = rectJson.requiredInt("y"),
                        width = rectJson.requiredInt("width"),
                        height = rectJson.requiredInt("height")
                    ),
                    durationMillis = frameJson.requiredLong("durationMs"),
                    velocity = PetVector(
                        x = velocityJson?.optDouble("x", 0.0)?.toFloat() ?: 0f,
                        y = velocityJson?.optDouble("y", 0.0)?.toFloat() ?: 0f
                    )
                )
            }
            PetPackClip(
                action = action,
                loops = clipJson.optBoolean("loop", false),
                nextAction = nextAction,
                frames = frames
            )
        }
        if (clipList.map(PetPackClip::action).distinct().size != clipList.size) {
            throw PetPackFormatException("clips must not contain duplicate actions")
        }
        val clips = clipList.associateBy(PetPackClip::action)

        PetPackManifest(
            schemaVersion = root.requiredInt("schemaVersion"),
            id = root.requiredString("id"),
            version = root.requiredInt("version"),
            name = root.requiredString("name"),
            author = root.optString("author").takeIf(String::isNotBlank),
            canvas = PetPackCanvas(
                width = canvasJson.requiredInt("width"),
                height = canvasJson.requiredInt("height"),
                defaultScale = canvasJson.optDouble("defaultScale", 1.0).toFloat()
            ),
            anchor = PetPackAnchor(
                x = anchorJson.requiredDouble("x").toFloat(),
                y = anchorJson.requiredDouble("y").toFloat()
            ),
            interaction = PetPackInteraction(
                tapAction = interactionJson.optString("tapAction", "tapped")
                    .toPetAction("interaction.tapAction")
            ),
            clips = clips,
            speechAnchor = speechAnchorJson?.let {
                PetPackAnchor(
                    x = it.requiredDouble("x").toFloat(),
                    y = it.requiredDouble("y").toFloat()
                )
            }
        )
    } catch (error: PetPackFormatException) {
        throw error
    } catch (error: JSONException) {
        throw PetPackFormatException("Malformed pet pack manifest", error)
    }

    private fun JSONObject.requiredObject(key: String): JSONObject =
        optJSONObject(key) ?: throw PetPackFormatException("$key must be an object")

    private fun JSONObject.requiredArray(key: String): JSONArray =
        optJSONArray(key) ?: throw PetPackFormatException("$key must be an array")

    private fun JSONObject.requiredString(key: String): String =
        optString(key).takeIf(String::isNotBlank)
            ?: throw PetPackFormatException("$key must be a non-empty string")

    private fun JSONObject.requiredInt(key: String): Int = try {
        getInt(key)
    } catch (error: JSONException) {
        throw PetPackFormatException("$key must be an integer", error)
    }

    private fun JSONObject.requiredLong(key: String): Long = try {
        getLong(key)
    } catch (error: JSONException) {
        throw PetPackFormatException("$key must be an integer", error)
    }

    private fun JSONObject.requiredDouble(key: String): Double = try {
        getDouble(key)
    } catch (error: JSONException) {
        throw PetPackFormatException("$key must be a number", error)
    }

    private fun JSONObject.requiredAction(key: String): PetAction =
        requiredString(key).toPetAction(key)

    private fun String.toPetAction(field: String): PetAction =
        PetAction.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
            ?: throw PetPackFormatException("$field contains unsupported action: $this")

    private fun JSONArray.objects(field: String): List<JSONObject> =
        List(length()) { index ->
            optJSONObject(index)
                ?: throw PetPackFormatException("$field[$index] must be an object")
        }
}
