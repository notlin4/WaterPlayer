package ru.kelcuprum.waterplayer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.meteordev.starscript.Starscript;
import org.meteordev.starscript.value.Value;
import org.meteordev.starscript.value.ValueMap;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.kelcuprum.alinlib.AlinLib;
import ru.kelcuprum.alinlib.api.KeyMappingHelper;
import ru.kelcuprum.alinlib.api.events.alinlib.LocalizationEvents;
import ru.kelcuprum.alinlib.api.events.client.*;
import ru.kelcuprum.alinlib.config.Config;
import ru.kelcuprum.alinlib.config.Localization;
import ru.kelcuprum.alinlib.gui.GuiUtils;
import ru.kelcuprum.alinlib.gui.toast.ToastBuilder;
import ru.kelcuprum.waterplayer.backend.KeyBind;
import ru.kelcuprum.waterplayer.backend.MusicPlayer;
import ru.kelcuprum.waterplayer.backend.WaterPlayerAPI;
import ru.kelcuprum.waterplayer.backend.command.WaterPlayerCommand;
import ru.kelcuprum.waterplayer.frontend.gui.TextureHelper;
import ru.kelcuprum.waterplayer.frontend.gui.overlays.SubtitlesHandler;
import ru.kelcuprum.waterplayer.frontend.gui.screens.control.ModernControlScreen;
import ru.kelcuprum.waterplayer.frontend.localization.MusicHelper;
import ru.kelcuprum.waterplayer.frontend.gui.screens.control.ControlScreen;
import ru.kelcuprum.waterplayer.frontend.gui.overlays.OverlayHandler;

import java.util.ArrayList;
import java.util.List;

public class WaterPlayer implements ClientModInitializer {
    public static Config config = new Config("config/WaterPlayer/config.json");
    public static final Logger LOG = LogManager.getLogger("WaterPlayer");
    public static MusicPlayer player;
    public static Localization localization = new Localization("waterplayer", "config/WaterPlayer/lang");

    @Override
    public void onInitializeClient() {
        log("Hello, world! UwU");
        WaterPlayerAPI.loadConfig();
        player = new MusicPlayer();
        registerBinds();
        ClientCommandRegistrationCallback.EVENT.register(WaterPlayerCommand::register);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            player.startAudioOutput();
            OverlayHandler hud = new OverlayHandler();
            SubtitlesHandler sub = new SubtitlesHandler();
            ScreenEvents.SCREEN_RENDER.register(hud);
            GuiRenderEvents.RENDER.register(hud);
            GuiRenderEvents.RENDER.register(sub);
            ClientTickEvents.START_CLIENT_TICK.register(hud);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(e -> {
            player.getAudioPlayer().stopTrack();
            TextureHelper.saveMap();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (KeyBind bind : keyBinds) {
                if (bind.key().consumeClick()) bind.onExecute().run();
            }
        });
        TextureManagerEvent.INIT.register(TextureHelper::loadTextures);
        LocalizationEvents.DEFAULT_PARSER_INIT.register((starScript -> {
            Starscript ss = starScript.ss;
            ss.set("waterplayer", new ValueMap()
                    .set("player", new ValueMap()
                            .set("volume", () -> Value.number(MusicHelper.getVolume()))
                            .set("speaker_icon", () -> Value.string(MusicHelper.getSpeakerVolume()))
                            .set("repeat_icon", () -> Value.string(MusicHelper.getRepeatState()))
                            .set("pause_icon", () -> Value.string(MusicHelper.getPauseState()))
                    ).set("format", new ValueMap()
                            .set("time", () -> Value.string(MusicHelper.getIsLive() ? WaterPlayer.localization.getLocalization("format.live", true)
                                    : WaterPlayer.localization.getLocalization("format.time", true)))
                            .set("title", () -> Value.string(WaterPlayer.localization.getLocalization("format.title", true)))
                            .set("author", () -> Value.string(WaterPlayer.localization.getLocalization("format.author", true)))
                    ).set("track", new ValueMap()
                            .set("title", () -> Value.string(MusicHelper.getTitle()))
                            .set("author", () -> Value.string(MusicHelper.getAuthor()))
                            .set("time", new ValueMap()
                                    .set("position", () -> Value.string(getTimestamp(MusicHelper.getPosition())))
                                    .set("duration", () -> Value.string(getTimestamp(MusicHelper.getDuration())))
                            )
                    )
            );
        }));

        ScreenEvents.KEY_PRESS.register((Screen screen, int code, int scan, int modifiers, CallbackInfoReturnable<Boolean> var5) -> {
            if (!WaterPlayer.config.getBoolean("ENABLE_KEYBINDS", false)) return;
            if (screen instanceof TitleScreen || screen instanceof PauseScreen || screen instanceof ControlScreen) {
                for (KeyBind bind : keyBinds) {
                    if ((bind.key().matches(code, scan) || bind.key().matchesMouse(code)) && bind.onExecute().run()) {
                        var5.setReturnValue(true);
                        var5.cancel();
                        break;
                    }
                }
            }
        });
    }

