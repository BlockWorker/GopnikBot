package com.blockworker.gopnik.messagehandlers

import com.blockworker.gopnik.music.PlaylistManager.{Loop, NoLoop, Shuffle}
import com.blockworker.gopnik.music.{PlaylistManager, VoiceHandler}
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

object MusicMessageHandler extends IMessageHandler {

  val GOPNIK_LIST = """https://www.youtube.com/playlist?list=PLAVlHb0DKf53tfdakIAS9-aT_XOcl_yrK"""

  val queueReg = """^queue (.+)$""".r
  val playNumReg = """^play (\d+)$""".r
  val playQueueReg = """^play (\D.+)$""".r
  val removeReg = """^remove (\d+(?:-\d+)?)$""".r
  val volumeReg = """^volume (\d+)$""".r
  val gotoRegSec = """^goto (\d+)$""".r
  val gotoRegTime = """^goto (\d+):(\d{2})$""".r
  val gotoRegTimeLong = """^goto (\d+):(\d{2}):(\d{2})$""".r

  /**
    * @return Whether this [[com.blockworker.gopnik.messagehandlers.IMessageHandler MessageHandler]] processed a command from the message
    */
  override def handleCommand(event: MessageReceivedEvent): Boolean = {
    val member = event.getMember
    val message = event.getMessage
    val channel = event.getTextChannel
    message.getContentRaw.substring(1) match {
      case "join" => PlaylistManager.join(event)
      case "disconnect" =>
        if (VoiceHandler.disconnect()) message.addReaction("✅").queue()
        else message.addReaction("❌").queue()
      case "queue gopnik" => PlaylistManager.addTrack(GOPNIK_LIST, message)
      case queueReg(id) => PlaylistManager.addTrack(id, message)
      case "playlist" => PlaylistManager.sendPlaylist(message)
      case "play" =>
        if (VoiceHandler.getTrack != null) message.addReaction("❌").queue()
        if (PlaylistManager.startNextTrack()) message.addReaction("✅").queue()
        else message.addReaction("❌").queue()
      case playNumReg(id) =>
        PlaylistManager.curIndex = math.max(-1, math.min(PlaylistManager.playlist.length - 2, id.toInt - 2))
        if (PlaylistManager.startNextTrack()) message.addReaction("✅").queue()
        else message.addReaction("❌").queue()
      case "play gopnik" => PlaylistManager.addTrack(GOPNIK_LIST, message, true)
      case playQueueReg(id) => PlaylistManager.addTrack(id, message, true)
      case "stop" =>
        if (VoiceHandler.stopTrack()) {
          VoiceHandler.setPaused(false)
          message.addReaction("✅").queue()
        } else message.addReaction("❌").queue()
      case "next" =>
        if (VoiceHandler.getTrack == null) message.addReaction("❌").queue()
        if (PlaylistManager.startNextTrack()) message.addReaction("✅").queue()
        else message.addReaction("❌").queue()
      case "pause" =>
        if (VoiceHandler.setPaused(true)) message.addReaction("✅").queue()
        else message.addReaction("❌").queue()
      case "resume" =>
        if (VoiceHandler.setPaused(false)) message.addReaction("✅").queue()
        else message.addReaction("❌").queue()
      case "noloop" => PlaylistManager.setPlayMode(NoLoop, message)
      case "loop" => PlaylistManager.setPlayMode(Loop, message)
      case "shuffle" => PlaylistManager.setPlayMode(Shuffle, message)
      case "remove" =>
        PlaylistManager.removeTracks(PlaylistManager.curIndex, 1, message)
        if (PlaylistManager.playlist.isEmpty) {
          VoiceHandler.stopTrack()
          VoiceHandler.setPaused(false)
        } else if (VoiceHandler.getTrack != null) {
          PlaylistManager.curIndex = math.max(-1, PlaylistManager.curIndex - 1)
          PlaylistManager.startNextTrack() //restart current track
        }
      case removeReg(ids) =>
        val (id, cnt) = getIndexAndCount(ids)
        PlaylistManager.removeTracks(id, cnt, message)
        if (PlaylistManager.playlist.isEmpty) {
          VoiceHandler.stopTrack()
          VoiceHandler.setPaused(false)
        } else if (VoiceHandler.getTrack != null && PlaylistManager.curIndex < id + cnt && PlaylistManager.curIndex >= id) { //if track is affected
          PlaylistManager.curIndex = math.max(-1, math.min(PlaylistManager.playlist.indices.last - 1, PlaylistManager.curIndex - 1))
          PlaylistManager.startNextTrack() //restart current track
        }
      case volumeReg(vol) =>
        if (VoiceHandler.setVolume(vol.toInt)) message.addReaction("✅").queue()
        else message.addReaction("❌").queue()
      case gotoRegSec(sec) =>
        if (VoiceHandler.setTrackPos(sec.toInt * 10000)) message.addReaction("✅").queue()
        else message.addReaction("❌").queue()
      case gotoRegTime(min, sec) =>
        if (sec.toInt >= 60) message.addReaction("❌").queue()
        if (VoiceHandler.setTrackPos(min.toInt * 600000 + sec.toInt * 10000)) message.addReaction("✅").queue()
        else message.addReaction("❌").queue()
      case gotoRegTimeLong(hr, min, sec) =>
        if (min.toInt >= 60 || sec.toInt >= 60) message.addReaction("❌").queue()
        if (VoiceHandler.setTrackPos(hr.toInt * 36000000 + min.toInt * 600000 + sec.toInt * 10000)) message.addReaction("✅").queue()
        else message.addReaction("❌").queue()
      case "clearplaylist" =>
        if (PlaylistManager.playlist.isEmpty || !VoiceHandler.isConnected) message.addReaction("❌").queue()
        else {
          if (VoiceHandler.getTrack != null) VoiceHandler.stopTrack()
          PlaylistManager.playlist.clear()
          message.addReaction("✅").queue()
        }
      case _ => return false
    }
    true
  }

  def getIndexAndCount(str: String): (Int, Int) = {
    if (str.contains("-")) {
      val parts = str.split("-")
      val id = math.max(0, parts(0).toInt - 1)
      (id, parts(1).toInt - id)
    } else (math.max(0, str.toInt - 1), 1)
  }

}
