package com.martypaz.myq

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import com.martypaz.myq.ui.HomeScreen
import com.martypaz.myq.ui.HomeViewModel
import com.martypaz.myq.ui.theme.MyQTheme

class MainActivity : ComponentActivity() {

    private val permissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            // The line-up was read before the grant arrived, so drop the empty
            // answer rather than waiting for the next launch to notice.
            if (granted[READ_TV_LISTINGS] == true) {
                (application as MyQApp).tvLineup.invalidate()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissions.launch(
            buildList {
                // Reading the tuner's line-up is what lets MyQ show the region
                // this box receives, and switch channel from a programme.
                add(READ_TV_LISTINGS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray(),
        )

        setContent {
            MyQTheme {
                val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(application))
                HomeScreen(viewModel)
            }
        }
    }

    private companion object {
        const val READ_TV_LISTINGS = "android.permission.READ_TV_LISTINGS"
    }
}
