package com.blockworker.gopnik.music

import java.awt.Color
import java.time.LocalDateTime

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.{AudioTrack, AudioTrackEndReason}
import VoiceHandler._
import com.blockworker.gopnik.{BotMain, EventListener}
import com.blockworker.gopnik.EventListener.prefix
import net.dv8tion.jda.core.{EmbedBuilder, MessageBuilder}
import net.dv8tion.jda.core.entities.{Message, MessageEmbed}
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

import scala.collection.mutable.ListBuffer
import scala.util.Random

object PlaylistManager {

  val yt_regex = """(?:https?://)?(?:(?:www\.)?youtube\.com|youtu\.be)/(?:watch\?v=)?([A-Za-z0-9-_]{11})""".r

  var playlist = ListBuffer[AudioTrack]()
  var curIndex = -1
  var playMode: PlayMode = NoLoop
  var statusMessage: Long = -1

  var currentIdent = ""
  var addRequest: Message = _
  var playOnFind = false

  def join(command: MessageReceivedEvent): Unit = {
    tryConnect(command.getMember, command.getTextChannel) match {
      case AlreadyConnected => command.getTextChannel.sendMessage(":musical_note: :rage: I don't have clones, блять! (Bot is already in another voice channel)").queue()
      case ConnectNoTarget => command.getTextChannel.sendMessage(":musical_note: :rage: You're not in a voice channel!").queue()
      case ConnectNoPermissions => command.getTextChannel.sendMessage(":musical_note: :rage: Insufficient permissions to join channel!").queue()
      case ConnectSuccess =>
        command.getMessage.addReaction("✅").queue()
        playlist.clear()
        curIndex = -1
        playMode = NoLoop
        sendStatusMessage()
      case _ => command.getTextChannel.sendMessage(":musical_note: :rage: An error occurred when trying to join channel.").queue()
    }
  }

  def startNextTrack(): Boolean = {
    if (!isConnected || playlist.isEmpty) return false
    if (playlist.length <= curIndex + 1) {
      curIndex = -1
      if (playMode == NoLoop) {
        stopTrack()
        return true
      }
    }
    curIndex += 1
    if (curIndex == playlist.length - 1 && playMode == Shuffle) playlist.sortWith((_, _) => Random.nextBoolean())
    if (!startTrack(playlist(curIndex))) {
      stopTrack()
      return false
    }
    true
  }

  def addTrack(ident: String, message: Message, play: Boolean = false): Unit = {
    if (!isConnected) {
      message.addReaction("❌").queue()
      return
    }
    if (currentIdent != "") {
      message.addReaction("⏳").queue()
      return
    }
    currentIdent = ident
    addRequest = message
    playOnFind = play
    playerManager.loadItem(ident, DefaultLoadHandler)
  }

  def removeTracks(id: Int, count: Int, message: Message): Unit = {
    if (!isConnected || playlist.length < id + count) { //fails if any index is outside of range
      message.addReaction("❌").queue()
      return
    }
    playlist.remove(id, count)
    message.addReaction("✅").queue()
  }

  def sendPlaylist(message: Message): Unit = {
    if (!isConnected) {
      message.addReaction("❌").queue()
      return
    }
    message.getAuthor.openPrivateChannel().queue(ch => {
      if (playlist.isEmpty) ch.sendMessage(":clipboard: Playlist empty!").queue()
      else {
        val builder = new EmbedBuilder().setAuthor("Gopnik Bot v1.0").setTitle(":clipboard: Playlist")
        playlist.zipWithIndex.foreach { case (tr, id) =>
          val info = tr.getInfo
          val num = if (id == curIndex) "**" + (id + 1) + ".**" else (id + 1) + "."
          builder.appendDescription(num + " [" + info.title + "](" + info.uri + ") (" + info.author + "), " + formatTime(tr.getDuration) + "\n")
        }
        ch.sendMessage(builder.build()).queue()
      }
    })
    message.addReaction("✅").queue()
  }

  def setPlayMode(mode: PlayMode, message: Message): Unit = {
    if (!isConnected) {
      message.addReaction("❌").queue()
      return
    }
    playMode = mode
    message.addReaction("✅").queue()
  }

  def sendStatusMessage(): Unit = {
    if (!isConnected) {
      statusMessage = -1
      return
    }
    if (statusMessage >= 0) getTextChannel.getMessageById(statusMessage).queue(_.delete().queue())
    getTextChannel.sendMessage(createEmbed()).queue(m => statusMessage = m.getIdLong)
  }

