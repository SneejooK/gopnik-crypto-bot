package com.tg.bot.telegramcryptobot.exceptions;

public class BotException extends RuntimeException {

    public static final String ERROR_MESSAGE_MESSAGE = "Error during execute SendMessage";
    public static final String ERROR_CALLBACK_MESSAGE = "Error during execute CallbackQuery";
    public static final String ERROR_ALERT_MESSAGE = "Error sending notification";

    public BotException(String message) {
        super(message);
    }

}
