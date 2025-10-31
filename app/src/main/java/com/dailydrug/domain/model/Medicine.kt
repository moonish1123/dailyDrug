package com.dailydrug.domain.model

import java.time.Instant

/**
 * Domain representation of a medicine the user manages.
 * Keeps UI-independent structure while remaining close to database entity for easy mapping.
 */
data class Medicine(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val color: Int,
    val memo: String = "",
    val createdAt: Instant = Instant.now()
)
