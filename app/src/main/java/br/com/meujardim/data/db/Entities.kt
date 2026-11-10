package br.com.meujardim.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.meujardim.util.toLocalDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class LightNeed(val label: String, val description: String) {
    SOMBRA("Sombra", "Pouca luz, longe de janelas"),
    MEIA_SOMBRA("Meia-sombra", "Luz suave ou filtrada"),
    LUZ_INDIRETA("Luz indireta", "Bem iluminado, sem sol direto"),
    SOL_PLENO("Sol pleno", "Algumas horas de sol direto")
}

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val species: String,
    /** Caminho absoluto da foto copiada para a pasta interna do app. */
    val photoPath: String? = null,
    val room: String,
    val frequencyDays: Int,
    /** true quando a frequência veio da sugestão automática da espécie. */
    val frequencyIsAuto: Boolean,
    val waterAmountMl: Int,
    val lastWateredAt: Long? = null,
    val nextWateringDate: LocalDate,
    val light: LightNeed,
    val notes: String = "",
    /** Quando o usuário toca em "Lembrar mais tarde", guarda até quando o aviso foi adiado. */
    val snoozedUntil: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isDue(today: LocalDate = LocalDate.now()) = !nextWateringDate.isAfter(today)
    fun isOverdue(today: LocalDate = LocalDate.now()) = nextWateringDate.isBefore(today)
    fun daysOverdue(today: LocalDate = LocalDate.now()) =
        ChronoUnit.DAYS.between(nextWateringDate, today).coerceAtLeast(0)
}

@Entity(
    tableName = "watering_logs",
    foreignKeys = [
        ForeignKey(
            entity = Plant::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plantId"), Index("wateredAt")]
)
data class WateringLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val wateredAt: Long,
    /** Data em que a rega estava prevista quando foi registrada. */
    val scheduledDate: LocalDate
)

/** Dias de atraso em relação à data prevista (0 quando foi no dia ou antes). */
val WateringLog.daysLate: Long
    get() = ChronoUnit.DAYS.between(scheduledDate, wateredAt.toLocalDate()).coerceAtLeast(0)

val WateringLog.wasLate: Boolean get() = daysLate > 0

/** Registro de rega acompanhado do nome e da foto da planta, para o histórico geral. */
data class LogEntry(
    @Embedded val log: WateringLog,
    val plantName: String,
    val plantPhoto: String?
)
