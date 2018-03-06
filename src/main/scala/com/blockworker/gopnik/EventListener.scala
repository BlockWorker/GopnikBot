package com.blockworker.gopnik


import com.blockworker.gopnik.messagehandlers._
import com.blockworker.gopnik.music.{PlaylistManager, VoiceHandler}
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.{Message, MessageChannel}
import net.dv8tion.jda.core.events.guild.voice.{GuildVoiceLeaveEvent, GuildVoiceMoveEvent}
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

import scala.collection.mutable

object EventListener extends ListenerAdapter {

  val ALLOWED_CHANNELS = Array[String]("admin", System.getenv("CHANNEL"))

  var prefix = System.getenv("PREFIX")(0)
  var locked = true //TODO: temp true

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
      val chan = BotMain.getServer.getTextChannelsByName(System.getenv("CHANNEL"), true).get(0)
      message.delete().queue()
      event.getAuthor.openPrivateChannel().queue(chan =>
        chan.sendMessage(new MessageBuilder().append("Please use the ").append(chan).append(" channel to command this bot!").build()).queue())
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
    /*val args = content.substring(1).split(" ")
    args(0).toLowerCase() match {
      case "help" => sendHelp(member, channel)
      case "ping" =>
        channel.sendMessage(":ping_pong: Pong!").queue()
      case "shutdown" if BotMain.isAdmin(member) =>
        BotMain.shutdown(channel)
      case "prefix" if BotMain.isAdmin(member) =>
        if (args.length != 2 || args(1).length != 1) wrongSyntax(channel)
        else {
          channel.sendMessage(":wrench: I shall now respond to `" + args(1) + "`").queue()
          prefix = args(1)(0)
        }
      case "join" => VoiceManager.join(member, channel)
      case "rebind" => VoiceManager.setTChannel(channel)
      case "queue" =>
        if (args.length < 2) wrongSyntax(channel)
        else VoiceManager.queue(concatArgs(args, 1), channel)
      case "playlist" => VoiceManager.printQueue(channel)
      case "play" =>
        if (args.length >= 2) VoiceManager.startPlaylist(args(1), channel)
        else VoiceManager.startPlaylist("0", channel)
      case "noloop" => VoiceManager.setNoLoop(channel)
      case "shuffle" => VoiceManager.setShuffle(channel)
      case "loop" => VoiceManager.setLoop(channel)
      case "next" => VoiceManager.nextTrack(channel)
      case "pause" => VoiceManager.pause(channel)
      case "resume" => VoiceManager.resume(channel)
      case "stop" => VoiceManager.stop(channel)
      case "remove" => VoiceManager.removeCurrent(channel)
      case "clearplaylist" => VoiceManager.clearPlaylist(channel)
      case "save" =>
        if (args.length < 2) wrongSyntax(channel)
        else VoiceManager.savePlaylist(args(1), false, channel)
      case "forcesave" if BotMain.isMod(member) =>
        if (args.length < 2) wrongSyntax(channel)
        else VoiceManager.savePlaylist(args(1), true, channel)
      case "load" =>
        if (args.length < 2) wrongSyntax(channel)
        else VoiceManager.loadPlaylist(args(1), channel)
      case "delete" if BotMain.isMod(member) =>
        if (args.length < 2) wrongSyntax(channel)
        else VoiceManager.deletePlaylist(args(1), channel)
      case "volume" =>
        if (args.length >= 2) VoiceManager.setVolume(args(1), channel)
        else VoiceManager.printVolume(channel)
      case "defaultvolume" if BotMain.isMod(member) =>
        if (args.length >= 2) VoiceManager.setDefaultVolume(args(1), channel)
        else VoiceManager.printDefaultVolume(channel)
      case "disconnect" => VoiceManager.disconnect(channel)
      case "lock" if BotMain.isAdmin(member) =>
        locked = !locked
        if (locked) channel.sendMessage(":lock: Bot has been locked!").queue()
        else channel.sendMessage(":unlock: Bot has been unlocked!").queue()
      case _ =>
    }*/
  }

  def wrongSyntax(message: Message): Unit = message.addReaction("❓").queue()

  override def onGuildVoiceLeave(event: GuildVoiceLeaveEvent) = {
    if (event.getChannelLeft == VoiceHandler.getChannel) PlaylistManager.checkDisconnect()
  }

  override def onGuildVoiceMove(event: GuildVoiceMoveEvent) = {
    if (event.getChannelLeft == VoiceHandler.getChannel) PlaylistManager.checkDisconnect()
  }

}
