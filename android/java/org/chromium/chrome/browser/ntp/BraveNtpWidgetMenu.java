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

/**
 * Shows a small popup menu on long-press of the NTP "Add site" (+) button containing the
 * widget-control options (show frequently visited / show shortcuts / hide widget).
 *
 * <p>"Add site" itself is omitted — tapping the button already triggers that action.
 */
@NullMarked
public class BraveNtpWidgetMenu {

    private static final int ID_SHOW_FREQUENT = 1;
    private static final int ID_SHOW_SHORTCUTS = 2;
    private static final int ID_HIDE_WIDGET = 3;

    /** Shows the widget-control popup anchored to {@code anchorView}. */
    public static void show(Context context, View anchorView) {
        PopupMenu popup = new PopupMenu(context, anchorView);
        Menu menu = popup.getMenu();

        menu.add(Menu.NONE, ID_SHOW_FREQUENT, 0, R.string.brave_ntp_show_frequently_visited)
                .setCheckable(true)
                .setChecked(NtpUtil.getTopSitesDisplayMode() == NtpUtil.TOP_SITES_MODE_FREQUENT);

        menu.add(Menu.NONE, ID_SHOW_SHORTCUTS, 1, R.string.brave_ntp_show_shortcuts)
                .setCheckable(true)
                .setChecked(NtpUtil.getTopSitesDisplayMode() == NtpUtil.TOP_SITES_MODE_SHORTCUTS);

        menu.add(Menu.NONE, ID_HIDE_WIDGET, 2, R.string.brave_ntp_hide_widget);

        popup.setOnMenuItemClickListener(
                item -> {
                    int id = item.getItemId();
                    if (id == ID_SHOW_FREQUENT) {
                        NtpUtil.setTopSitesDisplayMode(NtpUtil.TOP_SITES_MODE_FREQUENT);
                    } else if (id == ID_SHOW_SHORTCUTS) {
                        NtpUtil.setTopSitesDisplayMode(NtpUtil.TOP_SITES_MODE_SHORTCUTS);
                    } else if (id == ID_HIDE_WIDGET) {
                        NtpUtil.setDisplayTopSites(false);
                    }
                    return true;
                });

        popup.show();
    }
}
