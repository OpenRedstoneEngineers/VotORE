package org.openredstone

import co.aikar.commands.BaseCommand
import co.aikar.commands.BungeeCommandManager
import co.aikar.commands.CommandIssuer
import co.aikar.commands.RegisteredCommand
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.source.yaml.toYaml
import de.exceptionflug.protocolize.api.protocol.ProtocolAPI
import net.md_5.bungee.api.plugin.Plugin
import org.openredstone.commands.ElectionCommand
import org.openredstone.commands.VotOreException
import org.openredstone.entity.Ballot
import org.openredstone.entity.VotOreSpec
import org.openredstone.event.EventHandler
import org.openredstone.listener.ChatListener
import org.openredstone.manager.Sql
import java.io.File
import java.util.*
import java.util.logging.Level

class VotOre : Plugin() {
    var config = loadConfig()
    val database = Sql(
        config[VotOreSpec.VoterDatabase.host],
        config[VotOreSpec.VoterDatabase.port],
        config[VotOreSpec.VoterDatabase.database],
        config[VotOreSpec.VoterDatabase.username],
        config[VotOreSpec.VoterDatabase.password]
    )
    var ballots = mutableMapOf<UUID, Ballot>()

    override fun onEnable() {
        database.initTables()
        ProtocolAPI.getEventManager().apply {
            this.registerListener(ChatListener(this@VotOre))
        }
        proxy.pluginManager.registerListener(this, EventHandler(this))
        BungeeCommandManager(this).apply {
            commandConditions.addCondition("activeelection") {
                database.currentElectionId() ?: throw VotOreException("No active election!")
            }
            commandContexts.registerIssuerOnlyContext(Ballot::class.java) { context ->
                ballots[context.player.uniqueId]
                    ?: throw VotOreException("You are currently not voting. Run '/vote' to begin.")
            }
            setDefaultExceptionHandler(::handleCommandException, false)
            registerCommand(ElectionCommand(this@VotOre))
        }
    }

    private fun handleCommandException(
        command: BaseCommand,
        registeredCommand: RegisteredCommand<*>,
        sender: CommandIssuer,
        args: List<String>,
        throwable: Throwable
    ): Boolean {
        val exception = throwable as? VotOreException ?: run {
            logger.log(Level.SEVERE, "Error while executing command", throwable)
            return false
        }
        val message = exception.message ?: "Something went wrong!"
        val player = proxy.getPlayer(sender.uniqueId)!!
        player.sendVotoreError(message)
        return true
    }

    private fun loadConfig(reloaded: Boolean = false): Config {
        if (!dataFolder.exists()) {
            logger.log(Level.INFO, "No resource directory found, creating directory")
            dataFolder.mkdir()
        }
        val configFile = File(dataFolder, "config.yml")
        val loadedConfig = if (!configFile.exists()) {
            logger.log(Level.INFO, "No config file found, generating from default config.yml")
            configFile.createNewFile()
            Config { addSpec(VotOreSpec) }
        } else {
            Config { addSpec(VotOreSpec) }.from.yaml.watchFile(configFile)
        }
        loadedConfig.toYaml.toFile(configFile)
        logger.log(Level.INFO, "${if (reloaded) "Rel" else "L"}oaded config.yml")
        return loadedConfig
    }
}
