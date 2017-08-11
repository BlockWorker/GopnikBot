package com.blockworker.gopnik

import net.dv8tion.jda.core._
import net.dv8tion.jda.core.entities.{Guild, Member, Role}

object BotMain {

  private var api: JDA = _
  def getAPI = api

  private var server: Guild = _
  def getServer = server

  def main(args: Array[String]): Unit = {
    api = new JDABuilder(AccountType.BOT).setToken(System.getenv("BOT_TOKEN")).buildBlocking()
    api.addEventListener(MessageListener)
    server = api.getGuilds.get(0)
  }

  def hasRole(member: Member, role: Role): Boolean = server.getMembersWithRoles(role).contains(member)
  def hasRole(member: Member, roleName: String): Boolean = hasRole(member, server.getRolesByName(roleName, false).get(0))

}
