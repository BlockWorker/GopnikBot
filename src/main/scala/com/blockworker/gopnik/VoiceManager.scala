package com.blockworker.gopnik

import java.io._
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import net.dv8tion.jda.core.entities.{Member, MessageChannel, VoiceChannel}
import net.dv8tion.jda.core.managers.AudioManager
import com.sedmelluq.discord.lavaplayer.player.{AudioLoadResultHandler, AudioPlayer, DefaultAudioPlayerManager}
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.{AudioPlaylist, AudioTrack, AudioTrackEndReason}

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.util.Random

object VoiceManager extends AudioEventAdapter {

  val GOPNIK_LIST = """https://www.youtube.com/playlist?list=PLAVlHb0DKf53tfdakIAS9-aT_XOcl_yrK"""
  val AN_HYMN = """https://www.youtube.com/watch?v=vN-ARytZKgQ"""

  var channel: VoiceChannel = _
  var audioManager: AudioManager = _
  val playerManager = new DefaultAudioPlayerManager
  AudioSourceManagers.registerRemoteSources(playerManager)
  var player: AudioPlayer = _
  var autoChannel: MessageChannel = _
  var sendHandler: AudioPlayerSendHandler = _

  var playlist = mutable.Seq[AudioTrack]()
  var trackIndex = 0
  var shuffleLoop = 0

  var quietAdd = false
  var defaultVolume = 100

