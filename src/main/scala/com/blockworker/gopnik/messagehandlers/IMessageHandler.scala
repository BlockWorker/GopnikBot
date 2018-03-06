package com.blockworker.gopnik.messagehandlers

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

trait IMessageHandler {

  /**
    * @return Whether this [[com.blockworker.gopnik.messagehandlers.IMessageHandler MessageHandler]] processed a command from the message
    */
  def handleCommand(event: MessageReceivedEvent): Boolean

}
