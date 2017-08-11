package com.blockworker.gopnik

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.core.audio.AudioSendHandler

class AudioPlayerSendHandler(val audioPlayer: AudioPlayer) extends AudioSendHandler {
  private var lastFrame: AudioFrame = _

  override def canProvide: Boolean = {
    lastFrame = audioPlayer.provide()
    lastFrame != null
  }

  override def provide20MsAudio: Array[Byte] = lastFrame.data

  override def isOpus = true
}
