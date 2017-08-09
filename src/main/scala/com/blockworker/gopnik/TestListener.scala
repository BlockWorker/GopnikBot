package com.blockworker.gopnik

import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

object TestListener extends ListenerAdapter {

  override def onMessageReceived(event: MessageReceivedEvent): Unit = {
    if (event.getAuthor.isBot) return
    val content = event.getMessage.getRawContent
    val channel = event.getChannel
    content match {
      case "!ping" =>
        channel.sendMessage("Pong!").queue()
      case "!shutdown" =>
        channel.sendMessage("Gopnik out, блядь!").queue()
        BotMain.getAPI.shutdown()
    }
  }

}
