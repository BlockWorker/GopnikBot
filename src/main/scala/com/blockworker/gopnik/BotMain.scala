package com.blockworker.gopnik

import java.io.{File, FileReader, FileWriter}

import net.dv8tion.jda.core._
import net.dv8tion.jda.core.entities.{Guild, Member, MessageChannel, Role}

object BotMain {

  private var api: JDA = _
  def getAPI = api

  private var server: Guild = _
  def getServer = server

  val configFile = new File("bot-config")

  def main(args: Array[String]): Unit = {
    api = new JDABuilder(AccountType.BOT).setToken(System.getenv("BOT_TOKEN")).buildBlocking()
    api.addEventListener(MessageListener)
    server = api.getGuilds.get(0)
    if (configFile.exists()) {
      val reader = new FileReader(configFile)
      MessageListener.prefix = reader.read().toChar
      VoiceManager.defaultVolume = reader.read()
      reader.close()
    }
  }

  def hasRole(member: Member, role: Role): Boolean = server.getMembersWithRoles(role).contains(member)
  def hasRole(member: Member, roleName: String): Boolean = hasRole(member, server.getRolesByName(roleName, false).get(0))

  def shutdown(channel: MessageChannel): Unit = {
    if (configFile.exists()) configFile.delete()
    configFile.createNewFile()
    val writer = new FileWriter(configFile)
    writer.write(MessageListener.prefix)
    writer.write(VoiceManager.defaultVolume)
    writer.flush()
    writer.close()
    channel.sendMessage(":wave: Gopnik out, блядь!").queue()
    api.shutdown()
  }

}
