package org.openredstone.manager

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.openredstone.entity.Sql
import org.openredstone.entity.Vote
import org.openredstone.toBin
import org.openredstone.toUuid
import java.util.*

class Sql(
    host: String,
    port: Int,
    database: String,
    user: String,
    password: String,
    driver: String = "com.mysql.cj.jdbc.Driver"
) {
    private val database = Database.connect(
        "jdbc:mysql://${host}:${port}/${database}",
        driver = driver,
        user = user,
        password = password
    )

    fun initTables() = transaction(database) {
        SchemaUtils.create(
            Sql.Election,
            Sql.Candidate,
            Sql.Vote
        )
    }

    fun destroy() = transaction(database) {
        SchemaUtils.drop(
            Sql.Election,
            Sql.Candidate,
            Sql.Vote
        )
    }

    fun currentElectionId(): Int? = transaction(database) {
        Sql.Election.selectAll()
            .orderBy(Sql.Election.id to SortOrder.DESC)
            .firstOrNull().let {
                it ?: return@transaction null
                if (!it[Sql.Election.determined]) it[Sql.Election.id] else null
            }
    }

    fun lastElectionId(): Int? = transaction(database) {
        Sql.Election.selectAll()
            .orderBy(Sql.Election.id to SortOrder.DESC)
            .firstOrNull()?.get(Sql.Election.id)
    }

    fun electionBallot(electionId: Int): Map<Int, String> = transaction(database) {
        Sql.Election.select {
            Sql.Election.id eq electionId
        }.orderBy(Sql.Election.id to SortOrder.DESC)
            .first().let {
                Sql.Candidate.select {
                    Sql.Candidate.elecId eq it[Sql.Election.id]
                }.orderBy(Sql.Candidate.id to SortOrder.ASC).map {
                    it[Sql.Candidate.id] to it[Sql.Candidate.candidate]
                }.toMap()
            }
    }

    fun startElection(candidates: List<String>, creatorId: UUID) = transaction(database) {
        val elecId = Sql.Election.insert {
            it[creator] = creatorId.toBin()
        } get Sql.Election.id
        Sql.Candidate.batchInsert(candidates) { candidate ->
            this[Sql.Candidate.elecId] = elecId
            this[Sql.Candidate.candidate] = candidate
        }
    }

    fun endElection(electionId: Int) = transaction(database) {
        Sql.Election.update({ Sql.Election.id eq electionId }) {
            it[determined] = true
        }
    }

    fun voteCounts(electionId: Int): Int = transaction(database) {
        Sql.Vote.select {
            Sql.Vote.election eq electionId
        }.map {
            it[Sql.Vote.voter].toUuid()
        }.distinct().size
    }

    fun votes(electionId: Int): List<Vote> = transaction(database) {
        Sql.Vote.select {
            Sql.Vote.election eq electionId
        }.orderBy(Sql.Vote.index to SortOrder.ASC).map {
            Vote(
                it[Sql.Vote.voter].toUuid(),
                it[Sql.Vote.canId],
                it[Sql.Vote.index]
            )
        }
    }

    fun voterExists(electionId: Int, voterId: UUID): Boolean = transaction(database) {
        !Sql.Vote.select {
            (Sql.Vote.voter eq voterId.toBin()) and (Sql.Vote.election eq electionId)
        }.empty()
    }

    fun insertVote(electionId: Int, voterId: UUID, sVote: List<String>) = transaction(database) {
        Sql.Vote.batchInsert(sVote.withIndex()) { (index, candidate) ->
            this[Sql.Vote.voter] = voterId.toBin()
            this[Sql.Vote.index] = index + 1
            this[Sql.Vote.election] = electionId
            Sql.Candidate.select {
                (Sql.Candidate.candidate eq candidate) and (Sql.Candidate.elecId eq electionId)
            }.first().let {
                this[Sql.Vote.canId] = it[Sql.Candidate.id]
            }
        }
    }
}
