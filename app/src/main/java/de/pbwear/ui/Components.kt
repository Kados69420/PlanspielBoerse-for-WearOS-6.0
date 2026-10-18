package de.pbwear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

/** Scrollbare Seite mit Uhrzeit und Positionsanzeige. */
@Composable
fun ScreenList(content: ScalingLazyListScope.() -> Unit) {
    val state = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = state) },
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            autoCentering = null,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(top = 34.dp, bottom = 40.dp, start = 10.dp, end = 10.dp),
            content = content,
        )
    }
}

@Composable
fun PBChip(
    label: String,
    secondary: String? = null,
    background: Color = PB.Gray,
    content: Color = PB.Text,
    onClick: () -> Unit,
) {
    val colors = ChipDefaults.primaryChipColors(backgroundColor = background, contentColor = content)
    if (secondary != null) {
        Chip(
            onClick = onClick,
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            secondaryLabel = { Text(secondary, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        )
    } else {
        Chip(
            onClick = onClick,
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        )
    }
}

@Composable
fun PrimaryChip(label: String, onClick: () -> Unit) =
    PBChip(label = label, background = PB.Red, content = Color.White, onClick = onClick)

@Composable
fun GreenChip(label: String, onClick: () -> Unit) =
    PBChip(label = label, background = PB.Green, content = Color.White, onClick = onClick)

@Composable
fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.title3,
        color = PB.Red,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun Body(text: String, color: Color = PB.Text, bold: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.body2,
        color = color,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption2,
        color = PB.TextGray,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun Spinner() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(indicatorColor = PB.Red)
    }
}

/** Runder Knopf ohne zusätzliche Material-Abhängigkeiten. */
@Composable
fun RoundButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(PB.Gray)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.button, color = PB.Text, fontWeight = FontWeight.Bold)
    }
}

/** [-]  Wert  [+] */
@Composable
fun Stepper(value: String, minusLabel: String = "−", plusLabel: String = "+", onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundButton(minusLabel, onMinus)
        Text(
            text = value,
            style = MaterialTheme.typography.title3,
            color = PB.Text,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        RoundButton(plusLabel, onPlus)
    }
}

@Composable
fun InputField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    password: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(PB.Gray)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(hint, style = MaterialTheme.typography.body2, color = PB.HintGray)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.body2.copy(color = PB.Text),
            cursorBrush = SolidColor(PB.Red),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
                imeAction = imeAction,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
