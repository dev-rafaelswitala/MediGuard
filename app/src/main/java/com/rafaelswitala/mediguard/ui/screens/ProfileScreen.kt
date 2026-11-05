package com.rafaelswitala.mediguard.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.rafaelswitala.mediguard.domain.model.ContactCategory
import com.rafaelswitala.mediguard.domain.model.DocumentCategory
import com.rafaelswitala.mediguard.domain.model.HealthConditionType
import com.rafaelswitala.mediguard.ui.components.MediGuardBottomBar
import com.rafaelswitala.mediguard.ui.components.MediGuardTab
import com.rafaelswitala.mediguard.ui.components.StandardSectionCard
import com.rafaelswitala.mediguard.ui.localization.LocalAppStrings
import com.rafaelswitala.mediguard.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import java.util.Locale

private enum class ProfileSection { PERSONAL, GENERAL, ALLERGIES, HEALTH, INSURANCE, CONTACTS, DOCUMENTS }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateHome: () -> Unit = {},
    onNavigateMedications: () -> Unit = {},
    onNavigateHistory: () -> Unit = {},
    onNavigateMore: () -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val profile by viewModel.profile.collectAsState()
    val allergies by viewModel.allergies.collectAsState()
    val conditions by viewModel.conditions.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val uiState by viewModel.uiState
    var draft by remember(profile) { mutableStateOf(profile) }
    var expanded by remember { mutableStateOf<ProfileSection?>(null) }
    var editing by remember { mutableStateOf<ProfileSection?>(null) }
    var allergyName by remember { mutableStateOf("") }
    var chronicName by remember { mutableStateOf("") }
    var implantName by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactNotes by remember { mutableStateOf("") }
    var contactCategory by remember { mutableStateOf(ContactCategory.DOCTOR) }
    var contactEmergency by remember { mutableStateOf(false) }
    var documentTitle by remember { mutableStateOf("") }
    var documentCategory by remember { mutableStateOf(DocumentCategory.OTHER) }

    LaunchedEffect(Unit) {
        viewModel.seedSampleDocumentsIfEmpty()
    }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        val message = when (uiState.successMessage) {
            "document_imported" -> strings.documentImported
            else -> uiState.successMessage ?: uiState.errorMessage
        }
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importDocument(uri, documentTitle.ifBlank { "PDF" }, documentCategory, null)
            documentTitle = ""
        }
    }

    fun save(section: ProfileSection) {
        val message = when (section) {
            ProfileSection.PERSONAL -> strings.personalSaved
            ProfileSection.GENERAL -> strings.generalSaved
            ProfileSection.HEALTH -> strings.healthSaved
            ProfileSection.INSURANCE -> strings.insuranceSaved
            else -> strings.profileSectionSaved
        }
        viewModel.saveProfile(draft, message)
        editing = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.profile) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            MediGuardBottomBar(
                selected = MediGuardTab.PROFILE,
                onHome = onNavigateHome,
                onMedications = onNavigateMedications,
                onHistory = onNavigateHistory,
                onProfile = {},
                onMore = onNavigateMore
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(strings.profileDisclaimer, style = MaterialTheme.typography.bodySmall)

            ProfileAccordion(strings.sectionPersonal, ProfileSection.PERSONAL, expanded, { expanded = it }) {
                SectionActions(editing == ProfileSection.PERSONAL, strings.edit, strings.save, strings.cancel,
                    { editing = ProfileSection.PERSONAL }, { save(ProfileSection.PERSONAL) }) {
                    draft = profile; editing = null
                }
                if (editing == ProfileSection.PERSONAL) {
                    ProfileField(draft.fullName, strings.fullName) { draft = draft.copy(fullName = it.nullIfBlank()) }
                    ProfileField(draft.addressLine, strings.address) { draft = draft.copy(addressLine = it.nullIfBlank()) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileField(draft.postalCode, strings.postalCode, Modifier.weight(1f)) { draft = draft.copy(postalCode = it.nullIfBlank()) }
                        ProfileField(draft.city, strings.city, Modifier.weight(1f)) { draft = draft.copy(city = it.nullIfBlank()) }
                    }
                    ProfileField(draft.phone, strings.phone) { draft = draft.copy(phone = it.nullIfBlank()) }
                    ProfileField(draft.email, strings.email) { draft = draft.copy(email = it.nullIfBlank()) }
                } else {
                    ProfileValue(strings.fullName, profile.fullName, strings.notSet)
                    ProfileValue(strings.address, profile.addressLine, strings.notSet)
                    ProfileValue(strings.postalCode, profile.postalCode, strings.notSet)
                    ProfileValue(strings.city, profile.city, strings.notSet)
                    ProfileValue(strings.phone, profile.phone, strings.notSet)
                    ProfileValue(strings.email, profile.email, strings.notSet)
                }
            }

            ProfileAccordion(strings.sectionGeneralDetails, ProfileSection.GENERAL, expanded, { expanded = it }) {
                SectionActions(editing == ProfileSection.GENERAL, strings.edit, strings.save, strings.cancel,
                    { editing = ProfileSection.GENERAL }, { save(ProfileSection.GENERAL) }) { draft = profile; editing = null }
                if (editing == ProfileSection.GENERAL) {
                    BloodTypeField(draft.bloodType, strings.bloodType, strings.notSet) { draft = draft.copy(bloodType = it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(draft.heightCm?.toString(), strings.heightCm, Modifier.weight(1f)) { draft = draft.copy(heightCm = it.toDoubleOrNull()) }
                        NumberField(draft.weightKg?.toString(), strings.weightKg, Modifier.weight(1f)) { draft = draft.copy(weightKg = it.toDoubleOrNull()) }
                    }
                    ExpandingNotesField(draft.otherNotes.orEmpty(), { draft = draft.copy(otherNotes = it.nullIfBlank()) }, strings.otherNotes)
                    BooleanChoice(strings.organDonor, draft.organDonor, strings.yes, strings.no) { draft = draft.copy(organDonor = it) }
                    ProfileField(draft.preferredPharmacy, strings.preferredPharmacy) { draft = draft.copy(preferredPharmacy = it.nullIfBlank()) }
                    ProfileField(draft.preferredPharmacyAddress, strings.pharmacyAddress) { draft = draft.copy(preferredPharmacyAddress = it.nullIfBlank()) }
                    ProfileField(draft.preferredPharmacyPhone, strings.pharmacyPhone) { draft = draft.copy(preferredPharmacyPhone = it.nullIfBlank()) }
                } else {
                    ProfileValue(strings.bloodType, profile.bloodType, strings.notSet)
                    ProfileValue(strings.heightCm, profile.heightCm?.toString(), strings.notSet)
                    ProfileValue(strings.weightKg, profile.weightKg?.toString(), strings.notSet)
                    ProfileValue(strings.bmi, profile.bmi()?.let { String.format(Locale.getDefault(), "%.1f", it) }, strings.notSet)
                    ProfileValue(strings.otherNotes, profile.otherNotes, strings.notSet)
                    ProfileValue(strings.organDonor, profile.organDonor?.let { if (it) strings.yes else strings.no }, strings.notSet)
                    ProfileValue(strings.preferredPharmacy, profile.preferredPharmacy, strings.notSet)
                    ProfileValue(strings.pharmacyAddress, profile.preferredPharmacyAddress, strings.notSet)
                    ProfileValue(strings.pharmacyPhone, profile.preferredPharmacyPhone, strings.notSet)
                }
            }

            ProfileAccordion(strings.sectionAllergies, ProfileSection.ALLERGIES, expanded, { expanded = it }) {
                EditToggle(editing == ProfileSection.ALLERGIES, strings.edit, strings.done) {
                    editing = if (editing == ProfileSection.ALLERGIES) null else ProfileSection.ALLERGIES
                }
                if (allergies.isEmpty()) Text(strings.noAllergies)
                allergies.forEach { item ->
                    ListRow(item.name, editing == ProfileSection.ALLERGIES, strings.delete) { viewModel.deleteAllergy(item) }
                }
                if (editing == ProfileSection.ALLERGIES) {
                    ProfileField(allergyName, strings.allergyName) { allergyName = it }
                    Button(onClick = { viewModel.addAllergy(allergyName, null, null); allergyName = "" }, enabled = allergyName.isNotBlank()) {
                        Text(strings.addAllergy)
                    }
                }
            }

            ProfileAccordion(strings.sectionHealth, ProfileSection.HEALTH, expanded, { expanded = it }) {
                SectionActions(editing == ProfileSection.HEALTH, strings.edit, strings.save, strings.cancel,
                    { editing = ProfileSection.HEALTH }, { save(ProfileSection.HEALTH) }) { draft = profile; editing = null }
                Text(strings.chronicConditions, style = MaterialTheme.typography.titleSmall)
                val chronic = conditions.filter { it.type == HealthConditionType.CHRONIC }
                if (chronic.isEmpty()) Text(strings.notSet) else chronic.forEach { item ->
                    ListRow(item.name, editing == ProfileSection.HEALTH, strings.delete) { viewModel.deleteCondition(item) }
                }
                if (editing == ProfileSection.HEALTH) {
                    ConditionAdder(strings.chronicConditions, chronicName, { chronicName = it }) {
                        viewModel.addCondition(chronicName, HealthConditionType.CHRONIC); chronicName = ""
                    }
                }
                Text(strings.implantsNotes, style = MaterialTheme.typography.titleSmall)
                val implants = conditions.filter { it.type == HealthConditionType.IMPLANT }
                if (implants.isEmpty()) Text(strings.notSet) else implants.forEach { item ->
                    ListRow(item.name, editing == ProfileSection.HEALTH, strings.delete) { viewModel.deleteCondition(item) }
                }
                if (editing == ProfileSection.HEALTH) {
                    ConditionAdder(strings.implantsNotes, implantName, { implantName = it }) {
                        viewModel.addCondition(implantName, HealthConditionType.IMPLANT); implantName = ""
                    }
                    SwitchRow(strings.pregnant, draft.pregnant == true) { draft = draft.copy(pregnant = it) }
                    SwitchRow(strings.smoking, draft.smoking == true) { draft = draft.copy(smoking = it, cigarettesPerDay = if (it) draft.cigarettesPerDay else null) }
                    if (draft.smoking == true) NumberField(draft.cigarettesPerDay?.toString(), strings.cigarettesPerDay) {
                        draft = draft.copy(cigarettesPerDay = it.toIntOrNull())
                    }
                    ExpandingNotesField(draft.vaccinationNotes.orEmpty(), { draft = draft.copy(vaccinationNotes = it.nullIfBlank()) }, strings.vaccinationNotes)
                    ProfileField(draft.lastCheckupNotes, strings.lastCheckupNotes) { draft = draft.copy(lastCheckupNotes = it.nullIfBlank()) }
                    ProfileField(draft.lastBloodPressure, strings.lastBloodPressure) { draft = draft.copy(lastBloodPressure = it.nullIfBlank()) }
                    ProfileField(draft.lastBloodSugar, strings.lastBloodSugar) { draft = draft.copy(lastBloodSugar = it.nullIfBlank()) }
                } else {
                    ProfileValue(strings.pregnant, profile.pregnant?.let { if (it) strings.yes else strings.no }, strings.notSet)
                    ProfileValue(strings.smoking, profile.smoking?.let { if (it) strings.yes else strings.no }, strings.notSet)
                    if (profile.smoking == true) ProfileValue(strings.cigarettesPerDay, profile.cigarettesPerDay?.toString(), strings.notSet)
                    ProfileValue(strings.vaccinationNotes, profile.vaccinationNotes, strings.notSet)
                    ProfileValue(strings.lastCheckupNotes, profile.lastCheckupNotes, strings.notSet)
                    ProfileValue(strings.lastBloodPressure, profile.lastBloodPressure, strings.notSet)
                    ProfileValue(strings.lastBloodSugar, profile.lastBloodSugar, strings.notSet)
                }
            }

            ProfileAccordion(strings.sectionInsurance, ProfileSection.INSURANCE, expanded, { expanded = it }) {
                SectionActions(editing == ProfileSection.INSURANCE, strings.edit, strings.save, strings.cancel,
                    { editing = ProfileSection.INSURANCE }, { save(ProfileSection.INSURANCE) }) { draft = profile; editing = null }
                val fields = listOf(
                    strings.insuranceName to draft.healthInsuranceName,
                    strings.insuranceNumber to draft.healthInsuranceNumber,
                    strings.additionalInsurance to draft.additionalInsurance,
                    strings.careInsurance to draft.careInsurance
                )
                if (editing == ProfileSection.INSURANCE) {
                    fields.forEachIndexed { index, (label, value) ->
                        ProfileField(value, label) { text ->
                            draft = when (index) {
                                0 -> draft.copy(healthInsuranceName = text.nullIfBlank())
                                1 -> draft.copy(healthInsuranceNumber = text.nullIfBlank())
                                2 -> draft.copy(additionalInsurance = text.nullIfBlank())
                                else -> draft.copy(careInsurance = text.nullIfBlank())
                            }
                        }
                    }
                    ExpandingNotesField(draft.insuranceNotes.orEmpty(), { draft = draft.copy(insuranceNotes = it.nullIfBlank()) }, strings.notes)
                } else {
                    fields.forEach { (label, value) -> ProfileValue(label, value, strings.notSet) }
                    ProfileValue(strings.notes, profile.insuranceNotes, strings.notSet)
                }
            }

            ProfileAccordion(strings.sectionContacts, ProfileSection.CONTACTS, expanded, { expanded = it }) {
                EditToggle(editing == ProfileSection.CONTACTS, strings.edit, strings.done) {
                    editing = if (editing == ProfileSection.CONTACTS) null else ProfileSection.CONTACTS
                }
                if (contacts.isEmpty()) Text(strings.noContacts)
                contacts.forEach { contact ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(contact.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            if (editing == ProfileSection.CONTACTS) {
                                TextButton(onClick = { viewModel.deleteContact(contact) }) { Text(strings.delete) }
                            }
                        }
                        Text("${contact.category.label(strings)} · ${contact.phone ?: strings.notSet}")
                        contact.notes?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        if (contact.isEmergency) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(strings.contactEmergency) },
                                colors = AssistChipDefaults.assistChipColors(
                                    disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
                                    disabledLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        } else {
                            Text(
                                "${strings.contactEmergency}: ${strings.no}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (editing == ProfileSection.CONTACTS) {
                    ProfileField(contactName, strings.contactName) { contactName = it }
                    ProfileField(contactPhone, strings.contactPhone) { contactPhone = it }
                    ExpandingNotesField(contactNotes, { contactNotes = it }, strings.notes)
                    Text(strings.contactCategory, style = MaterialTheme.typography.titleSmall)
                    CategoryChipRow(
                        options = ContactCategory.entries.map { it to it.label(strings) },
                        selected = contactCategory
                    ) { contactCategory = it }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(contactEmergency, { contactEmergency = it })
                        Text(strings.contactEmergency)
                    }
                    Button(
                        enabled = contactName.isNotBlank(),
                        onClick = {
                            viewModel.addContact(contactName, contactCategory, null, contactPhone.nullIfBlank(), null, null,
                                contactNotes.nullIfBlank(), contactEmergency)
                            contactName = ""; contactPhone = ""; contactNotes = ""; contactEmergency = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(strings.addContact) }
                }
            }

            ProfileAccordion(strings.sectionDocuments, ProfileSection.DOCUMENTS, expanded, { expanded = it }) {
                EditToggle(editing == ProfileSection.DOCUMENTS, strings.edit, strings.done) {
                    editing = if (editing == ProfileSection.DOCUMENTS) null else ProfileSection.DOCUMENTS
                }
                if (documents.isEmpty()) Text(strings.noDocuments)
                documents.forEach { doc ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(doc.title, style = MaterialTheme.typography.titleSmall)
                        Text(doc.category.label(strings), style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                runCatching {
                                    val file = viewModel.documentFile(doc)
                                    require(file.exists())
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    })
                                }.onFailure { scope.launch { snackbar.showSnackbar(strings.documentOpenFailed) } }
                            }) { Text(strings.openDocument) }
                            if (editing == ProfileSection.DOCUMENTS) {
                                TextButton(onClick = { viewModel.deleteDocument(doc) }) { Text(strings.delete) }
                            }
                        }
                    }
                }
                if (editing == ProfileSection.DOCUMENTS) {
                    ProfileField(documentTitle, strings.documentTitle) { documentTitle = it }
                    Text(strings.documentCategory, style = MaterialTheme.typography.titleSmall)
                    CategoryChipRow(
                        options = DocumentCategory.entries.map { it to it.label(strings) },
                        selected = documentCategory
                    ) { documentCategory = it }
                    Button(
                        onClick = { pickPdf.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(strings.addDocument) }
                }
            }
        }
    }
}