    public static String getTimestamp(long milliseconds) {
        int seconds = (int) (milliseconds / 1000) % 60;
        int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
        int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);

        if (hours > 0)
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        else
            return String.format("%02d:%02d", minutes, seconds);
    }

    public static ToastBuilder getToast() {
        return new ToastBuilder().setIcon(Items.MUSIC_DISC_STRAD).setTitle(Component.translatable("waterplayer.name"));
    }

    public static List<KeyBind> keyBinds = new ArrayList<>();

    public static void registerBinds() {
        KeyMapping key1 = KeyMappingHelper.register(new KeyMapping(
                "waterplayer.key.control",
                GLFW.GLFW_KEY_ENTER, // The keycode of the key
                "waterplayer.name"
        ));
        KeyMapping key2 = KeyMappingHelper.register(new KeyMapping(
                "waterplayer.key.pause",
                GLFW.GLFW_KEY_P, // The keycode of the key
                "waterplayer.name"
        ));
        KeyMapping key3 = KeyMappingHelper.register(new KeyMapping(
                "waterplayer.key.skip",
                GLFW.GLFW_KEY_X, // The keycode of the key
                "waterplayer.name"
        ));
        KeyMapping key4 = KeyMappingHelper.register(new KeyMapping(
                "waterplayer.key.reset",
                GLFW.GLFW_KEY_DELETE, // The keycode of the key
                "waterplayer.name"
        ));
        KeyMapping key5 = KeyMappingHelper.register(new KeyMapping(
                "waterplayer.key.shuffle",
                GLFW.GLFW_KEY_PAGE_DOWN, // The keycode of the key
                "waterplayer.name"
        ));
        KeyMapping key6 = KeyMappingHelper.register(new KeyMapping(
                "waterplayer.key.repeating",
                GLFW.GLFW_KEY_PAGE_UP, // The keycode of the key
                "waterplayer.name"
        ));
        KeyMapping key7 = KeyMappingHelper.register(new KeyMapping(
                "waterplayer.key.volume.up",
                GLFW.GLFW_KEY_UP, // The keycode of the key
                "waterplayer.name"
        ));
        KeyMapping key8 = KeyMappingHelper.register(new KeyMapping(
                "waterplayer.key.volume.down",
                GLFW.GLFW_KEY_DOWN, // The keycode of the key
                "waterplayer.name"
        ));
        keyBinds.add(new KeyBind(key1, () -> {
            if (!(AlinLib.MINECRAFT.screen instanceof ControlScreen)) {
                AlinLib.MINECRAFT.setScreen(getControlScreen(AlinLib.MINECRAFT.screen));
                return true;
            } else return false;
        }));
        keyBinds.add(new KeyBind(key2, () -> {
            if (AlinLib.MINECRAFT.screen instanceof ControlScreen) {
                if (AlinLib.MINECRAFT.screen.getFocused() instanceof EditBox) return false;
                ((ControlScreen) AlinLib.MINECRAFT.screen).play.onPress();
            } else {
                player.getAudioPlayer().setPaused(!player.getAudioPlayer().isPaused());
            }
            if (WaterPlayer.config.getBoolean("ENABLE_NOTICE", true))
                getToast().setMessage(Localization.getText(player.getAudioPlayer().isPaused() ? "waterplayer.message.pause" : "waterplayer.message.play"))
                        .show(AlinLib.MINECRAFT.getToasts());
            return true;
        }));
        keyBinds.add(new KeyBind(key3, () -> {
            if (player.getTrackScheduler().queue.isEmpty() && player.getAudioPlayer().getPlayingTrack() == null)
                return false;
            player.getTrackScheduler().nextTrack();
            if (WaterPlayer.config.getBoolean("ENABLE_NOTICE", true))
                getToast().setMessage(Localization.getText("waterplayer.message.skip"))
                        .show(AlinLib.MINECRAFT.getToasts());
            return true;
        }));
        keyBinds.add(new KeyBind(key4, () -> {
            player.getTrackScheduler().skiping = false;
            if (!player.getTrackScheduler().queue.isEmpty()) {
                player.getTrackScheduler().queue.clear();
                if (WaterPlayer.config.getBoolean("ENABLE_NOTICE", true))
                    getToast().setMessage(Localization.getText("waterplayer.message.reset"))
                            .show(AlinLib.MINECRAFT.getToasts());
            }
            return true;
        }));
        keyBinds.add(new KeyBind(key5, () -> {
            if (player.getTrackScheduler().queue.size() >= 2) {
                player.getTrackScheduler().shuffle();
                if (WaterPlayer.config.getBoolean("ENABLE_NOTICE", true))
                    getToast().setMessage(Localization.getText("waterplayer.message.shuffle"))
                            .show(AlinLib.MINECRAFT.getToasts());
            }
            return true;
        }));
        keyBinds.add(new KeyBind(key6, () -> {
            if (player.getAudioPlayer().getPlayingTrack() == null) return false;
            if (AlinLib.MINECRAFT.screen instanceof ControlScreen) {
                if (AlinLib.MINECRAFT.screen.getFocused() instanceof EditBox) return false;
                ((ControlScreen) AlinLib.MINECRAFT.screen).repeat.onPress();
            } else {
                player.getTrackScheduler().changeRepeatStatus();
            }
            if (WaterPlayer.config.getBoolean("ENABLE_NOTICE", true))
                getToast().setIcon(player.getTrackScheduler().getRepeatIcon())
                        .setMessage(Localization.getText(player.getTrackScheduler().getRepeatStatus() == 0 ? "waterplayer.message.repeat.no" : player.getTrackScheduler().getRepeatStatus() == 1 ? "waterplayer.message.repeat" : "waterplayer.message.repeat.one"))
                        .show(AlinLib.MINECRAFT.getToasts());
            return true;
        }));
        keyBinds.add(new KeyBind(key7, () -> {
            if (player.getAudioPlayer().getPlayingTrack() == null || AlinLib.MINECRAFT.screen instanceof ControlScreen)
                return false;
            int current = config.getNumber("CURRENT_MUSIC_VOLUME", 3).intValue() + config.getNumber("SELECT_MUSIC_VOLUME", 1).intValue();
            if (current >= 100) current = 100;
            config.setNumber("CURRENT_MUSIC_VOLUME", current);
            player.getAudioPlayer().setVolume(current);
            config.save();
            return true;
        }));
        keyBinds.add(new KeyBind(key8, () -> {
            if (player.getAudioPlayer().getPlayingTrack() == null || AlinLib.MINECRAFT.screen instanceof ControlScreen)
                return false;
            int current = config.getNumber("CURRENT_MUSIC_VOLUME", 3).intValue() - config.getNumber("SELECT_MUSIC_VOLUME", 1).intValue();
            if (current <= 0) current = 0;
            config.setNumber("CURRENT_MUSIC_VOLUME", current);
            player.getAudioPlayer().setVolume(current);
            config.save();
            return true;
        }));
    }

    //

    public static Screen getControlScreen(Screen parent){
        return WaterPlayer.config.getBoolean("CONTROL.MODERN", true) ? new ModernControlScreen(parent) : new ControlScreen(parent);
    }

    // Logger
    public static void log(String message) {
        log(message, Level.INFO);
    }

    public static void log(String message, Level level) {
        LOG.log(level, "[" + LOG.getName() + "] " + message);
    }

    public static void confirmLinkNow(Screen screen, String link) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ConfirmLinkScreen((bl) -> {
            if (bl) {
                Util.getPlatform().openUri(link);
            }

            minecraft.setScreen(screen);
        }, link, true));
    }
    public interface Icons {
        ResourceLocation REPEAT = GuiUtils.getResourceLocation("waterplayer", "textures/player/repeat.png");
        ResourceLocation REPEAT_ONE = GuiUtils.getResourceLocation("waterplayer", "textures/player/one_repeat.png");
        ResourceLocation REPEAT_NON = GuiUtils.getResourceLocation("waterplayer", "textures/player/non_repeat.png");
        ResourceLocation PLAY = GuiUtils.getResourceLocation("waterplayer", "textures/player/play.png");
        ResourceLocation PAUSE = GuiUtils.getResourceLocation("waterplayer", "textures/player/pause.png");
        ResourceLocation RESET_QUEUE = GuiUtils.getResourceLocation("waterplayer", "textures/player/reset_queue.png");
        ResourceLocation SKIP = GuiUtils.getResourceLocation("waterplayer", "textures/player/skip.png");
        ResourceLocation SHUFFLE = GuiUtils.getResourceLocation("waterplayer", "textures/player/shuffle.png");

        ResourceLocation VOLUME_MAX = GuiUtils.getResourceLocation("waterplayer", "textures/player/volume_max.png");
        ResourceLocation VOLUME_OK = GuiUtils.getResourceLocation("waterplayer", "textures/player/volume_ok.png");
        ResourceLocation VOLUME_LOW = GuiUtils.getResourceLocation("waterplayer", "textures/player/volume_low.png");
        ResourceLocation VOLUME_MUTE = GuiUtils.getResourceLocation("waterplayer", "textures/player/volume_mute.png");

        ResourceLocation FILE_ICON = GuiUtils.getResourceLocation("waterplayer", "textures/file_icon.png");
        ResourceLocation MUSIC = GuiUtils.getResourceLocation("waterplayer", "textures/music.png");
        ResourceLocation NO_ICON = GuiUtils.getResourceLocation("waterplayer", "textures/no_icon.png");
        ResourceLocation SEARCH = GuiUtils.getResourceLocation("waterplayer", "textures/search.png");
        ResourceLocation THINK = GuiUtils.getResourceLocation("waterplayer", "textures/think.png");

        static ResourceLocation getPlayOrPause(boolean isPaused){
            return isPaused ? PLAY : PAUSE;
        }
    }
}