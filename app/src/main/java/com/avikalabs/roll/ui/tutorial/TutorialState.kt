package com.avikalabs.roll.ui.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

data class TutorialStep(
    val id: String,
    val targetId: String,
    val textResId: Int,
    val customText: String? = null
)

data class TutorialTipSet(
    val id: String,
    val steps: List<TutorialStep>
)

class TutorialState {
    private val _targetBounds = mutableStateMapOf<String, Rect>()
    val targetBounds: Map<String, Rect> get() = _targetBounds

    var currentTipSet by mutableStateOf<TutorialTipSet?>(null)
    var currentStepIndex by mutableStateOf(0)

    fun registerTarget(id: String, bounds: Rect) {
        _targetBounds[id] = bounds
    }

    fun unregisterTarget(id: String) {
        _targetBounds.remove(id)
    }

    var isDialogActive by mutableStateOf(false)

    val currentStep: TutorialStep?
        get() = currentTipSet?.steps?.getOrNull(currentStepIndex)

    fun findBounds(targetId: String): Rect? {
        val exact = _targetBounds[targetId]
        if (exact != null && exact.width > 0 && exact.height > 0) return exact
        if (targetId.endsWith("*")) {
            val prefix = targetId.dropLast(1)
            val match = _targetBounds.entries.firstOrNull { it.key.startsWith(prefix) && it.value.width > 0 && it.value.height > 0 }
            if (match != null) return match.value
        }
        return null
    }

    val currentTargetBounds: Rect?
        get() = currentStep?.let { findBounds(it.targetId) }

    fun hasTarget(targetId: String): Boolean {
        return findBounds(targetId) != null
    }

    fun nextStep() {
        val tipSet = currentTipSet ?: return
        var nextIdx = currentStepIndex + 1
        // Skip steps whose target is not on screen
        while (nextIdx < tipSet.steps.size) {
            val nextStep = tipSet.steps[nextIdx]
            if (hasTarget(nextStep.targetId)) {
                break
            }
            nextIdx++
        }
        if (nextIdx < tipSet.steps.size) {
            currentStepIndex = nextIdx
        } else {
            currentTipSet = null
            currentStepIndex = 0
        }
    }

    fun dismiss() {
        currentTipSet = null
        currentStepIndex = 0
    }
}

val LocalTutorialState: ProvidableCompositionLocal<TutorialState> = compositionLocalOf {
    TutorialState()
}

fun Modifier.tutorialTarget(vararg ids: String): Modifier = this.then(
    Modifier.onGloballyPositioned { coordinates ->
        if (coordinates.isAttached) {
            val bounds = coordinates.boundsInRoot()
            for (id in ids) {
                TutorialTargetRegistrar.register(id, bounds)
            }
        }
    }
)

object TutorialTargetRegistrar {
    var state: TutorialState? = null

    fun register(id: String, bounds: Rect) {
        state?.registerTarget(id, bounds)
    }
}
