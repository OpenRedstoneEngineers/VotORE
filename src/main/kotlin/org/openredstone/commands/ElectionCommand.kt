package org.openredstone.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.velocitypowered.api.proxy.Player
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
    fun info(player: Player) {
        player.sendVotore("VotORE version ${votore.version}")
    }

    @Subcommand("manage")
    @CommandPermission("election.manage")
    inner class Manage : BaseCommand() {
        @Subcommand("create")
        @CommandCompletion("@range:0-5 @players")
        fun create(player: Player, @Single winners: Int, message: String) {
            val elecId = votore.database.currentElectionId()
            if (elecId != null) throw VotOreException("Election already in progress.")
            val ballot = message
                .split("\\s".toRegex())
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
            votore.database.startElection(winners, ballot, player.uniqueId)
            player.sendVotore("Created election with ballot: ${ballot.joinToString(", ")}")
        }

        @Subcommand("votes")
        @Conditions("activeelection")
        fun votes(player: Player) {
            votore.database.currentElectionId()?.let {
                player.sendVotore("Total votes: ${votore.database.voteCounts(it)}")
            }
        }

        @Subcommand("end")
        @Conditions("activeelection")
        fun end(player: Player) {
            votore.database.currentElectionId()?.let {
                votore.database.endElection(it)
                player.sendVotore(miniMessage.deserialize(
                    "Ended election. View results by running <gray>" +
                        "<click:run_command:'/election manage results'><hover:show_text:'View results'>'/election manage results'"
                ))
            }
        }

        @Subcommand("add")
        fun add(player: Player, @Single candidate: String) {
            player.sendVotore("TODO - Unimplemented")
        }

        @Subcommand("remove")
        fun remove(player: Player, @Single candidate: String) {
            player.sendVotore("TODO - Unimplemented")
        }

        @Subcommand("results")
        fun results(player: Player) {
            val electionId = votore.database.lastElectionId() ?: run {
                player.sendVotoreError("No elections have taken place.")
                return
            }
            val candidates = votore.database.electionBallot(electionId)
            val votes = votore.database.votes(electionId)
            val indexOffset = candidates.keys.minOrNull()?.minus(1) ?: run {
                player.sendVotoreError("List of candidates are empty. (This isn't supposed to happen)")
                return
            }
            val winners = votore.database.electionWinners(electionId)
            val blt = StringBuilder()
            blt.appendLine("${candidates.size} $winners")
            votes.groupBy(keySelector = { it.voter }, valueTransform = { it.candidate - indexOffset }).forEach {
                blt.appendLine("1 ${it.value.joinToString(" ")} 0")
            }
            blt.appendLine("0")
            candidates.values.forEach {
                blt.appendLine("\"$it\"")
            }
            val response = khttp.post(
                url = "https://dpaste.com/api/v2/",
                headers = mapOf("User-Agent" to "ORE Election Services"),
                data = mapOf(
                    "content" to blt.toString(),
                    "syntax" to "text",
                    "title" to "ORE Election Results"
                )
            )
            player.sendVotore(miniMessage.deserialize(
                "URL: <gray><click:open_url:'${response.text.trim()}'>" +
                    "<hover:show_text:'${response.text.trim()}'><u>${response.text.trim()}</u></hover></click>"
            ))
        }
    }

    @Subcommand("vote")
    @Conditions("activeelection")
    @CommandPermission("election.vote")
    inner class Vote : BaseCommand() {

        @Default
        @CommandAlias("vote")
        @Subcommand("vote")
        fun vote(player: Player) {
            when {
                player.uniqueId in votore.ballots -> {
                    player.printBallot(votore.ballots[player.uniqueId]!!, "You are already voting!")
                }
                votore.database.voterExists(votore.database.currentElectionId()!!, player.uniqueId) -> {
                    player.sendVotoreError("You have already voted in this election!")
                }
                else -> {
                    votore.ballots[player.uniqueId] = Ballot(
                        votore.database.electionBallot(votore.database.currentElectionId()!!).values.shuffled()
                            .toMutableList(),
                        emptyList<String>().toMutableList()
                    )
                    player.printBallot(votore.ballots[player.uniqueId]!!)
                }
            }
        }

        @Subcommand("moveup")
        fun moveup(player: Player, ballot: Ballot, @Single candidate: String) {
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
        fun movedown(player: Player, ballot: Ballot, @Single candidate: String) {
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
        fun remove(player: Player, ballot: Ballot, @Single candidate: String) {
            when (candidate) {
                in ballot.includedNominees -> {
                    ballot.includedNominees.remove(candidate)
                    ballot.excludedNominees.add(candidate)
                    player.printBallot(ballot)
                }
                in ballot.excludedNominees -> player.printBallot(
                    ballot,
                    "Cannot remove candidate who is not part of included ballots!"
                )
                else -> player.printBallot(ballot, "Candidate not part of ballot!")
            }
        }

        @Subcommand("addback")
        fun addback(player: Player, ballot: Ballot, @Single candidate: String) {
            when (candidate) {
                in ballot.includedNominees -> player.printBallot(
                    ballot,
                    "Cannot add candidate who is already part of included ballot!"
                )
                in ballot.excludedNominees -> {
                    ballot.excludedNominees.remove(candidate)
                    ballot.includedNominees.add(candidate)
                    player.printBallot(ballot)
                }
                else -> player.printBallot(ballot, "Candidate not part of ballot!")
            }
        }

        @Subcommand("cancel")
        fun cancel(player: Player, ballot: Ballot) {
            votore.ballots.remove(player.uniqueId)
            player.sendVotore("Cancelled voting. You can vote any time by running '/vote'.")
        }

        @Subcommand("modifyballot")
        fun modifyballot(player: Player, ballot: Ballot) {
            ballot.submit = false
            player.printBallot(ballot)
        }

        @Subcommand("submit")
        fun submit(player: Player, ballot: Ballot) {
            ballot.submit = true
            player.printSubmittableBallot(ballot)
        }

        @CommandAlias("confirmvote")
        @Subcommand("confirmvote")
        fun confirmvote(player: Player, ballot: Ballot) {
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
            if (!votore.database.electionBallot(electionId).values.containsAll(ballot.includedNominees)) {
                player.sendVotoreError("The ballot you are trying to submit is malformed.")
                player.sendVotoreError("If you are encountering this error after guided voting, please tell Staff.")
                player.sendVotoreError("You may restart your voting process at any time.")
                votore.ballots.remove(player.uniqueId)
                return
            }
            votore.logger.info(
                "Confirmed by ${player.username}:${player.uniqueId} vote: ${
                    ballot.includedNominees.joinToString(
                        ", "
                    )
                }"
            )
            println("Confirmed vote: ${ballot.includedNominees.joinToString(", ")}")
            player.sendVotore("Your vote, in order of preference: ${ballot.includedNominees.joinToString(", ")}.")
            if (ballot.excludedNominees.isNotEmpty()) {
                player.sendVotore("Excluded from your vote: ${ballot.excludedNominees.joinToString(", ")}")
            }
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