  val loadHandler = new AudioLoadResultHandler {
    override def trackLoaded(track: AudioTrack): Unit = {
      playlist :+= track
      if (!quietAdd) autoChannel.sendMessage(":musical_note: :heavy_plus_sign: Added to playlist: `" + track.getInfo.title.filter(c => c != '`') + "` (<" + track.getInfo.uri + ">)").queue()
    }

    override def playlistLoaded(playlist: AudioPlaylist): Unit = {
      VoiceManager.playlist ++= playlist.getTracks.asScala
      if (!quietAdd) autoChannel.sendMessage(":musical_note: :heavy_plus_sign: Added to playlist: `" + playlist.getName.filter(c => c != '`') + "` (" + playlist.getTracks.size() + " tracks)").queue()
    }

    override def noMatches(): Unit = {
      if (!quietAdd) autoChannel.sendMessage(":musical_note: :rage: Hardbass not found!").queue()
    }

    override def loadFailed(exception: FriendlyException): Unit = {
      autoChannel.sendMessage(":musical_note: :rage: Something went wrong...").queue()
      val excfile = new File("loadexc-" + ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
      excfile.createNewFile()
      val writer = new PrintWriter(new BufferedWriter(new FileWriter(excfile)))
      exception.printStackTrace(writer)
      writer.flush()
      writer.close()
    }
  }

  def join(member: Member, tChannel: MessageChannel): Unit = {
    autoChannel = tChannel

    if (channel != null) {
      tChannel.sendMessage(":musical_note: :rage: I don't have clones, блядь! (Bot is already in another voice channel)").queue()
      return
    }
    if (!member.getVoiceState.inVoiceChannel()) {
      tChannel.sendMessage(":musical_note: :rage: You're not in a voice channel!").queue()
      return
    }
    channel = member.getVoiceState.getChannel
    tChannel.sendMessage(":musical_note: Joining channel `" + channel.getName + "`, output bound to `#" + tChannel.getName + "`.").queue()

    audioManager = BotMain.getServer.getAudioManager
    audioManager.openAudioConnection(channel)

    player = playerManager.createPlayer()
    player.addListener(this)
    player.setVolume(defaultVolume)

    sendHandler = new AudioPlayerSendHandler(player)
    audioManager.setSendingHandler(sendHandler)
  }

  def setTChannel(tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    if (autoChannel == tChannel) {
      tChannel.sendMessage(":musical_note: :rage: Output already bound to `#" + tChannel.getName + "`!").queue()
      return
    }
    autoChannel.sendMessage(":musical_note: Output now bound to `#" + tChannel.getName + "`").queue()
    autoChannel = tChannel
    tChannel.sendMessage(":musical_note: Output bound to `#" + tChannel.getName + "`").queue()
  }

  def queue(ident: String, tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    if (ident.toLowerCase() == "gopnik") playerManager.loadItem(GOPNIK_LIST, loadHandler)
    else playerManager.loadItem(ident, loadHandler)
  }

  def printQueue(tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    if (playlist.isEmpty) {
      tChannel.sendMessage(":musical_note: Playlist empty!").queue()
      return
    }
    var str = ":musical_note: Playlist (" + playlist.length + " tracks):"
    for (i <- playlist.indices) {
      if (str.length > 1800) {
        tChannel.sendMessage(str).queue()
        str = ""
      }
      else str += "\n"
      str += (if (i == trackIndex) "**" + (i + 1) + ".** " else (i + 1) + ". ")
      str += "`" + playlist(i).getInfo.title.filter(c => c != '`') + "` (<" + playlist(i).getInfo.uri + ">)"
    }
    tChannel.sendMessage(str).queue()
  }

  def startPlaylist(num: String, tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    if (playlist.isEmpty) {
      tChannel.sendMessage(":musical_note: :rage: How am I supposed to play an empty playlist?").queue()
      return
    }
    try {
      trackIndex = math.min(playlist.length - 1, math.max(0, Integer.parseInt(num) - 1))
    } catch {
      case _: NumberFormatException =>
        tChannel.sendMessage(":musical_note: :rage: That's not a valid number!").queue()
        return
    }
    if (trackIndex == 0 && shuffleLoop == 1) playlist = playlist.sortWith((_, _) => Random.nextBoolean())
    playlist(trackIndex) = playlist(trackIndex).makeClone()
    player.startTrack(playlist(trackIndex), false)
    tChannel.sendMessage(":musical_note: :arrow_forward: Playback started! Track: `" + playlist(trackIndex).getInfo.title.filter(c => c != '`') + "` (<" + playlist(trackIndex).getInfo.uri + ">)").queue()
  }

  def setNoLoop(tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    tChannel.sendMessage(":musical_note: :repeat_one: Playback will not loop").queue()
    shuffleLoop = 0
  }
  def setShuffle(tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    tChannel.sendMessage(":musical_note: :twisted_rightwards_arrows: Playback will loop and shuffle").queue()
    shuffleLoop = 1
  }
  def setLoop(tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    tChannel.sendMessage(":musical_note: :repeat: Playback will loop").queue()
    shuffleLoop = 2
  }

  def nextTrack(tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    if (playlist.isEmpty) {
      tChannel.sendMessage(":musical_note: :rage: If there's no next song, how am I supposed to play it?").queue()
      return
    }
    if (trackIndex == playlist.length - 1) shuffleLoop match {
      case 0 =>
        player.stopTrack()
        tChannel.sendMessage(":musical_note: :stop_button: Playlist ended!").queue()
        return
      case 1 =>
        playlist = playlist.sortWith((_, _) => Random.nextBoolean())
      case _ =>
    }
    trackIndex = (trackIndex + 1) % playlist.length
    playlist(trackIndex) = playlist(trackIndex).makeClone()
    player.startTrack(playlist(trackIndex), false)
    tChannel.sendMessage(":musical_note: :next_track: Next track: `" + playlist(trackIndex).getInfo.title.filter(c => c != '`') + "` (<" + playlist(trackIndex).getInfo.uri + ">)").queue()
  }

  def pause(tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    if (player.isPaused) {
      tChannel.sendMessage(":musical_note: :pause_button: Playback is already paused!").queue()
      return
    }
    player.setPaused(true)
    tChannel.sendMessage(":musical_note: :pause_button: Playback paused").queue()
  }

  def resume(tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    if (!player.isPaused) {
      if (player.getPlayingTrack != null) {
        tChannel.sendMessage(":musical_note: :arrow_forward: Playback already active!").queue()
        return
      }
      tChannel.sendMessage(":musical_note: :arrow_forward: Starting playback").queue()
      playlist(trackIndex) = playlist(trackIndex).makeClone()
      player.startTrack(playlist(trackIndex), false)
      return
    }
    player.setPaused(false)
    tChannel.sendMessage(":musical_note: :arrow_forward: Playback resumed").queue()
  }

  def stop(tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    tChannel.sendMessage(":musical_note: :stop_button: Stopping playback").queue()
    player.stopTrack()
  }

  def removeCurrent(tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    if (playlist.isEmpty) {
      tChannel.sendMessage(":musical_note: Playlist empty!").queue()
      return
    }
    val track = playlist(trackIndex)
    playlist = playlist.slice(0, trackIndex) ++ playlist.slice(trackIndex + 1, playlist.length)
    if (playlist.isEmpty){
      tChannel.sendMessage(":musical_note: :stop_button: Playlist is now empty!").queue()
      player.stopTrack()
      return
    }
    tChannel.sendMessage(":musical_note: Removed `" + track.getInfo.title.filter(c => c != '`') + "` (<" + track.getInfo.uri + ">) from the playlist").queue()
    nextTrack(tChannel)
  }

  def clearPlaylist(tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    tChannel.sendMessage(":musical_note: :stop_button: :x: Stopping and clearing playlist").queue()
    player.stopTrack()
    playlist = mutable.Seq[AudioTrack]()
    trackIndex = 0
  }

  def savePlaylist(name: String, force: Boolean, tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    if (playlist.isEmpty) {
      tChannel.sendMessage(":musical_note: :rage: I don't save empty playlists, they're worthless!").queue()
      return
    }
    val file = new File("playlist-" + name)
    if (file.exists()) {
      if (force) file.delete()
      else {
        tChannel.sendMessage(":musical_note: :floppy_disk: :rage: Playlist `" + name + "` already exists, only Pros can overwrite/delete playlists. Use `!forcesave` to overwrite.").queue()
        return
      }
    }
    file.createNewFile()
    val writer = new BufferedWriter(new FileWriter(file))
    for (t <- playlist) writer.write(t.getInfo.uri + ";")
    writer.newLine()
    writer.flush()
    writer.close()
    tChannel.sendMessage(":musical_note: :floppy_disk: Saved playlist (" + playlist.length + " tracks) to `" + name + "`").queue()
  }

  def loadPlaylist(name: String, tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    if (playlist.nonEmpty) {
      tChannel.sendMessage(":musical_note: Make sure the current playlist is empty before loading another one.").queue()
      return
    }
    val file = new File("playlist-" + name)
    if (!file.exists()) {
      tChannel.sendMessage(":musical_note: :floppy_disk: :rage: Playlist `" + name + "` doesn't exist!").queue()
      return
    }
    val reader = new BufferedReader(new FileReader(file))
    val list = reader.readLine().split(";")
    reader.close()
    quietAdd = true
    for (u <- list) playerManager.loadItem(u, loadHandler).get()
    quietAdd = false
    tChannel.sendMessage(":musical_note: :floppy_disk: Loaded playlist `" + name + "` (" + playlist.length + " tracks)").queue()
  }

  def deletePlaylist(name: String, tChannel: MessageChannel): Unit = {
    val file = new File("playlist-" + name)
    if (!file.exists()) {
      tChannel.sendMessage(":musical_note: :floppy_disk: :rage: Playlist `" + name + "` doesn't exist!").queue()
      return
    }
    file.delete()
    tChannel.sendMessage(":musical_note: :floppy_disk: Playlist `" + name + "` deleted.").queue()
  }

  def printVolume(tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    tChannel.sendMessage(":musical_note: :loud_sound: Volume: " + player.getVolume).queue()
  }

  def setVolume(volume: String, tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    var vol = 0
    try {
      vol = Integer.parseInt(volume)
    } catch {
      case _: NumberFormatException =>
        tChannel.sendMessage(":musical_note: :rage: That's not a valid number!").queue()
        return
    }
    player.setVolume(vol)
    tChannel.sendMessage(":musical_note: :loud_sound: Volume set to " + player.getVolume).queue()
  }

  def printDefaultVolume(tChannel: MessageChannel): Unit =
    tChannel.sendMessage(":musical_note: :loud_sound: Default volume: " + defaultVolume).queue()

  def setDefaultVolume(volume: String, tChannel: MessageChannel): Unit = {
    var vol = 0
    try {
      vol = Integer.parseInt(volume)
    } catch {
      case _: NumberFormatException =>
        tChannel.sendMessage(":musical_note: :rage: That's not a valid number!").queue()
        return
    }
    defaultVolume = math.min(150, math.max(0, vol))
    tChannel.sendMessage(":musical_note: :loud_sound: Default volume set to " + defaultVolume).queue()
  }

  override def onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason): Unit = {
    if (endReason.mayStartNext) nextTrack(autoChannel)
  }

  override def onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long): Unit = {
    autoChannel.sendMessage(":musical_note: :rage: Hardbass is broken, playing next track... (Stuck)").queue()
    nextTrack(autoChannel)
  }

  override def onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException): Unit = {
    autoChannel.sendMessage(":musical_note: :rage: Hardbass is broken, playing next track...").queue()
    val excfile = new File("trackexc-" + ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
    excfile.createNewFile()
    val writer = new PrintWriter(new BufferedWriter(new FileWriter(excfile)))
    exception.printStackTrace(writer)
    exception.printStackTrace()
    writer.flush()
    writer.close()
    nextTrack(autoChannel)
  }

  def disconnect(tChannel: MessageChannel): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    tChannel.sendMessage(":musical_note: I'm outta here!").queue()
    player.stopTrack()
    sendHandler = null
    audioManager.setSendingHandler(null)
    player.destroy()
    player = null
    audioManager.closeAudioConnection()
    channel = null
    autoChannel = null
    playlist = mutable.Seq[AudioTrack]()
    EventListener.locked = false
  }
}
