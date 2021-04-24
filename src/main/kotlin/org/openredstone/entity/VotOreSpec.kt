package org.openredstone.entity

import com.uchuhimo.konf.ConfigSpec

object VotOreSpec : ConfigSpec("") {
    object VoterDatabase : ConfigSpec() {
        val username by optional("votetest")
        val password by optional("votetest")
        val database by optional("votore_1")
        val host by optional("localhost")
        val port by optional(3306)
    }
}
