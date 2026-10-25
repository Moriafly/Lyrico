package com.lonx.lyrico.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lonx.lyrico.data.model.log.AppLogLevel
import com.lonx.lyrico.data.model.log.AppLogType
import com.lonx.lyrico.data.model.entity.SourcePluginEntity
import com.lonx.lyrico.data.model.plugin.PluginSourceType
import com.lonx.lyrico.data.repository.AppLogRepository
import com.lonx.lyrico.data.repository.SettingsRepository
import com.lonx.lyrico.data.repository.SourcePluginRepository
import com.lonx.lyrico.plugin.source.PluginSearchSourceManager
import com.lonx.lyrico.plugin.source.PluginImportSession
import com.lonx.lyrico.plugin.source.PluginVersionConflict
import com.lonx.lyrico.plugin.source.SourcePluginInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import com.lonx.lyrico.plugin.i18n.PluginLocales
import com.lonx.lyrico.plugin.i18n.PluginStrings
import com.lonx.lyrico.data.model.plugin.PluginManifest
import kotlinx.serialization.json.Json

sealed interface PluginMessage {
    enum class Status : PluginMessage {
        PACKAGE_SCANNED, INSTALLED, DELETED, NAME_SAVED, NO_PENDING_IMPORT, NO_SELECTION,
        BUSY, WORKING, TIMED_OUT, CANNOT_OPEN_FILE, NO_INSTALLABLE_PLUGIN, CANNOT_DELETE_FILES
    }

    data class Failure(val reason: String) : PluginMessage
}

data class PluginUiState(
    val isBusy: Boolean = false,
    val message: PluginMessage? = null,
    val messageVersion: Long = 0,
    val smokeResult: String = "",
    val pendingImport: PluginImportSession? = null,
    val selectedImportRoots: Set<String> = emptySet()
)

