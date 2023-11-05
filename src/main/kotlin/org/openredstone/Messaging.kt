package org.openredstone

import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.openredstone.entity.Ballot

val miniMessage = MiniMessage.miniMessage()
const val votoreBaseMessage = "<dark_gray>[<gray>VotORE<dark_gray>] <reset><message>"
const val votoreErrorMessage = "<dark_gray>[<red><b>!</b><gray>VotORE<dark_gray>] <reset><message>"

fun Player.printSubmittableBallot(ballot: Ballot) {
    for (i in 1..100) this.sendMessage(miniMessage.deserialize("<newline>"))
    this.sendMessage(miniMessage.deserialize(
        "<yellow><strikethrough>   <reset><gold> Your current submittable ballot <yellow><strikethrough>   "
    ))
    this.sendMessage(miniMessage.deserialize(
        "<gray> Shown below are the candidates you have selected for your ballot in order from most to least preferred. "
    ))
    ballot.includedNominees.forEachIndexed { i, it ->
        this.sendMessage(miniMessage.deserialize(
            "<gold><b>${i + 1}<reset><white> - <yellow><b>$it"
        ))
    }
    this.sendMessage(miniMessage.deserialize(
        "<gray>(Not selected in your vote: ${ballot.excludedNominees.joinToString(", ")})"
    ))
    this.sendMessage(miniMessage.deserialize(
        "<gold><b><click:run_command:'/election vote modifyballot'><hover:show_text:'Modify your ballot'>" +
            "Click here to modify your ballot</hover></click>"
    ))
    this.sendMessage(miniMessage.deserialize(
        "<gray>Run <red><b>/confirmvote<reset><gray> to submit your ballot " +
            "<red><b>(THIS ACTION CAN NOT BE UNDONE!)"
    ))
}

fun Player.printBallot(ballot: Ballot, error: String? = null) {
    for (i in 1..100) this.sendMessage(miniMessage.deserialize("<newline>"))
    error?.let {
        this.sendMessage(miniMessage.deserialize(
            "<dark_red><b>!<reset><dark_gray> - <gray>Error: $error"
        ))
    }
    this.sendMessage(miniMessage.deserialize(
        "<yellow><strikethrough>   <reset><gold> Your current ballot <yellow><strikethrough>      <reset>" +
            "<green><b><click:run_command:'/election vote submit'>" +
            "<hover:show_text:'Submit your vote (You can alter this later)'> ☑ </hover></click></green>" +
            "<yellow>|</yellow>" +
            "<red><b><click:run_command:'/election vote cancel'>" +
            "<hover:show_text:'Halt voting (You can vote again any time)'> ☒ </hover></click></b></red>"
    ))
    ballot.includedNominees.forEach {
        val prefix = if (ballot.includedNominees.indexOf(it) != 0) {
            "<click:run_command:'/election vote moveup $it'><hover:show_text:'Move $it up your ballot'><yellow><b>ᐱ</b></yellow></hover></click><gold><b> | "
        } else {
            "     "
        }
        this.sendMessage(miniMessage.deserialize(
            "$prefix<yellow><b><click:run_command:'/election vote movedown $it'>" +
                "<hover:show_text:'Move $it down your ballot'>ᐯ</hover></click></b></yellow><gold><b> | </b></gold>" +
                "<red><b><click:run_command:'/election vote remove $it'>" +
                "<hover:show_text:'Remove $it from your ballot'>✕</hover></click></b></red><reset><white>   $it</white>"
        ))
    }
    if (ballot.excludedNominees.isNotEmpty()) {
        this.sendMessage(miniMessage.deserialize(
            "<yellow><strikethrough>   <reset><gold> Persons not included in your ballot <yellow><strikethrough>   "
        ))
        ballot.excludedNominees.sortedWith(String.CASE_INSENSITIVE_ORDER).forEach {
            this.sendMessage(miniMessage.deserialize(
                "<yellow><b><click:run_command:'/election vote addback $it'>" +
                    "<hover:show_text:'Add $it back to your ballot'>ᐱ</hover></click></b></yellow><white> - $it"
            ))
        }
    }
}

fun Player.sendVotoreError(message: String) =
    this.sendMessage(
        votoreErrorMessage.render(message)
    )

fun Player.sendVotore(message: String) =
    this.sendMessage(
        votoreBaseMessage.render(message)
    )

fun Player.sendVotore(message: Component) =
    this.sendMessage(
        votoreBaseMessage.render(message)
    )

fun String.render(
    message: String
): Component = this.render(
    mapOf("message" to Component.text(message))
)

fun String.render(
    message: Component,
): Component = this.render(
    mapOf("message" to message)
)

fun String.render(
    replacements: Map<String, Component> = emptyMap()
): Component = miniMessage.deserialize(
    this,
    *replacements.map { Placeholder.component(it.key, it.value) }.toTypedArray()
)