@Composable
private fun ProfileAccordion(
    title: String,
    section: ProfileSection,
    expanded: ProfileSection?,
    onExpanded: (ProfileSection?) -> Unit,
    content: @Composable () -> Unit
) = StandardSectionCard {
    Row(
        Modifier.fillMaxWidth().clickable { onExpanded(if (expanded == section) null else section) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Icon(if (expanded == section) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
    }
    if (expanded == section) Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun SectionActions(editing: Boolean, edit: String, save: String, cancel: String, onEdit: () -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        if (editing) {
            TextButton(onClick = onCancel) { Text(cancel) }
            Button(onClick = onSave) { Text(save) }
        } else OutlinedButton(onClick = onEdit) { Text(edit) }
    }
}

@Composable
private fun EditToggle(editing: Boolean, edit: String, done: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        OutlinedButton(onClick = onClick) { Text(if (editing) done else edit) }
    }
}

@Composable
private fun ProfileField(value: String?, label: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    OutlinedTextField(value.orEmpty(), onChange, label = { Text(label) }, modifier = modifier, singleLine = true)
}

@Composable
private fun NumberField(value: String?, label: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    OutlinedTextField(
        value.orEmpty(),
        { onChange(it.filter { char -> char.isDigit() || char == '.' || char == ',' }.replace(',', '.')) },
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}

@Composable
fun ExpandingNotesField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = 1,
        maxLines = 4,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> CategoryChipRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    val colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
                colors = colors
            )
        }
    }
}

