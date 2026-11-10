package br.com.meujardim.ui.plants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meujardim.AppContainer
import br.com.meujardim.data.catalog.SpeciesCatalog
import br.com.meujardim.data.catalog.SpeciesInfo
import br.com.meujardim.data.db.LightNeed
import br.com.meujardim.data.db.Plant
import br.com.meujardim.util.PhotoStorage
import br.com.meujardim.util.atTimeMillis
import br.com.meujardim.util.toLocalDate
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class PlantForm(
    val name: String = "",
    val species: String = "",
    val photoPath: String? = null,
    val room: String = "Sala",
    val autoFrequency: Boolean = true,
    val frequencyDays: Int = SpeciesCatalog.fallback.frequencyDays,
    val waterAmountMl: Int = SpeciesCatalog.fallback.waterAmountMl,
    val light: LightNeed = LightNeed.LUZ_INDIRETA,
    val lastWatered: LocalDate? = LocalDate.now(),
    val nextWatering: LocalDate = LocalDate.now().plusDays(SpeciesCatalog.fallback.frequencyDays.toLong()),
    /** true quando o usuário escolheu manualmente a próxima data. */
    val nextOverridden: Boolean = false,
    val notes: String = ""
) {
    val matchedSpecies: SpeciesInfo? get() = SpeciesCatalog.find(species)
}

class EditPlantViewModel(private val app: AppContainer, private val plantId: Long?) : ViewModel() {

    var form by mutableStateOf(PlantForm())
        private set
    var loaded by mutableStateOf(plantId == null)
        private set
    var nameError by mutableStateOf(false)
        private set

    private var original: Plant? = null

    val isNew: Boolean get() = plantId == null

    init {
        if (plantId != null) viewModelScope.launch {
            val p = app.repository.getPlant(plantId)
            if (p != null) {
                original = p
                val last = p.lastWateredAt?.toLocalDate()
                form = PlantForm(
                    name = p.name,
                    species = p.species,
                    photoPath = p.photoPath,
                    room = p.room,
                    autoFrequency = p.frequencyIsAuto,
                    frequencyDays = p.frequencyDays,
                    waterAmountMl = p.waterAmountMl,
                    light = p.light,
                    lastWatered = last,
                    nextWatering = p.nextWateringDate,
                    nextOverridden = p.nextWateringDate != computeNext(last, p.frequencyDays),
                    notes = p.notes
                )
            }
            loaded = true
        }
    }

    fun suggestions(): List<SpeciesInfo> =
        if (form.matchedSpecies != null) emptyList() else SpeciesCatalog.search(form.species)

    fun onName(value: String) {
        form = form.copy(name = value)
        if (value.isNotBlank()) nameError = false
    }

    fun onSpecies(value: String) {
        form = applyAuto(form.copy(species = value))
    }

    fun pickSpecies(info: SpeciesInfo) {
        form = applyAuto(form.copy(species = info.popularName, light = if (form.autoFrequency) info.light else form.light))
    }

    fun onRoom(value: String) { form = form.copy(room = value) }

    fun onAutoFrequency(enabled: Boolean) {
        form = if (enabled) applyAuto(form.copy(autoFrequency = true)) else form.copy(autoFrequency = false)
    }

    /** Alterar a frequência manualmente desliga a sugestão automática. */
    fun onFrequency(days: Int) {
        form = recompute(form.copy(frequencyDays = days.coerceIn(1, 90), autoFrequency = false))
    }

    fun onWaterAmount(ml: Int) {
        form = form.copy(waterAmountMl = ml.coerceIn(25, 5000), autoFrequency = false)
    }

    fun onLight(light: LightNeed) { form = form.copy(light = light) }

    fun onLastWatered(date: LocalDate?) { form = recompute(form.copy(lastWatered = date)) }

    fun onNextWatering(date: LocalDate) { form = form.copy(nextWatering = date, nextOverridden = true) }

    fun resetNextWatering() { form = recompute(form.copy(nextOverridden = false)) }

    fun onNotes(value: String) { form = form.copy(notes = value) }

    fun onPhoto(path: String?) {
        // Remove a foto temporária substituída se ela não for a original salva.
        val previous = form.photoPath
        if (previous != null && previous != original?.photoPath && previous != path) PhotoStorage.delete(previous)
        form = form.copy(photoPath = path)
    }

    fun save(onSaved: (Long) -> Unit) {
        val f = form
        if (f.name.isBlank()) {
            nameError = true
            return
        }
        viewModelScope.launch {
            val base = original
            val lastDate = f.lastWatered
            val originalLast = base?.lastWateredAt
            val lastMillis: Long? = when {
                lastDate == null -> null
                originalLast != null && originalLast.toLocalDate() == lastDate -> originalLast
                lastDate == LocalDate.now() -> System.currentTimeMillis()
                else -> lastDate.atTimeMillis(LocalTime.NOON)
            }
            val plant = (base ?: Plant(
                name = "", species = "", room = "", frequencyDays = 1, frequencyIsAuto = true,
                waterAmountMl = 0, nextWateringDate = LocalDate.now(), light = LightNeed.LUZ_INDIRETA
            )).copy(
                name = f.name.trim(),
                species = f.species.trim(),
                photoPath = f.photoPath,
                room = f.room.trim().ifBlank { "Sala" },
                frequencyDays = f.frequencyDays,
                frequencyIsAuto = f.autoFrequency,
                waterAmountMl = f.waterAmountMl,
                lastWateredAt = lastMillis,
                nextWateringDate = f.nextWatering,
                light = f.light,
                notes = f.notes.trim()
            )
            val id = app.repository.savePlant(plant)
            onSaved(id)
        }
    }

    /** Ao sair sem salvar, apaga a foto nova que não chegou a ser usada. */
    fun discardUnsavedPhoto() {
        val p = form.photoPath
        if (p != null && p != original?.photoPath) PhotoStorage.delete(p)
    }

    private fun applyAuto(f: PlantForm): PlantForm {
        if (!f.autoFrequency) return f
        val info = f.matchedSpecies ?: SpeciesCatalog.fallback
        return recompute(
            f.copy(
                frequencyDays = info.frequencyDays,
                waterAmountMl = info.waterAmountMl,
                light = if (f.matchedSpecies != null) info.light else f.light
            )
        )
    }

    private fun recompute(f: PlantForm): PlantForm =
        if (f.nextOverridden) f else f.copy(nextWatering = computeNext(f.lastWatered, f.frequencyDays))

    private fun computeNext(last: LocalDate?, frequency: Int): LocalDate =
        last?.plusDays(frequency.toLong()) ?: LocalDate.now()
}
