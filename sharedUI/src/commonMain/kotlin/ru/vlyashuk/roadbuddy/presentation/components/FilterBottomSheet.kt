package ru.vlyashuk.roadbuddy.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.vlyashuk.roadbuddy.domain.model.RequestStatus
import ru.vlyashuk.roadbuddy.domain.model.RequestType
import org.jetbrains.compose.resources.stringResource
import roadbuddy.sharedui.generated.resources.Res
import roadbuddy.sharedui.generated.resources.filters
import roadbuddy.sharedui.generated.resources.clear_all
import roadbuddy.sharedui.generated.resources.apply

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    onDismissRequest: () -> Unit,
    selectedType: RequestType?,
    onTypeSelected: (RequestType?) -> Unit,
    selectedStatus: RequestStatus?,
    onStatusSelected: (RequestStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.filters),
                    style = MaterialTheme.typography.titleMedium
                )
                if (selectedType != null || selectedStatus != null) {
                    TextButton(onClick = {
                        onTypeSelected(null)
                        onStatusSelected(null)
                    }) {
                        Text(stringResource(Res.string.clear_all))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FilterBar(
                selectedType = selectedType,
                onTypeSelected = onTypeSelected,
                selectedStatus = selectedStatus,
                onStatusSelected = onStatusSelected
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(stringResource(Res.string.apply))
            }
        }
    }
}
