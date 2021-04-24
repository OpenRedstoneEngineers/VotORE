package org.openredstone.listener

import de.exceptionflug.protocolize.api.event.PacketReceiveEvent
import de.exceptionflug.protocolize.api.event.PacketSendEvent
import de.exceptionflug.protocolize.api.handler.PacketAdapter
import de.exceptionflug.protocolize.api.protocol.Stream
import de.exceptionflug.protocolize.items.packet.BlockPlacement
import de.exceptionflug.protocolize.items.packet.UseItem
import net.md_5.bungee.protocol.packet.Chat
import org.openredstone.VotOre
import org.openredstone.printBallot

class ChatListener(private val votOre: VotOre): PacketAdapter<Chat>(Stream.DOWNSTREAM, Chat::class.java) {
    override fun receive(event: PacketReceiveEvent<Chat>) {
        if (votOre.ballots.containsKey(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }

    override fun send(event: PacketSendEvent<Chat>) {
        if (votOre.ballots.containsKey(event.player.uniqueId)) {
            val packet = event.packet
            if (packet.message.startsWith("/")) {
                event.player.printBallot(votOre.ballots[event.player.uniqueId]!!, "Cannot send commands while voting!")
            } else {
                event.player.printBallot(votOre.ballots[event.player.uniqueId]!!, "You have to exit voting in order to chat!")
            }
            // Set message to empty. Cancelling, or setting to null, breaks the proxy.
            packet.message = ""
        }
    }
}

// FIXME Unused
class UseListener(private val votOre: VotOre): PacketAdapter<UseItem>(Stream.UPSTREAM, UseItem::class.java) {
    override fun send(event: PacketSendEvent<UseItem>) {
        println("send useitem ${event.player.name}")
        if (votOre.ballots.containsKey(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }

    override fun receive(event: PacketReceiveEvent<UseItem>) {
        println("receive useitem ${event.player.name}")
        if (votOre.ballots.containsKey(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }
}

// FIXME Unused
class PlaceListener(private val votOre: VotOre): PacketAdapter<BlockPlacement>(Stream.UPSTREAM, BlockPlacement::class.java) {
    override fun send(event: PacketSendEvent<BlockPlacement>) {
        println("send blockplacement ${event.player.name}")
        if (votOre.ballots.containsKey(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }

    override fun receive(event: PacketReceiveEvent<BlockPlacement>) {
        println("receive blockplacement ${event.player.name}")
        if (votOre.ballots.containsKey(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }
}
