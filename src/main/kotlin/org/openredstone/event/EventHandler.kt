package org.openredstone.event

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.proxy.Player
import org.openredstone.VotOre
import org.openredstone.printBallot

class EventHandler(
    private val votOre: VotOre
) {
    @Subscribe
    fun onLeave(event: DisconnectEvent) {
        votOre.ballots.remove(event.player.uniqueId)
    }

    @Subscribe
    fun onChat(event: PlayerChatEvent) {
        if (event.player !is Player) {
            return
        }
        if (event.message.startsWith("/")) {
            return
        }
        if (votOre.ballots.containsKey(event.player.uniqueId)) {
            event.player.printBallot(votOre.ballots[event.player.uniqueId]!!, "You are still in the process of voting!")
        }
    }
}
