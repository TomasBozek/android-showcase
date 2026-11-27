package com.example.androidproject1.feature.inventory.presentation.inventoryeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.androidproject1.core.ui.component.choice.CheckState
import com.example.androidproject1.feature.inventory.api.InventoryEditorDestination
import com.example.androidproject1.feature.inventory.domain.InventoryRepository
import com.example.androidproject1.feature.inventory.domain.ItemCategory
import com.example.androidproject1.feature.inventory.domain.ItemCondition
import com.example.androidproject1.feature.inventory.domain.ItemTag
import com.example.androidproject1.feature.inventory.presentation.R
import com.example.androidproject1.service.core.domain.FormDrafts
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.form.ALERT_ID_DISCARD
import com.example.androidproject1.service.core.ui.form.Form
import com.example.androidproject1.service.core.ui.form.discardAlert
import com.example.androidproject1.service.core.ui.state.clearAlert
import com.example.androidproject1.service.core.ui.state.setAlert
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.scanner.CodeScanner
import com.example.androidproject1.service.scanner.ScanResult
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import com.example.androidproject1.service.core.ui.R as CoreR

class InventoryEditorViewModel(
    logger: Logger,
    // The route key, handed in by the destination. Available in `init`, and it comes back with
    // the back stack entry after process death.
    private val args: InventoryEditorDestination,
    private val inventoryRepository: InventoryRepository,
    private val codeScanner: CodeScanner,
    savedStateHandle: SavedStateHandle,
    private val formDrafts: FormDrafts,
) : BaseViewModel<InventoryEditorState, InventoryEditorEvent, InventoryEditorNavigation>(
    // The form draws at once; if the id names an item, its fields arrive over the blank ones.
    initialState = InventoryEditorState(itemId = args.itemId, loading = true),
    logger = logger.withTag("InventoryEditorViewModel"),
    savedStateHandle = savedStateHandle,
) {

    // The form as one snapshot, written on every edit: `null` until the first, and once there it
    // wins over the stored item, which still says what the form started from. Twice — in saved
    // state, which a process death hands back as it was, and in `formDrafts` under [slot],
    // which a relaunch finds and says so.
    private var savedDraft: String? by saved(KEY_DRAFT, null)
    private val restored: Boolean = restore()

    /**
     * Where this form's draft waits between launches, known once the load says what the id is:
     * an item's own slot, or the one slot every new item shares — a new item gets a new id each
     * time, so the next Add is what finds a half-made one.
     */
    private var slot: String? = null

    /** Saved or discarded: nothing typed is waiting any more, so nothing is written after it. */
    private var finished = false

    init {
        load()
    }

    override fun onSystemEvent(event: SystemEvent) {
        if (event is SystemEvent.AlertResult.Confirmed && event.id == ALERT_ID_DISCARD) {
            uiState.clearAlert()
            finished = true
            forget()
            navigate(InventoryEditorNavigation.NavigateUp)
            return
        }
        if (event is SystemEvent.SnackbarAction && event.id == SNACKBAR_ID_DRAFT) {
            updateData {
                original?.let { InventoryEditorState.forItem(it, step = InventoryEditorState.STEP_BASICS) }
                    ?: InventoryEditorState(itemId = itemId)
            }
            forget()
            return
        }
        super.onSystemEvent(event)
    }

    override fun onUiEvent(event: InventoryEditorEvent) {
        handle(event)
        remember()
    }

    private fun handle(event: InventoryEditorEvent) {
        when (event) {
            is InventoryEditorEvent.NameChanged -> updateData { copy(name = name.changed(event.name)) }
            is InventoryEditorEvent.CategorySelected -> updateData { copy(category = event.category) }
            is InventoryEditorEvent.ConditionSelected -> updateData { copy(condition = event.condition) }
            is InventoryEditorEvent.AcquiredOnChanged -> updateData { copy(acquiredOn = event.date) }
            is InventoryEditorEvent.QuantityChanged -> updateData { copy(quantity = event.quantity) }
            is InventoryEditorEvent.PriceChanged -> updateData {
                copy(priceMinor = InventoryEditorState.priceMinorFor(event.fraction))
            }
            is InventoryEditorEvent.InsuredChanged -> updateData { copy(insured = event.insured) }
            is InventoryEditorEvent.TagChanged -> updateData {
                copy(tags = if (event.checked) tags + event.tag else tags - event.tag)
            }
            // From "some", a tap on the master selects the rest — never clears what was chosen.
            is InventoryEditorEvent.AllTagsChanged -> updateData {
                val all = event.checked || allTagsState == CheckState.Indeterminate
                copy(tags = if (all) ItemTag.entries.toSet() else emptySet())
            }
            is InventoryEditorEvent.OwnerSelected -> updateData { copy(owner = event.owner) }
            is InventoryEditorEvent.ImageUrlChanged -> updateData { copy(imageUrl = event.imageUrl) }
            is InventoryEditorEvent.NotesChanged -> updateData { copy(notes = event.notes) }
            is InventoryEditorEvent.BarcodeChanged -> updateData { copy(barcode = event.barcode) }
            InventoryEditorEvent.ScanTapped -> scan()
            InventoryEditorEvent.NextClicked -> next()
            InventoryEditorEvent.NavigateUpClicked -> back()
            InventoryEditorEvent.BackRequested -> uiState.setAlert(discardAlert())
        }
    }

    // Silent: an id that names nothing is the "new item" case, not a failure — and a read error
    // leaves a blank form, which is still a form.
    private fun load() = execute(
        errorDisplay = ErrorDisplay.Silent,
        action = { inventoryRepository.getItem(args.itemId) },
        onData = { item ->
            updateData {
                when {
                    restored -> copy(loading = false, original = item)
                    item == null -> copy(loading = false)
                    else -> InventoryEditorState.forItem(item, step = step)
                }
            }
            slot = if (item == null) SLOT_NEW else SLOT_ITEM_PREFIX + item.id
            if (!restored) resumeStoredDraft()
        },
    )

    /**
     * Reads one code and puts it in the field, where it can be corrected like anything typed.
     * Backing out changes nothing; a scanner that cannot be shown — no Play services — says so and
     * leaves the field for typing, which is always there.
     */
    private fun scan() {
        viewModelScope.launch {
            when (val result = codeScanner.scan()) {
                is ScanResult.Code -> {
                    updateData { copy(barcode = result.value) }
                    remember()
                }
                ScanResult.Cancelled -> Unit
                ScanResult.Unavailable -> showSnackbar(R.string.inventory_editor_scan_unavailable.toUiText())
            }
        }
    }

    /**
     * Next stays enabled on an invalid step on purpose: pressing it touches every field,
     * which is what reveals the errors, and the first one is repeated in a snackbar for anyone
     * who is looking at the button rather than the field.
     */
    private fun next() {
        val state = uiState.value.data ?: return
        if (!state.canContinue) {
            val (name) = Form.touchAll(state.name)
            val revealed = state.copy(name = name)
            updateData { revealed }
            revealed.firstError?.let { showSnackbar(it) }
            return
        }
        if (state.step == InventoryEditorState.STEP_REVIEW) {
            save(state)
        } else {
            updateData { copy(step = step + 1) }
        }
    }

    private fun back() {
        val state = uiState.value.data ?: return
        if (state.step > InventoryEditorState.STEP_BASICS) {
            updateData { copy(step = step - 1) }
        } else {
            navigate(InventoryEditorNavigation.NavigateUp)
        }
    }

    // The id is the one the route carried, so an edit replaces the row rather than adding one.
    private fun save(state: InventoryEditorState) = execute(
        loading = overlay(),
        action = { inventoryRepository.saveItem(state.toItem()) },
        onData = {
            finished = true
            forget()
            navigate(InventoryEditorNavigation.Saved)
        },
    )

    // A form with nothing changed is no draft: opening an item and leaving must not leave one.
    private fun remember() {
        val state = uiState.value.data ?: return
        if (state.loading || finished) return
        if (!state.isDirty) {
            forget()
            return
        }
        val encoded = Json.encodeToString(Draft.serializer(), Draft.of(state))
        savedDraft = encoded
        slot?.let { key -> persist { formDrafts.write(key, encoded) } }
    }

    private fun forget() {
        savedDraft = null
        slot?.let { key -> persist { formDrafts.clear(key) } }
    }

    private fun resumeStoredDraft() {
        val key = slot ?: return
        viewModelScope.launch {
            val stored = formDrafts.read(key)?.let(::decode) ?: return@launch
            updateData { stored.applyTo(this) }
            showSnackbar(
                message = CoreR.string.core_draft_restored.toUiText(),
                actionLabel = CoreR.string.core_action_discard.toUiText(),
                id = SNACKBAR_ID_DRAFT,
            )
        }
    }

    // Not cancellable: the editor closes as it saves or discards, and a write cut off there would
    // bring the draft back on the next launch.
    private fun persist(write: suspend () -> Unit) {
        viewModelScope.launch { withContext(NonCancellable) { write() } }
    }

    private fun decode(encoded: String): Draft? =
        runCatching { Json.decodeFromString(Draft.serializer(), encoded) }.getOrNull()

    private fun restore(): Boolean {
        val draft = savedDraft?.let(::decode) ?: return false
        updateData { draft.applyTo(this) }
        return true
    }

    /** What the user has put into the form, and the step they were on. */
    @Serializable
    private data class Draft(
        val step: Int,
        val name: String,
        val category: ItemCategory,
        val condition: ItemCondition,
        val acquiredOnEpochDay: Long?,
        val quantity: Int,
        val priceMinor: Long,
        val insured: Boolean,
        val tags: Set<ItemTag>,
        val owner: String,
        val imageUrl: String,
        val notes: String,
        val barcode: String,
    ) {

        fun applyTo(state: InventoryEditorState) = state.copy(
            step = step,
            name = state.name.changed(name),
            category = category,
            condition = condition,
            acquiredOn = acquiredOnEpochDay?.let(LocalDate::ofEpochDay),
            quantity = quantity,
            priceMinor = priceMinor,
            insured = insured,
            tags = tags,
            owner = owner,
            imageUrl = imageUrl,
            notes = notes,
            barcode = barcode,
        )

        companion object {

            fun of(state: InventoryEditorState) = Draft(
                step = state.step,
                name = state.name.value,
                category = state.category,
                condition = state.condition,
                acquiredOnEpochDay = state.acquiredOn?.toEpochDay(),
                quantity = state.quantity,
                priceMinor = state.priceMinor,
                insured = state.insured,
                tags = state.tags,
                owner = state.owner,
                imageUrl = state.imageUrl,
                notes = state.notes,
                barcode = state.barcode,
            )
        }
    }

    private companion object {

        const val KEY_DRAFT = "inventory_editor_draft"
        const val SNACKBAR_ID_DRAFT = "inventory_editor_draft_restored"
        const val SLOT_NEW = "inventory_editor_new"
        const val SLOT_ITEM_PREFIX = "inventory_editor_item_"
    }
}
