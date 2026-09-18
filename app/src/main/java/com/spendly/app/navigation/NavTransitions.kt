package com.spendly.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private const val DURATION_MS = 280

val forwardEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    slideInHorizontally(animationSpec = tween(DURATION_MS), initialOffsetX = { it / 4 }) +
        fadeIn(animationSpec = tween(DURATION_MS))
}

val forwardExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    slideOutHorizontally(animationSpec = tween(DURATION_MS), targetOffsetX = { -it / 4 }) +
        fadeOut(animationSpec = tween(DURATION_MS))
}

val backEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    slideInHorizontally(animationSpec = tween(DURATION_MS), initialOffsetX = { -it / 4 }) +
        fadeIn(animationSpec = tween(DURATION_MS))
}

val backExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    slideOutHorizontally(animationSpec = tween(DURATION_MS), targetOffsetX = { it / 4 }) +
        fadeOut(animationSpec = tween(DURATION_MS))
}

val crossfadeEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(DURATION_MS))
}

val crossfadeExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(DURATION_MS))
}
