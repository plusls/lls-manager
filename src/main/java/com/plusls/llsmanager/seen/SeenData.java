package com.plusls.llsmanager.seen;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

class SeenData implements Comparable<SeenData> {
    public long time;
    public Component text;
    public Component listText;

    public SeenData(long time, Component text, Component listText) {
        this.time = time;
        this.text = text;
        this.listText = listText;
    }

    @Override
    public int compareTo(@NotNull SeenData o) {
        return Long.compare(time, o.time);
    }
}