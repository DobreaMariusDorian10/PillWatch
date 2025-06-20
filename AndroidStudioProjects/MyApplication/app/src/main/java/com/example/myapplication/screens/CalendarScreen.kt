package com.example.myapplication.screens

import android.app.*
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.utils.*
import com.example.myapplication.data_class.Alarm as FirebaseAlarm
import com.example.myapplication.utils.firebase.deleteAlarmFromFirebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

data class LocalAlarm(val id: String, val timestamp: Long, val label: String)

@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var currentUserRole by remember { mutableStateOf<String?>(null) }
    var isRoleLoaded by remember { mutableStateOf(false) }

    var targetUser by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showUserPicker by remember { mutableStateOf(false) }
    var allUsers by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var userSearch by remember { mutableStateOf("") }

    val calendar = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var alarmLabel by remember { mutableStateOf("") }
    var alarmSet by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var alarms by remember { mutableStateOf(listOf<LocalAlarm>()) }
    var isRepeating by remember { mutableStateOf(false) }
    var intervalHours by remember { mutableStateOf("4") }
    var repetitions by remember { mutableStateOf("6") }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val fullDateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    fun loadAlarms(userId: String) {
        val dbRef = FirebaseDatabase.getInstance().reference.child("alarms").child(userId)
        dbRef.get().addOnSuccessListener { snapshot ->
            val list = mutableListOf<LocalAlarm>()
            for (child in snapshot.children) {
                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: continue
                val label = child.child("message").getValue(String::class.java) ?: "Alarmă"
                list.add(LocalAlarm(child.key ?: UUID.randomUUID().toString(), timestamp, label))
            }
            alarms = list.filter { it.timestamp > System.currentTimeMillis() }.sortedBy { it.timestamp }
        }
    }

    LaunchedEffect(currentUserId) {
        currentUserId?.let { uid ->
            val ref = FirebaseDatabase.getInstance().reference.child("users").child(uid)
            ref.get().addOnSuccessListener { snapshot ->
                val role = snapshot.child("role").getValue(String::class.java)
                currentUserRole = role
                isRoleLoaded = true

                val name = snapshot.child("name").getValue(String::class.java) ?: "Utilizator"
                targetUser = uid to name
            }.addOnFailureListener {
                isRoleLoaded = true
            }
        } ?: run {
            isRoleLoaded = true
        }
    }

    LaunchedEffect(targetUser) {
        targetUser?.first?.let { userId ->
            loadAlarms(userId)
        }
    }

    LaunchedEffect(showUserPicker) {
        if (showUserPicker && allUsers.isEmpty()) {
            val dbRef = FirebaseDatabase.getInstance().reference.child("users")
            dbRef.get().addOnSuccessListener { snapshot ->
                val list = snapshot.children.mapNotNull {
                    val id = it.key ?: return@mapNotNull null
                    val name = it.child("name").getValue(String::class.java) ?: id
                    id to name
                }
                allUsers = list.sortedBy { it.second.lowercase(Locale.getDefault()) }
            }
        }
    }

    val filteredUsers = remember(userSearch, allUsers) {
        if (userSearch.isBlank()) allUsers
        else allUsers.filter { it.second.contains(userSearch, ignoreCase = true) }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            calendar.set(year, month, day)
            selectedDate = dateFormatter.format(calendar.time)
            alarmSet = false
            errorMessage = null
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            selectedTime = timeFormatter.format(calendar.time)
            alarmSet = false
            errorMessage = null
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isRoleLoaded) {
            CircularProgressIndicator()
            return@Column
        }

        if (currentUserRole == "doctor" || currentUserRole == "pacient") {
            if (currentUserRole == "doctor") {
                Text("Setează alarma pentru utilizator")
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetUser?.second ?: "",
                    onValueChange = {},
                    label = { Text("Utilizator selectat") },
                    modifier = Modifier.fillMaxWidth().clickable { showUserPicker = true },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showUserPicker = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Caută utilizator")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Text("Setează o alarmă pentru tine")
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isRepeating, onCheckedChange = { isRepeating = it })
                Text("Alarmă recurentă (la interval fix)")
            }

            if (isRepeating) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = intervalHours,
                        onValueChange = { intervalHours = it },
                        label = { Text("Interval (ore)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = repetitions,
                        onValueChange = { repetitions = it },
                        label = { Text("Repetări") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (selectedDate.isEmpty()) "Alege data" else "Data: $selectedDate")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(onClick = { timePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (selectedTime.isEmpty()) "Alege ora" else "Ora: $selectedTime")
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = alarmLabel,
                onValueChange = { alarmLabel = it },
                label = { Text("Denumire alarmă") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (selectedDate.isEmpty() || selectedTime.isEmpty() || alarmLabel.isEmpty()) {
                        errorMessage = "Completează toate câmpurile!"
                        alarmSet = false
                        return@Button
                    }

                    val alarmTimeMillis = calendar.timeInMillis
                    if (alarmTimeMillis <= System.currentTimeMillis()) {
                        errorMessage = "Nu poți seta o alarmă în trecut!"
                        alarmSet = false
                        return@Button
                    }

                    if (!checkExactAlarmPermission(context)) {
                        errorMessage = "Permisiune necesară pentru alarme exacte!"
                        requestExactAlarmPermission(context)
                        return@Button
                    }

                    val (userId, _) = targetUser ?: return@Button
                    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@Button

                    if (isRepeating) {
                        createRepeatingAlarms(
                            context = context,
                            targetUserId = userId,
                            startTimestamp = alarmTimeMillis,
                            intervalHours = intervalHours.toIntOrNull() ?: 4,
                            repetitions = repetitions.toIntOrNull() ?: 6,
                            label = alarmLabel
                        ) {
                            Toast.makeText(context, "Alarme recurente setate!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val dbRef = FirebaseDatabase.getInstance().reference.child("alarms").child(userId)
                        val newAlarmRef = dbRef.push()
                        val alarmId = newAlarmRef.key ?: UUID.randomUUID().toString()

                        val alarmObject = FirebaseAlarm(
                            alarmId = alarmId,
                            message = alarmLabel,
                            timestamp = alarmTimeMillis,
                            createdBy = currentUserUid,
                            assignedTo = userId
                        )

                        newAlarmRef.setValue(alarmObject).addOnSuccessListener {
                            scheduleReminder(context, alarmTimeMillis, alarmLabel, userId, alarmId)
                            Toast.makeText(context, "Alarmă setată!", Toast.LENGTH_SHORT).show()
                            alarmSet = true
                            errorMessage = null
                            alarms = (alarms + LocalAlarm(alarmId, alarmTimeMillis, alarmLabel)).sortedBy { it.timestamp }
                        }.addOnFailureListener {
                            errorMessage = "Eroare la setarea alarmei."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Setează alarmă")
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Text("Accesul este permis doar utilizatorilor autentificați.")
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Buton Refresh alarme
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = {
                targetUser?.first?.let { userId ->
                    loadAlarms(userId)
                }
            }) {
                Text("Refresh alarme")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Alarme active pentru ${targetUser?.second ?: "utilizatorul selectat"}:")
        Spacer(modifier = Modifier.height(4.dp))

        if (alarms.isEmpty()) {
            Text("Nicio alarmă activă")
        } else {
            LazyColumn {
                items(alarms) { alarm ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(alarm.label, style = MaterialTheme.typography.bodyLarge)
                                Text(fullDateFormatter.format(Date(alarm.timestamp)), style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = {
                                targetUser?.first?.let { userId ->
                                    deleteAlarmFromFirebase(context, userId, alarm.id) {
                                        cancelSystemAlarm(context, alarm.id, userId)
                                        alarms = alarms.filter { it.id != alarm.id }
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Șterge alarmă")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUserPicker) {
        AlertDialog(
            onDismissRequest = { showUserPicker = false },
            title = { Text("Selectează utilizator") },
            text = {
                Column {
                    OutlinedTextField(
                        value = userSearch,
                        onValueChange = { userSearch = it },
                        label = { Text("Caută utilizator") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(filteredUsers) { (userId, name) ->
                            Text(
                                text = name,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    targetUser = userId to name
                                    showUserPicker = false
                                    userSearch = ""
                                }.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUserPicker = false }) {
                    Text("Închide")
                }
            }
        )
    }
}

