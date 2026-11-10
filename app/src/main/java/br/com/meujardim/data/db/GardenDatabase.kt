package br.com.meujardim.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun lightToString(light: LightNeed?): String? = light?.name

    @TypeConverter
    fun stringToLight(value: String?): LightNeed? =
        value?.let { runCatching { LightNeed.valueOf(it) }.getOrDefault(LightNeed.LUZ_INDIRETA) }
}

@Database(
    entities = [Plant::class, WateringLog::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class GardenDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun wateringLogDao(): WateringLogDao

    companion object {
        fun create(context: Context): GardenDatabase =
            Room.databaseBuilder(context, GardenDatabase::class.java, "meu_jardim.db").build()
    }
}
