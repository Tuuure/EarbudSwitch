package app.tuuure.earbudswitch.data.db

import androidx.room.*

@Dao
interface DbDao {
    @Query("SELECT * FROM earbuds")
    fun getAll(): List<DbRecord>

    @Query("SELECT * FROM earbuds WHERE address = :address")
    fun findByAddress(address: String): DbRecord

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg items: DbRecord)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg items: DbRecord)

    @Delete
    fun delete(vararg items: DbRecord)

    @Query("DELETE FROM earbuds")
    fun deleteAll()
}