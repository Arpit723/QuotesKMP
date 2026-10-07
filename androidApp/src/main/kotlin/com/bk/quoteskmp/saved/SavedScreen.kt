package com.bk.quoteskmp.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.demo.quotes.presentation.SavedUiState
import com.demo.quotes.presentation.SavedViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SavedScreen(
    viewModel: SavedViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SavedScreenContent(
        uiState = uiState,
        onRetryClick = viewModel::retry,
        onDeleteQuoteClick = viewModel::delete,
    )
}

@Composable
fun SavedScreenContent(
    uiState: SavedUiState,
    onRetryClick: () -> Unit,
    onDeleteQuoteClick: (Long) -> Unit,
) {
    MaterialTheme {
        when (uiState) {
            SavedUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            SavedUiState.Empty -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.saved_empty),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            is SavedUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(uiState.quotes, key = { it.id }) { quote ->
                    Card(
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = quote.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = quote.author,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            IconButton(onClick = { onDeleteQuoteClick(quote.id) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.cd_delete_quote),
                                )
                            }
                        }
                    }
                }
            }

            is SavedUiState.Error -> Column(
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

private val savedPreviewQuotes = listOf(
    Quote(id = 1L, text = "The best way to predict the future is to invent it.", author = "Alan Kay"),
    Quote(id = 2L, text = "Simplicity is prerequisite for reliability.", author = "Edsger W. Dijkstra"),
)

@Preview(name = "Saved - Loading", showBackground = true)
@Composable
private fun SavedLoadingPreview() {
    SavedScreenContent(
        uiState = SavedUiState.Loading,
        onRetryClick = {},
        onDeleteQuoteClick = {},
    )
}

@Preview(name = "Saved - Empty", showBackground = true)
@Composable
private fun SavedEmptyPreview() {
    SavedScreenContent(
        uiState = SavedUiState.Empty,
        onRetryClick = {},
        onDeleteQuoteClick = {},
    )
}

@Preview(name = "Saved - Content", showBackground = true)
@Composable
private fun SavedContentPreview() {
    SavedScreenContent(
        uiState = SavedUiState.Content(savedPreviewQuotes),
        onRetryClick = {},
        onDeleteQuoteClick = {},
    )
}

@Preview(name = "Saved - Error", showBackground = true)
@Composable
private fun SavedErrorPreview() {
    SavedScreenContent(
        uiState = SavedUiState.Error("Could not load saved quotes."),
        onRetryClick = {},
        onDeleteQuoteClick = {},
    )
}
