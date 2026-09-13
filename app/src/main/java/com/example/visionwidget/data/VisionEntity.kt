package com.example.visionwidget.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * The stored form of a vision. [isMain] is exclusive to at most one row at a time —
 * enforced by [VisionDao.setMainVision], never by a column constraint — and is the one
 * the widgets and the Today tab read from.
 */
@Entity(tableName = "visions")
data class VisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goal: String,
    val why: String,
    val targetDateMillis: Long,
    val isMain: Boolean
)

/**
 * One step toward a vision. Milestones are deleted along with the vision they belong
 * to — there's nowhere else for an orphaned one to make sense.
 */
@Entity(
    tableName = "milestones",
    foreignKeys = [
        ForeignKey(
            entity = VisionEntity::class,
            parentColumns = ["id"],
            childColumns = ["visionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("visionId")]
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val visionId: Long,
    val step: String,
    val dueDateMillis: Long,
    val checked: Boolean
)

/** A vision joined with its milestones, in the one shape [VisionDao] reads them as. */
data class VisionWithMilestones(
    @Embedded val vision: VisionEntity,
    @Relation(parentColumn = "id", entityColumn = "visionId")
    val milestones: List<MilestoneEntity>
)
