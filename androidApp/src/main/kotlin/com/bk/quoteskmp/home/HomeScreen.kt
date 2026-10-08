package com.bk.quoteskmp.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bk.quoteskmp.R
import com.demo.quotes.domain.Quote
import com.demo.quotes.presentation.HomeUiState
import com.demo.quotes.presentation.HomeViewModel
import org.koin.compose.koinInject

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        uiState = uiState,
        onNewQuoteClick = viewModel::loadNewQuote,
        onRetryClick = viewModel::loadNewQuote,
        onToggleSaveClick = viewModel::toggleSave,
    )
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onNewQuoteClick: () -> Unit,
    onRetryClick: () -> Unit,
    onToggleSaveClick: () -> Unit,
) {
    MaterialTheme {
        when (uiState) {
            HomeUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is HomeUiState.Success -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = uiState.quote.text,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = uiState.quote.author,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                if (uiState.isOffline) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.label_offline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = onNewQuoteClick) {
                        Text(stringResource(R.string.action_new_quote))
                    }
                    IconButton(onClick = onToggleSaveClick) {
                        if (uiState.isSaved) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = stringResource(R.string.cd_remove_from_saved),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(R.string.cd_save_quote),
                            )
                        }
                    }
                }
            }

            is HomeUiState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onRetryClick) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        }
    }
}

private val homePreviewQuote = Quote(
    id = 1L,
    text = "The best way to predict the future is to invent it.",
    author = "Alan Kay",
)

@Preview(name = "Home - Loading", showBackground = true)
@Composable
private fun HomeLoadingPreview() {
    HomeScreenContent(
        uiState = HomeUiState.Loading,
        onNewQuoteClick = {},
        onRetryClick = {},
        onToggleSaveClick = {},
    )
}

@Preview(name = "Home - Success (online, unsaved)", showBackground = true)
@Composable
private fun HomeSuccessOnlineUnsavedPreview() {
    HomeScreenContent(
        uiState = HomeUiState.Success(quote = homePreviewQuote, isSaved = false, isOffline = false),
        onNewQuoteClick = {},
        onRetryClick = {},
        onToggleSaveClick = {},
    )
}

@Preview(name = "Home - Success (online, saved)", showBackground = true)
@Composable
private fun HomeSuccessOnlineSavedPreview() {
    HomeScreenContent(
        uiState = HomeUiState.Success(quote = homePreviewQuote, isSaved = true, isOffline = false),
        onNewQuoteClick = {},
        onRetryClick = {},
        onToggleSaveClick = {},
    )
}

@Preview(name = "Home - Success (offline, unsaved)", showBackground = true)
@Composable
private fun HomeSuccessOfflineUnsavedPreview() {
    HomeScreenContent(
        uiState = HomeUiState.Success(quote = homePreviewQuote, isSaved = false, isOffline = true),
        onNewQuoteClick = {},
        onRetryClick = {},
        onToggleSaveClick = {},
    )
}

@Preview(name = "Home - Success (offline, saved)", showBackground = true)
@Composable
private fun HomeSuccessOfflineSavedPreview() {
    HomeScreenContent(
        uiState = HomeUiState.Success(quote = homePreviewQuote, isSaved = true, isOffline = true),
        onNewQuoteClick = {},
        onRetryClick = {},
        onToggleSaveClick = {},
    )
}

@Preview(name = "Home - Error", showBackground = true)
@Composable
private fun HomeErrorPreview() {
    HomeScreenContent(
        uiState = HomeUiState.Error("Could not load a quote."),
        onNewQuoteClick = {},
        onRetryClick = {},
        onToggleSaveClick = {},
    )
}
