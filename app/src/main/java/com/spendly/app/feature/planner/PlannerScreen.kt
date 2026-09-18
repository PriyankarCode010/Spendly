package com.spendly.app.feature.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The "required monthly saving toward a target date" calculation - the core
 * of deal planning - lives on each Goal card (see GoalsScreen). This screen
 * just points there rather than duplicating that logic in a second place.
 */
@Composable
fun PlannerScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Filled.Savings, contentDescription = null)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Planning a purchase?", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            "Create it as a Goal with a target date - Spendly will work out how much to save each month to hit it on time.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
