package fr.richoux.pobo.screens

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.coroutineScope

fun Modifier.customDialogModifier() = layout { measurable, constraints ->
  val placeable = measurable.measure(constraints);
  layout(constraints.maxWidth, constraints.maxHeight) {
    placeable.place(
      (constraints.maxWidth - placeable.width) / 2,
      9 * (constraints.maxHeight - placeable.height) / 10,
      10f
    )
  }
}

fun Modifier.disableSplitMotionEvents() =
  pointerInput(Unit) {
    coroutineScope {
      var currentId: Long = -1L
      awaitPointerEventScope {
        while (true) {
          awaitPointerEvent(PointerEventPass.Initial).changes.forEach { pointerInfo ->
            when {
              pointerInfo.pressed && currentId == -1L -> currentId = pointerInfo.id.value
              pointerInfo.pressed.not() && currentId == pointerInfo.id.value -> currentId = -1
              pointerInfo.id.value != currentId && currentId != -1L -> pointerInfo.consume()
              else -> Unit
            }
          }
        }
      }
    }
  }