/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.cordova.statusbar;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONException;

public class StatusBar extends CordovaPlugin {
    private static final String TAG = "StatusBar";

    private static final String ACTION_HIDE = "hide";
    private static final String ACTION_SHOW = "show";
    private static final String ACTION_READY = "_ready";
    private static final String ACTION_BACKGROUND_COLOR_BY_HEX_STRING = "backgroundColorByHexString";
    private static final String ACTION_NAVIGATION_BACKGROUND_COLOR_BY_HEX_STRING = "navigationBackgroundColorByHexString";
    private static final String ACTION_OVERLAYS_WEB_VIEW = "overlaysWebView";
    private static final String ACTION_STYLE_DEFAULT = "styleDefault";
    private static final String ACTION_STYLE_LIGHT_CONTENT = "styleLightContent";

    private static final String STYLE_DEFAULT = "default";
    private static final String STYLE_LIGHT_CONTENT = "lightcontent";

    private AppCompatActivity activity;
    private Window window;

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    @Override
    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
        LOG.v(TAG, "StatusBar: initialization");
        super.initialize(cordova, webView);

        activity = this.cordova.getActivity();
        window = activity.getWindow();

        activity.runOnUiThread(() -> {
            // Clear flag FLAG_FORCE_NOT_FULLSCREEN which is set initially
            // by the Cordova.
            window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

            // Read 'StatusBarOverlaysWebView' from config.xml, default is true.
            setStatusBarTransparent(preferences.getBoolean("StatusBarOverlaysWebView", true));

            // Read 'StatusBarBackgroundColor' from config.xml, default is #000000.
            setStatusBarBackgroundColor(preferences.getString("StatusBarBackgroundColor", "#000000"));

            // Read 'NavigationBarBackgroundColor' from config.xml, default is #000000.
            setNavigationBarBackgroundColor(preferences.getString("NavigationBarBackgroundColor", "#000000"));

            // Read 'StatusBarStyle' from config.xml, default is 'lightcontent'.
            // setStatusBarStyle(
            //     preferences.getString("StatusBarStyle", STYLE_LIGHT_CONTENT).toLowerCase()
            // );
        });
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false otherwise.
     */
    @Override
    public boolean execute(final String action, final CordovaArgs args, final CallbackContext callbackContext) {
        LOG.v(TAG, "Executing action: " + action);

        switch (action) {
            case ACTION_READY:
                boolean statusBarVisible = (window.getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == 0;
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, statusBarVisible));
                return true;

            case ACTION_SHOW:
                activity.runOnUiThread(() -> {
                    int uiOptions = window.getDecorView().getSystemUiVisibility();
                    uiOptions &= ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                    uiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;

                    window.getDecorView().setSystemUiVisibility(uiOptions);

                    // CB-11197 We still need to update LayoutParams to force status bar
                    // to be hidden when entering e.g. text fields
                    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                });
                return true;

            case ACTION_HIDE:
                activity.runOnUiThread(() -> {
                    int uiOptions = window.getDecorView().getSystemUiVisibility()
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN;

                    window.getDecorView().setSystemUiVisibility(uiOptions);

                    // CB-11197 We still need to update LayoutParams to force status bar
                    // to be hidden when entering e.g. text fields
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                });
                return true;

            case ACTION_BACKGROUND_COLOR_BY_HEX_STRING:
                activity.runOnUiThread(() -> {
                    try {
                        setStatusBarBackgroundColor(args.getString(0));
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid hexString argument, use f.i. '#777777'");
                    }
                });
                return true;

            case ACTION_NAVIGATION_BACKGROUND_COLOR_BY_HEX_STRING:
                activity.runOnUiThread(() -> {
                    try {
                        setNavigationBarBackgroundColor(args.getString(0));
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid hexString argument, use f.i. '#777777'");
                    }
                });
                return true;

            case ACTION_OVERLAYS_WEB_VIEW:
                activity.runOnUiThread(() -> {
                    try {
                        setStatusBarTransparent(args.getBoolean(0));
                    } catch (JSONException ignore) {
                        LOG.e(TAG, "Invalid boolean argument");
                    }
                });
                return true;

            case ACTION_STYLE_DEFAULT:
                activity.runOnUiThread(() -> setStatusBarStyle(STYLE_DEFAULT));
                return true;

            case ACTION_STYLE_LIGHT_CONTENT:
                activity.runOnUiThread(() -> setStatusBarStyle(STYLE_LIGHT_CONTENT));
                return true;

            default:
                return false;
        }
    }

    private void setStatusBarBackgroundColor(final String colorPref) {
        if (colorPref.isEmpty()) return;

        int color;
        try {
            color = Color.parseColor(colorPref);
        } catch (IllegalArgumentException ignore) {
            LOG.e(TAG, "Invalid hexString argument, use f.i. '#999999'");
            return;
        }

        // Decide foreground depending on background.
        final boolean useLightForeground = isLightModeNeeded(color);

        LOG.d(TAG, "Setting status bar color to " + colorPref + " foreground " + (useLightForeground ? "light" : "dark"));

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS); // SDK 19-30
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS); // SDK 21
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // A funcao e depreciada a partir do Android 15 e nao tem mais efeito (GESTOR-21).
            window.setStatusBarColor(color);
        } else {
            // Android 15+: em modo edge-to-edge (ver hooks/after_prepare do gestor-android), o
            // cordova-android core (SystemBarPlugin) cria uma View marcada "statusBarView" atras
            // da status bar em vez de pintar via Window - pintamos essa mesma View (GESTOR-20).
            View edgeToEdgeStatusBar = findEdgeToEdgeStatusBarView();
            if (edgeToEdgeStatusBar != null) {
                edgeToEdgeStatusBar.setBackgroundColor(color);
            }
        }

        if (useLightForeground) {
            setStatusBarStyle(STYLE_LIGHT_CONTENT);
        } else {
            setStatusBarStyle(STYLE_DEFAULT);
        }

    }

    private void setStatusBarTransparent(final boolean isTransparent) {
        final Window window = cordova.getActivity().getWindow();
        int visibility = isTransparent
            ? View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            : View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_VISIBLE;

        window.getDecorView().setSystemUiVisibility(visibility);

        if (isTransparent) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                // A funcao e depreciada a partir do Android 15 e nao tem mais efeito (GESTOR-21).
                window.setStatusBarColor(Color.TRANSPARENT);
            } else {
                View edgeToEdgeStatusBar = findEdgeToEdgeStatusBarView();
                if (edgeToEdgeStatusBar != null) {
                    edgeToEdgeStatusBar.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        }
    }

    private void setStatusBarStyle(final String style) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || style.isEmpty()) return;

        View decorView = window.getDecorView();
        WindowInsetsControllerCompat windowInsetsControllerCompat = WindowCompat.getInsetsController(window, decorView);

        if (windowInsetsControllerCompat == null) {
            return;
        }

        if (style.equals(STYLE_DEFAULT)) {
            windowInsetsControllerCompat.setAppearanceLightStatusBars(true);
        } else if (style.equals(STYLE_LIGHT_CONTENT)) {
            windowInsetsControllerCompat.setAppearanceLightStatusBars(false);
        } else {
            LOG.e(TAG, "Invalid style, must be either 'default' or 'lightcontent'");
        }
    }

    private boolean isLightModeNeeded(int color) {
        return Color.luminance(color) < 0.5;
    }

    /**
     * Tenta localizar a "statusBarView" criada pelo cordova-android core (CordovaActivity /
     * SystemBarPlugin) quando a Activity roda em modo edge-to-edge (Android 11+ / API 30+ - ver
     * hooks/after_prepare/020_fix_keyboard_resize_old_android.js no gestor-android, que so deixa
     * o modo edge-to-edge do core rodar nessas versoes). Retorna null em versoes mais antigas,
     * onde o app ainda usa o layout classico (sem edge-to-edge) e a View nunca e criada.
     */
    private View findEdgeToEdgeStatusBarView() {
        ViewParent parent = webView.getView().getParent();
        if (!(parent instanceof FrameLayout)) {
            return null;
        }

        FrameLayout root = (FrameLayout) parent;
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if ("statusBarView".equals(child.getTag())) {
                return child;
            }
        }

        return null;
    }

    private void setNavigationBarBackgroundColor(final String colorPref) {
        if (colorPref.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return; // SDK 26

        int color;
        try {
            color = Color.parseColor(colorPref);
        } catch (IllegalArgumentException ignore) {
            LOG.e(TAG, "Invalid hexString argument, use f.i. '#999999'");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // A funcao e depreciada a partir do Android 15 e nao tem mais efeito (GESTOR-21).
            // Diferente da status bar, o cordova-android core tambem nao tem uma View de scrim
            // para a nav bar (o proprio SystemBarPlugin dele ainda chama Window.setNavigationBarColor
            // sem alternativa) - entao, sem uma cor real pintada, nao forcamos a aparencia dos
            // icones aqui tambem, para nao repetir o mesmo descompasso do GESTOR-20 (icones
            // claros/escuros pensados para uma cor que nunca chega a ser aplicada).
            return;
        }

        window.setNavigationBarColor(color);

        final boolean useLightForeground = isLightModeNeeded(color);

        LOG.d(TAG, "Setting navigation bar color to " + colorPref + " foreground " + (useLightForeground ? "light" : "dark"));

        final View decorView = window.getDecorView();
        WindowInsetsControllerCompat windowInsetsControllerCompat = WindowCompat.getInsetsController(window, decorView);

        if (windowInsetsControllerCompat == null) {
            return;
        }

        windowInsetsControllerCompat.setAppearanceLightNavigationBars(!useLightForeground);
    }
}
