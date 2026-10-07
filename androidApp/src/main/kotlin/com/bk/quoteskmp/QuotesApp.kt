package com.bk.quoteskmp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.bk.quoteskmp.home.HomeScreen
import com.bk.quoteskmp.saved.SavedScreen

@Composable
fun QuotesApp() {
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    QuotesAppContent(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        homeContent = { HomeScreen() },
        savedContent = { SavedScreen() },
    )
}

@Composable
fun QuotesAppContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    homeContent: @Composable () -> Unit,
    savedContent: @Composable () -> Unit,
) {
    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.tab_home)) },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(R.string.tab_saved)) },
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (selectedTab) {
                    0 -> homeContent()
                    else -> savedContent()
                }
            }
        }
    }
}

@Preview(name = "App - Home tab selected", showBackground = true)
@Composable
private fun QuotesAppHomeTabPreview() {
    QuotesAppContent(
        selectedTab = 0,
        onTabSelected = {},
        homeContent = { Text(text = "Home") },
        savedContent = { Text(text = "Saved") },
    )
}

@Preview(name = "App - Saved tab selected", showBackground = true)
@Composable
private fun QuotesAppSavedTabPreview() {
    QuotesAppContent(
        selectedTab = 1,
        onTabSelected = {},
        homeContent = { Text(text = "Home") },
        savedContent = { Text(text = "Saved") },
    )
}
