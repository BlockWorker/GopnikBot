package com.blockworker.gopnik

import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDABuilder

object BotMain {

  def main(args: Array[String]): Unit = {
    val api = new JDABuilder(AccountType.BOT).setToken(System.getenv("BOT_TOKEN")).buildAsync
  }

}
