package com.example.ethktprototype.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import android.util.Log
import androidx.room.TypeConverters
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.File

@Database(
    entities = [
        TransactionEntity::class,
        PatientEntity::class,
        PractitionerEntity::class,
        DocumentReferenceEntity::class,
        MedicationRequestEntity::class,
        MedicationStatementEntity::class,
        ConditionEntity::class,
        AllergyIntoleranceEntity::class,
        DiagnosticReportEntity::class,
        ImmunizationEntity::class,
        DeviceEntity::class,
        ProcedureEntity::class,
        EncounterEntity::class,
        ObservationEntity::class,
        ZkpEntity::class
               ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, passphrase: String): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphraseBytes = SQLiteDatabase.getBytes(passphrase.toCharArray())
                val factory = SupportFactory(passphraseBytes)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database_encrypted"
                )
                    .openHelperFactory(factory)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
    fun clearDatabase() {
        INSTANCE?.clearAllTables()
    }
    fun logDbSize(context: Context) {
        val dbFile = context.getDatabasePath("app_database_encrypted")
        val walFile = File(dbFile.parent, "app_database_encrypted-wal")
        val shmFile = File(dbFile.parent, "app_database_encrypted-shm")

        val totalSize = dbFile.length() + walFile.length() + shmFile.length()
        val sizeInMB = totalSize / (1024.0 * 1024.0)

        Log.d("DatabaseSize", "Total DB size including WAL: %.4f MB".format(sizeInMB))
    }
}