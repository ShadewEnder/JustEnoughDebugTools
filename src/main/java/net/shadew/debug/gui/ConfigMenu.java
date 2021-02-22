package net.shadew.debug.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class ConfigMenu extends DrawableHelper implements Element, Drawable {
    private static final Identifier TEXTURE = new Identifier("debug:textures/gui/options.png");
    public static final int MENU_WIDTH = 128;
    public static final int ITEM_HEIGHT = 20;
    private static final int TOP_PADDING = ITEM_HEIGHT / 2 - 4;
    private static final int SIDE_PADDING = 6;
    private static final int FOREGROUND = 0xFFFFFF;

    private final Text title;
    private Runnable closeHandler;

    private final List<Entry> entries = new ArrayList<>();
    private final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
    private final TextureManager textures = MinecraftClient.getInstance().getTextureManager();
    private boolean visible;
    private boolean closed = true;
    private ConfigMenu swapPartner;
    private ConfigMenu swapManager;
    private float lastVisibility;
    private float visibility;
    private int left;
    private int height;

    private double scroll;

    @Deprecated
    public ConfigMenu(Text title, Runnable closeHandler) {
        this.title = title;
        this.closeHandler = closeHandler;
    }

    public ConfigMenu(Text title) {
        this.title = title;
    }

    public void setCloseHandler(Runnable closeHandler) {
        this.closeHandler = closeHandler;
    }

    public void sort() {
        entries.sort(null);
    }

    public boolean canRender() {
        return visibility > 0 || lastVisibility > 0 || swapPartner != null || swapManager != null;
    }

    public boolean canInteract() {
        return visibility == 1 && lastVisibility == 1 && swapManager == null && swapPartner == null;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isFullyClosed() {
        return closed && !canRender();
    }

    public void addEntry(Entry entry) {
        entries.add(entry);
    }

    public void clearEntries() {
        entries.clear();
    }

    public void forceVisible(boolean visible) {
        this.visible = visible;
        this.visibility = visible ? 1 : 0;
        this.lastVisibility = visible ? 1 : 0;
    }

    public void close() {
        closeQuietly();
        closeHandler.run();
    }

    public void closeQuietly() {
        setVisible(false);
        setClosed(true);
    }

    public void forceClose() {
        forceVisible(false);
        setClosed(true);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setLeftOffset(int off) {
        left = off;
    }

    public int getDisplayableWidth(float partialTicks) {
        if (swapManager != null) {
            return MENU_WIDTH;
        }

        float visibility = MathHelper.lerp(partialTicks, lastVisibility, this.visibility);
        float widthf = 1;

        if (visibility < 1) {
            if (visibility < 0.5) {
                float slideProgress = visibility * 2;
                widthf = -MathHelper.cos(slideProgress * (float) Math.PI) * 0.5f + 0.5f;
            }
        }

        return (int) (MENU_WIDTH * widthf);
    }

    public void swapWith(ConfigMenu other) {
        swapPartner = other;
        other.swapManager = this;
    }

    public void open() {
        setVisible(true);
        closed = false;
    }

    public void tick() {
        if (swapManager != null) {
            swapManager.tick();
            return;
        }
        for (int i = 0; i < 2; i++) {
            lastVisibility = visibility;
            visibility += visible && swapPartner == null ? 0.1 : -0.1;
            if (visibility >= 1) {
                visibility = 1;
            }
            if (visibility <= 0) {
                visibility = 0;
            }
            if (visibility <= 0.5 && swapPartner != null) {
                swapPartner.visible = true;
                swapPartner.visibility = visibility;
                swapPartner.lastVisibility = lastVisibility;
                swapPartner.swapManager = null;
                swapPartner = null;
                visible = false;
                visibility = 0;
                lastVisibility = 0;
            }
        }

        int itemsHeight = entries.size() * ITEM_HEIGHT;
        int viewHeight = height - ITEM_HEIGHT;
        int overflow = itemsHeight - viewHeight;
        if (overflow <= 0) scroll = 0;
    }

    private void playClickSound(float pitch) {
        MinecraftClient.getInstance()
                       .getSoundManager()
                       .play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!canInteract() || button != GLFW.GLFW_MOUSE_BUTTON_LEFT)
            return false;

        int itemsHeight = entries.size() * ITEM_HEIGHT;
        int viewHeight = height - ITEM_HEIGHT;
        int overflow = itemsHeight - viewHeight;
        int offset = overflow <= 0 ? 0 : (int) (overflow * scroll);

        if (mouseY >= ITEM_HEIGHT) {
            int y = ITEM_HEIGHT - offset;
            for (Entry entry : entries) {
                int x1 = left;
                int y1 = y;
                int x2 = left + MENU_WIDTH;
                int y2 = y + ITEM_HEIGHT;

                if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
                    if (entry instanceof SpinnerEntry) {
                        int sx1 = left + MENU_WIDTH - ITEM_HEIGHT + 6;
                        int sy1 = y + 4;
                        int sx2 = sx1 + 8;
                        int sy2 = sy1 + 12;
                        int syh = sy1 + 8;
                        if (mouseX >= sx1 && mouseX <= sx2 && mouseY >= sy1 && mouseY <= syh) {
                            playClickSound(1.2f);
                            ((SpinnerEntry) entry).upperClickHandler.run();
                        } else if (mouseX >= sx1 && mouseX <= sx2 && mouseY >= syh && mouseY <= sy2) {
                            playClickSound(0.8f);
                            ((SpinnerEntry) entry).lowerClickHandler.run();
                        } else {
                            playClickSound(1);
                            entry.clickHandler.run();
                        }
                    } else {
                        playClickSound(1);
                        entry.clickHandler.run();
                    }
                    return true;
                }
                y += ITEM_HEIGHT;
            }
        }

        int x1 = left + MENU_WIDTH - ITEM_HEIGHT;
        int y1 = 0;
        int x2 = left + MENU_WIDTH;
        int y2 = ITEM_HEIGHT;

        if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
            closed = true;
            playClickSound(1);
            closeHandler.run();
            setVisible(false);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX >= left && mouseX <= left + MENU_WIDTH) {
            int itemsHeight = entries.size() * ITEM_HEIGHT;
            int viewHeight = height - ITEM_HEIGHT;
            int overflow = itemsHeight - viewHeight;
            if (overflow > 0) {
                double offset = overflow * scroll;
                offset -= amount * ITEM_HEIGHT / 2;
                scroll = offset / overflow;
            }
        }

        scroll = MathHelper.clamp(scroll, 0, 1);

        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= left && mouseX <= left + MENU_WIDTH;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        if (swapManager != null) {
            swapManager.render(matrices, mouseX, mouseY, partialTicks);
            return;
        }

        RenderSystem.enableBlend();
        float visibility = MathHelper.lerp(partialTicks, lastVisibility, this.visibility);

        if (visibility <= 0) {
            return;
        }

        float itemOpacity = 1;
        float widthf = 1;
        boolean interactive = true;

        if (visibility < 1) {
            if (visibility < 0.5) {
                float slideProgress = visibility * 2;
                itemOpacity = 0;
                interactive = false;
                widthf = -MathHelper.cos(slideProgress * (float) Math.PI) * 0.5f + 0.5f;
            } else {
                float slideProgress = visibility * 2 - 1;
                itemOpacity = -MathHelper.cos(slideProgress * (float) Math.PI) * 0.5f + 0.5f;
                interactive = false;
            }
        }

        int width = (int) (MENU_WIDTH * widthf);
        int alphaFactor = (int) (itemOpacity * 255) << 24;

        if (widthf > 0) {
            textures.bindTexture(TEXTURE);
            int h = height;

            while (h > 0) {
                drawTexture(matrices, left, height - h, MENU_WIDTH - width, 0, width, Math.min(height, h));
                h -= height;
            }
        }

        if ((alphaFactor & 0xFC000000) != 0) {
            int itemsHeight = entries.size() * ITEM_HEIGHT;
            int viewHeight = height - ITEM_HEIGHT;
            int overflow = itemsHeight - viewHeight;
            int offset = overflow <= 0 ? 0 : (int) (overflow * scroll);

            int y = ITEM_HEIGHT - offset;
            for (Entry entry : entries) {
                int x1 = left;
                int y1 = y;
                int x2 = left + width;
                int y2 = y + ITEM_HEIGHT;
                int tc = entry.textColor | alphaFactor;

                RenderSystem.enableBlend();

                int bottomY = y + ITEM_HEIGHT;
                int visibleHeight = Math.min(ITEM_HEIGHT, bottomY - ITEM_HEIGHT / 2);

                if (visibleHeight > 0 && (y >= 0 || y <= height)) {
                    RenderSystem.color4f(1, 1, 1, itemOpacity);
                    textures.bindTexture(TEXTURE);
                    int uOffset = ITEM_HEIGHT - visibleHeight;
                    int topY = bottomY - visibleHeight;

                    if (interactive && mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2) {
                        drawTexture(matrices, left, topY, MENU_WIDTH, ITEM_HEIGHT * (3 + entry.type() * 2) + uOffset, width, visibleHeight);
                    } else {
                        drawTexture(matrices, left, topY, MENU_WIDTH, ITEM_HEIGHT * (2 + entry.type() * 2) + uOffset, width, visibleHeight);
                    }
                    RenderSystem.color4f(1, 1, 1, 1);

                    RenderSystem.enableBlend();
                    drawTextWithShadow(matrices, textRenderer, entry.text, SIDE_PADDING + left, TOP_PADDING + y, tc);

                    Text extra = entry.extraInfo();
                    if (extra != null) {
                        int wdt = textRenderer.getWidth(extra);
                        textRenderer.drawWithShadow(matrices, extra, x2 - ITEM_HEIGHT - wdt, TOP_PADDING + y, tc);
                    }
                }

                y += ITEM_HEIGHT;
            }
        }

        matrices.push();
        matrices.translate(0, 0, 10);

        if (widthf > 0) {

            textures.bindTexture(TEXTURE);
            drawTexture(matrices, left, 0, 2 * MENU_WIDTH - width, 0, width, ITEM_HEIGHT);

            int cbWidth = Math.min(ITEM_HEIGHT, width);

            int x1 = left + width - cbWidth;
            int y1 = 0;
            int x2 = left + width;
            int y2 = ITEM_HEIGHT;

            if (interactive && mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2) {
                textures.bindTexture(TEXTURE);
                drawTexture(matrices, x1, 0, 2 * MENU_WIDTH - cbWidth, ITEM_HEIGHT, cbWidth, ITEM_HEIGHT);
            }
        }


        if ((alphaFactor & 0xFC000000) != 0) {
            textRenderer.draw(matrices, title, SIDE_PADDING + left, TOP_PADDING, alphaFactor);
        }

        matrices.pop();
    }

    public static class Entry implements Comparable<Entry> {
        private Text text;
        private final int textColor;
        private final Runnable clickHandler;
        protected Supplier<Text> extraInfo = () -> null;

        public Entry(Text text, Runnable clickHandler, int textColor) {
            this.text = text;
            this.clickHandler = clickHandler;
            this.textColor = textColor == -1 ? FOREGROUND : textColor & 0xFFFFFF;
        }

        public Entry(Text text, Runnable clickHandler) {
            this(text, clickHandler, FOREGROUND);
        }

        public Entry(Text text, Runnable clickHandler, Supplier<Text> extraInfo) {
            this(text, clickHandler, FOREGROUND);
            this.extraInfo = extraInfo;
        }

        public void setText(Text text) {
            this.text = text;
        }

        public Text getText() {
            return text;
        }

        protected int type() {
            return 0;
        }

        public boolean hasCheck() {
            return false;
        }

        protected Text extraInfo() {
            return extraInfo.get();
        }

        @Override
        public int compareTo(@NotNull ConfigMenu.Entry o) {
            if (o instanceof MenuEntry) {
                return -o.compareTo(this);
            }
            return text.getString().compareTo(o.text.getString());
        }
    }

    public static class MenuEntry extends Entry {
        public MenuEntry(Text text, Runnable clickHandler, int textColor) {
            super(text, clickHandler, textColor);
        }

        public MenuEntry(Text text, Runnable clickHandler) {
            super(text, clickHandler);
        }

        public MenuEntry(Text text, Runnable clickHandler, Supplier<Text> extraInfo) {
            super(text, clickHandler);
            this.extraInfo = extraInfo;
        }

        @Override
        protected int type() {
            return 1;
        }

        @Override
        public int compareTo(@NotNull Entry o) {
            if (o instanceof MenuEntry) {
                return getText().getString().compareTo(o.getText().getString());
            }
            return -1;
        }
    }

    public static class CheckableEntry extends Entry {
        private final BooleanSupplier hasCheck;

        public CheckableEntry(Text text, Runnable clickHandler, BooleanSupplier hasCheck, int textColor) {
            super(text, clickHandler, textColor);
            this.hasCheck = hasCheck;
        }

        public CheckableEntry(Text text, Runnable clickHandler, BooleanSupplier hasCheck) {
            super(text, clickHandler);
            this.hasCheck = hasCheck;
        }

        public CheckableEntry(Text text, Runnable clickHandler, BooleanSupplier hasCheck, Supplier<Text> extraInfo) {
            super(text, clickHandler);
            this.hasCheck = hasCheck;
            this.extraInfo = extraInfo;
        }

        @Override
        protected int type() {
            return hasCheck.getAsBoolean() ? 2 : 0;
        }
    }

    public static class SpinnerEntry extends Entry {
        private final Runnable upperClickHandler;
        private final Runnable lowerClickHandler;

        public SpinnerEntry(Text text, Runnable clickHandler, Runnable upperClickHandler, Runnable lowerClickHandler, IntSupplier value, int textColor) {
            super(text, clickHandler, textColor);
            this.extraInfo = () -> new LiteralText(value.getAsInt() + "");
            this.upperClickHandler = upperClickHandler;
            this.lowerClickHandler = lowerClickHandler;
        }

        public SpinnerEntry(Text text, Runnable clickHandler, Runnable upperClickHandler, Runnable lowerClickHandler, IntSupplier value) {
            super(text, clickHandler);
            this.extraInfo = () -> new LiteralText(value.getAsInt() + "");
            this.upperClickHandler = upperClickHandler;
            this.lowerClickHandler = lowerClickHandler;
        }

        public SpinnerEntry(Text text, Runnable clickHandler, Runnable upperClickHandler, Runnable lowerClickHandler, Supplier<Text> value, int textColor) {
            super(text, clickHandler, textColor);
            this.extraInfo = value;
            this.upperClickHandler = upperClickHandler;
            this.lowerClickHandler = lowerClickHandler;
        }

        public SpinnerEntry(Text text, Runnable clickHandler, Runnable upperClickHandler, Runnable lowerClickHandler, Supplier<Text> value) {
            super(text, clickHandler);
            this.extraInfo = value;
            this.upperClickHandler = upperClickHandler;
            this.lowerClickHandler = lowerClickHandler;
        }

        @Override
        protected int type() {
            return 3;
        }
    }
}