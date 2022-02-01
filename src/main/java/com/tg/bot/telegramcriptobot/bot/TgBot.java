package com.tg.bot.telegramcriptobot.bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import com.tg.bot.telegramcriptobot.exceptions.BotException;
import com.tg.bot.telegramcriptobot.util.Messenger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class TgBot {

    private final Messenger messenger;
    private final BotProcessor processor;
    private final TelegramBot bot;

    private final Executor executor = Executors.newFixedThreadPool(4);

    @Autowired
    public TgBot(Messenger messenger, BotProcessor processor) {
        this.messenger = messenger;
        this.processor = processor;
        this.bot = new TelegramBot(System.getenv("TOKEN_BOT"));
    }

    public void serve() {
        bot.setUpdatesListener(updates -> {
            updates.stream().filter(update -> update.message() != null)
                    .forEach(update -> executor.execute(() -> process(update.message())));
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    public void sendMessage(long chatId, String text) {
        bot.execute(new SendMessage(chatId, text));
    }

    private void process(Message message) {
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
                    sendMessage(chatId, processor.doPrice(values));
                    break;
                case GET:
                    sendMessage(chatId, processor.doGet(chatId));
                    break;
                case ADD:
                    sendMessage(chatId, processor.doAdd(values, chatId, message.from()));
                    break;
                case REMOVE:
                    sendMessage(chatId, processor.doRemove(values, chatId));
                    break;
                default:
                    sendMessage(chatId, messenger.codeMessage("tg.message.unknown-command"));
            }

        } catch (ArrayIndexOutOfBoundsException ex) {
            sendMessage(chatId, messenger.codeMessage("tg.message.not-enough-data"));
        } catch (BotException ex) {
            sendMessage(chatId, ex.getMessage());
        } catch (Exception ex) {
            sendMessage(chatId, messenger.codeMessage("tg.message.error.message"));
            sendMessage(Long.parseLong(System.getenv("CHAT_Id")), messenger.codeMessage(
                    "tg.message.error",
                    ex.getMessage(),
                    Arrays.toString(ex.getStackTrace())
            ));
        }

    }

}
