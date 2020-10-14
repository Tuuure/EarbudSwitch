package app.tuuure.earbudswitch.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DbRecord::class], version = 1, exportSchema = false)
abstract class EarbudsDatabase : RoomDatabase() {
    abstract fun dbDao(): DbDao

    companion object {
        fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                EarbudsDatabase::class.java,
                "Earbuds.db"
            ).build()
    }
}
