package com.blockworker.gopnik.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.core.audio.AudioSendHandler

class AudioPlayerSendHandler(val audioPlayer: AudioPlayer) extends AudioSendHandler {
  private var lastFrame: AudioFrame = _
  private var counter = 0

  override def canProvide: Boolean = {
    lastFrame = audioPlayer.provide()
    if (lastFrame != null) {
      counter += 1
      if (counter % 25 == 0) {
        counter = 0
        PlaylistManager.updateTrackInfo()
      }
      true
    }
    else false
  }

  override def provide20MsAudio: Array[Byte] = lastFrame.data

  override def isOpus = true
}
