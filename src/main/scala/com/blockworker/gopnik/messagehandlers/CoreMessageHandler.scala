package com.blockworker.gopnik.messagehandlers

import java.time.Instant
import java.time.temporal.ChronoUnit

import com.blockworker.gopnik.music.PlaylistManager
import com.blockworker.gopnik.{BotMain, EventListener}
import net.dv8tion.jda.core.entities.{Member, MessageChannel, PrivateChannel}
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

object CoreMessageHandler extends IMessageHandler {

  /**
    * @return Whether this [[com.blockworker.gopnik.messagehandlers.IMessageHandler MessageHandler]] processed a command from the message
    */
  override def handleCommand(event: MessageReceivedEvent): Boolean = {
    val member = event.getMember
    val message = event.getMessage
    val channel = event.getTextChannel
    message.getContentRaw.substring(1) match {
      case "help" =>
        sendHelp(member, channel)
        message.addReaction("✅").queue()
      case "ping" =>
        val now = Instant.now()
        val time = 2 * message.getCreationTime.toInstant.until(now, ChronoUnit.MILLIS)
        channel.sendMessage(":ping_pong: Pong! `" + time + " ms`").queue()
      case "shutdown" if BotMain.isAdmin(member) =>
        BotMain.shutdown(channel)
      case "lock" if BotMain.isAdmin(member) =>
        EventListener.locked = !EventListener.locked
        if (EventListener.locked) message.addReaction("\uD83D\uDD12").queue() //closed lock
        else message.addReaction("\uD83D\uDD13").queue() //open lock
      case _ => return false
    }
    true
  }

  def sendHelp(member: Member, channel: MessageChannel): Unit = {
    var str = "**Gopnik Bot v0.2** by alex_6611\n" +
      "__Prefix:__ `" + EventListener.prefix + "`\n" +
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
  }

}
