package com.blockworker.gopnik

import java.io.{File, FileOutputStream, PrintStream}
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

  var channel: VoiceChannel = _
  var audioManager: AudioManager = _
  val playerManager = new DefaultAudioPlayerManager
  AudioSourceManagers.registerRemoteSources(playerManager)
  var player: AudioPlayer = _
  var tChannel: MessageChannel = _
  var sendHandler: AudioPlayerSendHandler = _

  var playlist = mutable.Seq[AudioTrack]()
  var trackIndex = 0
  var shuffleLoop = 0

  val loadHandler = new AudioLoadResultHandler {
    override def trackLoaded(track: AudioTrack): Unit = {
      playlist :+= track
      tChannel.sendMessage(":musical_note: :heavy_plus_sign: Added to playlist: `" + track.getInfo.title + "` (<" + track.getInfo.uri + ">)").queue()
    }

    override def playlistLoaded(playlist: AudioPlaylist): Unit = {
      VoiceManager.playlist ++= playlist.getTracks.asScala
      tChannel.sendMessage(":musical_note: :heavy_plus_sign: Added to playlist: `" + playlist.getName + "` (" + playlist.getTracks.size() + " tracks)").queue()
    }

    override def noMatches(): Unit = {
      tChannel.sendMessage(":musical_note: :rage: Hardbass not found!").queue()
    }

    override def loadFailed(exception: FriendlyException): Unit = {
      tChannel.sendMessage(":musical_note: :rage: Something went wrong...").queue()
      val excfile = new File("loadexc-" + ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
      excfile.createNewFile()
      exception.printStackTrace(new PrintStream(new FileOutputStream(excfile), true))
    }
  }

  def join(member: Member, textChannel: MessageChannel): Unit = {
    tChannel = textChannel

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

    sendHandler = new AudioPlayerSendHandler(player)
    audioManager.setSendingHandler(sendHandler)
  }

  def queue(ident: String): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    playerManager.loadItem(ident, loadHandler)
  }

  def printQueue(): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    if (playlist.isEmpty) {
      tChannel.sendMessage(":musical_note: Playlist empty!").queue()
      return
    }
    tChannel.sendMessage(":musical_note: Playlist:").queue()
    for (t <- playlist) tChannel.sendMessage("`" + t.getInfo.title + "` (<" + t.getInfo.uri + ">)").queue()
  }

  def startPlaylist(): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    if (playlist.isEmpty) {
      tChannel.sendMessage(":musical_note: :rage: How am I supposed to play an empty playlist?").queue()
      return
    }
    if (shuffleLoop == 1) playlist.sortWith((_, _) => Random.nextBoolean())
    playlist(trackIndex) = playlist(trackIndex).makeClone()
    trackIndex = 0
    player.startTrack(playlist.head, false)
    tChannel.sendMessage(":musical_note: :arrow_forward: Playback started! First track: `" + playlist(trackIndex).getInfo.title + "` (<" + playlist(trackIndex).getInfo.uri + ">)").queue()
  }

  def setNoLoop(): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    tChannel.sendMessage(":musical_note: :repeat_one: Playback will not loop").queue()
    shuffleLoop = 0
  }
  def setShuffle(): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    tChannel.sendMessage(":musical_note: :twisted_rightwards_arrows: Playback will loop and shuffle").queue()
    shuffleLoop = 1
  }
  def setLoop(): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    tChannel.sendMessage(":musical_note: :repeat: Playback will loop").queue()
    shuffleLoop = 2
  }

  def nextTrack(): Unit = {
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
        playlist.sortWith((_, _) => Random.nextBoolean())
    }
    playlist(trackIndex) = playlist(trackIndex).makeClone()
    trackIndex = (trackIndex + 1) % playlist.length
    player.startTrack(playlist(trackIndex), false)
    tChannel.sendMessage(":musical_note: :next_track: Next track: `" + playlist(trackIndex).getInfo.title + "` (<" + playlist(trackIndex).getInfo.uri + ">)").queue()
  }

  def pause(): Unit = {
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

  def resume(): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    if (!player.isPaused) {
      tChannel.sendMessage(":musical_note: :arrow_forward: Playback is not paused!").queue()
      return
    }
    player.setPaused(false)
    tChannel.sendMessage(":musical_note: :arrow_forward: Playback resumed").queue()
  }

  def clearPlaylist(): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    tChannel.sendMessage(":musical_note: :stop_button: :x: Stopping and clearing playlist").queue()
    player.stopTrack()
    playlist = mutable.Seq[AudioTrack]()
    trackIndex = 0
  }

  def printVolume(): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    tChannel.sendMessage(":musical_note: :loud_sound: Volume: " + player.getVolume).queue()
  }

  def setVolume(volume: String): Unit = {
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

  override def onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason): Unit = {
    if (endReason.mayStartNext) nextTrack()
  }

  override def onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long): Unit = {
    tChannel.sendMessage(":musical_note: :rage: Hardbass is broken, playing next track...").queue()
    nextTrack()
  }

  override def onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException): Unit = {
    tChannel.sendMessage(":musical_note: :rage: Hardbass is broken, playing next track...").queue()
    nextTrack()
    val excfile = new File("trackexc-" + ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
    excfile.createNewFile()
    exception.printStackTrace(new PrintStream(new FileOutputStream(excfile), true))
  }

  def disconnect(): Unit = {
    if (channel == null) {
      tChannel.sendMessage(":musical_note: :rage: Bot is not in a voice channel!").queue()
      return
    }
    tChannel.sendMessage(":musical_note: I'm outta here!").queue()
    sendHandler = null
    audioManager.setSendingHandler(null)
    player.stopTrack()
    player.destroy()
    player = null
    audioManager.closeAudioConnection()
    channel = null
    tChannel = null
    playlist = mutable.Seq[AudioTrack]()
  }
}
