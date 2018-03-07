package com.blockworker.gopnik.music

import com.blockworker.gopnik.{BotMain, EventListener}
import com.sedmelluq.discord.lavaplayer.player.{AudioPlayer, DefaultAudioPlayerManager}
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.{AudioTrack, AudioTrackEndReason}
import net.dv8tion.jda.core.entities.{Member, TextChannel, VoiceChannel}
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException
import net.dv8tion.jda.core.managers.AudioManager

object VoiceHandler extends AudioEventAdapter {

  private var channel: VoiceChannel = _
  private var textChannel: TextChannel = _
  private var audioManager: AudioManager = _
  val playerManager = new DefaultAudioPlayerManager
  AudioSourceManagers.registerRemoteSources(playerManager)
  private var player: AudioPlayer = _
  private var sendHandler: AudioPlayerSendHandler = _

  var defaultVolume = 20

  def init(): Unit = {
    audioManager = BotMain.getServer.getAudioManager
  }

  override def onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) = {
    PlaylistManager.onTrackEnd(player, track, endReason)
  }

  override def onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) = {
    PlaylistManager.onTrackError(player, track, exception)
  }

  override def onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) = {
    PlaylistManager.onTrackError(player, track, null)
  }

  def tryConnect(member: Member, tChannel: TextChannel): ConnectResult = {
    if (channel != null) return AlreadyConnected

    textChannel = tChannel
    if (!member.getVoiceState.inVoiceChannel()) return ConnectNoTarget

    channel = member.getVoiceState.getChannel
    try audioManager.openAudioConnection(channel)
    catch {
      case _: InsufficientPermissionException =>
        channel = null
        return ConnectNoPermissions
      case _: Throwable =>
        channel = null
        return ConnectOtherError
    }

    player = playerManager.createPlayer()
    player.addListener(this)
    player.setVolume(defaultVolume)

    sendHandler = new AudioPlayerSendHandler(player)
    audioManager.setSendingHandler(sendHandler)

    ConnectSuccess
  }

  sealed trait ConnectResult
  case object ConnectSuccess extends ConnectResult
  case object AlreadyConnected extends ConnectResult
  case object ConnectNoTarget extends ConnectResult
  case object ConnectNoPermissions extends ConnectResult
  case object ConnectOtherError extends ConnectResult

  def disconnect(): Boolean = {
    if (channel == null) return false
    player.stopTrack()
    sendHandler = null
    audioManager.setSendingHandler(null)
    player.destroy()
    player = null
    audioManager.closeAudioConnection()
    channel = null
    EventListener.locked = false
    PlaylistManager.onDisconnect()
    textChannel = null
    true
  }

  /**
    * Clones the track and plays it.
    */
  def startTrack(track: AudioTrack): Boolean = {
    if (channel == null) return false
    player.startTrack(track.makeClone(), false)
  }

  def setTrackPos(pos: Long): Boolean = {
    val track = getTrack
    if (track == null || !track.isSeekable) return false
    track.setPosition(pos)
    true
  }

  def stopTrack(): Boolean = {
    if (channel == null) return false
    player.stopTrack()
    true
  }

  def setPaused(paused: Boolean): Boolean = {
    if (channel == null || isPaused == paused) return false
    player.setPaused(paused)
    true
  }

  def setVolume(volume: Int): Boolean = {
    if (channel == null) return false
    player.setVolume(volume)
    true
  }

  def setDefaultVolume (volume: Int): Unit = {
    defaultVolume = math.max(0, math.min(150, volume))
  }

  def getTrack: AudioTrack = if (channel == null) null else player.getPlayingTrack
  def getTrackLength: Long = if (getTrack == null) -1 else getTrack.getDuration
  def getTrackPos: Long = if (getTrack == null) -1 else getTrack.getPosition
  def isPaused: Boolean = if (channel == null) false else player.isPaused
  def getVolume: Int = if (channel == null) -1 else player.getVolume
  def getDefaultVolume: Int = defaultVolume
  def getChannel: VoiceChannel = channel
  def isConnected: Boolean = channel != null
  def getTextChannel: TextChannel = textChannel

}
