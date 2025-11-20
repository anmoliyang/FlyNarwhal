package com.jankinwu.fntv.client.ui.component.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jankinwu.fntv.client.LocalTypography
import com.jankinwu.fntv.client.data.convertor.convertToSubtitleItemList
import com.jankinwu.fntv.client.icons.Download
import com.jankinwu.fntv.client.viewmodel.SubtitleSearchViewModel
import com.jankinwu.fntv.client.viewmodel.UiState
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.DialogSize
import io.github.composefluent.component.FluentDialog
import io.github.composefluent.component.FlyoutPlacement
import io.github.composefluent.component.Icon
import io.github.composefluent.component.MenuFlyoutContainer
import io.github.composefluent.component.MenuFlyoutItem
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Dismiss
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SubtitleSearchDialog(
    title: String,
    visible: Boolean,
    size: DialogSize = DialogSize.Max,
    mediaGuid: String,
    mediaFileName: String,
    onDismissRequest: () -> Unit = {}
) {
    var language by remember { mutableStateOf("zh-CN") }
    val subtitleSearchViewModel: SubtitleSearchViewModel = koinViewModel()
    val subtitleSearchState by subtitleSearchViewModel.uiState.collectAsState()
    LaunchedEffect(language) {
        if (mediaGuid.isNotBlank()) {
            subtitleSearchViewModel.searchSubtitles(language, mediaGuid)
        }
    }

    FluentDialog(visible, size) {
        Column {
            Column(
                Modifier
                    .fillMaxWidth()
//                    .background(FluentTheme.colors.background.layer.alt)
                    .padding(24.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        style = FluentTheme.typography.subtitle,
                        text = title,
                    )
                    Icon(
                        imageVector = Icons.Regular.Dismiss,
                        contentDescription = "",
                        tint = FluentTheme.colors.text.text.primary,
                        modifier = Modifier
                            .clickable(
                                onClick = {
                                    onDismissRequest()
                                }
                            )
                            .size(16.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        style = LocalTypography.current.subtitle,
                        text = mediaFileName,
                    )
                    LanguageSwitchFlyout(onLanguageSelected = { language = it })
                }
                Text(
                    style = LocalTypography.current.body,
                    text = "按相关度排序：",
                )
                when (subtitleSearchState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.Gray)
                        )
                    }

                    is UiState.Success -> {
                        val subtitleSearchResponse =
                            (subtitleSearchState as UiState.Success).data
                        val subtitleItemList = convertToSubtitleItemList(subtitleSearchResponse.subtitles)
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun LanguageSwitchFlyout(
    onLanguageSelected: (String) -> Unit,
) {
    var buttonName by remember { mutableStateOf("中文") }
    MenuFlyoutContainer(
        flyout = {
            languageList.forEach { language ->
                MenuFlyoutItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = language.name,
                                color = FluentTheme.colors.text.text.secondary,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .width(120.dp)
                            )
                        }
                    },
                    onClick = {
                        isFlyoutVisible = false
                        buttonName = language.buttonName
                        onLanguageSelected(language.code)
                    },
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                )
            }
        },
        content = {
            FlyoutButton(
                isSelected = isFlyoutVisible,
                onClick = {
                    isFlyoutVisible = !isFlyoutVisible
                },
                buttonText = buttonName,
            )
        },
        placement = FlyoutPlacement.BottomAlignedEnd,
        modifier = Modifier.offset(x = 16.dp)
    )
}

@Composable
fun SubtitleResultList(results: List<SubtitleItemData>) {
    val listState = rememberLazyListState()

    // 简单的滚动条可见性逻辑：正在滚动时显示
    val isScrolling = listState.isScrollInProgress

    // 使用 AnimatedVisibility 实现淡入淡出
    // 注意：在 Desktop 上可以使用 VerticalScrollbar，但在 Android 上通常没有。
    // 这里我们实现一个简单的自定义滚动条，以适应 Multiplatform 的通用需求。

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(end = 8.dp) // 给滚动条留点位置
        ) {
            items(results) { item ->
                SubtitleListItem(item)
            }
        }

        // 自定义滚动条 (覆盖在右侧)
        AnimatedVisibility(
            visible = isScrolling,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            // 这里是一个简单的滚动条指示器 UI
            // 在实际生产中，计算滑块位置和大小需要根据 listState.layoutInfo 进行复杂的计算
            // 为了简化演示，这里展示一个视觉上的滚动条占位符，
            // 或者如果是在 Desktop 平台，可以直接使用 VerticalScrollbar

            // 简易视觉效果：
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .height(40.dp) // 简化：固定高度滑块，实际应动态计算
                        .fillMaxWidth()
                        // 简单的动态位置模拟 (仅作演示，精确同步需要自定义 Modifier)
                        .align(Alignment.TopCenter)
                        .offset(y = (listState.firstVisibleItemIndex * 2).dp)
                        .background(Color.Gray, CircleShape)
                )
            }
        }

        /*
           注意：如果你主要针对 Desktop (JVM)，推荐使用官方的 VerticalScrollbar：

           val adapter = rememberScrollbarAdapter(listState)
           AnimatedVisibility(visible = isScrolling, modifier = Modifier.align(Alignment.CenterEnd)) {
               VerticalScrollbar(
                   adapter = adapter,
                   style = ScrollbarStyle(
                       minimalHeight = 16.dp,
                       thickness = 8.dp,
                       shape = CircleShape,
                       hoverDurationMillis = 300,
                       unhoverColor = Color.Gray,
                       hoverColor = Color.White
                   )
               )
           }
        */
    }
}

// --- 组件：单行字幕项 ---
@Composable
fun SubtitleListItem(item: SubtitleItemData) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.fileName,
                color = FluentTheme.colors.text.text.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )

            Text(
                text = "下载量: ${item.download}",
                color = FluentTheme.colors.text.text.secondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { /* 下载逻辑 */ },
//            colors = ButtonDefaults.buttonColors(containerColor = SubtitleTheme.ButtonBg),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(
                imageVector = Download,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = FluentTheme.colors.text.text.secondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "下载字幕", fontSize = 12.sp, color = FluentTheme.colors.text.text.secondary)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}


private data class Language(
    val name: String,
    val code: String,
    val buttonName: String
)

private val languageList = listOf(
    Language("简体中文", "zh-CN", "中文"),
    Language("英文", "en", "英文")
)

data class SubtitleItemData(
    val fileName: String,
    val download: Int,
    val trimId: String,
)

