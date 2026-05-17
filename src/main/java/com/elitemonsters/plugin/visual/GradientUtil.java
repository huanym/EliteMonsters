package com.elitemonsters.plugin.visual;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradientUtil {

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<g:#([0-9a-fA-F]{6}):#([0-9a-fA-F]{6})>(.+?)</g>");
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
        LegacyComponentSerializer.builder().character('&').hexColors().build();

    public static Component parse(String text) {
        if (text == null) return Component.empty();
        if (!text.contains("<g:")) return LEGACY_SERIALIZER.deserialize(text);

        TextComponent.Builder builder = Component.text();
        int lastEnd = 0;
        Matcher matcher = GRADIENT_PATTERN.matcher(text);

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String before = text.substring(lastEnd, matcher.start());
                builder.append(LEGACY_SERIALIZER.deserialize(before));
            }
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);
            String content = matcher.group(3);
            builder.append(gradient(content, startHex, endHex));
            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            builder.append(LEGACY_SERIALIZER.deserialize(remaining));
        }

        return builder.build();
    }

    public static Component gradient(String text, String startHex, String endHex) {
        TextColor startColor = TextColor.fromHexString("#" + startHex);
        TextColor endColor = TextColor.fromHexString("#" + endHex);
        if (startColor == null || endColor == null) return Component.text(text);
        return gradient(text, startColor, endColor);
    }

    public static Component gradient(String text, TextColor start, TextColor end) {
        if (text == null || text.isEmpty()) return Component.empty();
        if (text.length() == 1) return Component.text(text).color(start);
        TextComponent.Builder builder = Component.text();
        int len = text.length();
        for (int i = 0; i < len; i++) {
            float ratio = (float) i / (len - 1);
            int r = (int) (start.red() + (end.red() - start.red()) * ratio);
            int g = (int) (start.green() + (end.green() - start.green()) * ratio);
            int b = (int) (start.blue() + (end.blue() - start.blue()) * ratio);
            builder.append(Component.text(String.valueOf(text.charAt(i))).color(TextColor.color(r, g, b)));
        }
        return builder.build();
    }

    public static Component gradientFast(String text, String startHex, String endHex) {
        return gradient(text, startHex, endHex);
    }
}