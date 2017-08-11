package com.blockworker.gopnik

import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

object MessageListener extends ListenerAdapter {

  var prefix = '!'

  override def onMessageReceived(event: MessageReceivedEvent): Unit = {
    val author = event.getAuthor
    val member = event.getMember
    if (author.isBot) return
    val content = event.getMessage.getRawContent
    val channel = event.getChannel
    if (content(0) != prefix) return
    val args = content.substring(1).split(" ")
    args(0).toLowerCase() match {
      case "ping" =>
        channel.sendMessage(":ping_pong: Pong!").queue()
      case "shutdown" if BotMain.hasRole(member, "Hackerman") =>
        BotMain.shutdown(channel)
      case "prefix" if BotMain.hasRole(member, "Hackerman") =>
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
      case "forcesave" if BotMain.hasRole(member, "Pro") || BotMain.hasRole(member, "Hackerman") =>
        if (args.length < 2) wrongSyntax(channel)
        else VoiceManager.savePlaylist(args(1), true, channel)
      case "load" =>
        if (args.length < 2) wrongSyntax(channel)
        else VoiceManager.loadPlaylist(args(1), channel)
      case "delete" if BotMain.hasRole(member, "Pro") || BotMain.hasRole(member, "Hackerman") =>
        if (args.length < 2) wrongSyntax(channel)
        else VoiceManager.deletePlaylist(args(1), channel)
      case "volume" =>
        if (args.length >= 2) VoiceManager.setVolume(args(1), channel)
        else VoiceManager.printVolume(channel)
      case "defaultvolume" if BotMain.hasRole(member, "Pro") || BotMain.hasRole(member, "Hackerman") =>
        if (args.length >= 2) VoiceManager.setDefaultVolume(args(1), channel)
        else VoiceManager.printDefaultVolume(channel)
      case "disconnect" => VoiceManager.disconnect(channel)
      case _ =>
    }
  }

  def wrongSyntax(channel: MessageChannel): Unit = channel.sendMessage(":rage: Wrong use of ~~meme~~ command!").queue()

  def concatArgs(args: Array[String], start: Int): String = {
    var str = args(start)
    for (i <- start + 1 until args.length) str += " " + args(i)
    str
  }

}
