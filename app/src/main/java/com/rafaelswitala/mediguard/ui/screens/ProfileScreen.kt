package com.rafaelswitala.mediguard.ui.screens

/**
 * Profil-Hub mit aufklappbaren Abschnitten für Gesundheitsdaten, Kontakte und Dokumente.
 */

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.rafaelswitala.mediguard.domain.model.ContactCategory
import com.rafaelswitala.mediguard.domain.model.DocumentCategory
import com.rafaelswitala.mediguard.domain.model.HealthProfile
import com.rafaelswitala.mediguard.ui.components.MediGuardBottomBar
import com.rafaelswitala.mediguard.ui.components.MediGuardTab
import com.rafaelswitala.mediguard.ui.components.StandardSectionCard
import com.rafaelswitala.mediguard.ui.localization.LocalAppStrings
import com.rafaelswitala.mediguard.viewmodel.ProfileViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    val profile by viewModel.profile.collectAsState()
    val allergies by viewModel.allergies.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val documents by viewModel.documents.collectAsState()

    var bloodType by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var aidsNotes by remember { mutableStateOf("") }
    var organDonor by remember { mutableStateOf<Boolean?>(null) }
    var preferredPharmacy by remember { mutableStateOf("") }
    var otherNotes by remember { mutableStateOf("") }
    var chronicConditions by remember { mutableStateOf("") }
    var implantsNotes by remember { mutableStateOf("") }
    var pregnancyNotes by remember { mutableStateOf("") }
    var smokingAlcoholNotes by remember { mutableStateOf("") }
    var vaccinationNotes by remember { mutableStateOf("") }
    var lastCheckupNotes by remember { mutableStateOf("") }
    var lastBloodPressure by remember { mutableStateOf("") }
    var lastBloodSugar by remember { mutableStateOf("") }
    var insuranceName by remember { mutableStateOf("") }
    var insuranceNumber by remember { mutableStateOf("") }
    var emergencyNotes by remember { mutableStateOf("") }

    var expandedGeneral by remember { mutableStateOf(true) }
    var expandedAllergies by remember { mutableStateOf(false) }
    var expandedHealth by remember { mutableStateOf(false) }
    var expandedInsurance by remember { mutableStateOf(false) }
    var expandedEmergency by remember { mutableStateOf(false) }
    var expandedContacts by remember { mutableStateOf(false) }
    var expandedDocuments by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    var allergyName by remember { mutableStateOf("") }
    var allergySeverity by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactCategory by remember { mutableStateOf(ContactCategory.DOCTOR) }
    var contactEmergency by remember { mutableStateOf(false) }
    var documentTitle by remember { mutableStateOf("") }
    var documentCategory by remember { mutableStateOf(DocumentCategory.OTHER) }

    LaunchedEffect(profile) {
        bloodType = profile.bloodType.orEmpty()
        heightCm = profile.heightCm?.toString().orEmpty()
        weightKg = profile.weightKg?.toString().orEmpty()
        aidsNotes = profile.aidsNotes.orEmpty()
        organDonor = profile.organDonor
        preferredPharmacy = profile.preferredPharmacy.orEmpty()
        otherNotes = profile.otherNotes.orEmpty()
        chronicConditions = profile.chronicConditions.orEmpty()
        implantsNotes = profile.implantsNotes.orEmpty()
        pregnancyNotes = profile.pregnancyNotes.orEmpty()
        smokingAlcoholNotes = profile.smokingAlcoholNotes.orEmpty()
        vaccinationNotes = profile.vaccinationNotes.orEmpty()
        lastCheckupNotes = profile.lastCheckupNotes.orEmpty()
        lastBloodPressure = profile.lastBloodPressure.orEmpty()
        lastBloodSugar = profile.lastBloodSugar.orEmpty()
        insuranceName = profile.insuranceName.orEmpty()
        insuranceNumber = profile.insuranceNumber.orEmpty()
        emergencyNotes = profile.emergencyNotes.orEmpty()
    }

    val pickPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importDocument(
                uri = uri,
                title = documentTitle.ifBlank { "PDF" },
                category = documentCategory,
                notes = null
            )
            documentTitle = ""
        }
    }

    fun currentDraft(): HealthProfile = profile.copy(
        bloodType = bloodType.trim().ifBlank { null },
        heightCm = heightCm.replace(',', '.').toDoubleOrNull(),
        weightKg = weightKg.replace(',', '.').toDoubleOrNull(),
        aidsNotes = aidsNotes.trim().ifBlank { null },
        organDonor = organDonor,
        preferredPharmacy = preferredPharmacy.trim().ifBlank { null },
        otherNotes = otherNotes.trim().ifBlank { null },
        chronicConditions = chronicConditions.trim().ifBlank { null },
        implantsNotes = implantsNotes.trim().ifBlank { null },
        pregnancyNotes = pregnancyNotes.trim().ifBlank { null },
        smokingAlcoholNotes = smokingAlcoholNotes.trim().ifBlank { null },
        vaccinationNotes = vaccinationNotes.trim().ifBlank { null },
        lastCheckupNotes = lastCheckupNotes.trim().ifBlank { null },
        lastBloodPressure = lastBloodPressure.trim().ifBlank { null },
        lastBloodSugar = lastBloodSugar.trim().ifBlank { null },
        insuranceName = insuranceName.trim().ifBlank { null },
        insuranceNumber = insuranceNumber.trim().ifBlank { null },
        emergencyNotes = emergencyNotes.trim().ifBlank { null }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.profile) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                strings.profileDisclaimer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AccordionSection(strings.sectionGeneral, expandedGeneral, { expandedGeneral = it }) {
                OutlinedTextField(bloodType, { bloodType = it }, label = { Text(strings.bloodType) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(heightCm, { heightCm = it }, label = { Text(strings.heightCm) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(weightKg, { weightKg = it }, label = { Text(strings.weightKg) }, modifier = Modifier.fillMaxWidth())
                val bmi = currentDraft().bmi()
                Text(
                    "${strings.bmi}: ${bmi?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: strings.notSet}",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(aidsNotes, { aidsNotes = it }, label = { Text(strings.aidsNotes) }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.organDonor, modifier = Modifier.weight(1f))
                    TextButton(onClick = { organDonor = true }) { Text(strings.yes) }
                    TextButton(onClick = { organDonor = false }) { Text(strings.no) }
                }
                OutlinedTextField(preferredPharmacy, { preferredPharmacy = it }, label = { Text(strings.preferredPharmacy) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(otherNotes, { otherNotes = it }, label = { Text(strings.otherNotes) }, modifier = Modifier.fillMaxWidth())
            }

            AccordionSection(strings.sectionAllergies, expandedAllergies, { expandedAllergies = it }) {
                if (allergies.isEmpty()) Text(strings.noAllergies)
                allergies.forEach { allergy ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${allergy.name}${allergy.severity?.let { " ($it)" } ?: ""}")
                        TextButton(onClick = { viewModel.deleteAllergy(allergy) }) { Text(strings.delete) }
                    }
                }
                OutlinedTextField(allergyName, { allergyName = it }, label = { Text(strings.allergyName) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(allergySeverity, { allergySeverity = it }, label = { Text(strings.allergySeverity) }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    viewModel.addAllergy(allergyName, allergySeverity.ifBlank { null }, null)
                    allergyName = ""
                    allergySeverity = ""
                }) { Text(strings.addAllergy) }
            }

            AccordionSection(strings.sectionHealth, expandedHealth, { expandedHealth = it }) {
                OutlinedTextField(chronicConditions, { chronicConditions = it }, label = { Text(strings.chronicConditions) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(implantsNotes, { implantsNotes = it }, label = { Text(strings.implantsNotes) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(pregnancyNotes, { pregnancyNotes = it }, label = { Text(strings.pregnancyNotes) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(smokingAlcoholNotes, { smokingAlcoholNotes = it }, label = { Text(strings.smokingAlcoholNotes) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(vaccinationNotes, { vaccinationNotes = it }, label = { Text(strings.vaccinationNotes) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(lastCheckupNotes, { lastCheckupNotes = it }, label = { Text(strings.lastCheckupNotes) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(lastBloodPressure, { lastBloodPressure = it }, label = { Text(strings.lastBloodPressure) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(lastBloodSugar, { lastBloodSugar = it }, label = { Text(strings.lastBloodSugar) }, modifier = Modifier.fillMaxWidth())
            }

            AccordionSection(strings.sectionInsurance, expandedInsurance, { expandedInsurance = it }) {
                OutlinedTextField(insuranceName, { insuranceName = it }, label = { Text(strings.insuranceName) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(insuranceNumber, { insuranceNumber = it }, label = { Text(strings.insuranceNumber) }, modifier = Modifier.fillMaxWidth())
            }

            AccordionSection(strings.sectionEmergency, expandedEmergency, { expandedEmergency = it }) {
                OutlinedTextField(emergencyNotes, { emergencyNotes = it }, label = { Text(strings.emergencyNotes) }, modifier = Modifier.fillMaxWidth())
                contacts.filter { it.isEmergency }.forEach { contact ->
                    Text("${contact.name}${contact.phone?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            AccordionSection(strings.sectionContacts, expandedContacts, { expandedContacts = it }) {
                if (contacts.isEmpty()) Text(strings.noContacts)
                contacts.forEach { contact ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(contact.name, style = MaterialTheme.typography.titleSmall)
                            Text(contact.category.label(strings))
                            contact.phone?.let { Text(it) }
                        }
                        TextButton(onClick = { viewModel.deleteContact(contact) }) { Text(strings.delete) }
                    }
                }
                OutlinedTextField(contactName, { contactName = it }, label = { Text(strings.contactName) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(contactPhone, { contactPhone = it }, label = { Text(strings.contactPhone) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ContactCategory.entries.forEach { cat ->
                        OutlinedButton(onClick = { contactCategory = cat }) {
                            Text(cat.label(strings))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = contactEmergency, onCheckedChange = { contactEmergency = it })
                    Text(strings.contactEmergency)
                }
                Button(onClick = {
                    viewModel.addContact(
                        name = contactName,
                        category = contactCategory,
                        specialty = null,
                        phone = contactPhone.ifBlank { null },
                        email = null,
                        address = null,
                        notes = null,
                        isEmergency = contactEmergency
                    )
                    contactName = ""
                    contactPhone = ""
                    contactEmergency = false
                }) { Text(strings.addContact) }
            }

            AccordionSection(strings.sectionDocuments, expandedDocuments, { expandedDocuments = it }) {
                if (documents.isEmpty()) Text(strings.noDocuments)
                documents.forEach { doc ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.title, style = MaterialTheme.typography.titleSmall)
                            Text(doc.category.label(strings))
                        }
                        TextButton(onClick = {
                            val file = viewModel.documentFile(doc)
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, doc.mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                runCatching { context.startActivity(intent) }
                            }
                        }) { Text(strings.openDocument) }
                        TextButton(onClick = { viewModel.deleteDocument(doc) }) { Text(strings.delete) }
                    }
                }
                OutlinedTextField(documentTitle, { documentTitle = it }, label = { Text(strings.documentTitle) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DocumentCategory.entries.forEach { cat ->
                        OutlinedButton(onClick = { documentCategory = cat }) {
                            Text(cat.label(strings))
                        }
                    }
                }
                Button(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                    Text(strings.addDocument)
                }
            }

            Button(
                onClick = { viewModel.saveProfile(currentDraft()) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.saveProfile) }

            OutlinedButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.clearProfile) }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(strings.clearProfile) },
            text = { Text(strings.clearProfileConfirm) },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearAll()
                    showClearDialog = false
                }) { Text(strings.delete) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun AccordionSection(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    StandardSectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}
