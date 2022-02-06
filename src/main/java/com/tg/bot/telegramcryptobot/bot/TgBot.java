package com.tg.bot.telegramcryptobot.bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.tg.bot.telegramcryptobot.exceptions.BotException;
import com.tg.bot.telegramcryptobot.util.Command;
import com.tg.bot.telegramcryptobot.util.Messenger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class TgBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(TgBot.class);

    private final Messenger messenger;
    private final MessageProcessor messageProcessor;
    private final CallbackProcessor callbackProcessor;
    private final TelegramBot bot;

    private final Executor executor = Executors.newFixedThreadPool(4);

    @Autowired
    public TgBot(Messenger messenger, MessageProcessor messageProcessor, CallbackProcessor callbackProcessor) {
        this.messenger = messenger;
        this.messageProcessor = messageProcessor;
        this.callbackProcessor = callbackProcessor;
        this.bot = new TelegramBot(System.getProperty("TOKEN_BOT"));
    }

    public void serve() {
        bot.setUpdatesListener(updates -> {
            updates.stream().filter(update -> update.message() != null || update.callbackQuery() != null)
                    .forEach(update -> executor.execute(() -> processManager(update)));
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }


    public SendResponse sendMessage(long chatId, String text) {
        return bot.execute(new SendMessage(chatId, text));
    }

    private void processManager(Update update) {
        if (update.message() != null) {
            process(update.message());
        } else if (update.callbackQuery() != null) {
            process(update.callbackQuery());
        }
    }

    private void process(Message message) {
        LocaleContextHolder.setLocale(Locale.forLanguageTag(message.from().languageCode()));
        long chatId = message.chat().id();
        String command = message.text();

        if (command.startsWith("/help") || command.startsWith("/start")) {
            sendMessage(chatId, messenger.codeMessage("tg.message.help", message.from().firstName()));
            return;
        }

        String[] values = command.split(" ");

        try {

            switch (Command.of(values[0])) {
                case PRICE:
                    sendMessage(chatId, messageProcessor.doPrice(values));
                    break;
                case LIST:
                    bot.execute(messageProcessor.doList(chatId));
                    break;
                case ADD:
                    sendMessage(chatId, messageProcessor.doAdd(values, chatId, message.from()));
                    break;
                case REMOVE:
                    bot.execute(messageProcessor.doRemove(chatId));
                    break;
                default:
                    sendMessage(chatId, messenger.codeMessage("tg.message.unknown-command"));
            }

        } catch (ArrayIndexOutOfBoundsException ex) {
            sendMessage(chatId, messenger.codeMessage("tg.message.not-enough-data"));
        } catch (BotException ex) {
            sendMessage(chatId, ex.getMessage());
        } catch (Exception ex) {
            LOGGER.warn("Error during execute SendMessage:", ex);
            sendMessage(chatId, messenger.codeMessage("tg.message.error.message"));
            sendMessage(Long.parseLong(System.getProperty("CHAT_ID")), messenger.codeMessage(
                    "tg.message.error",
                    ex.getMessage(),
                    Arrays.toString(ex.getStackTrace())
            ));
        }

    }

    private void process(CallbackQuery callback) {
        LocaleContextHolder.setLocale(Locale.forLanguageTag(callback.from().languageCode()));
        String[] values = callback.data().split(" ");

        long chatId = callback.from().id();
        int messageId = callback.message().messageId();
        Command command = Command.of(values[0]);
        Command backCommand = Command.of(values[1]);

        try {

            switch (command) {
                case GET:
                    bot.execute(callbackProcessor.getAlerts(chatId, messageId, values[2]));
                    break;
                case INFO:
                    bot.execute(callbackProcessor.getInfoAlert(chatId, messageId, Integer.parseInt(values[2])));
                    break;
                case REMOVE:
                    bot.execute(callbackProcessor.removeAlert(chatId, messageId, values[2]));
                    break;
                case BACK:
                    bot.execute(callbackProcessor.back(chatId, messageId, backCommand, values[2]));
                    break;
                default:
                    sendMessage(chatId, messenger.codeMessage("tg.message.unknown-command"));
            }

        } catch (Exception ex) {
            LOGGER.warn("Error during execute CallbackQuery:", ex);
            sendMessage(chatId, messenger.codeMessage("tg.message.error.message"));
            sendMessage(Long.parseLong(System.getProperty("CHAT_ID")), messenger.codeMessage(
                    "tg.message.error",
                    ex.getMessage(),
                    Arrays.toString(ex.getStackTrace())
            ));
        }

    }

}
