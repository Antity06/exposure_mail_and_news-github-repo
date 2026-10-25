package com.coolnydude.exposureletters.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Simple multi-line text editor drawn without a frame and with dark text, so it can sit on paper.
 * Supports typing, Enter, Backspace/Delete, arrows, Home/End, Ctrl+A/C/V/X, click-to-place cursor, and read-only mode.
 */
public class PaperTextArea extends AbstractWidget {
    public static final int LINE_HEIGHT = 9;

    private final Font font;
    private String value = "";
    private int cursor;
    private boolean allSelected;
    private int maxLength = 256;
    private boolean multiline = true;
    private boolean editable = true;
    private int textColor = 0xFF000000;
    private int backgroundColor = 0;
    private Component placeholder = Component.empty();
    private Consumer<String> responder = s -> {};
    private long focusedTime;

    private record Line(int start, int end) {}

    public PaperTextArea(Font font, int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
        this.font = font;
    }

    public PaperTextArea setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public PaperTextArea setMultiline(boolean multiline) {
        this.multiline = multiline;
        return this;
    }

    public PaperTextArea setEditable(boolean editable) {
        this.editable = editable;
        return this;
    }

    public PaperTextArea setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    public PaperTextArea setBackgroundColor(int color) {
        this.backgroundColor = color;
        return this;
    }

    public PaperTextArea setPlaceholder(Component placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    public PaperTextArea setResponder(Consumer<String> responder) {
        this.responder = responder;
        return this;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = filter(value == null ? "" : value);
        if (this.value.length() > maxLength) this.value = this.value.substring(0, maxLength);
        this.cursor = this.value.length();
        this.allSelected = false;
    }

    private int innerWidth() {
        return width - 4;
    }

    private int maxLines() {
        return Math.max(1, (height - 4) / LINE_HEIGHT);
    }

    private List<Line> lines(String text) {
        List<Line> lines = new ArrayList<>();
        font.getSplitter().splitLines(text, innerWidth(), Style.EMPTY, false, (style, start, end) -> lines.add(new Line(start, end)));
        if (lines.isEmpty() || text.endsWith("\n")) lines.add(new Line(text.length(), text.length()));
        return lines;
    }

    private int lineOf(List<Line> lines, int index) {
        int result = 0;
        for (int i = 0; i < lines.size(); i++)
            if (lines.get(i).start() <= index) result = i;
        return result;
    }

    private String filter(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\n' && multiline) b.append(c);
            else if (c == '\r') continue;
            else if (c != '§' && StringUtil.isAllowedChatCharacter(c)) b.append(c);
        }
        return b.toString();
    }

    private void insert(String text) {
        if (!editable) return;
        text = filter(text);
        String base = value;
        int at = cursor;
        if (allSelected) {
            base = "";
            at = 0;
        }
        int room = maxLength - base.length();
        if (room <= 0 && !text.isEmpty()) return;
        if (text.length() > room) text = text.substring(0, room);
        String candidate = base.substring(0, at) + text + base.substring(at);
        if (lines(candidate).size() > maxLines()) return;
        value = candidate;
        cursor = at + text.length();
        allSelected = false;
        responder.accept(value);
    }

    private void deleteRange(int from, int to) {
        if (!editable || from < 0 || to > value.length() || from >= to) return;
        value = value.substring(0, from) + value.substring(to);
        cursor = from;
        responder.accept(value);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!isFocused()) return false;
        if (Screen.isSelectAll(key)) {
            allSelected = true;
            return true;
        }
        if (Screen.isCopy(key)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(value);
            return true;
        }
        if (!editable) {
            return false;
        }
        if (Screen.isCut(key)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(value);
            if (allSelected) clearAll();
            return true;
        }
        if (Screen.isPaste(key)) {
            insert(Minecraft.getInstance().keyboardHandler.getClipboard());
            return true;
        }
        List<Line> lines = lines(value);
        switch (key) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (allSelected) clearAll();
                else deleteRange(cursor - 1, cursor);
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (allSelected) clearAll();
                else deleteRange(cursor, cursor + 1);
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (multiline) {
                    insert("\n");
                    return true;
                }
                return false;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                allSelected = false;
                cursor = Math.max(0, cursor - 1);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                allSelected = false;
                cursor = Math.min(value.length(), cursor + 1);
                return true;
            }
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                allSelected = false;
                int li = lineOf(lines, cursor);
                int target = li + (key == GLFW.GLFW_KEY_UP ? -1 : 1);
                if (target >= 0 && target < lines.size()) {
                    Line cur = lines.get(li);
                    int x = font.width(value.substring(cur.start(), Math.min(cursor, cur.end())));
                    cursor = indexAt(lines.get(target), x);
                }
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                allSelected = false;
                cursor = Screen.hasControlDown() ? 0 : lines.get(lineOf(lines, cursor)).start();
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                allSelected = false;
                cursor = Screen.hasControlDown() ? value.length() : lines.get(lineOf(lines, cursor)).end();
                return true;
            }
            default -> {
                // Swallow everything else that could be a game key (inventory, drop...), Escape is handled by the screen.
                return key != GLFW.GLFW_KEY_ESCAPE && key != GLFW.GLFW_KEY_TAB;
            }
        }
    }

    private void clearAll() {
        value = "";
        cursor = 0;
        allSelected = false;
        responder.accept(value);
    }

    private int indexAt(Line line, int x) {
        String text = value.substring(line.start(), line.end());
        return line.start() + font.getSplitter().plainIndexAtWidth(text, x, Style.EMPTY);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (!editable || !isFocused() || !StringUtil.isAllowedChatCharacter(c)) return false;
        insert(Character.toString(c));
        return true;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!editable) return;
        List<Line> lines = lines(value);
        int li = (int) ((mouseY - getY() - 2) / LINE_HEIGHT);
        li = Math.max(0, Math.min(lines.size() - 1, li));
        cursor = indexAt(lines.get(li), (int) (mouseX - getX() - 2));
        allSelected = false;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) focusedTime = System.currentTimeMillis();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (backgroundColor != 0)
            g.fill(getX(), getY(), getX() + width, getY() + height, backgroundColor);
        int x = getX() + 2, y = getY() + 2;

        if (value.isEmpty() && (!isFocused() || !editable)) {
            if (editable) {
                g.drawString(font, placeholder, x, y, 0xFF8A8270, false);
            }
            return;
        }

        List<Line> lines = lines(value);
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            int ly = y + i * LINE_HEIGHT;
            String text = value.substring(line.start(), line.end());
            if (allSelected && !text.isEmpty())
                g.fill(x, ly - 1, x + font.width(text), ly + LINE_HEIGHT - 1, 0x803366FF);
            g.drawString(font, text, x, ly, textColor, false);
        }

        if (editable && isFocused() && ((System.currentTimeMillis() - focusedTime) / 500) % 2 == 0) {
            int li = lineOf(lines, cursor);
            Line line = lines.get(li);
            int cx = x + font.width(value.substring(line.start(), Math.min(Math.max(cursor, line.start()), line.end())));
            int cy = y + li * LINE_HEIGHT;
            g.fill(cx, cy - 1, cx + 1, cy + LINE_HEIGHT - 1, textColor);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
