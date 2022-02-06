package com.tg.bot.telegramcryptobot.util;

public class CallbackDataBuilder {

    public static String buildCallback(Command command, Object value) {
        return String.format("%s %s %s", command.getCommandName(), Command.EMPTY.getCommandName(), value);
    }

    public static String buildCallback(Command command, Command backCommand) {
        return String.format("%s %s %s", command.getCommandName(), backCommand.getCommandName(), Command.EMPTY.getCommandName());
    }

    public static String buildCallback(Command command, Command backCommand, Object value) {
        return String.format("%s %s %s", command.getCommandName(), backCommand.getCommandName(), value);
    }

}
