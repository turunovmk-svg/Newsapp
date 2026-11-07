package com.mikhail.newsapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mikhail.newsapp.UiSizes
import com.mikhail.newsapp.data.Post

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(viewModel: MainViewModel, onAddSource: () -> Unit) {
    val posts by viewModel.posts.collectAsState()
    val isRefreshing by viewModel.isLoading.collectAsState()
    val shortPosts = posts.take(10)

    Column(modifier = Modifier.fillMaxSize()) {
        if (shortPosts.isEmpty()) {
            EmptyShortsState(onAddSource = onAddSource)
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.loadPosts() },
                modifier = Modifier.fillMaxSize(),
                state = rememberPullToRefreshState()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shortPosts) { post ->
                        ShortCard(post = post)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyShortsState(onAddSource: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(UiSizes.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(UiSizes.ShortSpacerBig))
        Text(
            text = stringResource(id = R.string.empty_shorts),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(UiSizes.ShortSpacerMedium))
        Text(
            text = stringResource(id = R.string.add_sources_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(UiSizes.ShortSpacerBig))
        Button(
            onClick = onAddSource,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.add_source_button))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortCard(post: Post) {
    Surface(
        modifier = Modifier
            .padding(horizontal = UiSizes.ShortCardHorizontalPadding, vertical = UiSizes.ShortCardVerticalPadding)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(UiSizes.ShortCardInnerPadding)) {
            post.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(UiSizes.ShortImageHeight)
                        .clip(RoundedCornerShape(UiSizes.ShortImageCornerRadius))
                )
            }
            Spacer(modifier = Modifier.height(UiSizes.ShortSpacerMedium))
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(UiSizes.ShortSpacerSmall))
            post.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
            Spacer(modifier = Modifier.height(UiSizes.ShortSpacerSmall))
            post.publishedAt?.let { date ->
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
