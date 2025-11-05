package com.jankinwu.fntv.client.ui.component.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jankinwu.fntv.client.data.constants.Colors
import com.jankinwu.fntv.client.icons.ArrowUp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.FlyoutPlacement
import io.github.composefluent.component.Icon
import io.github.composefluent.component.MenuFlyoutContainer
import io.github.composefluent.component.MenuFlyoutItem
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Checkmark
import kotlinx.coroutines.delay


@Composable
fun StreamSelector(
    audioOptions: List<StreamOptionItem>,
    selectedItemLabel: String,
    onSelected: (String) -> Unit
) {
    if (audioOptions.isNotEmpty() && audioOptions.size > 1) {
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        MenuFlyoutContainer(
            flyout = {
                audioOptions.forEach { audioOption ->
                    MenuFlyoutItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
//                                    .background(if (audioOption.isSelected) FluentTheme.colors.subtleFill.tertiary else Color.Transparent)
                                    .padding(vertical = 8.dp)
                                    .hoverable(interactionSource)
                                    .pointerHoverIcon(PointerIcon.Hand)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.weight(5f)
                                ) {
                                    Text(
                                        text = audioOption.title + if (audioOption.isDefault) " - 默认" else "",
                                        color = if (audioOption.isSelected) Colors.PrimaryColor else FluentTheme.colors.text.text.primary,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 15.sp,
                                        modifier = Modifier
                                            .width(120.dp)
                                    )
                                    Text(
                                        text = "${audioOption.subtitle1} ${audioOption.subtitle2}  ${audioOption.subtitle3}",
                                        color = if (audioOption.isSelected) Colors.PrimaryColor else FluentTheme.colors.text.text.secondary,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
//                                            .width(170.dp)
                                    )
                                }
                                if (audioOption.isSelected) {
                                    Icon(
                                        imageVector = Icons.Regular.Checkmark,
                                        contentDescription = "",
                                        tint = Colors.PrimaryColor,
                                        modifier = Modifier
                                            .weight(1f)
                                            .size(18.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelected(audioOption.audioGuid)
//                            currentAudioStream = audioStreams.first { it.guid == audioOption.audioGuid }
                            isFlyoutVisible = false
                        },
                        modifier = Modifier
                            .width(240.dp)
                            .hoverable(interactionSource)
//                            .padding(vertical = 4.dp)
//                            .background(if (audioOption.isSelected) FluentTheme.colors.subtleFill.tertiary else Color.Transparent, RoundedCornerShape(4.dp))
//                        colors = mediaDetailsSelectedListItemColors()
                    )
                }
            },
            content = {
                StreamSelectorLabel(
                    modifier = Modifier.hoverable(interactionSource),
                    selectedLabel = selectedItemLabel,
                    isHovered = isHovered,
                    onFlyoutVisibilityChange = { isVisible -> isFlyoutVisible = isVisible }
                )
            },
//            placement = FlyoutPlacement.BottomAlignedStart,
            placement = FlyoutPlacement.Auto,
        )
    } else {
        Text(
            text = selectedItemLabel,
            color = FluentTheme.colors.text.text.secondary,
            fontSize = 14.sp
        )
    }
}


@Composable
fun StreamSelectorLabel(
    selectedLabel: String,
    isHovered: Boolean,
    modifier: Modifier = Modifier,
    onFlyoutVisibilityChange: (Boolean) -> Unit
) {
    val targetRotation = if (isHovered) -180f else 0f
    val animatedRotation by animateFloatAsState(targetValue = targetRotation)

    LaunchedEffect(isHovered) {
        if (isHovered) {
            onFlyoutVisibilityChange(true)
        } else {
            delay(200)
            onFlyoutVisibilityChange(false)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = selectedLabel,
            color = FluentTheme.colors.text.text.secondary,
            fontSize = 14.sp
        )
        Icon(
            imageVector = ArrowUp,
            contentDescription = "下拉框箭头",
            tint = FluentTheme.colors.text.text.tertiary,
            modifier = Modifier
                .size(16.dp)
                .rotate(animatedRotation)
        )
    }
}

data class StreamOptionItem(
    val audioGuid: String,
    val title: String,
    val subtitle1: String,
    val subtitle2: String,
    val subtitle3: String,
    val isDefault: Boolean = false,
    val isSelected: Boolean = false
)