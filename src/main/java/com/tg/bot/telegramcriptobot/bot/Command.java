package com.tg.bot.telegramcriptobot.bot;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Command {
    PRICE("/price"), GET("/get"), ADD("/add"), REMOVE("/rm"), NOT_FOUND("");

    private final String commandName;

    Command(String command) {
        this.commandName = command;
    }

    public static Command of(String command) {
        return Arrays.stream(values()).filter(c -> c.commandName.equals(command)).findFirst().orElse(Command.NOT_FOUND);
    }

}