  def createEmbed(): MessageEmbed = {
    val builder = new EmbedBuilder() //basic embed frame
      .setAuthor("Gopnik Music Bot v1.0", null, BotMain.getAPI.getSelfUser.getAvatarUrl)
      .setColor(new Color(0, 200, 255))
      .setFooter("HALE HORTLER", "http://p.fod4.com/p/channels/shack/profile/p8YTnc07RESVKAIW3qYO_adolf_hitler.jpg")
      .setTimestamp(LocalDateTime.of(1938, 11, 9, 13, 37, 0, 0))
      .setThumbnail("https://steamuserimages-a.akamaihd.net/ugc/261591516980362979/03CECD0BCC55D26F772C3A99A492A8F1D3AE265B/")

    if (getTrack == null) {
      builder.setTitle(":stop_button: `Stopped`").setDescription("Use " + prefix + "help for a list of commands.") //track stopped
    } else if (isPaused) {
      builder.setTitle(":pause_button: `Paused`").setDescription("Use " + prefix + "resume to resume playback.") //track paused
    } else {
      builder.setTitle(":arrow_forward: `Playing`").setDescription("Use " + prefix + "help for a list of commands.") //track playing
    }

    if (getTrack != null) {
      val info = getTrack.getInfo
      val pos = getTrack.getPosition
      val dur = getTrack.getDuration
      yt_regex.findFirstMatchIn(info.uri) match {
        case Some(mtc) => builder.setThumbnail("http://img.youtube.com/vi/" + mtc.group(1) + "/0.jpg") //add youtube thumbnail if youtube link
        case _ =>
      }
      builder.addField(":musical_note: `Current Track`", "[" + info.title + "](" + info.uri + ") (" + info.author + "), " + formatTime(dur), false) //add general track info
    }

    builder.addField(":loud_sound: `Volume`", getVolume + "%", true) //volume

    playMode match {
      case NoLoop => builder.addField(":arrows_clockwise: `Playlist Mode`", ":repeat_one: Play once", true) //play mode
      case Loop => builder.addField(":arrows_clockwise: `Playlist Mode`", ":repeat: Loop", true)
      case Shuffle => builder.addField(":arrows_clockwise: `Playlist Mode`", ":twisted_rightwards_arrows: Shuffle", true)
    }

    if (EventListener.locked) builder.addField(":lock: `Locked`", "Bot is locked.", true) //say that it's locked

    if (playlist.length > curIndex + 1) {
      val next = playlist(curIndex + 1) //next track info
      val nextInfo = next.getInfo
      builder.addField(":next_track: `Next Track`", "[" + nextInfo.title + "](" + nextInfo.uri + ") (" + nextInfo.author + "), " + formatTime(next.getDuration), false)
    } else if (playlist.nonEmpty) playMode match {
      case NoLoop => builder.addField(":next_track: `Next Track`", ":stop_button: End of Playlist", false) //end of playlist instead
      case _ => //start of playlist again (loop/shuffle)
        val next = playlist.head
        val nextInfo = next.getInfo
        builder.addField(":next_track: `Next Track`", "[" + nextInfo.title + "](" + nextInfo.uri + ") (" + nextInfo.author + "), " + formatTime(next.getDuration), false)
    } else builder.addField(":next_track: `Next Track`", ":eject: Playlist is empty!", false) //empty playlist instead

    builder.build()
  }

  def updateTrackInfo(): Unit = {
    val track = getTrack
    if (track != null && statusMessage >= 0) {
      getTextChannel.getMessageById(statusMessage).queue(_.editMessage(createEmbed()).queue())
    }
  }

  def statusToFront(): Unit = {
    if (isConnected && statusMessage >= 0) sendStatusMessage()
  }

  def onTrackError(player: AudioPlayer, track: AudioTrack, exception: FriendlyException): Unit = {
    startNextTrack()
    BotMain.getServer.getTextChannelsByName("admin", true).get(0).sendMessage(
      new MessageBuilder().append("Error occured while playing track\n")
        .append(track.getInfo.title + "(<" + track.getInfo.uri + ">)\n")
        .append(exception.getMessage + "\n")
        .append(exception.getStackTrace()(0).toString)
        .build()
    ).queue()
  }

  def onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason): Unit = {
    if (endReason.mayStartNext) startNextTrack()
  }

  def onDisconnect(): Unit = {
    if (statusMessage >= 0) getTextChannel.getMessageById(statusMessage).queue(_.delete().queue())
    playlist.clear()
    curIndex = -1
    playMode = NoLoop
    statusMessage = -1
  }

  def checkDisconnect(): Unit = {
    if (!isConnected) return
    if (getChannel.getMembers.size() <= 1) disconnect()
  }

  def getPlayerStatus: String = {
    if (!isConnected) return "Disconnected"
    if (getTrack == null) return "Stopped"
    if (isPaused) return "Paused"
    "Playing"
  }

  def formatTime(time: Long): String = {
    if (time >= 3600000) "%d:%02d:%02d".format(time / 3600000, (time / 60000) % 60, (time / 1000) % 60)
    else "%d:%02d".format(time / 60000, (time / 1000) % 60)
  }

  sealed trait PlayMode
  case object NoLoop extends PlayMode
  case object Loop extends PlayMode
  case object Shuffle extends PlayMode

}
