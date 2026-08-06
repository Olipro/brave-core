/* Copyright (c) 2026 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.native_page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.chromium.base.BravePreferenceKeys;
import org.chromium.base.test.BaseRobolectricTestRunner;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.native_page.ContextMenuManager.ContextMenuItemId;
import org.chromium.chrome.browser.ntp.NtpUtil;
import org.chromium.chrome.browser.preferences.ChromeSharedPreferences;
import org.chromium.chrome.browser.ui.native_page.TouchEnabledDelegate;
import org.chromium.ui.listmenu.ListItemType;
import org.chromium.ui.listmenu.ListMenuItemProperties;
import org.chromium.ui.modelutil.MVCListAdapter;

import java.util.List;

/**
 * Unit tests for {@link BraveContextMenuManager}. Verifies: (1) the "Open in new tab in group"
 * native page context menu item is only shown when the "Enable tab groups" master switch is on
 * (mirrors upstream {@link ContextMenuManager}'s test); (2) icons are attached to standard menu
 * items; (3) the NTP top-sites widget-control items are added before/after the standard items when
 * the delegate is a {@link BraveNtpDelegate}; (4) clicks on Brave items are dispatched correctly.
 */
@RunWith(BaseRobolectricTestRunner.class)
public class BraveContextMenuManagerTest {
    @Rule public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private NativePageNavigationDelegate mNavigationDelegate;
    @Mock private TouchEnabledDelegate mTouchEnabledDelegate;
    @Mock private ContextMenuManager.Delegate mDelegate;
    @Mock private BraveNtpDelegate mBraveDelegate;

    private BraveContextMenuManager mManager;

    @Before
    public void setUp() {
        mManager =
                new BraveContextMenuManager(
                        mNavigationDelegate, mTouchEnabledDelegate, () -> {}, "");
    }

    @After
    public void tearDown() {
        ChromeSharedPreferences.getInstance()
                .removeKey(BravePreferenceKeys.BRAVE_TAB_GROUPS_FEATURE_ENABLED);
    }

    @Test
    public void testOpenInNewTabInGroup_TabGroupsEnabled_Shown() {
        // Default state: the master switch is on, so the item is shown when upstream allows it.
        doReturn(true).when(mDelegate).isItemSupported(ContextMenuItemId.OPEN_IN_NEW_TAB_IN_GROUP);
        doReturn(true).when(mNavigationDelegate).isOpenInNewTabInGroupEnabled();

        assertTrue(mManager.shouldShowItem(ContextMenuItemId.OPEN_IN_NEW_TAB_IN_GROUP, mDelegate));
    }

    @Test
    public void testOpenInNewTabInGroup_TabGroupsDisabled_Hidden() {
        ChromeSharedPreferences.getInstance()
                .writeBoolean(BravePreferenceKeys.BRAVE_TAB_GROUPS_FEATURE_ENABLED, false);
        doReturn(true).when(mDelegate).isItemSupported(ContextMenuItemId.OPEN_IN_NEW_TAB_IN_GROUP);
        doReturn(true).when(mNavigationDelegate).isOpenInNewTabInGroupEnabled();

        assertFalse(mManager.shouldShowItem(ContextMenuItemId.OPEN_IN_NEW_TAB_IN_GROUP, mDelegate));
    }

    @Test
    public void testOtherItemUnaffectedWhenTabGroupsDisabled() {
        // A non-group item stays visible regardless of the master switch.
        ChromeSharedPreferences.getInstance()
                .writeBoolean(BravePreferenceKeys.BRAVE_TAB_GROUPS_FEATURE_ENABLED, false);
        doReturn(true).when(mDelegate).isItemSupported(ContextMenuItemId.OPEN_IN_NEW_TAB);

        assertTrue(mManager.shouldShowItem(ContextMenuItemId.OPEN_IN_NEW_TAB, mDelegate));
    }

    // ---- getIconIdForMenuItem ----

    @Test
    public void testGetIconIdForMenuItem_standardItemsHaveIcons() {
        assertEquals(
                R.drawable.ic_open_in_new,
                mManager.getIconIdForMenuItem(ContextMenuItemId.OPEN_IN_NEW_TAB));
        assertEquals(
                R.drawable.ic_delete_24dp,
                mManager.getIconIdForMenuItem(ContextMenuItemId.REMOVE));
        assertEquals(
                R.drawable.ic_delete_24dp,
                mManager.getIconIdForMenuItem(ContextMenuItemId.REMOVE_ALL));
        assertEquals(
                R.drawable.ic_edit_24dp,
                mManager.getIconIdForMenuItem(ContextMenuItemId.EDIT_SHORTCUT));
    }

