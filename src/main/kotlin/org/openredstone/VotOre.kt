package org.openredstone

import co.aikar.commands.*
import com.google.inject.Inject
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.source.yaml.toYaml
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.openredstone.commands.ElectionCommand
import org.openredstone.commands.VotOreException
import org.openredstone.entity.Ballot
import org.openredstone.entity.VotOreSpec
import org.openredstone.event.EventHandler
import org.openredstone.manager.Sql
import org.slf4j.Logger
import java.io.File
import java.nio.file.Path
import java.util.*

private const val VERSION = "0.1.0-SNAPSHOT"

@Plugin(
    id = "votore",
    name = "VotORE",
    version = VERSION,
    url = "https://openredstone.org",
    description = "An ingame election management system for ORE",
    authors = ["Nickster258", "PaukkuPalikka"],
    dependencies = [Dependency(id = "luckperms")]
)
class VotOre @Inject constructor(
    val proxy: ProxyServer,
    val logger: Logger,
    @DataDirectory dataFolder: Path
) {
    private val dataFolder = dataFolder.toFile()
    private var config = loadConfig()
    var version = VERSION
    val database = Sql(
        config[VotOreSpec.VoterDatabase.host],
        config[VotOreSpec.VoterDatabase.port],
        config[VotOreSpec.VoterDatabase.database],
        config[VotOreSpec.VoterDatabase.username],
        config[VotOreSpec.VoterDatabase.password]
    )
    var ballots = mutableMapOf<UUID, Ballot>()

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        database.initTables()
        proxy.eventManager.register(this, EventHandler(this))
        VelocityCommandManager(this.proxy, this).apply {
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
            logger.error("Error while executing command", throwable)
            return false
        }
        val message = exception.message ?: "Something went wrong!"
        proxy.getPlayer(sender.uniqueId).ifPresent {
            it.sendVotoreError(message)
        }
        return true
    }

    private fun loadConfig(reloaded: Boolean = false): Config {
        if (!dataFolder.exists()) {
            logger.info("No resource directory found, creating directory")
            dataFolder.mkdir()
        }
        val configFile = File(dataFolder, "config.yml")
        val loadedConfig = if (!configFile.exists()) {
            logger.info("No config file found, generating from default config.yml")
            configFile.createNewFile()
            Config { addSpec(VotOreSpec) }
        } else {
            Config { addSpec(VotOreSpec) }.from.yaml.watchFile(configFile)
        }
        loadedConfig.toYaml.toFile(configFile)
        logger.info("${if (reloaded) "Rel" else "L"}oaded config.yml")
        return loadedConfig
    }
}
