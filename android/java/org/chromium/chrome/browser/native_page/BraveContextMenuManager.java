/* Copyright (c) 2026 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.native_page;

import androidx.annotation.DrawableRes;

import org.chromium.build.annotations.NullMarked;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.tasks.tab_management.BraveTabUiFeatureUtilities;
import org.chromium.chrome.browser.ui.native_page.TouchEnabledDelegate;
import org.chromium.components.browser_ui.widget.ListItemBuilder;
import org.chromium.ui.listmenu.ListItemType;
import org.chromium.ui.modelutil.MVCListAdapter;
import org.chromium.ui.modelutil.PropertyModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Brave's {@link ContextMenuManager}. Hides the "Open in new tab in group" item from native page
 * context menus (e.g. New Tab Page shortcut tiles, Recent tabs) when the Brave "Enable tab groups"
 * master switch is off, adds icons to native page context menu items, and adds the NTP top-sites
 * widget-control items ("Add site" / "Show frequently visited" / "Show shortcuts" / "Hide widget")
 * when the delegate is a {@link BraveNtpDelegate}. Instantiated in place of the upstream class via
 * a plaster redirect.
 */
@NullMarked
public class BraveContextMenuManager extends ContextMenuManager {
    public BraveContextMenuManager(
            NativePageNavigationDelegate navigationDelegate,
            TouchEnabledDelegate touchEnabledDelegate,
            Runnable closeContextMenuCallback,
            String userActionPrefix) {
        super(navigationDelegate, touchEnabledDelegate, closeContextMenuCallback, userActionPrefix);
    }

    @Override
    protected boolean shouldShowItem(@ContextMenuItemId int itemId, Delegate delegate) {
        if (itemId == ContextMenuItemId.OPEN_IN_NEW_TAB_IN_GROUP
                && !BraveTabUiFeatureUtilities.isTabGroupsEnabled()) {
            return false;
        }
        return super.shouldShowItem(itemId, delegate);
    }

    @Override
    protected @DrawableRes int getIconIdForMenuItem(@ContextMenuItemId int id) {
        switch (id) {
            case ContextMenuItemId.OPEN_IN_NEW_TAB:
            case ContextMenuItemId.OPEN_IN_NEW_WINDOW:
            case ContextMenuItemId.OPEN_IN_OTHER_WINDOW:
                return R.drawable.ic_open_in_new;
            case ContextMenuItemId.OPEN_IN_NEW_TAB_IN_GROUP:
                return R.drawable.ic_tab_group_24dp;
            case ContextMenuItemId.OPEN_IN_INCOGNITO_TAB:
            case ContextMenuItemId.OPEN_IN_INCOGNITO_WINDOW:
                return R.drawable.ic_incognito_24dp;
            case ContextMenuItemId.SAVE_FOR_OFFLINE:
                return R.drawable.ic_file_download_24dp;
            case ContextMenuItemId.REMOVE:
            case ContextMenuItemId.REMOVE_ALL:
                return R.drawable.ic_delete_24dp;
            case ContextMenuItemId.PIN_THIS_SHORTCUT:
            case ContextMenuItemId.UNPIN:
                return R.drawable.ic_offline_pin_fill_24dp;
            case ContextMenuItemId.EDIT_SHORTCUT:
                return R.drawable.ic_edit_24dp;
            default:
                return 0;
        }
    }

    /** "Add site" appears at the top, before the standard Chromium items. */
    @Override
    protected List<MVCListAdapter.ListItem> getMenuItemsBeforeStandardItems(Delegate delegate) {
        if (!(delegate instanceof BraveNtpDelegate braveDelegate)
                || !braveDelegate.isBraveItemSupported(BraveNtpDelegate.BRAVE_ADD_SITE)) {
            return Collections.emptyList();
        }
        return List.of(
                new ListItemBuilder()
                        .withTitleRes(R.string.brave_ntp_add_site)
                        .withMenuId(BraveNtpDelegate.BRAVE_ADD_SITE)
                        .withStartIconRes(R.drawable.ic_add_box_rounded_corner)
                        .build());
    }

    /** NTP widget control options appear below a divider at the bottom of the menu. */
    @Override
    protected List<MVCListAdapter.ListItem> getMenuItemsAfterStandardItems(Delegate delegate) {
        if (!(delegate instanceof BraveNtpDelegate braveDelegate)) {
            return Collections.emptyList();
        }

        int currentMode = braveDelegate.getBraveTopSitesDisplayMode();
        int[] widgetItemIds = {
            BraveNtpDelegate.BRAVE_SHOW_FREQUENT,
            BraveNtpDelegate.BRAVE_SHOW_SHORTCUTS,
            BraveNtpDelegate.BRAVE_HIDE_WIDGET
        };
        int[] widgetItemRes = {
            R.string.brave_ntp_show_frequently_visited,
            R.string.brave_ntp_show_shortcuts,
            R.string.brave_ntp_hide_widget
        };
        int[] widgetItemIcons = {
            R.drawable.ic_history_24dp,
            R.drawable.ic_star_24dp,
            R.drawable.ic_eye_crossed
        };

        List<MVCListAdapter.ListItem> items = new ArrayList<>();
        for (int i = 0; i < widgetItemIds.length; i++) {
            if (!braveDelegate.isBraveItemSupported(widgetItemIds[i])) continue;
            boolean isSelected =
                    (widgetItemIds[i] == BraveNtpDelegate.BRAVE_SHOW_FREQUENT
                                    && currentMode == 1)
                            || (widgetItemIds[i] == BraveNtpDelegate.BRAVE_SHOW_SHORTCUTS
                                    && currentMode == 0);
            ListItemBuilder builder =
                    new ListItemBuilder()
                            .withTitleRes(widgetItemRes[i])
                            .withMenuId(widgetItemIds[i])
                            .withStartIconRes(widgetItemIcons[i]);
            if (isSelected) {
                int endIconRes = braveDelegate.getSelectedModeEndIconRes();
                if (endIconRes != 0) builder.withEndIconRes(endIconRes);
            }
            items.add(builder.build());
        }

        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        List<MVCListAdapter.ListItem> result = new ArrayList<>();
        result.add(new MVCListAdapter.ListItem(ListItemType.DIVIDER, new PropertyModel()));
        result.addAll(items);
        return result;
    }

    @Override
    protected boolean handleMenuItemClick(@ContextMenuItemId int itemId, Delegate delegate) {
        if (delegate instanceof BraveNtpDelegate braveDelegate) {
            if (itemId == BraveNtpDelegate.BRAVE_ADD_SITE) {
                braveDelegate.braveAddSite();
                return true;
            } else if (itemId == BraveNtpDelegate.BRAVE_SHOW_FREQUENT) {
                braveDelegate.braveShowFrequent();
                return true;
            } else if (itemId == BraveNtpDelegate.BRAVE_SHOW_SHORTCUTS) {
                braveDelegate.braveShowShortcuts();
                return true;
            } else if (itemId == BraveNtpDelegate.BRAVE_HIDE_WIDGET) {
                braveDelegate.braveHideWidget();
                return true;
            }
        }
        return super.handleMenuItemClick(itemId, delegate);
    }
}
