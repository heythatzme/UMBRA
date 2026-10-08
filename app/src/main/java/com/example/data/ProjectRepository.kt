package com.example.data

import android.content.Context
import com.example.model.BwMixState
import com.example.model.CurvePoint
import com.example.model.CurveState
import com.example.model.DetailState
import com.example.model.EditState
import com.example.model.GrainPreset
import com.example.model.GrainState
import com.example.model.NoirPresetId
import com.example.model.SavedProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val projectsFile = File(context.filesDir, "noir_projects.json")
    private val _projects = MutableStateFlow<List<SavedProject>>(emptyList())
    val projects: StateFlow<List<SavedProject>> = _projects.asStateFlow()

    suspend fun loadProjects() = withContext(Dispatchers.IO) {
        if (!projectsFile.exists()) {
            _projects.value = emptyList()
            return@withContext
        }

        try {
            val content = projectsFile.readText()
            val jsonArray = JSONArray(content)
            val list = mutableListOf<SavedProject>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(deserializeProject(obj))
            }
            _projects.value = list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            _projects.value = emptyList()
        }
    }

    suspend fun saveProject(
        imageUri: String,
        name: String,
        width: Int,
        height: Int,
        editState: EditState
    ): SavedProject = withContext(Dispatchers.IO) {
        val newProject = SavedProject(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "UMBRA_${System.currentTimeMillis() / 1000}" },
            imageUri = imageUri,
            originalWidth = width,
            originalHeight = height,
            timestamp = System.currentTimeMillis(),
            editState = editState
        )

        val updated = listOf(newProject) + _projects.value.filter { it.id != newProject.id }
        _projects.value = updated
        persist(updated)
        newProject
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        val updated = _projects.value.filter { it.id != id }
        _projects.value = updated
        persist(updated)
    }

    private fun persist(list: List<SavedProject>) {
        try {
            val jsonArray = JSONArray()
            for (p in list) {
                jsonArray.put(serializeProject(p))
            }
            projectsFile.writeText(jsonArray.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun serializeProject(p: SavedProject): JSONObject {
        val obj = JSONObject()
        obj.put("id", p.id)
        obj.put("name", p.name)
        obj.put("imageUri", p.imageUri)
        obj.put("originalWidth", p.originalWidth)
        obj.put("originalHeight", p.originalHeight)
        obj.put("timestamp", p.timestamp)

        val es = JSONObject()
        es.put("exposure", p.editState.exposure.toDouble())
        es.put("brightness", p.editState.brightness)
        es.put("contrast", p.editState.contrast)
        es.put("highlights", p.editState.highlights)
        es.put("shadows", p.editState.shadows)
        es.put("whites", p.editState.whites)
        es.put("blacks", p.editState.blacks)
        es.put("clarity", p.editState.clarity)
        es.put("texture", p.editState.texture)
        es.put("sharpness", p.editState.sharpness)
        es.put("noirIntensity", p.editState.noirIntensity)
        es.put("fade", p.editState.fade)
        es.put("presetId", p.editState.presetId.name)

        es.put("vignetteAmount", p.editState.vignetteAmount)
        es.put("vignetteMidpoint", p.editState.vignetteMidpoint)
        es.put("vignetteFeather", p.editState.vignetteFeather)
        es.put("vignetteRoundness", p.editState.vignetteRoundness)

        // B&W mix
        val mix = JSONObject()
        mix.put("reds", p.editState.bwMix.reds)
        mix.put("oranges", p.editState.bwMix.oranges)
        mix.put("yellows", p.editState.bwMix.yellows)
        mix.put("greens", p.editState.bwMix.greens)
        mix.put("cyans", p.editState.bwMix.cyans)
        mix.put("blues", p.editState.bwMix.blues)
        mix.put("magentas", p.editState.bwMix.magentas)
        es.put("bwMix", mix)

        // Curves
        val ptsArr = JSONArray()
        for (pt in p.editState.curves.points) {
            val ptObj = JSONObject()
            ptObj.put("x", pt.x.toDouble())
            ptObj.put("y", pt.y.toDouble())
            ptsArr.put(ptObj)
        }
        es.put("curves", ptsArr)

        // Grain
        val grain = JSONObject()
        grain.put("amount", p.editState.grain.amount)
        grain.put("size", p.editState.grain.size)
        grain.put("roughness", p.editState.grain.roughness)
        grain.put("preset", p.editState.grain.preset.name)
        es.put("grain", grain)

        obj.put("editState", es)
        return obj
    }

    private fun deserializeProject(obj: JSONObject): SavedProject {
        val id = obj.getString("id")
        val name = obj.getString("name")
        val imageUri = obj.getString("imageUri")
        val width = obj.getInt("originalWidth")
        val height = obj.getInt("originalHeight")
        val timestamp = obj.getLong("timestamp")

        val esObj = obj.getJSONObject("editState")
        val exposure = esObj.optDouble("exposure", 0.0).toFloat()
        val brightness = esObj.optInt("brightness", 0)
        val contrast = esObj.optInt("contrast", 0)
        val highlights = esObj.optInt("highlights", 0)
        val shadows = esObj.optInt("shadows", 0)
        val whites = esObj.optInt("whites", 0)
        val blacks = esObj.optInt("blacks", 0)
        val clarity = esObj.optInt("clarity", 0)
        val texture = esObj.optInt("texture", 0)
        val sharpness = esObj.optInt("sharpness", 0)
        val noirIntensity = esObj.optInt("noirIntensity", 100)
        val fade = esObj.optInt("fade", 0)

        val presetId = try {
            NoirPresetId.valueOf(esObj.optString("presetId", "PURE"))
        } catch (_: Exception) {
            NoirPresetId.PURE
        }

        val vAmount = esObj.optInt("vignetteAmount", 0)
        val vMidpoint = esObj.optInt("vignetteMidpoint", 50)
        val vFeather = esObj.optInt("vignetteFeather", 60)
        val vRoundness = esObj.optInt("vignetteRoundness", 50)

        var bwMix = BwMixState()
        if (esObj.has("bwMix")) {
            val m = esObj.getJSONObject("bwMix")
            bwMix = BwMixState(
                reds = m.optInt("reds", 0),
                oranges = m.optInt("oranges", 0),
                yellows = m.optInt("yellows", 0),
                greens = m.optInt("greens", 0),
                cyans = m.optInt("cyans", 0),
                blues = m.optInt("blues", 0),
                magentas = m.optInt("magentas", 0)
            )
        }

        var curves = CurveState()
        if (esObj.has("curves")) {
            val cArr = esObj.getJSONArray("curves")
            val pts = mutableListOf<CurvePoint>()
            for (j in 0 until cArr.length()) {
                val ptObj = cArr.getJSONObject(j)
                pts.add(CurvePoint(ptObj.getDouble("x").toFloat(), ptObj.getDouble("y").toFloat()))
            }
            if (pts.isNotEmpty()) curves = CurveState(pts)
        }

        var grain = GrainState()
        if (esObj.has("grain")) {
            val g = esObj.getJSONObject("grain")
            val gPreset = try {
                GrainPreset.valueOf(g.optString("preset", "NONE"))
            } catch (_: Exception) {
                GrainPreset.NONE
            }
            grain = GrainState(
                amount = g.optInt("amount", 0),
                size = g.optInt("size", 30),
                roughness = g.optInt("roughness", 50),
                preset = gPreset
            )
        }

        val editState = EditState(
            exposure = exposure,
            brightness = brightness,
            contrast = contrast,
            highlights = highlights,
            shadows = shadows,
            whites = whites,
            blacks = blacks,
            clarity = clarity,
            texture = texture,
            sharpness = sharpness,
            noirIntensity = noirIntensity,
            fade = fade,
            presetId = presetId,
            vignetteAmount = vAmount,
            vignetteMidpoint = vMidpoint,
            vignetteFeather = vFeather,
            vignetteRoundness = vRoundness,
            bwMix = bwMix,
            curves = curves,
            grain = grain,
            detail = DetailState()
        )

        return SavedProject(
            id = id,
            name = name,
            imageUri = imageUri,
            originalWidth = width,
            originalHeight = height,
            timestamp = timestamp,
            editState = editState
        )
    }
}
