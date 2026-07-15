/* Copyright (c) 2026 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.ntp;

import android.content.Context;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

import org.chromium.build.annotations.NullMarked;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.suggestions.tile.TileGroup;

/** Shows the long-press popup menu on the NTP top-sites "Add site" button. */
@NullMarked
public class BraveNtpTopSiteMenu {
    private static final int MENU_ID_ADD_SITE = 1;
    private static final int MENU_ID_SHOW_FREQUENT = 2;
    private static final int MENU_ID_SHOW_SHORTCUTS = 3;
    private static final int MENU_ID_HIDE_WIDGET = 4;

    /**
     * Attaches a long-click listener to {@code tileView} that shows the top-sites options menu.
     * Called from TileRenderer when building the "Add site" button tile.
     */
    public static void attachLongClickHandler(
            Context context,
            View tileView,
            TileGroup.CustomTileModificationDelegate delegate) {
        tileView.setOnLongClickListener(
                v -> {
                    showMenu(context, v, delegate);
                    return true;
                });
    }

    private static void showMenu(
            Context context, View anchorView, TileGroup.CustomTileModificationDelegate delegate) {
        PopupMenu popup = new PopupMenu(context, anchorView);
        Menu menu = popup.getMenu();

        menu.add(Menu.NONE, MENU_ID_ADD_SITE, 0, R.string.brave_ntp_add_site);
        menu.add(Menu.NONE, MENU_ID_SHOW_FREQUENT, 1, R.string.brave_ntp_show_frequently_visited);
        menu.add(Menu.NONE, MENU_ID_SHOW_SHORTCUTS, 2, R.string.brave_ntp_show_shortcuts);
        menu.add(Menu.NONE, MENU_ID_HIDE_WIDGET, 3, R.string.brave_ntp_hide_widget);

        int currentMode = NtpUtil.getTopSitesDisplayMode();
        menu.findItem(MENU_ID_SHOW_FREQUENT)
                .setCheckable(true)
                .setChecked(currentMode == NtpUtil.TOP_SITES_MODE_FREQUENT);
        menu.findItem(MENU_ID_SHOW_SHORTCUTS)
                .setCheckable(true)
                .setChecked(currentMode == NtpUtil.TOP_SITES_MODE_SHORTCUTS);

        popup.setOnMenuItemClickListener(
                item -> {
                    int id = item.getItemId();
                    if (id == MENU_ID_ADD_SITE) {
                        delegate.add();
                    } else if (id == MENU_ID_SHOW_FREQUENT) {
                        NtpUtil.setTopSitesDisplayMode(NtpUtil.TOP_SITES_MODE_FREQUENT);
                    } else if (id == MENU_ID_SHOW_SHORTCUTS) {
                        NtpUtil.setTopSitesDisplayMode(NtpUtil.TOP_SITES_MODE_SHORTCUTS);
                    } else if (id == MENU_ID_HIDE_WIDGET) {
                        NtpUtil.setDisplayTopSites(false);
                    }
                    return true;
                });

        popup.show();
    }
}
