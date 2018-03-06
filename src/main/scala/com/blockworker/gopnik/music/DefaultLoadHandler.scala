package com.blockworker.gopnik.music

import scala.collection.JavaConverters._
import com.blockworker.gopnik.BotMain
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.{AudioPlaylist, AudioTrack}
import net.dv8tion.jda.core.MessageBuilder

object DefaultLoadHandler extends AudioLoadResultHandler {

  override def trackLoaded(track: AudioTrack) = {
    PlaylistManager.playlist += track
    val index = PlaylistManager.playlist.indices.last - 1
    PlaylistManager.addRequest.addReaction("✅").queue()
    PlaylistManager.currentIdent = ""
    PlaylistManager.addRequest = null
    if (PlaylistManager.playOnFind){
      PlaylistManager.curIndex = index
      PlaylistManager.startNextTrack()
      PlaylistManager.playOnFind = false
    }
  }

  override def playlistLoaded(playlist: AudioPlaylist) = {
    var index = 0
    if (playlist.isSearchResult) {
      PlaylistManager.playlist += playlist.getTracks.get(0)
      index = PlaylistManager.playlist.indices.last - 1
    } else {
      PlaylistManager.playlist ++= playlist.getTracks.asScala
      index = PlaylistManager.playlist.length - playlist.getTracks.size() - 2
    }
    PlaylistManager.addRequest.addReaction("✅").queue()
    PlaylistManager.currentIdent = ""
    PlaylistManager.addRequest = null
    if (PlaylistManager.playOnFind){
      PlaylistManager.curIndex = index
      PlaylistManager.startNextTrack()
      PlaylistManager.playOnFind = false
    }
  }

  override def noMatches() = {
    VoiceHandler.playerManager.loadItem("ytsearch:" + PlaylistManager.currentIdent, AutoSearchHandler)
  }

  override def loadFailed(exception: FriendlyException) = {
    BotMain.getServer.getTextChannelsByName("admin", true).get(0).sendMessage(
      new MessageBuilder().append("Error occured while loading track (DefaultLoadHandler)\n")
        .append(exception.getMessage + "\n")
        .append(exception.getStackTrace()(0).toString)
        .build()
    ).queue()
    PlaylistManager.addRequest.addReaction("⁉").queue()
    PlaylistManager.currentIdent = ""
    PlaylistManager.addRequest = null
    PlaylistManager.playOnFind = false
  }

  object AutoSearchHandler extends AudioLoadResultHandler {

    override def trackLoaded(track: AudioTrack) = {
      PlaylistManager.playlist += track
      val index = PlaylistManager.playlist.indices.last - 1
      PlaylistManager.addRequest.addReaction("✅").queue()
      PlaylistManager.addRequest.addReaction("\uD83D\uDD0E").queue() //magnifying glass
      PlaylistManager.currentIdent = ""
      PlaylistManager.addRequest = null
      if (PlaylistManager.playOnFind){
        PlaylistManager.curIndex = index
        PlaylistManager.startNextTrack()
        PlaylistManager.playOnFind = false
      }
    }

    override def playlistLoaded(playlist: AudioPlaylist) = {
      trackLoaded(playlist.getTracks.get(0))
    }

    override def noMatches() = {
      PlaylistManager.addRequest.addReaction("❌").queue()
      PlaylistManager.addRequest.addReaction("❔").queue()
      PlaylistManager.currentIdent = ""
      PlaylistManager.addRequest = null
      PlaylistManager.playOnFind = false
    }

    override def loadFailed(exception: FriendlyException) = {
      BotMain.getServer.getTextChannelsByName("admin", true).get(0).sendMessage(
        new MessageBuilder().append("Error occured while loading track (AutoSearchHandler)\n")
          .append(exception.getMessage + "\n")
          .append(exception.getStackTrace()(0).toString)
          .build()
      ).queue()
      PlaylistManager.addRequest.addReaction("⁉").queue()
      PlaylistManager.currentIdent = ""
      PlaylistManager.addRequest = null
      PlaylistManager.playOnFind = false
    }

  }

}
