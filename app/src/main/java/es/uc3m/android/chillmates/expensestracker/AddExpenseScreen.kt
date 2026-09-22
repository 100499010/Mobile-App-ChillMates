package es.uc3m.android.chillmates.expensestracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import es.uc3m.android.chillmates.R

data class ExpenseResult(
    val title: String,
    val amount: Double,
    val paidBy: String,
    val splitBetween: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    availableFlatmates: List<String>,
    initialTitle: String = "",
    initialPaidBy: String = "",
    initialSplitBetween: List<String> = emptyList(),
    onSave: (ExpenseResult) -> Unit,
    onDismiss: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var amountStr by rememberSaveable { mutableStateOf("") }

    val defaultPaidBy = if (
        initialPaidBy.isNotBlank() && availableFlatmates.contains(initialPaidBy)
    ) {
        initialPaidBy
    } else {
        availableFlatmates.firstOrNull() ?: ""
    }

    var paidBy by rememberSaveable { mutableStateOf(defaultPaidBy) }
    var paidByExpanded by remember { mutableStateOf(false) }

    val initialSelection = if (initialSplitBetween.isNotEmpty()) {
        initialSplitBetween.toSet()
    } else {
        availableFlatmates.toSet()
    }

    var selectedFlatmates by remember { mutableStateOf(initialSelection) }
    var splitExpanded by remember { mutableStateOf(false) }

    val amountValue = amountStr.toDoubleOrNull()
    val canSave = title.isNotBlank() && amountValue != null && amountValue > 0 && paidBy.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_add_expense),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.label_expense_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(stringResource(R.string.add_expense_amount_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.label_paid_by),
                    fontWeight = FontWeight.Bold
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = paidBy,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = paidByExpanded,
                        onDismissRequest = { paidByExpanded = false }
                    ) {
                        availableFlatmates.forEach { person ->
                            DropdownMenuItem(
                                text = { Text(person) },
                                onClick = {
                                    paidBy = person
                                    paidByExpanded = false
                                }
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.matchParentSize(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        onClick = { paidByExpanded = true }
                    ) {}
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.label_split_between),
                    fontWeight = FontWeight.Bold
                )

                OutlinedCard(
                    onClick = { splitExpanded = !splitExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedFlatmates.size} ${stringResource(R.string.label_selected)}",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (splitExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }

                        if (splitExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = { 
                                    selectedFlatmates = availableFlatmates.toSet()
                                }) {
                                    Text(stringResource(R.string.btn_select_all))
                                }
                                TextButton(onClick = { 
                                    selectedFlatmates = emptySet()
                                }) {
                                    Text(stringResource(R.string.btn_unselect_all))
                                }
                            }

                            Column {
                                availableFlatmates.forEach { person ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = person in selectedFlatmates,
                                            onCheckedChange = { isChecked ->
                                                selectedFlatmates =
                                                    if (isChecked) selectedFlatmates + person
                                                    else selectedFlatmates - person
                                            }
                                        )
                                        Text(text = person)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                    Button(
                        onClick = {
                            val amount = amountValue ?: return@Button
                            onSave(ExpenseResult(title.trim(), amount, paidBy, selectedFlatmates.toList()))
                        },
                        enabled = canSave,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.btn_save))
                    }
                }
            }
        }
    }
}