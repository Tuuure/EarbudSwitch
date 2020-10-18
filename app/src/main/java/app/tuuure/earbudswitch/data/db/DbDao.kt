package app.tuuure.earbudswitch.data.db

import androidx.room.*

@Dao
interface DbDao {
    @Query("SELECT * FROM earbuds")
    fun getAll(): List<Earbud>

    @Query("SELECT * FROM earbuds WHERE address = :address")
    fun findByAddress(address: String): Earbud

    @Query("SELECT * FROM earbuds WHERE isA2dpConnected = 1 OR isHeadSetConnected = 1")
    fun getConnected(): List<Earbud>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg items: Earbud)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg items: Earbud)

    @Delete
    fun delete(vararg items: Earbud)

    @Query("DELETE FROM earbuds")
    fun deleteAll()
}