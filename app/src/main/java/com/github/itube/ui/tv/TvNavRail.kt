package com.github.itube.ui.tv

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.itube.Account
import com.github.itube.ChannelAvatar
import com.github.itube.ShortsSetting
import com.github.itube.Tab

val TvRailWidth = 72.dp
val TvRailExpandedWidth = 228.dp

private val RailBg = Color(0xFF0F0F0F).copy(alpha = 0.98f)
private val RailActiveBg = Color.White.copy(alpha = 0.14f)
private val RailFocusBg = Color.White
private val RailFocusInk = Color(0xFF0F0F0F)
private val RailMutedInk = Color(0xFFAAAAAA)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvNavRail(
    selectedTab: Tab,
    onSelectTab: (Tab) -> Unit,
    onAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bridge = LocalTvFocusBridge.current
    var railHasFocus by remember { mutableStateOf(false) }

    val railWidth by animateDpAsState(
        targetValue = if (railHasFocus) TvRailExpandedWidth else TvRailWidth,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 900f),
        label = "tvRailWidth",
    )

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth)
            .onFocusChanged { railHasFocus = it.hasFocus }
            .focusProperties {
                exit = { direction ->
                    if (direction == FocusDirection.Right) {
                        bridge?.exitToContentRequester() ?: FocusRequester.Default
                    } else {
                        FocusRequester.Default
                    }
                }
            }
            .focusGroup(),
        color = RailBg,
        shape = RectangleShape,
        tonalElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .padding(vertical = 22.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top Profile / Account switcher (YouTube TV style)
                TvProfileRailItem(
                    expanded = railHasFocus,
                    onClick = onAccount,
                )

                // Middle Navigation Tabs (YouTube TV order)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    val tabs = buildList {
                        add(Tab.Search)
                        add(Tab.Home)
                        if (ShortsSetting.enabled) add(Tab.Shorts)
                        add(Tab.Subscriptions)
                        add(Tab.Music)
                        add(Tab.Library)
                    }

                    tabs.forEach { tab ->
                        val selected = tab == selectedTab
                        TvNavRailItem(
                            tab = tab,
                            isSelected = selected,
                            expanded = railHasFocus,
                            modifier = if (selected && bridge != null) Modifier.focusRequester(bridge.railRequester) else Modifier,
                            onClick = { onSelectTab(tab) },
                        )
                    }
                }

                // Bottom Settings row (YouTube TV style)
                TvSettingsRailItem(
                    expanded = railHasFocus,
                    onClick = onAccount,
                )
            }

            // Subtle cinema hairline separating sidebar from browse canvas
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.08f)),
            )
        }
    }
}

@Composable
private fun TvProfileRailItem(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    val view = LocalView.current
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 1350f),
        label = "tvProfileScale",
    )
    val profile = Account.profile
    val signedIn = profile != null
    val accountName = profile?.name?.takeIf { it.isNotBlank() } ?: if (signedIn) "Account" else "Sign in"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(if (isFocused) RailFocusBg else Color.Transparent)
            .onFocusChanged { isFocused = it.hasFocus || it.isFocused }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onClick()
            }
            .padding(horizontal = if (expanded) 10.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
    ) {
        if (profile != null && !profile.avatar.isNullOrBlank()) {
            ChannelAvatar(
                url = profile.avatar,
                name = accountName,
                size = 30,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = accountName,
                tint = if (isFocused) RailFocusInk else Color(0xFFF1F1F1),
                modifier = Modifier.size(28.dp),
            )
        }
        if (expanded) {
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = accountName,
                    color = if (isFocused) RailFocusInk else Color(0xFFF1F1F1),
                    fontSize = 14.sp,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (signedIn) "Switch account" else "Sign in for feeds",
                    color = if (isFocused) RailFocusInk.copy(alpha = 0.72f) else RailMutedInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TvSettingsRailItem(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    val view = LocalView.current
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 1350f),
        label = "tvSettingsScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(if (isFocused) RailFocusBg else Color.Transparent)
            .onFocusChanged { isFocused = it.hasFocus || it.isFocused }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onClick()
            }
            .padding(horizontal = if (expanded) 14.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = "Settings",
            tint = if (isFocused) RailFocusInk else RailMutedInk,
            modifier = Modifier.size(23.dp),
        )
        if (expanded) {
            Spacer(Modifier.width(16.dp))
            Text(
                text = "Settings",
                color = if (isFocused) RailFocusInk else Color(0xFFF1F1F1),
                fontSize = 15.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TvNavRailItem(
    tab: Tab,
    isSelected: Boolean,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    val view = LocalView.current

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 1350f),
        label = "tvRailItemScale",
    )

    val itemBgColor = when {
        isFocused -> RailFocusBg
        isSelected -> RailActiveBg
        else -> Color.Transparent
    }

    val itemContentColor = when {
        isFocused -> RailFocusInk
        isSelected -> Color.White
        else -> RailMutedInk
    }

    val displayLabel = if (tab == Tab.Subscriptions) "Subscriptions" else tab.label

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(itemBgColor)
            .onFocusChanged { isFocused = it.hasFocus || it.isFocused }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                onClick()
            }
            .padding(horizontal = if (expanded) 14.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
    ) {
        Icon(
            imageVector = if (isSelected || isFocused) tab.selectedIcon else tab.icon,
            contentDescription = displayLabel,
            tint = itemContentColor,
            modifier = Modifier.size(23.dp),
        )
        if (expanded) {
            Spacer(Modifier.width(16.dp))
            Text(
                text = displayLabel,
                color = itemContentColor,
                fontSize = 15.sp,
                fontWeight = when {
                    isFocused -> FontWeight.Bold
                    isSelected -> FontWeight.SemiBold
                    else -> FontWeight.Medium
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
