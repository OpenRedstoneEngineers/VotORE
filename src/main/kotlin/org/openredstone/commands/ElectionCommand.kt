package org.openredstone.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import net.md_5.bungee.api.connection.ProxiedPlayer
import org.openredstone.*
import org.openredstone.entity.Ballot

@CommandAlias("election|elec")
@Description("A command to manage and participate in elections")
class ElectionCommand(
    private val votore: VotOre,
) : BaseCommand() {
    @Default
    @CatchUnknown
    @Subcommand("info")
    @CommandPermission("election.info")
    fun info(player: ProxiedPlayer) {
        player.sendVotore("VotORE version ${votore.description.version}")
    }

    @Subcommand("manage")
    @CommandPermission("election.manage")
    inner class Manage : BaseCommand() {
        @Subcommand("create")
        fun create(player: ProxiedPlayer, message: String) {
            val elecId = votore.database.currentElectionId()
            if (elecId != null) throw VotOreException("Election already in progress.")
            val ballot = message
                .split("\\s".toRegex())
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
            votore.database.startElection(ballot, player.uniqueId)
            player.sendVotore("Created election with ballot: ${ballot.joinToString(", ")}")
        }

        @Subcommand("end")
        @Conditions("activeelection")
        fun end(player: ProxiedPlayer) {
            val elecId = votore.database.currentElectionId()
            votore.database.endElection(elecId!!)
            player.sendVotore("Ended election. View results by running '/election manage results'")
        }

        @Subcommand("add")
        fun add(player: ProxiedPlayer, @Single candidate: String) {
            player.sendVotore("TODO - Unimplemented")
        }

        @Subcommand("remove")
        fun remove(player: ProxiedPlayer, @Single candidate: String) {
            player.sendVotore("TODO - Unimplemented")
        }

        @Subcommand("results")
        fun results(player: ProxiedPlayer) {
            player.sendVotore("TODO - Unimplemented")
        }
    }

    @Subcommand("vote")
    @Conditions("activeelection")
    @CommandPermission("election.vote")
    inner class Vote : BaseCommand() {

        @Default
        @CommandAlias("vote")
        @Subcommand("vote")
        fun vote(player: ProxiedPlayer) {
            when {
                player.uniqueId in votore.ballots -> {
                    player.printBallot(votore.ballots[player.uniqueId]!!, "You are already voting!")
                }
                votore.database.voterExists(votore.database.currentElectionId()!!, player.uniqueId) -> {
                    player.sendVotoreError("You have already voted in this election!")
                }
                else -> {
                    votore.ballots[player.uniqueId] = Ballot(
                        votore.database.electionBallot(votore.database.currentElectionId()!!).shuffled().toMutableList(),
                        emptyList<String>().toMutableList()
                    )
                    player.printBallot(votore.ballots[player.uniqueId]!!)
                }
            }
        }

        @Subcommand("moveup")
        fun moveup(player: ProxiedPlayer, ballot: Ballot, @Single candidate: String) {
            when (candidate) {
                in ballot.includedNominees -> {
                    if (ballot.includedNominees.first() == candidate) {
                        player.printBallot(ballot, "Candidate cannot be moved up!")
                    } else {
                        val insertIndex = ballot.includedNominees.indexOf(candidate) - 1
                        ballot.includedNominees.remove(candidate)
                        ballot.includedNominees.add(insertIndex, candidate)
                        player.printBallot(ballot)
                    }
                }
                in ballot.excludedNominees -> {
                    ballot.includedNominees.add(candidate)
                    ballot.excludedNominees.remove(candidate)
                    player.printBallot(ballot)
                }
                else -> player.printBallot(ballot, "Candidate not part of ballot!")
            }
        }

        @Subcommand("movedown")
        fun movedown(player: ProxiedPlayer, ballot: Ballot, @Single candidate: String) {
            when (candidate) {
                in ballot.includedNominees -> {
                    if (ballot.includedNominees.last() == candidate) {
                        ballot.includedNominees.remove(candidate)
                        ballot.excludedNominees.add(candidate)
                    } else {
                        val insertIndex = ballot.includedNominees.indexOf(candidate) + 1
                        ballot.includedNominees.remove(candidate)
                        ballot.includedNominees.add(insertIndex, candidate)
                    }
                    player.printBallot(ballot)
                }
                in ballot.excludedNominees -> player.printBallot(ballot, "Cannot manipulate excluded candidates!")
                else -> player.printBallot(ballot, "Candidate not part of ballot!")
            }
        }

        @Subcommand("remove")
        fun remove(player: ProxiedPlayer, ballot: Ballot, @Single candidate: String) {
            when (candidate) {
                in ballot.includedNominees -> {
                    ballot.includedNominees.remove(candidate)
                    ballot.excludedNominees.add(candidate)
                    player.printBallot(ballot)
                }
                in ballot.excludedNominees -> player.printBallot(ballot, "Cannot remove candidate who is not part of included ballots!")
                else -> player.printBallot(ballot, "Candidate not part of ballot!")
            }
        }

        @Subcommand("addback")
        fun addback(player: ProxiedPlayer, ballot: Ballot, @Single candidate: String) {
            when (candidate) {
                in ballot.includedNominees -> player.printBallot(ballot, "Cannot add candidate who is already part of included ballot!")
                in ballot.excludedNominees -> {
                    ballot.excludedNominees.remove(candidate)
                    ballot.includedNominees.add(candidate)
                    player.printBallot(ballot)
                }
                else -> player.printBallot(ballot, "Candidate not part of ballot!")
            }
        }

        @Subcommand("cancel")
        fun cancel(player: ProxiedPlayer, ballot: Ballot) {
            votore.ballots.remove(player.uniqueId)
            player.sendVotore("Cancelled voting. You can vote any time by running '/vote'.")
        }

        @Subcommand("modifyballot")
        fun modifyballot(player: ProxiedPlayer, ballot: Ballot) {
            ballot.submit = false
            player.printBallot(ballot)
        }

        @Subcommand("submit")
        fun submit(player: ProxiedPlayer, ballot: Ballot) {
            ballot.submit = true
            player.printSubmittableBallot(ballot)
        }

        @CommandAlias("confirmvote")
        @Subcommand("confirmvote")
        fun confirmvote(player: ProxiedPlayer, ballot: Ballot) {
            if (!ballot.submit) {
                player.printBallot(ballot, "You have no vote to confirm.")
                return
            }
            if (ballot.includedNominees.isEmpty()) {
                player.printBallot(ballot, "You cannot submit a blank vote.")
                ballot.submit = false
                return
            }
            val electionId = votore.database.currentElectionId()!!
            if (votore.database.voterExists(electionId, player.uniqueId)) {
                player.sendVotoreError("You have already voted this election.")
                votore.ballots.remove(player.uniqueId)
                return
            }
            if (!votore.database.electionBallot(electionId).containsAll(ballot.includedNominees)) {
                player.sendVotoreError("The ballot you are trying to submit is malformed.")
                player.sendVotoreError("If you are encountering this error after guided voting, please tell Staff immediately.")
                player.sendVotoreError("You may restart your voting process at any time.")
                votore.ballots.remove(player.uniqueId)
                return
            }
            votore.logger.info("Confirmed by ${player.name}:${player.uniqueId} vote: ${ballot.includedNominees.joinToString(", ")}")
            println("Confirmed vote: ${ballot.includedNominees.joinToString(", ")}")
            player.sendVotore("Your vote, in order of preference: ${ballot.includedNominees.joinToString(", ")}.")
            player.sendVotore("Excluded from your vote: ${ballot.excludedNominees.joinToString(", ")}")
            player.sendVotore("If there is an error in your submitted vote, please tell Staff immediately.")
            votore.database.insertVote(electionId, player.uniqueId, ballot.includedNominees)
            votore.ballots.remove(player.uniqueId)
        }
    }
}

class VotOreException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
