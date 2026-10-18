package com.wwwescape.devicehousekeeping.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wwwescape.devicehousekeeping.R
import com.wwwescape.devicehousekeeping.data.scan.JunkCategory
import com.wwwescape.devicehousekeeping.data.storage.StorageAccess
import com.wwwescape.devicehousekeeping.ui.CleanerPhase
import com.wwwescape.devicehousekeeping.ui.HousekeepingViewModel
import com.wwwescape.devicehousekeeping.ui.components.PermissionCard
import com.wwwescape.devicehousekeeping.ui.components.ReadableWidth
import com.wwwescape.devicehousekeeping.ui.components.StorageHero

private enum class Stage { IDLE, SCANNING, RESULTS, CLEANING, DONE }

private val CleanerPhase.stage: Stage
    get() = when (this) {
        CleanerPhase.Idle -> Stage.IDLE
        is CleanerPhase.Scanning -> Stage.SCANNING
        is CleanerPhase.Results -> Stage.RESULTS
        is CleanerPhase.Cleaning -> Stage.CLEANING
        is CleanerPhase.Done -> Stage.DONE
    }

@Composable
fun HomeScreen(
    viewModel: HousekeepingViewModel,
    onOpenCategory: (JunkCategory) -> Unit,
    onOpenAppCaches: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val storage by viewModel.storage.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val appCaches by viewModel.appCaches.collectAsStateWithLifecycle()

    // Access is granted on system screens, so re-check whenever we come back.
    var hasAccess by remember { mutableStateOf(StorageAccess.hasStorageAccess(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasAccess = StorageAccess.hasStorageAccess(context)
        viewModel.refreshStorage()
        viewModel.loadAppCaches()
    }
    val legacyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasAccess = StorageAccess.hasStorageAccess(context)
    }
    val requestAccess = {
        if (StorageAccess.usesAllFilesAccess) {
            StorageAccess.launchFirst(context, StorageAccess.allFilesAccessIntents(context))
        } else {
            legacyLauncher.launch(StorageAccess.legacyPermissions)
        }
    }

    val results = (phase as? CleanerPhase.Results)?.result
    val selectedItems = remember(results, selection) { results?.items?.filter { it.path in selection }.orEmpty() }
    val selectedBytes = selectedItems.sumOf { it.sizeBytes }
    var confirming by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    LaunchedEffect(phase.stage) {
        if (phase.stage != Stage.RESULTS) scroll.animateScrollTo(0)
    }

    Box(modifier = modifier.fillMaxSize()) {
        ReadableWidth {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StorageHero(storage = storage, reclaimableBytes = selectedBytes)

                AnimatedVisibility(visible = !hasAccess) {
                    PermissionCard(
                        icon = Icons.Rounded.FolderOpen,
                        title = stringResource(R.string.permission_storage_title),
                        body = stringResource(
                            if (StorageAccess.usesAllFilesAccess) R.string.permission_storage_body_all_files else R.string.permission_storage_body_legacy,
                        ),
                        action = stringResource(R.string.action_grant),
                        onClick = requestAccess,
                    )
                }

                AnimatedContent(
                    targetState = phase,
                    contentKey = { it.stage },
                    transitionSpec = {
                        (fadeIn(tween(350, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(350, delayMillis = 90)))
                            .togetherWith(fadeOut(tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)))
                    },
                    label = "stage",
                ) { current ->
                    when (current) {
                        CleanerPhase.Idle -> IdleStage(
                            history = history,
                            onScan = { if (hasAccess) viewModel.startScan() else requestAccess() },
                        )
                        is CleanerPhase.Scanning -> ScanningStage(progress = current.progress, onCancel = viewModel::cancelScan)
                        is CleanerPhase.Results -> ResultsStage(
                            result = current.result,
                            selection = selection,
                            onToggleCategory = viewModel::setCategorySelected,
                            onOpenCategory = onOpenCategory,
                            onOpenAppCaches = onOpenAppCaches,
                            onRescan = viewModel::startScan,
                            onDismiss = viewModel::finish,
                        )
                        is CleanerPhase.Cleaning -> CleaningStage(progress = current.progress)
                        is CleanerPhase.Done -> DoneStage(report = current.report, onFinish = viewModel::finish)
                    }
                }

                if (phase.stage == Stage.IDLE || phase.stage == Stage.DONE) {
                    AppCachesCard(state = appCaches, onClick = onOpenAppCaches)
                    SafetyPromiseCard()
                }

                // Room for the floating Clean bar, so the last card can scroll above it.
                if (phase.stage == Stage.RESULTS) Spacer(modifier = Modifier.height(112.dp))
            }
        }

        AnimatedVisibility(
            visible = phase.stage == Stage.RESULTS && !results?.items.isNullOrEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)) { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            ReadableWidth {
                CleanBar(
                    selectedBytes = selectedBytes,
                    selectedCount = selectedItems.size,
                    onClean = { if (settings.cleaning.confirmBeforeCleaning) confirming = true else viewModel.clean() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (confirming) {
        ConfirmCleanDialog(
            items = selectedItems,
            onConfirm = {
                confirming = false
                viewModel.clean()
            },
            onDismiss = { confirming = false },
        )
    }
}
