package com.blockworker.gopnik

import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

object MessageListener extends ListenerAdapter {

  var prefix = "!"

  override def onMessageReceived(event: MessageReceivedEvent): Unit = {
    val author = event.getAuthor
    val member = event.getMember
    if (author.isBot) return
    val content = event.getMessage.getRawContent
    val channel = event.getChannel
    if (!content.startsWith(prefix)) return
    val args = content.substring(1).split(" ")
    args(0) match {
      case "ping" =>
        channel.sendMessage(":ping_pong: Pong!").queue()
      case "shutdown" if BotMain.hasRole(member, "Hackerman") =>
        channel.sendMessage(":wave: Gopnik out, блядь!").queue()
        BotMain.getAPI.shutdown()
      case "prefix" if BotMain.hasRole(member, "Hackerman") =>
        if (args.length != 2 || args(1).length != 1) wrongSyntax(channel)
        else {
          channel.sendMessage(":wrench: I shall now respond to `" + args(1) + "`").queue()
          prefix = args(1)
        }
      case "join" => VoiceManager.join(member, channel)
      case "queue" =>
        if (args.length < 2) wrongSyntax(channel)
        else VoiceManager.queue(concatArgs(args, 1))
      case "playlist" => VoiceManager.printQueue()
      case "play" => VoiceManager.startPlaylist()
      case "noloop" => VoiceManager.setNoLoop()
      case "shuffle" => VoiceManager.setShuffle()
      case "loop" => VoiceManager.setLoop()
      case "next" => VoiceManager.nextTrack()
      case "pause" => VoiceManager.pause()
      case "resume" => VoiceManager.resume()
      case "clearplaylist" => VoiceManager.clearPlaylist()
      case "volume" =>
        if (args.length > 2) wrongSyntax(channel)
        else if (args.length == 2) VoiceManager.setVolume(args(1))
        else VoiceManager.printVolume()
      case "disconnect" => VoiceManager.disconnect()
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
