package com.tg.bot.telegramcryptobot.util;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum Command {

    PRICE("/price"),
    PRICELIST("/pricelist"),
    LIST("/list"),
    GET("/get"),
    ADD("/add"),
    REMOVE("/rm"),
    INFO("/info"),
    BACK("/back"),
    ClOSE("/close"),
    EMPTY("/nothing");

    private final String commandName;

    Command(String command) {
        this.commandName = command;
    }

    public static Command of(String command) {
        return Arrays.stream(values()).filter(c -> c.commandName.equals(command)).findFirst().orElse(Command.EMPTY);
    }

}
