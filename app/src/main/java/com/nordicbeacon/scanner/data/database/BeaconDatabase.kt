package com.nordicbeacon.scanner.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

/**
 * 💾 Room Database - Nordic Beacon Storage
 * 
 * Main database cho Nordic beacon detection persistence
 * Includes migration strategy và database versioning
 * 
 * @author Senior Android Developer  
 */
@Database(
    entities = [BeaconEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BeaconDatabase : RoomDatabase() {
    
    /**
     * 📊 Beacon data access object
     */
    abstract fun beaconDao(): BeaconDao
    
    companion object {
        const val DATABASE_NAME = "nordic_beacon_database"
        
        /**
         * 🏗️ Create database instance với proper configuration
         */
        fun create(context: Context): BeaconDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                BeaconDatabase::class.java,
                DATABASE_NAME
            )
            .addMigrations(MIGRATION_1_2) // Future migrations
            .addCallback(DatabaseCallback())
            .fallbackToDestructiveMigration() // Development only - remove in production
            .build()
        }
        
        /**
         * 🔄 Database migration from version 1 to 2 (future)
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Future schema changes will go here
                // Example: database.execSQL("ALTER TABLE beacon_sightings ADD COLUMN new_column TEXT")
            }
        }
    }
    
    /**
     * 🔧 Database creation callback cho initial setup
     */
    private class DatabaseCallback : RoomDatabase.Callback() {
        
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            
            // Database creation logging
            android.util.Log.i("BeaconDatabase", "✅ Nordic beacon database created successfully")
            
            // Optional: Pre-populate với default data nếu cần
            // insertDefaultData(db)
        }
        
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            android.util.Log.d("BeaconDatabase", "📂 Nordic beacon database opened")
            
            // Optional: Database optimization on open
            // db.execSQL("PRAGMA optimize")
        }
    }
}

/**
 * 🔄 Type Converters cho Room database
 * 
 * Handles conversion của complex types cho database storage
 */
class Converters {
    // Currently no custom type conversions needed
    // Future: Date conversions, enum conversions, etc.
}