class PluginViewModel(
    private val repository: SourcePluginRepository,
    private val settingsRepository: SettingsRepository,
    private val installer: SourcePluginInstaller,
    private val pluginManager: PluginSearchSourceManager,
    private val appLogRepository: AppLogRepository
) : ViewModel() {
    private val manifestJson = Json { ignoreUnknownKeys = true }
    val plugins: StateFlow<List<SourcePluginEntity>> =
        repository.observePlugins()
            .combine(PluginLocales.preferences) { plugins, locales ->
                plugins.map { plugin ->
                    runCatching {
                        val root = File(plugin.pluginDir)
                        val manifest = manifestJson.decodeFromString<PluginManifest>(File(root, "manifest.json").readText())
                        val localized = PluginStrings.load(root, manifest).snapshot(locales).localize(manifest)
                        plugin.copy(name = localized.name, description = localized.description)
                    }.getOrElse { plugin }
                }
            }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(PluginUiState())
    val uiState: StateFlow<PluginUiState> = _uiState.asStateFlow()
    private val actionMutex = Mutex()

    init {
        viewModelScope.launch {
            installer.synchronizeInstalledPluginManifestMetadata()
        }
    }

    fun importPlugin(context: Context, uri: Uri) {
        runBusy(PluginMessage.Status.PACKAGE_SCANNED) {
            _uiState.value.pendingImport?.let { installer.discardImport(it) }
            val installRoot = File(context.filesDir, "plugins/sources")
            val input = context.contentResolver.openInputStream(uri)
                ?: throw PluginActionFailure(PluginMessage.Status.CANNOT_OPEN_FILE)
            input.use {
                val session = installer.prepareImport(
                    input = it,
                    installRoot = installRoot
                )
                if (session.candidates.isEmpty()) {
                    installer.discardImport(session)
                    val failureReason = session.failed.firstOrNull()?.reason ?: "No installable plugin found"
                    logPluginError("Import failed", failureReason, session.failed.joinToString("\n") { "${it.rootPath}: ${it.reason}" })
                    throw PluginActionFailure(PluginMessage.Status.NO_INSTALLABLE_PLUGIN, failureReason)
                }
                _uiState.update { state ->
                    state.copy(
                        pendingImport = session,
                        selectedImportRoots = session.candidates
                            .filter { candidate -> candidate.versionConflict != PluginVersionConflict.DOWNGRADE }
                            .mapTo(mutableSetOf()) { candidate -> candidate.relativeRootInArchive }
                    )
                }
            }
        }
    }

    fun setImportCandidateSelected(rootPath: String, selected: Boolean) {
        _uiState.update { state ->
            val next = state.selectedImportRoots.toMutableSet()
            if (selected) {
                next += rootPath
            } else {
                next -= rootPath
            }
            state.copy(selectedImportRoots = next)
        }
    }

    fun installPendingImport() {
        val session = _uiState.value.pendingImport ?: run {
            publishMessage(PluginMessage.Status.NO_PENDING_IMPORT)
            return
        }

        val selectedRoots = _uiState.value.selectedImportRoots
        if (selectedRoots.isEmpty()) {
            publishMessage(PluginMessage.Status.NO_SELECTION)
            return
        }

        val allowDowngrade = session.candidates.any { candidate ->
            candidate.relativeRootInArchive in selectedRoots &&
                    candidate.versionConflict == PluginVersionConflict.DOWNGRADE
        }

        runBusy(PluginMessage.Status.INSTALLED) {
            val result = installer.installPrepared(
                session = session,
                enabled = true,
                selectedRoots = selectedRoots,
                allowDowngrade = allowDowngrade
            )

            if (result.installed.isEmpty()) {
                val failureReason = result.failed.firstOrNull()?.reason ?: "No installable plugin found"
                logPluginError(
                    message = "Install failed",
                    detail = failureReason,
                    fullDetail = result.failed.joinToString("\n") { "${it.rootPath}: ${it.reason}" }
                )
                throw PluginActionFailure(PluginMessage.Status.NO_INSTALLABLE_PLUGIN, failureReason)
            }
        }
    }
    fun discardPendingImportFiles() {
        _uiState.value.pendingImport?.let { installer.discardImport(it) }
    }
    fun clearPendingImport() {
        _uiState.update {
            it.copy(
                pendingImport = null,
                selectedImportRoots = emptySet()
            )
        }
    }

    fun dismissPendingImport() {
        discardPendingImportFiles()
        clearPendingImport()
    }

    fun setEnabled(id: String, sourceType: PluginSourceType, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(id, sourceType, enabled)
        }
    }

    fun setPluginOrder(
        plugins: List<SourcePluginEntity>,
        sourceType: PluginSourceType
    ) {
        viewModelScope.launch {
            repository.updateSortOrders(
                ids = plugins.map { it.id },
                sourceType = sourceType
            )
        }
    }

    fun setCustomName(id: String, name: String) {
        viewModelScope.launch {
            repository.updateCustomName(id, name)
            publishMessage(PluginMessage.Status.NAME_SAVED)
        }
    }


    private fun runBusy(successMessage: PluginMessage.Status, block: suspend () -> Unit) {
        viewModelScope.launch {
            if (actionMutex.isLocked) {
                publishMessage(PluginMessage.Status.BUSY)
                return@launch
            }
            _uiState.update {
                it.copy(
                    isBusy = true,
                    message = PluginMessage.Status.WORKING,
                    messageVersion = System.nanoTime()
                )
            }
            try {
                actionMutex.withLock {
                    withTimeout(ACTION_TIMEOUT_MS) {
                        withContext(Dispatchers.IO) {
                            block()
                        }
                    }
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        message = successMessage,
                        messageVersion = System.nanoTime()
                    )
                }
            } catch (e: TimeoutCancellationException) {
                logPluginError(
                    message = "Plugin action timed out",
                    detail = "Timed out after ${ACTION_TIMEOUT_MS / 1000}s",
                    fullDetail = e.stackTraceToString()
                )
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        message = PluginMessage.Status.TIMED_OUT,
                        messageVersion = System.nanoTime(),
                        smokeResult = "Timed out after ${ACTION_TIMEOUT_MS / 1000}s"
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val message = e.message ?: e.javaClass.simpleName
                logPluginError(
                    message = "Plugin action failed",
                    detail = message,
                    fullDetail = e.stackTraceToString()
                )
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        message = if (e is PluginActionFailure) e.feedback else PluginMessage.Failure(message),
                        messageVersion = System.nanoTime(),
                        smokeResult = message
                    )
                }
            }
        }
    }

    private fun publishMessage(message: PluginMessage) {
        _uiState.update {
            it.copy(
                message = message,
                messageVersion = System.nanoTime()
            )
        }
    }

    fun uninstallPlugin(id: String) {
        runBusy(PluginMessage.Status.DELETED) {
            val plugin = repository.getPlugin(id)
            if (plugin != null) {
                pluginManager.invalidate(plugin.id)
                settingsRepository.removePluginSettings(plugin.id)
                val pluginDir = File(plugin.pluginDir)
                if (pluginDir.exists()) {
                    if (!pluginDir.deleteRecursively()) {
                        throw PluginActionFailure(
                            PluginMessage.Status.CANNOT_DELETE_FILES,
                            "Failed to remove plugin files: ${pluginDir.absolutePath}",
                        )
                    }
                }
                repository.uninstallPlugin(id)
            }
        }
    }

    private suspend fun logPluginError(message: String, detail: String, fullDetail: String? = null) {
        try {
            appLogRepository.log(
                level = AppLogLevel.ERROR,
                type = AppLogType.PLUGIN,
                tag = TAG,
                message = message,
                detail = fullDetail ?: detail
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write plugin log", e)
        }
    }

    private companion object {
        const val ACTION_TIMEOUT_MS = 30_000L
        const val TAG = "PluginViewModel"
    }
}

private class PluginActionFailure(val feedback: PluginMessage.Status, detail: String? = null) :
    IllegalStateException(detail)
