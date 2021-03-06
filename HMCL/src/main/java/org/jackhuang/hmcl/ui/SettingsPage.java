/*
 * Hello Minecraft! Launcher.
 * Copyright (C) 2018  huangyuhui <huanghongxun2008@126.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see {http://www.gnu.org/licenses/}.
 */
package org.jackhuang.hmcl.ui;

import com.jfoenix.controls.*;
import com.jfoenix.effects.JFXDepthManager;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.When;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.jackhuang.hmcl.setting.*;
import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.ui.construct.FontComboBox;
import org.jackhuang.hmcl.ui.construct.MultiFileItem;
import org.jackhuang.hmcl.ui.construct.Validator;
import org.jackhuang.hmcl.ui.wizard.DecoratorPage;
import org.jackhuang.hmcl.upgrade.RemoteVersion;
import org.jackhuang.hmcl.upgrade.UpdateChecker;
import org.jackhuang.hmcl.upgrade.UpdateHandler;
import org.jackhuang.hmcl.util.Lang;
import org.jackhuang.hmcl.util.i18n.Locales;

import static org.jackhuang.hmcl.setting.ConfigHolder.config;
import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

import java.net.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public final class SettingsPage extends StackPane implements DecoratorPage {
    private final StringProperty title = new SimpleStringProperty(this, "title", i18n("settings.launcher"));

    @FXML
    private JFXTextField txtProxyHost;
    @FXML
    private JFXTextField txtProxyPort;
    @FXML
    private JFXTextField txtProxyUsername;
    @FXML
    private JFXPasswordField txtProxyPassword;
    @FXML
    private JFXTextField txtFontSize;
    @FXML
    private JFXComboBox<Label> cboLanguage;
    @FXML
    private JFXComboBox<?> cboDownloadSource;
    @FXML
    private FontComboBox cboFont;
    @FXML
    private MultiFileItem<EnumCommonDirectory> fileCommonLocation;
    @FXML
    private Label lblDisplay;
    @FXML
    private Label lblUpdate;
    @FXML
    private Label lblUpdateSub;
    @FXML
    private Text lblUpdateNote;
    @FXML
    private JFXRadioButton chkUpdateStable;
    @FXML
    private JFXRadioButton chkUpdateDev;
    @FXML
    private JFXButton btnUpdate;
    @FXML
    private ScrollPane scroll;
    @FXML
    private MultiFileItem<EnumBackgroundImage> backgroundItem;
    @FXML
    private StackPane themeColorPickerContainer;
    @FXML
    private JFXCheckBox chkEnableProxy;
    @FXML
    private JFXRadioButton chkProxyHttp;
    @FXML
    private JFXRadioButton chkProxySocks;
    @FXML
    private JFXCheckBox chkProxyAuthentication;
    @FXML
    private GridPane authPane;
    @FXML
    private Pane proxyPane;

    private ObjectProperty<Proxy.Type> selectedProxyType;

    private InvalidationListener updateListener;

    public SettingsPage() {
        FXUtils.loadFXML(this, "/assets/fxml/setting.fxml");

        FXUtils.smoothScrolling(scroll);

        cboDownloadSource.getSelectionModel().select(DownloadProviders.DOWNLOAD_PROVIDERS.indexOf(Settings.instance().getDownloadProvider()));
        cboDownloadSource.getSelectionModel().selectedIndexProperty().addListener((a, b, newValue) -> Settings.instance().setDownloadProvider(DownloadProviders.getDownloadProvider(newValue.intValue())));

        cboFont.getSelectionModel().select(Settings.instance().getFont().getFamily());
        cboFont.valueProperty().addListener((a, b, newValue) -> {
            Font font = Font.font(newValue, Settings.instance().getFont().getSize());
            Settings.instance().setFont(font);
            lblDisplay.setStyle("-fx-font: " + font.getSize() + " \"" + font.getFamily() + "\";");
        });

        txtFontSize.setText(Double.toString(Settings.instance().getFont().getSize()));
        txtFontSize.getValidators().add(new Validator(it -> Lang.toDoubleOrNull(it) != null));
        txtFontSize.textProperty().addListener((a, b, newValue) -> {
            if (txtFontSize.validate()) {
                Font font = Font.font(Settings.instance().getFont().getFamily(), Double.parseDouble(newValue));
                Settings.instance().setFont(font);
                lblDisplay.setStyle("-fx-font: " + font.getSize() + " \"" + font.getFamily() + "\";");
            }
        });

        lblDisplay.setStyle("-fx-font: " + Settings.instance().getFont().getSize() + " \"" + Settings.instance().getFont().getFamily() + "\";");

        ObservableList<Label> list = FXCollections.observableArrayList();
        for (Locales.SupportedLocale locale : Locales.LOCALES)
            list.add(new Label(locale.getName(config().getLocalization().getResourceBundle())));

        cboLanguage.setItems(list);
        cboLanguage.getSelectionModel().select(Locales.LOCALES.indexOf(config().getLocalization()));
        cboLanguage.getSelectionModel().selectedIndexProperty().addListener((a, b, newValue) -> config().setLocalization(Locales.getLocale(newValue.intValue())));

        // ==== Proxy ====
        txtProxyHost.textProperty().bindBidirectional(config().proxyHostProperty());
        txtProxyPort.textProperty().bindBidirectional(config().proxyPortProperty());
        txtProxyUsername.textProperty().bindBidirectional(config().proxyUserProperty());
        txtProxyPassword.textProperty().bindBidirectional(config().proxyPassProperty());

        proxyPane.disableProperty().bind(chkEnableProxy.selectedProperty().not());
        authPane.disableProperty().bind(chkProxyAuthentication.selectedProperty().not());

        chkEnableProxy.selectedProperty().bindBidirectional(config().hasProxyProperty());
        chkProxyAuthentication.selectedProperty().bindBidirectional(config().hasProxyAuthProperty());

        selectedProxyType = new SimpleObjectProperty<Proxy.Type>(Proxy.Type.HTTP) {
            {
                invalidated();
            }

            @Override
            protected void invalidated() {
                Proxy.Type type = Objects.requireNonNull(get());
                if (type == Proxy.Type.DIRECT) {
                    set(Proxy.Type.HTTP); // HTTP by default
                } else {
                    chkProxyHttp.setSelected(type == Proxy.Type.HTTP);
                    chkProxySocks.setSelected(type == Proxy.Type.SOCKS);
                }
            }
        };
        selectedProxyType.bindBidirectional(config().proxyTypeProperty());

        ToggleGroup proxyConfigurationGroup = new ToggleGroup();
        chkProxyHttp.setUserData(Proxy.Type.HTTP);
        chkProxyHttp.setToggleGroup(proxyConfigurationGroup);
        chkProxySocks.setUserData(Proxy.Type.SOCKS);
        chkProxySocks.setToggleGroup(proxyConfigurationGroup);
        proxyConfigurationGroup.getToggles().forEach(
                toggle -> toggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) {
                        selectedProxyType.set((Proxy.Type) toggle.getUserData());
                    }
                }));
        // ====

        fileCommonLocation.loadChildren(Arrays.asList(
                fileCommonLocation.createChildren(i18n("launcher.common_directory.disabled"), EnumCommonDirectory.DISABLED),
                fileCommonLocation.createChildren(i18n("launcher.common_directory.default"), EnumCommonDirectory.DEFAULT)
        ), EnumCommonDirectory.CUSTOM);
        fileCommonLocation.selectedDataProperty().bindBidirectional(config().commonDirTypeProperty());
        fileCommonLocation.customTextProperty().bindBidirectional(config().commonDirectoryProperty());
        fileCommonLocation.subtitleProperty().bind(
                Bindings.createObjectBinding(() -> Optional.ofNullable(Settings.instance().getCommonDirectory())
                                .orElse(i18n("launcher.common_directory.disabled")),
                        config().commonDirectoryProperty(), config().commonDirTypeProperty()));

        // ==== Update ====
        FXUtils.installTooltip(btnUpdate, i18n("update.tooltip"));
        updateListener = any -> {
            btnUpdate.setVisible(UpdateChecker.isOutdated());

            if (UpdateChecker.isOutdated()) {
                lblUpdateSub.setText(i18n("update.newest_version", UpdateChecker.getLatestVersion().getVersion()));
                lblUpdateSub.getStyleClass().setAll("update-label");

                lblUpdate.setText(i18n("update.found"));
                lblUpdate.getStyleClass().setAll("update-label");
            } else if (UpdateChecker.isCheckingUpdate()) {
                lblUpdateSub.setText(i18n("update.checking"));
                lblUpdateSub.getStyleClass().setAll("subtitle-label");

                lblUpdate.setText(i18n("update"));
                lblUpdate.getStyleClass().setAll();
            } else {
                lblUpdateSub.setText(i18n("update.latest"));
                lblUpdateSub.getStyleClass().setAll("subtitle-label");

                lblUpdate.setText(i18n("update"));
                lblUpdate.getStyleClass().setAll();
            }
        };
        UpdateChecker.latestVersionProperty().addListener(new WeakInvalidationListener(updateListener));
        UpdateChecker.outdatedProperty().addListener(new WeakInvalidationListener(updateListener));
        UpdateChecker.checkingUpdateProperty().addListener(new WeakInvalidationListener(updateListener));
        updateListener.invalidated(null);

        lblUpdateNote.setWrappingWidth(470);

        ObjectProperty<EnumUpdateChannel> updateChannel = new SimpleObjectProperty<EnumUpdateChannel>() {
            @Override
            protected void invalidated() {
                EnumUpdateChannel updateChannel = Objects.requireNonNull(get());
                chkUpdateDev.setSelected(updateChannel == EnumUpdateChannel.DEVELOPMENT);
                chkUpdateStable.setSelected(updateChannel == EnumUpdateChannel.STABLE);
            }
        };

        ToggleGroup updateChannelGroup = new ToggleGroup();
        chkUpdateDev.setToggleGroup(updateChannelGroup);
        chkUpdateDev.setUserData(EnumUpdateChannel.DEVELOPMENT);
        chkUpdateStable.setToggleGroup(updateChannelGroup);
        chkUpdateStable.setUserData(EnumUpdateChannel.STABLE);
        updateChannelGroup.getToggles().forEach(
                toggle -> toggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) {
                        updateChannel.set((EnumUpdateChannel) toggle.getUserData());
                    }
                }));
        updateChannel.bindBidirectional(ConfigHolder.config().updateChannelProperty());
        // ====

        // ==== Background ====
        backgroundItem.loadChildren(Collections.singletonList(
                backgroundItem.createChildren(i18n("launcher.background.default"), EnumBackgroundImage.DEFAULT)
        ), EnumBackgroundImage.CUSTOM);
        backgroundItem.customTextProperty().bindBidirectional(config().backgroundImageProperty());
        backgroundItem.selectedDataProperty().bindBidirectional(config().backgroundImageTypeProperty());
        backgroundItem.subtitleProperty().bind(
                new When(backgroundItem.selectedDataProperty().isEqualTo(EnumBackgroundImage.DEFAULT))
                        .then(i18n("launcher.background.default"))
                        .otherwise(config().backgroundImageProperty()));
        // ====

        // ==== Theme ====
        JFXColorPicker picker = new JFXColorPicker(Color.web(config().getTheme().getColor()), null);
        picker.setCustomColorText(i18n("color.custom"));
        picker.setRecentColorsText(i18n("color.recent"));
        picker.getCustomColors().setAll(Theme.SUGGESTED_COLORS);
        picker.setOnAction(e -> {
            Theme theme = Theme.custom(Theme.getColorDisplayName(picker.getValue()));
            config().setTheme(theme);
            Controllers.getScene().getStylesheets().setAll(theme.getStylesheets());
        });
        themeColorPickerContainer.getChildren().setAll(picker);
        Platform.runLater(() -> JFXDepthManager.setDepth(picker, 0));
        // ====
    }

    public String getTitle() {
        return title.get();
    }

    @Override
    public StringProperty titleProperty() {
        return title;
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    @FXML
    private void onUpdate() {
        RemoteVersion target = UpdateChecker.getLatestVersion();
        if (target == null) {
            return;
        }
        UpdateHandler.updateFrom(target);
    }
}
