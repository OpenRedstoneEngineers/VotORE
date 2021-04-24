package org.openredstone.entity

import org.jetbrains.exposed.sql.Table

object Sql {
    object Election : Table("election") {
        val id = integer("elec_id").autoIncrement()
        val creator = binary("elec_creator", 16)
        val determined = bool("elec_determined").default(false)
        override val primaryKey = PrimaryKey(id)
    }

    object Vote : Table("vote") {
        val election = integer("ball_election") references Election.id
        val voter = binary("vote_voter", 16)
        val canId = integer("vote_can") references Candidate.id
        val index = integer("vote_index")
        override val primaryKey = PrimaryKey(election, voter, canId)
    }

    object Candidate : Table("candidate") {
        val id = integer("can_id").autoIncrement()
        val elecId = integer("can_election") references Election.id
        val candidate = varchar("can_user", 16)
        override val primaryKey = PrimaryKey(id)
    }
}

data class Ballot(
    val includedNominees: MutableList<String>,
    val excludedNominees: MutableList<String>,
    var submit: Boolean = false
)
