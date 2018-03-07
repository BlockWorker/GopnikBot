package com.blockworker.gopnik

//import java.io.{File, FileReader, FileWriter}

import com.blockworker.gopnik.music.VoiceHandler
import net.dv8tion.jda.core._
import net.dv8tion.jda.core.entities._

object BotMain {

  private var api: JDA = _
  def getAPI = api

  private var server: Guild = _
  def getServer = server

  //val configFile = new File("bot-config")
  val adminRoleName = "Hackerman"
  val modRoleName = "Pro"

  private var adminRole: Role = _
  private var modRole: Role = _

  def main(args: Array[String]): Unit = {
    api = new JDABuilder(AccountType.BOT)
      .setToken(System.getenv("BOT_TOKEN"))
      .setGame(Game.playing(EventListener.prefix + "help for commands"))
      .buildBlocking()
    EventListener.init()
    api.addEventListener(EventListener)
    server = api.getGuilds.get(0)
    VoiceHandler.init()
    /*if (configFile.exists()) {
      val reader = new FileReader(configFile)
      VoiceManager.defaultVolume = reader.read()
      reader.close()
    }*/
    adminRole = server.getRolesByName(adminRoleName, false).get(0)
    modRole = server.getRolesByName(modRoleName, false).get(0)
  }

  def hasRole(member: Member, role: Role): Boolean = server.getMembersWithRoles(role).contains(member)
  def hasRole(member: Member, roleName: String): Boolean = hasRole(member, server.getRolesByName(roleName, false).get(0))
  def isAdmin(member: Member) = hasRole(member, adminRole)
  def isMod(member: Member) = isAdmin(member) || hasRole(member, modRole)

  def shutdown(channel: MessageChannel): Unit = {
    /*if (configFile.exists()) configFile.delete()
    configFile.createNewFile()
    val writer = new FileWriter(configFile)
    writer.write(VoiceManager.defaultVolume)
    writer.flush()
    writer.close()*/
    VoiceHandler.disconnect()
    channel.sendMessage(":wave: Gopnik out, блядь!").queue()
    api.shutdown()
  }

}
