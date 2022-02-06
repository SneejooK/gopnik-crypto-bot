package com.tg.bot.telegramcryptobot;

import com.tg.bot.telegramcryptobot.bot.TgBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Locale;

@EnableScheduling
@SpringBootApplication
public class TelegramCryptoBotApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(TgBot.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(TelegramCryptoBotApplication.class, args);
        Locale.setDefault(Locale.forLanguageTag("ru"));
        run.getBean(TgBot.class).serve();

        LOGGER.info("Telegram bot started working.");
    }

}
