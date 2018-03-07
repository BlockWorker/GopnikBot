package com.blockworker.gopnik


import com.blockworker.gopnik.messagehandlers._
import com.blockworker.gopnik.music.{PlaylistManager, VoiceHandler}
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.guild.voice.{GuildVoiceLeaveEvent, GuildVoiceMoveEvent}
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

import scala.collection.mutable

object EventListener extends ListenerAdapter {

  val ALLOWED_CHANNELS = Array[String]("admin", System.getenv("CHANNEL"))

  var prefix = System.getenv("PREFIX")(0)
  var locked = false

  private val messageHandlers = mutable.Set[IMessageHandler]()

  def init(): Unit = {
    messageHandlers.add(CoreMessageHandler)
    messageHandlers.add(MusicMessageHandler)
  }

  override def onMessageReceived(event: MessageReceivedEvent): Unit = {
    if (event.getAuthor.isBot) return
    val channel = event.getChannel
    val message = event.getMessage
    if (message.getContentRaw()(0) != prefix) return
    if (!ALLOWED_CHANNELS.contains(channel.getName)) {
      message.delete().queue()
      event.getAuthor.openPrivateChannel().queue(chan =>
        chan.sendMessage("Please use the #" + System.getenv("CHANNEL") + " channel to command this bot!").queue())
      return
    }
    if (locked && !BotMain.isAdmin(event.getMember)) {
      message.addReaction("❌").queue()
      message.addReaction("\uD83D\uDD12").queue() //closed lock
      PlaylistManager.statusToFront()
      return
    }
    var handled = false
    val iter = messageHandlers.iterator
    while (!handled && iter.hasNext) { //try every MessageHandler until one of them processes it
      handled = iter.next().handleCommand(event)
    }
    if (!handled) message.addReaction("❓").queue() //if prefix exists but command is not handled
    PlaylistManager.statusToFront()
  }

  def wrongSyntax(message: Message): Unit = message.addReaction("❓").queue()

  override def onGuildVoiceLeave(event: GuildVoiceLeaveEvent) = {
    if (event.getChannelLeft == VoiceHandler.getChannel) PlaylistManager.checkDisconnect()
  }

  override def onGuildVoiceMove(event: GuildVoiceMoveEvent) = {
    if (event.getChannelLeft == VoiceHandler.getChannel) PlaylistManager.checkDisconnect()
  }

}
