package org.openredstone.event

import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ChatEvent
import net.md_5.bungee.api.event.PlayerDisconnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import org.openredstone.VotOre
import org.openredstone.printBallot

class EventHandler(private val votOre: VotOre) : Listener {
    @EventHandler
    fun onLeave(event: PlayerDisconnectEvent) {
        votOre.ballots.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onChat(event: ChatEvent) {
        if (event.sender !is ProxiedPlayer) {
            return
        }
        if (event.isProxyCommand) {
            return
        }
        val player = event.sender as ProxiedPlayer
        if (votOre.ballots.containsKey(player.uniqueId)) {
            event.isCancelled = true
            player.printBallot(votOre.ballots[player.uniqueId]!!, "You have to exit voting in order to chat!")
        }
    }
}