    @Test
    public void testGetIconIdForMenuItem_unmappedItemsReturnZero() {
        assertEquals(0, mManager.getIconIdForMenuItem(ContextMenuItemId.MOVE_UP));
        assertEquals(0, mManager.getIconIdForMenuItem(ContextMenuItemId.HIDE_ALL));
    }

    // ---- getMenuItemsBeforeStandardItems ("Add site") ----

    @Test
    public void testMenuItemsBefore_nonBraveDelegate_empty() {
        assertTrue(mManager.getMenuItemsBeforeStandardItems(mDelegate).isEmpty());
    }

    @Test
    public void testMenuItemsBefore_braveDelegateButNotSupported_empty() {
        doReturn(false).when(mBraveDelegate).isBraveItemSupported(BraveNtpDelegate.BRAVE_ADD_SITE);
        assertTrue(mManager.getMenuItemsBeforeStandardItems(mBraveDelegate).isEmpty());
    }

    @Test
    public void testMenuItemsBefore_supported_returnsAddSiteItem() {
        doReturn(true).when(mBraveDelegate).isBraveItemSupported(BraveNtpDelegate.BRAVE_ADD_SITE);

        List<MVCListAdapter.ListItem> items = mManager.getMenuItemsBeforeStandardItems(mBraveDelegate);

        assertEquals(1, items.size());
        assertEquals(
                BraveNtpDelegate.BRAVE_ADD_SITE,
                items.get(0).model.get(ListMenuItemProperties.MENU_ITEM_ID));
        assertEquals(
                R.drawable.ic_add_box_rounded_corner,
                items.get(0).model.get(ListMenuItemProperties.START_ICON_ID));
    }

    // ---- getMenuItemsAfterStandardItems (widget-control items) ----

    @Test
    public void testMenuItemsAfter_nonBraveDelegate_empty() {
        assertTrue(mManager.getMenuItemsAfterStandardItems(mDelegate).isEmpty());
    }

    @Test
    public void testMenuItemsAfter_noneSupported_empty() {
        doReturn(false).when(mBraveDelegate).isBraveItemSupported(BraveNtpDelegate.BRAVE_SHOW_FREQUENT);
        doReturn(false)
                .when(mBraveDelegate)
                .isBraveItemSupported(BraveNtpDelegate.BRAVE_SHOW_SHORTCUTS);
        doReturn(false).when(mBraveDelegate).isBraveItemSupported(BraveNtpDelegate.BRAVE_HIDE_WIDGET);

        assertTrue(mManager.getMenuItemsAfterStandardItems(mBraveDelegate).isEmpty());
    }

    @Test
    public void testMenuItemsAfter_allSupported_leadsWithDividerThenThreeItems() {
        doReturn(true).when(mBraveDelegate).isBraveItemSupported(BraveNtpDelegate.BRAVE_SHOW_FREQUENT);
        doReturn(true)
                .when(mBraveDelegate)
                .isBraveItemSupported(BraveNtpDelegate.BRAVE_SHOW_SHORTCUTS);
        doReturn(true).when(mBraveDelegate).isBraveItemSupported(BraveNtpDelegate.BRAVE_HIDE_WIDGET);
        doReturn(NtpUtil.TOP_SITES_MODE_FREQUENT).when(mBraveDelegate).getBraveTopSitesDisplayMode();
        doReturn(0).when(mBraveDelegate).getSelectedModeEndIconRes();

        List<MVCListAdapter.ListItem> items = mManager.getMenuItemsAfterStandardItems(mBraveDelegate);

        assertEquals(4, items.size());
        assertEquals(ListItemType.DIVIDER, items.get(0).type);
        assertEquals(
                BraveNtpDelegate.BRAVE_SHOW_FREQUENT,
                items.get(1).model.get(ListMenuItemProperties.MENU_ITEM_ID));
        assertEquals(
                BraveNtpDelegate.BRAVE_SHOW_SHORTCUTS,
                items.get(2).model.get(ListMenuItemProperties.MENU_ITEM_ID));
        assertEquals(
                BraveNtpDelegate.BRAVE_HIDE_WIDGET,
                items.get(3).model.get(ListMenuItemProperties.MENU_ITEM_ID));
    }

