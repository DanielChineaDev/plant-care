package com.BPO.plantcare.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migraciones del esquema de Room.
 *
 * Checklist al subir version:
 * 1. Bumpea @Database(version = N+1)
 * 2. Anade aqui una Migration(N, N+1)
 * 3. Anade la migracion a DatabaseModule.addMigrations(...)
 * 4. Verifica en un dispositivo con datos de la version anterior
 */

/** v1 -> v2: anade tabla watering_logs (historial de riegos). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `watering_logs` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `plantId` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `note` TEXT,
                FOREIGN KEY(`plantId`) REFERENCES `plants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_watering_logs_plantId` ON `watering_logs` (`plantId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_watering_logs_timestamp` ON `watering_logs` (`timestamp`)"
        )
    }
}

/** v2 -> v3: anade tabla plant_photos (diario fotografico). */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `plant_photos` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `plantId` INTEGER NOT NULL,
                `path` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `note` TEXT,
                FOREIGN KEY(`plantId`) REFERENCES `plants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_plant_photos_plantId` ON `plant_photos` (`plantId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_plant_photos_timestamp` ON `plant_photos` (`timestamp`)"
        )
    }
}

/** v3 -> v4: anade columna isOutdoor a plants (nullable). */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `plants` ADD COLUMN `isOutdoor` INTEGER")
    }
}

/**
 * v4 -> v5: anade tabla plant_tasks (tareas de cuidado configurables por
 * planta: abonar, podar, trasplantar, rotar, limpiar hojas, fumigar...).
 *
 * NO seed: no creamos tareas automaticas para plantas existentes. El user
 * las activa explicitamente desde la ficha de planta.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `plant_tasks` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `plantId` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `intervalDays` INTEGER NOT NULL,
                `lastDoneAt` INTEGER,
                `snoozedUntil` INTEGER,
                `enabled` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`plantId`) REFERENCES `plants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_plant_tasks_plantId` ON `plant_tasks` (`plantId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_plant_tasks_plantId_type` ON `plant_tasks` (`plantId`, `type`)"
        )
    }
}

/** v5 -> v6: anade columna room a plants (nullable). */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `plants` ADD COLUMN `room` TEXT")
    }
}

/**
 * v6 -> v7: fotos en la nube. Anade:
 *  - `userPhotoUrl` a plants: URL en Storage de la foto principal del usuario.
 *  - `remoteUrl` a plant_photos: URL en Storage de cada foto de la galeria.
 * Permiten recuperar las fotos al cambiar de dispositivo.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `plants` ADD COLUMN `userPhotoUrl` TEXT")
        db.execSQL("ALTER TABLE `plant_photos` ADD COLUMN `remoteUrl` TEXT")
    }
}

/**
 * v7 -> v8: visibilidad publica del diario fotografico y las notas.
 *   - `photosPublic`: 1 = el diario de fotos se muestra en el perfil publico.
 *   - `notesPublic`:  1 = las notas se muestran en el perfil publico.
 * Ambas en 0 (false) para filas existentes (privado por defecto).
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `plants` ADD COLUMN `photosPublic` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `plants` ADD COLUMN `notesPublic` INTEGER NOT NULL DEFAULT 0")
    }
}

val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
)
