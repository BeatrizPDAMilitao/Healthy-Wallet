package com.example.ethktprototype.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.TypeConverters
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory


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
}