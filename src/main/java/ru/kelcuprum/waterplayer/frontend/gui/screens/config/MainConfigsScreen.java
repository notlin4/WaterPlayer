package ru.kelcuprum.waterplayer.frontend.gui.screens.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.kelcuprum.alinlib.config.Localization;
import ru.kelcuprum.alinlib.gui.InterfaceUtils;
import ru.kelcuprum.alinlib.gui.components.buttons.ButtonConfigBoolean;
import ru.kelcuprum.alinlib.gui.components.buttons.base.Button;
import ru.kelcuprum.alinlib.gui.components.selector.SelectorIntegerButton;
import ru.kelcuprum.alinlib.gui.components.text.TextBox;
import ru.kelcuprum.alinlib.gui.screens.ConfigScreenBuilder;
import ru.kelcuprum.waterplayer.WaterPlayer;
import ru.kelcuprum.waterplayer.frontend.gui.screens.LoadMusicScreen;

public class MainConfigsScreen{
    private static final Component MainConfigCategory = Localization.getText("waterplayer.config");
    private static final Component LocalizationConfigCategory = Localization.getText("waterplayer.config.localization");
    private static final Component SecretConfigCategory = Localization.getText("waterplayer.secret");
    private static final Component PlaylistsCategory = Localization.getText("waterplayer.playlists");
    private static final Component PlayCategory = Localization.getText("waterplayer.play");
    // CATEGORY CONTENT
    private static final Component enableBossBarText = Localization.getText("waterplayer.config.enable_bossbar");
    private static final Component enableOverlayText = Localization.getText("waterplayer.config.enable_overlay");
    private static final Component overlayPositionText = Localization.getText("waterplayer.config.overlay.position");
    private static final Component enableNoticeText = Localization.getText("waterplayer.config.enable_notice");
    private static final Component enableChangeTitleText = Localization.getText("waterplayer.config.enable_change_title");

    private final InterfaceUtils.DesignType designType = InterfaceUtils.DesignType.FLAT;
    public Screen build(Screen parent) {
        String[] type = {
                Component.translatable("waterplayer.config.overlay.position.top_left").getString(),
                Component.translatable("waterplayer.config.overlay.position.top_right").getString(),
                Component.translatable("waterplayer.config.overlay.position.bottom_left").getString(),
                Component.translatable("waterplayer.config.overlay.position.bottom_right").getString()
        };
        return new ConfigScreenBuilder(parent, Component.translatable("waterplayer.name"), designType)
                .addPanelWidget(new Button(10, 40, designType, MainConfigCategory, (e) -> {
                    Minecraft.getInstance().setScreen(new MainConfigsScreen().build(parent));
                }))
                .addPanelWidget(new Button(10, 65, designType, LocalizationConfigCategory, (e) -> {
                    Minecraft.getInstance().setScreen(new LocalizationConfigsScreen().build(parent));
                }))
                .addPanelWidget(new Button(10, 90, designType, SecretConfigCategory, (e) -> {
                    Minecraft.getInstance().setScreen(new SecretConfigsScreen().build(parent));
                }))
                .addPanelWidget(new Button(10, 115, designType, PlaylistsCategory, (e) -> {
                    Minecraft.getInstance().setScreen(new PlaylistsScreen().build(parent));
                }))
                .addPanelWidget(new Button(10, 140, designType, PlayCategory, (e) -> {
                    Minecraft.getInstance().setScreen(new LoadMusicScreen(this.build(parent)));
                }))
                ///
                .addWidget(new TextBox(140, 5, MainConfigCategory, true))
                .addWidget(new ButtonConfigBoolean(140, 30, designType, WaterPlayer.config, "ENABLE_BOSS_BAR", false, enableBossBarText))
                .addWidget(new ButtonConfigBoolean(140, 55, designType, WaterPlayer.config, "ENABLE_OVERLAY", false, enableOverlayText))
                .addWidget(new SelectorIntegerButton(140, 80, designType, type, WaterPlayer.config, "OVERLAY.POSITION", 0, overlayPositionText))
                .addWidget(new ButtonConfigBoolean(140, 105, designType, WaterPlayer.config, "ENABLE_NOTICE", false, enableNoticeText))
                .addWidget(new ButtonConfigBoolean(140, 130, designType, WaterPlayer.config, "ENABLE_CHANGE_TITLE", false, enableChangeTitleText))
                .addWidget(new ButtonConfigBoolean(140, 155, designType, WaterPlayer.config, "ENABLE_DISCORD_RPC", false, Component.translatable("waterplayer.config.enable_discord_rpc")))
                .build();
    }
}
