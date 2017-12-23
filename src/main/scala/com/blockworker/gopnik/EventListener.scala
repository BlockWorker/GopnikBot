package com.blockworker.gopnik


import net.dv8tion.jda.core.entities.{MessageChannel, PrivateChannel, Member}
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

object EventListener extends ListenerAdapter {

  val ALLOWED_CHANNELS = Array[String]("admin", "commands")

  var prefix = System.getenv("PREFIX")
  var locked = false

  override def onMessageReceived(event: MessageReceivedEvent): Unit = {
    if (event.getAuthor.isBot) return
    val member = event.getMember
    val content = event.getMessage.getRawContent
    val channel = event.getChannel
    if (content(0) != prefix) return
    if (!ALLOWED_CHANNELS.contains(channel.getName)) {
      channel.sendMessage(":x: Please use one the #commands channel to command the bot!").queue()
      return
    }
    if (locked && !BotMain.isAdmin(member)) {
      channel.sendMessage(":lock: The bot is currently locked!").queue()
      return
    }
    val args = content.substring(1).split(" ")
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
    }
  }

  def wrongSyntax(channel: MessageChannel): Unit = channel.sendMessage(":rage: Wrong use of ~~meme~~ command!").queue()

  def concatArgs(args: Array[String], start: Int): String = {
    var str = args(start)
    for (i <- start + 1 until args.length) str += " " + args(i)
    str
  }

  def sendHelp(member: Member, channel: MessageChannel): Unit = {
    var str = "**Gopnik Bot v0.2** by alex_6611\n" +
      "__Prefix:__ `" + prefix + "`\n" +
      "__Commands:__\n" +
      "```ping - Test command to check whether the bot is responding\n" +
      "join - Invites the bot to your voice channel to play music\n" +
      "rebind - Binds the music status output to the current channel\n" +
      "queue <link> - Adds one or multiple tracks to the playlist\n" +
      "queue gopnik - Adds the official Gopnik List(tm) to the playlist\n" +
      "playlist - Shows the current playlist\n" +
      "play - Starts playing from the beginning\n" +
      "play <number> - Starts playing the specified track\n" +
      "noloop - Tells the bot to stop playing after the last track\n" +
      "shuffle - Tells the bot to loop and shuffle the playlist after the last track\n" +
      "loop - Tells the bot to loop the playlist after the last track\n" +
      "next - Skips the current track and plays the next one\n" +
      "pause - Pauses the music\n" +
      "resume - Resumes the music when paused or stopped\n" +
      "stop - Stops the music\n" +
      "remove - Removes the current track from the playlist and skips to the next one\n" +
      "clearplaylist - Stops the music and clears the playlist\n" +
      "save <name> - Saves the current playlist as <name>\n" +
      "load <name> - Loads playlist <name>, only works if current playlist is empty\n" +
      "volume - Shows current volume level\n" +
      "volume <number> - Sets volume to <number> (0-150)\n" +
      "disconnect - Disconnects the bot from its voice channel```"
    if (BotMain.isMod(member)) str += "\n__Pro Commands:__\n" +
      "```forcesave <name> - Saves the current playlist as <name>, even if that name already exists\n" +
      "delete <name> - Deletes playlist <name>\n" +
      "defaultvolume - Shows default volume level\n" +
      "defaultvolume <number> - Sets default volume to <number> (0-150)```"
    if (BotMain.isAdmin(member)) str += "\n__Admin Commands:__\n" +
      "```shutdown - Shuts the Bot down\n" +
      "prefix <char> - Changes the command prefix to <char>\n" +
      "lock - Locks or unlocks the bot for non-admins```"
    member.getUser.openPrivateChannel().queue((t: PrivateChannel) => t.sendMessage(str).queue())
    channel.sendMessage(":question: Help sent, check your private messages!").queue()
  }

}
