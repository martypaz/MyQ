package com.martypaz.freeviewguide

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.martypaz.freeviewguide.ui.HomeScreen
import com.martypaz.freeviewguide.ui.HomeViewModel
import com.martypaz.freeviewguide.ui.theme.FreeviewGuideTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            FreeviewGuideTheme {
                val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(application))
                HomeScreen(viewModel)
            }
        }
    }
}