@Composable
private fun ProfileValue(label: String, value: String?, notSet: String) {
    Text("$label: ${value?.takeIf { it.isNotBlank() } ?: notSet}", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun BooleanChoice(label: String, value: Boolean?, yes: String, no: String, onChange: (Boolean) -> Unit) {
    val colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        FilterChip(value == true, { onChange(true) }, { Text(yes) }, colors = colors)
        FilterChip(value == false, { onChange(false) }, { Text(no) }, modifier = Modifier.padding(start = 6.dp), colors = colors)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChange)
    }
}

@Composable
private fun ListRow(name: String, editable: Boolean, delete: String, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f))
        if (editable) TextButton(onClick = onDelete) { Text(delete) }
    }
}

@Composable
private fun ConditionAdder(label: String, value: String, onChange: (String) -> Unit, onAdd: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.weight(1f))
        Button(onClick = onAdd, enabled = value.isNotBlank()) { Text("+") }
    }
}

@Composable
private fun BloodTypeField(value: String?, label: String, notSet: String, onChange: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${value ?: notSet}")
        }
        DropdownMenu(open, { open = false }) {
            listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "0+", "0-").forEach { bloodType ->
                DropdownMenuItem({ Text(bloodType) }, onClick = { onChange(bloodType); open = false })
            }
            DropdownMenuItem({ Text(notSet) }, onClick = { onChange(null); open = false })
        }
    }
}

private fun String.nullIfBlank(): String? = trim().ifBlank { null }
