package com.vk.directop.serverdrivenuiexamplepv

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MainComposable(
    viewModel: MainActivityViewModel = viewModel()
) {
    val layoutInformation by viewModel.layoutInformationFlow.collectAsState()
    when (layoutInformation) {
        null -> LoadingComponent()
        else -> NewsFeedScreen(layoutInformation = layoutInformation!!,
            addNewItemClick = { viewModel.onAddNewItemClick() },
        )
    }
}

sealed interface LayoutType {
    object List : LayoutType
    data class Grid(val columns: Int) : LayoutType
}

data class LayoutMeta(
    val layoutType: LayoutType,
    val favoriteEnabled: Boolean
)

data class LayoutInformation(
    val layoutMeta: LayoutMeta,
    val layoutData: List<MainActivityViewModel.NewsItem>
)

@Composable
fun NewsFeedScreen(
    layoutInformation: LayoutInformation,
    addNewItemClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Button(onClick = { addNewItemClick() }) {
            Text(text = "Add new item")
        }
        when (layoutInformation.layoutMeta.layoutType) {
            is LayoutType.List -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = layoutInformation.layoutData, key = { newsItem -> newsItem.id }) {
                        NewsItemComponent(
                            newsItem = it,
                            favoriteEnabled = layoutInformation.layoutMeta.favoriteEnabled
                        )
                    }
                }
            }

            is LayoutType.Grid -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(layoutInformation.layoutMeta.layoutType.columns),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = layoutInformation.layoutData, key = { newsItem -> newsItem.id }) {
                        NewsItemComponent(
                            newsItem = it,
                            favoriteEnabled = layoutInformation.layoutMeta.favoriteEnabled
                        )
                    }
                }
            }
        }


    }
}


@Composable
fun LoadingComponent() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "Loading...")
        //        CircularProgressIndicator(
//            modifier = Modifier
//                .size(50.dp)
//                .align(Alignment.Center),
//            color = MaterialTheme.colorScheme.tertiary
//        )
    }
}

@Composable
fun NewsItemComponent(
    newsItem: MainActivityViewModel.NewsItem,
    favoriteEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .background(
                color = Color.LightGray,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = newsItem.title)
            Spacer(modifier = Modifier.weight(1f))
            if (favoriteEnabled) {
                val icon =
                    if (newsItem.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder
                Icon(imageVector = icon,
                    contentDescription = "Favorite",
                    modifier = Modifier.clickable {
                        Log.e("FAVORITE", "Handle onClick for ${newsItem.id}")
                    }
                )
            }
        }
        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(color = Color.DarkGray, shape = RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.height(15.dp))
        Text(text = newsItem.description)

    }

}