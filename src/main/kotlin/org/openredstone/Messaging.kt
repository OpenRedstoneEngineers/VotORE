package org.openredstone

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.hover.content.Text
import net.md_5.bungee.api.connection.ProxiedPlayer
import org.openredstone.entity.Ballot


// TODO ........... clean this up
fun ProxiedPlayer.printSubmittableBallot(ballot: Ballot) {
    for (i in 1..100) this.sendMessage(*ComponentBuilder().create())
    this.sendMessage(
        *ComponentBuilder()
            .append("   ").reset().color(ChatColor.YELLOW).strikethrough(true)
            .append(" Your current submittable ballot ").reset().color(ChatColor.GOLD).bold(true)
            .append("   ").reset().color(ChatColor.YELLOW).strikethrough(true)
            .create()
    )
    this.sendMessage(
        *ComponentBuilder()
            .append(" Shown below are the candidates you have selected for your ballot in order from most to least preferred. ")
            .color(ChatColor.GRAY)
            .create()
    )
    ballot.includedNominees.forEachIndexed { i, it ->
        this.sendMessage(
            *ComponentBuilder()
                .append("${i + 1}").color(ChatColor.GOLD).bold(true)
                .append(" - ").reset().color(ChatColor.WHITE)
                .append(it).color(ChatColor.YELLOW).bold(true)
                .create()
        )
    }
    this.sendMessage(
        *ComponentBuilder()
            .append("(Not selected in your vote: ${ballot.excludedNominees.joinToString(", ")})").color(ChatColor.GRAY)
            .create()
    )
    this.sendMessage(
        *ComponentBuilder()
            .append("Click here to modify your ballot").color(ChatColor.GOLD).bold(true)
            .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/election vote modifyballot"))
            .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("Modify your ballot")))
            .create()
    )
    this.sendMessage(
        *ComponentBuilder()
            .append("Run ").color(ChatColor.GRAY)
            .append("/confirmvote").reset().color(ChatColor.RED).bold(true)
            .append(" to submit your ballot (THIS ACTION CAN NOT BE UNDONE!)").reset().color(ChatColor.GRAY)
            .create()
    )
}

// TODO .................... clean this up also
fun ProxiedPlayer.printBallot(ballot: Ballot, error: String? = null) {
    for (i in 1..100) this.sendMessage(*ComponentBuilder().create())
    error?.let {
        this.sendMessage(
            *ComponentBuilder()
                .append("!").color(ChatColor.DARK_RED).bold(true)
                .append(" - ").reset().color(ChatColor.DARK_GRAY)
                .append(" Error: $error").reset().color(ChatColor.GRAY)
                .create()
        )
    }
    this.sendMessage(
        *ComponentBuilder()
            .append("   ").reset().color(ChatColor.YELLOW).strikethrough(true)
            .append(" Your current ballot ").reset().color(ChatColor.GOLD).bold(true)
            .append("      ").reset().color(ChatColor.YELLOW).strikethrough(true)
            .append(" ☑ ").reset().color(ChatColor.GREEN).bold(true)
            .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/election vote submit"))
            .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("Submit your vote (You can alter this later)")))
            .append("|").reset().color(ChatColor.YELLOW).bold(true)
            .append(" ☒ ").reset().color(ChatColor.RED).bold(true)
            .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/election vote cancel"))
            .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("Halt voting (You can vote again any time)")))
            .create()
    )
    ballot.includedNominees.forEach {
        this.sendMessage(
            *ComponentBuilder()
                .apply {
                    if (ballot.includedNominees.indexOf(it) != 0) {
                        this.append("ᐱ").reset().color(ChatColor.YELLOW).bold(true)
                            .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/election vote moveup $it"))
                            .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("Move $it up your ballot")))
                            .append(" | ").reset().color(ChatColor.GOLD).bold(true)
                    } else {
                        this.append("    ")
                    }
                }
                .append("ᐯ").reset().color(ChatColor.YELLOW).bold(true)
                .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/election vote movedown $it"))
                .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("Move $it down your ballot")))
                .append(" | ").reset().color(ChatColor.GOLD).bold(true)
                .append("✕").reset().color(ChatColor.RED).bold(true)
                .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/election vote remove $it"))
                .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("Remove $it from your ballot")))
                .append("   $it").reset().color(ChatColor.WHITE)
                .create()
        )
    }
    if (ballot.excludedNominees.isNotEmpty()) {
        this.sendMessage(
            *ComponentBuilder()
                .append("   ").color(ChatColor.YELLOW).strikethrough(true)
                .append(" Persons not included in your ballot ").reset().color(ChatColor.GOLD).bold(true)
                .append("   ").color(ChatColor.YELLOW).strikethrough(true)
                .create()
        )
        ballot.excludedNominees.sortedWith(String.CASE_INSENSITIVE_ORDER).forEach {
            this.sendMessage(
                *ComponentBuilder()
                    .append("ᐱ").color(ChatColor.YELLOW).bold(true)
                    .event(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/election vote addback $it"))
                    .event(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("Add $it back to your ballot")))
                    .append(" - $it").reset().color(ChatColor.WHITE)
                    .create()
            )
        }
    }
}

fun ProxiedPlayer.sendVotoreError(message: String) =
    this.sendVotore(
        ComponentBuilder()
            .append("[").color(ChatColor.DARK_GRAY)
            .append("!").color(ChatColor.RED).bold(true)
            .append("] ").reset().color(ChatColor.DARK_GRAY)
            .append(message).color(ChatColor.GRAY)
            .create()
    )

fun ProxiedPlayer.sendVotore(message: String) =
    this.sendVotore(
        ComponentBuilder()
            .append(message)
            .create()
    )

fun ProxiedPlayer.sendVotore(component: Array<BaseComponent>) =
    this.sendMessage(
        *ComponentBuilder()
            .append("[").color(ChatColor.DARK_GRAY)
            .append("VotORE").color(ChatColor.GRAY)
            .append("] ").color(ChatColor.DARK_GRAY)
            .append(component).color(ChatColor.GRAY)
            .create()
    )
