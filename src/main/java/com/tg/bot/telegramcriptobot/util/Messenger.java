package com.tg.bot.telegramcriptobot.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class Messenger {

    public final String[] emoji = {
            "\uD83D\uDE31",
            "\uD83E\uDD73",
            "\uD83D\uDC4F",
            "\uD83D\uDC14",
            "\uD83D\uDCA5",
            "\uD83D\uDD25",
            "\uD83C\uDF08",
            "\uD83C\uDF40",
            "\uD83C\uDFC6"};

    private final MessageSource messageSource;
    private final Random random;

    @Autowired
    public Messenger(MessageSource messageSource) {
        this.messageSource = messageSource;
        this.random = new Random();
    }

    public String codeMessage(String codeMessage) {
        return messageSource.getMessage(codeMessage, null, LocaleContextHolder.getLocale());
    }

    public String codeMessage(String codeMessage, Object... args) {
        return messageSource.getMessage(codeMessage, args, LocaleContextHolder.getLocale());
    }

    public String getRandomEmoji() {
        return emoji[random.nextInt(emoji.length)];
    }

}
