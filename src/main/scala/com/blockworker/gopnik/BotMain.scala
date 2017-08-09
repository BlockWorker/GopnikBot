package com.blockworker.gopnik

import net.dv8tion.jda.core._

object BotMain {

  private var api: JDA = _
  def getAPI = api

  def main(args: Array[String]): Unit = {
    api = new JDABuilder(AccountType.BOT).setToken(System.getenv("BOT_TOKEN")).buildAsync
    api.addEventListener(TestListener)
  }

}
