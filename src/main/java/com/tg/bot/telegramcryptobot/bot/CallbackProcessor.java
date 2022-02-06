package com.tg.bot.telegramcryptobot.bot;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.EditMessageText;
import com.tg.bot.telegramcryptobot.entities.Alert;
import com.tg.bot.telegramcryptobot.services.AlertService;
import com.tg.bot.telegramcryptobot.util.Command;
import com.tg.bot.telegramcryptobot.util.Messenger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.tg.bot.telegramcryptobot.util.CallbackDataBuilder.buildCallback;

@Component
public class CallbackProcessor {

    private final AlertService alertService;
    private final MessageProcessor messageProcessor;
    private final Messenger messenger;

    @Autowired
    public CallbackProcessor(AlertService alertService, MessageProcessor messageProcessor, Messenger messenger) {
        this.alertService = alertService;
        this.messageProcessor = messageProcessor;
        this.messenger = messenger;
    }

    public BaseRequest back(long chatId, int messageId, Command command, String value) {
        switch (command) {
            case GET:
                return getAlerts(chatId, messageId, value);
            case LIST:
                return messageProcessor.doList(chatId, messageId);
            case REMOVE:
                return removeAlert(chatId, messageId, value);
            default:
                return new DeleteMessage(chatId, messageId);
        }
    }

    public EditMessageText getInfoAlert(long chatId, int messageId, int alertId) {
        Alert alert = alertService.getAlertById(alertId);
        String text = messenger.codeMessage(
                "tg.message.alert.info",
                            alert.getCurrency(),
                            alert.isPositive() ?
                                    messenger.codeMessage("tg.message.alert.more") :
                                    messenger.codeMessage("tg.message.alert.less"),
                            alert.getPrice()
                );

        InlineKeyboardMarkup keyboardMarkups = new InlineKeyboardMarkup();
        keyboardMarkups.addRow(new InlineKeyboardButton(messenger.codeMessage("tg.button.remove")).callbackData(
                buildCallback(Command.REMOVE, alertId)
        ));

        keyboardMarkups.addRow(new InlineKeyboardButton(messenger.codeMessage("tg.button.back")).callbackData(
                buildCallback(Command.BACK, Command.GET, alert.getCurrency())
        ));

        return new EditMessageText(chatId, messageId, text).replyMarkup(keyboardMarkups);
    }

    public EditMessageText getAlerts(long chatId, int messageId, String currency) {
        List<Alert> alerts = alertService.getAllByChatIdAndCurrency(chatId, currency);

        if (alerts.isEmpty()) {
            return (EditMessageText) back(chatId, messageId, Command.LIST, Command.EMPTY.getCommandName());
        }

        StringBuilder builder = new StringBuilder();
        List<InlineKeyboardButton> buttons = new ArrayList<>();

        if (alerts.size() > 1) {
            builder.append(messenger.codeMessage(
                    "tg.message.list.alerts", currency, messageProcessor.getActualPrice(currency)));
        } else {
            builder.append(messenger.codeMessage(
                    "tg.message.list.alert", currency, messageProcessor.getActualPrice(currency)));
        }

        for (int i = 0; i < alerts.size(); i++) {

            Alert alert = alerts.get(i);

            builder.append(i + 1)
                    .append(" : ")
                    .append(messenger.codeMessage("tg.message.alert.cost"))
                    .append(" ");

            if (alert.isPositive()) {
                builder.append(messenger.codeMessage("tg.message.alert.more"));
            } else {
                builder.append(messenger.codeMessage("tg.message.alert.less"));
            }

            builder.append(" - ")
                    .append(alert.getPrice())
                    .append("$");

            buttons.add(new InlineKeyboardButton(String.valueOf(i + 1)).callbackData(
                    buildCallback(Command.INFO, alert.getId())
            ));

        }

        InlineKeyboardMarkup keyboardMarkups = new InlineKeyboardMarkup();
        keyboardMarkups.addRow(buttons.toArray(InlineKeyboardButton[]::new));
        keyboardMarkups.addRow(
                new InlineKeyboardButton(messenger.codeMessage("tg.button.back")).callbackData(
                        buildCallback(Command.BACK, Command.LIST)
                ));

        return new EditMessageText(chatId, messageId, builder.toString()).replyMarkup(keyboardMarkups);
    }

    public EditMessageText removeAlert(long chatId, int messageId, String value) {

        if (Command.EMPTY.getCommandName().equals(value)) {
            alertService.removeAllByChatId(chatId);
            return new EditMessageText(chatId, messageId, messenger.codeMessage("tg.message.remove.alerts"));
        }

        int alertId = Integer.parseInt(value);
        Alert alert = alertService.getAlertById(alertId);
        alertService.remove(alertId);
        return (EditMessageText) back(chatId, messageId, Command.GET, alert.getCurrency());
    }

}
