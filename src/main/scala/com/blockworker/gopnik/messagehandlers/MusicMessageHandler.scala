package com.blockworker.gopnik.messagehandlers

import com.blockworker.gopnik.music.PlaylistManager.{Loop, NoLoop, Shuffle}
import com.blockworker.gopnik.music.{PlaylistManager, VoiceHandler}
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

object MusicMessageHandler extends IMessageHandler {

  val queueReg = """^queue (.+)$""".r
  val playNumReg = """^play (\d+)$""".r
  val playQueueReg = """^play (\D.+)$""".r
  val removeReg = """^remove (\d+(?:-\d+)?)$""".r
  val volumeReg = """^volume (\d+)$""".r
  val gotoRegSec = """^goto (\d+)$""".r
  val gotoRegTime = """^goto (\d+):(\d{2})$""".r

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
      case queueReg(id) => PlaylistManager.addTrack(id, message)
      case "playlist" => PlaylistManager.sendPlaylist(message)
      case "play" =>
        if (VoiceHandler.getTrack != null) message.addReaction("❌").queue()
        if (PlaylistManager.startNextTrack()) message.addReaction("✅").queue()
        else message.addReaction("❌").queue()
      case playNumReg(id) =>
        PlaylistManager.curIndex = math.max(-1, math.min(PlaylistManager.playlist.length - 2, id.toInt - 1))
        if (PlaylistManager.startNextTrack()) message.addReaction("✅").queue()
        else message.addReaction("❌").queue()
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
      case "remove" => PlaylistManager.removeTracks(null, message)
      case removeReg(ids) => PlaylistManager.removeTracks(getIDs(ids), message)
      case volumeReg(vol) =>
        if (VoiceHandler.setVolume(vol.toInt)) message.addReaction("✅").queue()
        else message.addReaction("❌").queue()
      case gotoRegSec(sec) =>
        if (VoiceHandler.setTrackPos(sec.toInt * 1000)) message.addReaction("✅").queue()
        else message.addReaction("❌").queue()
      case gotoRegTime(min, sec) =>
        if (sec.toInt >= 60) message.addReaction("❌").queue()
        if (VoiceHandler.setTrackPos(min.toInt * 60000 + sec.toInt * 1000)) message.addReaction("✅").queue()
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

  def getIDs(str: String): Array[Int] = {
    if (str.contains("-")) {
      val parts = str.split("-")
      (math.max(0, parts(0).toInt - 1) until parts(1).toInt).toArray
    } else Array(str.toInt)
  }

}
