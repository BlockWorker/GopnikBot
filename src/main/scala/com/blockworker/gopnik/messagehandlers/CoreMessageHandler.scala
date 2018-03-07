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
      "```ping - Returns the ping of the bot. (sometimes broken)\n" +
      "join - Invites the bot to your voice channel to play music\n" +
      "queue <link> - Adds one or multiple tracks to the playlist\n" +
      "queue gopnik - Adds the official Gopnik List™ to the playlist\n" +
      "playlist - Shows the current playlist\n" +
      "play - Starts playing from the beginning\n" +
      "play <number> - Starts playing the specified track\n" +
      "play <link> - Like queue, but immediately starts playing queued track(s)\n" +
      "play gopnik - Queues and plays the official Gopnik List™\n" +
      "noloop - Tells the bot to stop playing after the last track\n" +
      "loop - Tells the bot to loop the playlist after the last track\n" +
      "shuffle - Tells the bot to loop and shuffle the playlist after the last track\n" +
      "goto <time> - Skips to <time> in the track if possible (seconds, m:s or h:m:s)\n" +
      "next - Skips the current track and plays the next one\n" +
      "pause - Pauses the music\n" +
      "resume - Resumes the music when paused or stopped\n" +
      "stop - Stops the music\n" +
      "remove - Removes the current track from the playlist and skips to the next one\n" +
      "remove <number> - Removes the specified track from the playlist\n" +
      "remove <num-num> - Removes all tracks in the specified range from the playlist\n" +
      "clearplaylist - Stops the music and clears the playlist\n" +
      "volume <number> - Sets volume to <number> (0-150)\n" +
      "disconnect - Disconnects the bot from its voice channel```"
    if (BotMain.isAdmin(member)) str += "\n__Admin Commands:__\n" +
      "```shutdown - Shuts the Bot down\n" +
      "lock - Locks or unlocks the bot for non-admins```"
    str += "\n__Possible responses:__\n" +
      ":white_check_mark: / :x: - General success / fail\n" +
      ":question: - Unknown command or wrong syntax\n" +
      ":interrobang: - Internal error, try again or ask hackerman\n" +
      ":white_check_mark: :mag_right: - Found track with auto-search\n" +
      ":grey_question: - No tracks found\n" +
      ":hourglass_flowing_sand: - Bot busy, try again in a few seconds\n" +
      ":lock: - Bot is locked"
    member.getUser.openPrivateChannel().queue((t: PrivateChannel) => t.sendMessage(str).queue())
  }

}
