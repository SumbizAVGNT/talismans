package me.sumbiz.monntalismans.util;

import java.util.logging.Logger;

public final class Msg {
    private Msg() {}
    public static void info(Logger log, String s) { log.info(s); }
}