    @Test
    public void testMenuItemsAfter_selectedModeGetsEndIcon() {
        doReturn(true).when(mBraveDelegate).isBraveItemSupported(BraveNtpDelegate.BRAVE_SHOW_FREQUENT);
        doReturn(true)
                .when(mBraveDelegate)
                .isBraveItemSupported(BraveNtpDelegate.BRAVE_SHOW_SHORTCUTS);
        doReturn(true).when(mBraveDelegate).isBraveItemSupported(BraveNtpDelegate.BRAVE_HIDE_WIDGET);
        // Current mode is SHORTCUTS (0).
        doReturn(NtpUtil.TOP_SITES_MODE_SHORTCUTS).when(mBraveDelegate).getBraveTopSitesDisplayMode();
        doReturn(R.drawable.brave_ntp_check_selected_circle)
                .when(mBraveDelegate)
                .getSelectedModeEndIconRes();

        List<MVCListAdapter.ListItem> items = mManager.getMenuItemsAfterStandardItems(mBraveDelegate);

        // items[0]=divider, items[1]=Show frequent (not selected), items[2]=Show shortcuts
        // (selected), items[3]=Hide widget.
        assertEquals(
                0, items.get(1).model.get(ListMenuItemProperties.END_ICON_ID));
        assertEquals(
                R.drawable.brave_ntp_check_selected_circle,
                items.get(2).model.get(ListMenuItemProperties.END_ICON_ID));
        assertEquals(
                0, items.get(3).model.get(ListMenuItemProperties.END_ICON_ID));
    }

    @Test
    public void testMenuItemsAfter_partiallySupported_onlyIncludesSupportedItems() {
        doReturn(true).when(mBraveDelegate).isBraveItemSupported(BraveNtpDelegate.BRAVE_SHOW_FREQUENT);
        doReturn(false)
                .when(mBraveDelegate)
                .isBraveItemSupported(BraveNtpDelegate.BRAVE_SHOW_SHORTCUTS);
        doReturn(false).when(mBraveDelegate).isBraveItemSupported(BraveNtpDelegate.BRAVE_HIDE_WIDGET);
        doReturn(NtpUtil.TOP_SITES_MODE_FREQUENT).when(mBraveDelegate).getBraveTopSitesDisplayMode();
        doReturn(0).when(mBraveDelegate).getSelectedModeEndIconRes();

        List<MVCListAdapter.ListItem> items = mManager.getMenuItemsAfterStandardItems(mBraveDelegate);

        // divider + the single supported item.
        assertEquals(2, items.size());
        assertEquals(ListItemType.DIVIDER, items.get(0).type);
        assertEquals(
                BraveNtpDelegate.BRAVE_SHOW_FREQUENT,
                items.get(1).model.get(ListMenuItemProperties.MENU_ITEM_ID));
    }

    // ---- handleMenuItemClick ----

    @Test
    public void testHandleMenuItemClick_addSite_dispatchesAndConsumes() {
        boolean handled = mManager.handleMenuItemClick(BraveNtpDelegate.BRAVE_ADD_SITE, mBraveDelegate);

        assertTrue(handled);
        verify(mBraveDelegate).braveAddSite();
    }

    @Test
    public void testHandleMenuItemClick_showFrequent_dispatchesAndConsumes() {
        boolean handled =
                mManager.handleMenuItemClick(BraveNtpDelegate.BRAVE_SHOW_FREQUENT, mBraveDelegate);

        assertTrue(handled);
        verify(mBraveDelegate).braveShowFrequent();
    }

    @Test
    public void testHandleMenuItemClick_showShortcuts_dispatchesAndConsumes() {
        boolean handled =
                mManager.handleMenuItemClick(BraveNtpDelegate.BRAVE_SHOW_SHORTCUTS, mBraveDelegate);

        assertTrue(handled);
        verify(mBraveDelegate).braveShowShortcuts();
    }

    @Test
    public void testHandleMenuItemClick_hideWidget_dispatchesAndConsumes() {
        boolean handled =
                mManager.handleMenuItemClick(BraveNtpDelegate.BRAVE_HIDE_WIDGET, mBraveDelegate);

        assertTrue(handled);
        verify(mBraveDelegate).braveHideWidget();
    }

    @Test
    public void testHandleMenuItemClick_standardItem_fallsThroughToSuper() {
        boolean handled =
                mManager.handleMenuItemClick(ContextMenuItemId.REMOVE, mBraveDelegate);

        assertTrue(handled);
        verify(mBraveDelegate).removeItem();
        verify(mBraveDelegate, never()).braveAddSite();
    }

    @Test
    public void testHandleMenuItemClick_nonBraveDelegate_standardItemStillWorks() {
        boolean handled = mManager.handleMenuItemClick(ContextMenuItemId.REMOVE, mDelegate);

        assertTrue(handled);
        verify(mDelegate).removeItem();
    }
}
