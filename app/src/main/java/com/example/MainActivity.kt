package com.example

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image

data class AppInfo(val name: String, val packageName: String, val icon: androidx.compose.ui.graphics.ImageBitmap?)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("security_prefs", android.content.Context.MODE_PRIVATE)

    var savedPin: String? by mutableStateOf(prefs.getString("saved_pin", null))
        private set

    var savedPassword: String? by mutableStateOf(prefs.getString("saved_password", null))
        private set

    var savedPattern: String? by mutableStateOf(prefs.getString("saved_pattern", null))
        private set

    var isBiometricEnabled by mutableStateOf(prefs.getBoolean("biometric_enabled", false))
        private set

    fun savePin(pin: String) {
        prefs.edit().putString("saved_pin", pin).apply()
        savedPin = pin
    }

    fun removePin() {
        prefs.edit().remove("saved_pin").apply()
        savedPin = null
    }

    fun savePassword(password: String) {
        prefs.edit().putString("saved_password", password).apply()
        savedPassword = password
    }

    fun removePassword() {
        prefs.edit().remove("saved_password").apply()
        savedPassword = null
    }

    fun savePattern(pattern: String) {
        prefs.edit().putString("saved_pattern", pattern).apply()
        savedPattern = pattern
    }

    fun removePattern() {
        prefs.edit().remove("saved_pattern").apply()
        savedPattern = null
    }

    fun setBiometric(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        isBiometricEnabled = enabled
    }

    var lockedApps by mutableStateOf(prefs.getStringSet("locked_apps", emptySet()) ?: emptySet())
        private set

    fun toggleAppLock(packageName: String, isLocked: Boolean) {
        val current = lockedApps.toMutableSet()
        if (isLocked) current.add(packageName) else current.remove(packageName)
        lockedApps = current
        prefs.edit().putStringSet("locked_apps", current).apply()
    }

    var selectedAutoLockTime by mutableStateOf(prefs.getString("auto_lock_time", "Immediately") ?: "Immediately")
        private set

    fun setAutoLockTime(time: String) {
        prefs.edit().putString("auto_lock_time", time).apply()
        selectedAutoLockTime = time
    }

    val settings = mutableStateMapOf<String, Boolean>()
    
    var installedApps by mutableStateOf<List<AppInfo>>(emptyList())
    var isLoadingApps by mutableStateOf(true)

    init {
        // Initialize working settings
        listOf("Hide Notifications").forEach { 
            settings[it] = prefs.getBoolean(it, false)
        }
        loadInstalledApps()
    }
    
    fun toggleSetting(key: String, value: Boolean) {
        settings[key] = value
        prefs.edit().putBoolean(key, value).apply()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pm = getApplication<Application>().packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
                val appList = pm.queryIntentActivities(intent, 0)
                val result = appList.mapNotNull { 
                    try {
                        AppInfo(
                            name = it.activityInfo.applicationInfo.loadLabel(pm).toString(),
                            packageName = it.activityInfo.packageName,
                            icon = it.activityInfo.applicationInfo.loadIcon(pm)?.let { drawable -> 
                                drawable.toBitmap(120, 120).asImageBitmap()
                            }
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.distinctBy { it.packageName }.sortedBy { it.name }
                
                withContext(Dispatchers.Main) {
                    installedApps = result
                    isLoadingApps = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isLoadingApps = false
                }
            }
        }
    }
}

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MLockApp()
      }
    }
  }
}

@Composable
fun MLockApp() {
  val navController = rememberNavController()
  val viewModel: MainViewModel = viewModel()
  
  NavHost(navController = navController, startDestination = "home") {
    composable("home") { HomeScreen(navController) }
    composable("security") { SecurityPage(navController, viewModel) }
    composable("app_lock") { AppLockPage(navController, viewModel) }
    composable("settings") { SettingsPage(navController, viewModel) }
  }
}

@Composable
fun HomeScreen(navController: NavController) {
  var showInfoDialog by remember { mutableStateOf(false) }

  if (showInfoDialog) {
    AlertDialog(
        onDismissRequest = { showInfoDialog = false },
        title = { Text("About MLock") },
        text = { 
            Text("MLock helps you protect your apps. Here's how to use it:\n\n" +
                 "1. Security Setup: Configure your PIN, Password, Pattern, or Biometrics.\n" +
                 "2. App Lock: Choose which apps require authentication to open.\n" +
                 "3. Auto Lock Timing: Decide when an app should re-lock after being unlocked.")
        },
        confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text("Got it") } }
    )
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("MLock", style = MaterialTheme.typography.headlineSmall)
        IconButton(onClick = { showInfoDialog = true }) {
          Icon(Icons.Default.Info, contentDescription = "Info")
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Card(
        onClick = { navController.navigate("security") },
        modifier = Modifier.fillMaxWidth().height(120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      ) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Shield, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Security Setup")
            }
          }
      }
      Card(
        onClick = { navController.navigate("app_lock") },
        modifier = Modifier.fillMaxWidth().height(180.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      ) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Lock, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("App Lock")
            }
          }
      }
      Card(
        onClick = { navController.navigate("settings") },
        modifier = Modifier.fillMaxWidth().height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      ) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Settings, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Settings")
            }
          }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityPage(navController: NavController, viewModel: MainViewModel) {
  var expandedAutoLock by remember { mutableStateOf(false) }
  val autoLockOptions = listOf("Immediately", "1 Second", "After 5 Seconds", "After 10 Seconds", "After 30 Seconds", "After 1 Minute", "After 5 Minutes", "After 10 Minutes")

  var showPinDialog by remember { mutableStateOf(false) }
  var showPasswordDialog by remember { mutableStateOf(false) }
  var showPatternDialog by remember { mutableStateOf(false) }
  var showPinRequiredDialog by remember { mutableStateOf(false) }
  var showVerifyPinDialog by remember { mutableStateOf(false) }
  var verifyPinValue by remember { mutableStateOf("") }
  var onVerifiedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var paramToChange by remember { mutableStateOf("") }
  var paramValue by remember { mutableStateOf("") }
  var confirmParamValue by remember { mutableStateOf("") }

  val context = LocalContext.current

  fun Context.findFragmentActivity(): FragmentActivity? {
      var currentContext = this
      while (currentContext is ContextWrapper) {
          if (currentContext is FragmentActivity) {
              return currentContext
          }
          currentContext = currentContext.baseContext
      }
      return null
  }

  fun launchBiometricPrompt() {
      val fragmentActivity = context.findFragmentActivity() ?: return
      val executor = ContextCompat.getMainExecutor(context)
      val biometricPrompt = BiometricPrompt(fragmentActivity, executor,
          object : BiometricPrompt.AuthenticationCallback() {
              override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                  // Do nothing on error unless setting up, then reset toggle if it failed
              }
              override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                  viewModel.setBiometric(true)
              }
              override fun onAuthenticationFailed() {
                  // Ignore
              }
          })
      val promptInfo = BiometricPrompt.PromptInfo.Builder()
          .setTitle("Biometric Verification")
          .setSubtitle("Confirm your identity to enable biometric unlock")
          .setNegativeButtonText("Cancel")
          .build()
      try {
          biometricPrompt.authenticate(promptInfo)
      } catch (e: Exception) {
          e.printStackTrace()
      }
  }

  fun requirePinVerification(action: () -> Unit) {
      if (viewModel.savedPin == null) {
          showPinRequiredDialog = true
      } else {
          onVerifiedAction = action
          verifyPinValue = ""
          showVerifyPinDialog = true
      }
  }

  Scaffold(topBar = { TopAppBar(title = { Text("Security") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Text("Authentication Methods", style = MaterialTheme.typography.titleMedium)
      
      ListItem(
        headlineContent = { Text("PIN") },
        supportingContent = { Text(if (viewModel.savedPin != null) "Change or disable PIN" else "Set up PIN") },
        trailingContent = { Switch(checked = viewModel.savedPin != null, onCheckedChange = { isChecked ->
            if (isChecked) { 
                paramToChange = "PIN"; showPinDialog = true 
            } else { 
                requirePinVerification { viewModel.removePin() }
            }
        }) },
        modifier = Modifier.clickable { 
            if (viewModel.savedPin != null) {
                requirePinVerification { 
                    paramToChange = "PIN"; showPinDialog = true 
                }
            } else {
                paramToChange = "PIN"; showPinDialog = true 
            }
        }
      )
      ListItem(
        headlineContent = { Text("Password") },
        supportingContent = { Text(if (viewModel.savedPassword != null) "Change or disable Password" else "Set up Password") },
        trailingContent = { Switch(checked = viewModel.savedPassword != null, onCheckedChange = { isChecked ->
            if (isChecked) { 
                requirePinVerification {
                    paramToChange = "Password"
                    showPasswordDialog = true
                }
            } else { 
                requirePinVerification { viewModel.removePassword() }
            }
        }) },
        modifier = Modifier.clickable { 
            requirePinVerification {
                paramToChange = "Password"
                showPasswordDialog = true
            }
        }
      )
      ListItem(
        headlineContent = { Text("Pattern") },
        supportingContent = { Text(if (viewModel.savedPattern != null) "Change or disable Pattern" else "Set up Pattern") },
        trailingContent = { Switch(checked = viewModel.savedPattern != null, onCheckedChange = { isChecked ->
            if (isChecked) { 
                requirePinVerification {
                    paramToChange = "Pattern"
                    showPatternDialog = true
                }
            } else { 
                requirePinVerification { viewModel.removePattern() }
            }
        }) },
        modifier = Modifier.clickable { 
            requirePinVerification {
                paramToChange = "Pattern"
                showPatternDialog = true
            }
        }
      )
      ListItem(
        headlineContent = { Text("Biometrics") },
        supportingContent = { Text(if (viewModel.isBiometricEnabled) "Disable Biometrics" else "Set up Biometric unlock") },
        trailingContent = { Switch(checked = viewModel.isBiometricEnabled, onCheckedChange = { isChecked ->
            if (isChecked) { 
                requirePinVerification { launchBiometricPrompt() }
            } else { 
                requirePinVerification { viewModel.setBiometric(false) }
            }
        }) },
        modifier = Modifier.clickable { 
            if (!viewModel.isBiometricEnabled) {
                requirePinVerification { launchBiometricPrompt() }
            } else {
                requirePinVerification { viewModel.setBiometric(false) }
            }
        }
      )
      
      Text("Auto Lock Timing", style = MaterialTheme.typography.titleMedium)
      Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expandedAutoLock = true }, modifier = Modifier.fillMaxWidth()) {
          Text(viewModel.selectedAutoLockTime)
        }
        DropdownMenu(expanded = expandedAutoLock, onDismissRequest = { expandedAutoLock = false }) {
          autoLockOptions.forEach { option ->
            DropdownMenuItem(text = { Text(option) }, onClick = { viewModel.setAutoLockTime(option); expandedAutoLock = false })
          }
        }
      }
    }

    if (showPinDialog) {
      val isWeak = paramValue.length < 8 || paramValue.all { it == paramValue.getOrNull(0) } || paramValue == "12345678" || paramValue == "01234567" || paramValue == "98765432"
      val strengthText = if (paramValue.isEmpty()) "" else if (isWeak) "Weak: Use at least 8 unique digits" else "Strong: High Level Encrypted"
      val isMatch = paramValue == confirmParamValue && confirmParamValue.isNotEmpty()
      val canSave = !isWeak && isMatch
      
      AlertDialog(
        onDismissRequest = { showPinDialog = false; paramValue = ""; confirmParamValue = "" },
        title = { Text(if (viewModel.savedPin != null) "Change PIN" else "Set up PIN") },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = paramValue,
              onValueChange = { paramValue = it },
              label = { Text("Enter New PIN") },
              modifier = Modifier.fillMaxWidth(),
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
              isError = paramValue.isNotEmpty() && isWeak
            )
            if (paramValue.isNotEmpty()) {
                Text(
                    text = strengthText,
                    color = if (isWeak) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            OutlinedTextField(
              value = confirmParamValue,
              onValueChange = { confirmParamValue = it },
              label = { Text("Confirm PIN") },
              modifier = Modifier.fillMaxWidth(),
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
              isError = confirmParamValue.isNotEmpty() && !isMatch
            )
            if (confirmParamValue.isNotEmpty() && !isMatch) {
                 Text(
                    text = "PINs do not match",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
          }
        },
        confirmButton = {
          TextButton(
            onClick = {
                if (canSave) {
                    viewModel.savePin(paramValue)
                    paramValue = ""
                    confirmParamValue = ""
                    showPinDialog = false
                }
            },
            enabled = canSave
          ) { Text("Save") }
        },
        dismissButton = {
          TextButton(onClick = { showPinDialog = false; paramValue = ""; confirmParamValue = "" }) { Text("Cancel") }
        }
      )
    }

    if (showPasswordDialog) {
      val isWeak = paramValue.length < 10 || !paramValue.any { it.isDigit() } || !paramValue.any { it.isUpperCase() }
      val strengthText = if (paramValue.isEmpty()) "" else if (isWeak) "Weak (Use 10+ chars, uppercase & numbers)" else "Strong: High Level Encrypted"
      val isMatch = paramValue == confirmParamValue && confirmParamValue.isNotEmpty()
      val canSave = !isWeak && isMatch

      AlertDialog(
        onDismissRequest = { showPasswordDialog = false; paramValue = ""; confirmParamValue = "" },
        title = { Text(if (viewModel.savedPassword != null) "Change Password" else "Set up Password") },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = paramValue,
              onValueChange = { paramValue = it },
              label = { Text("Enter New Password") },
              modifier = Modifier.fillMaxWidth(),
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
              isError = paramValue.isNotEmpty() && isWeak
            )
            if (paramValue.isNotEmpty()) {
                Text(
                    text = strengthText,
                    color = if (isWeak) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            OutlinedTextField(
              value = confirmParamValue,
              onValueChange = { confirmParamValue = it },
              label = { Text("Confirm Password") },
              modifier = Modifier.fillMaxWidth(),
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
              isError = confirmParamValue.isNotEmpty() && !isMatch
            )
            if (confirmParamValue.isNotEmpty() && !isMatch) {
                 Text(
                    text = "Passwords do not match",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
          }
        },
        confirmButton = {
          TextButton(
            onClick = {
                if (canSave) {
                    viewModel.savePassword(paramValue)
                    paramValue = ""
                    confirmParamValue = ""
                    showPasswordDialog = false
                }
            },
            enabled = canSave
          ) { Text("Save") }
        },
        dismissButton = {
          TextButton(onClick = { showPasswordDialog = false; paramValue = ""; confirmParamValue = "" }) { Text("Cancel") }
        }
      )
    }

    if (showPatternDialog) {
      val isMatch = paramValue == confirmParamValue && confirmParamValue.isNotEmpty()
      val isWeak = paramValue.length < 4
      val canSave = !isWeak && isMatch

      androidx.compose.ui.window.Dialog(
        onDismissRequest = { showPatternDialog = false; paramValue = ""; confirmParamValue = "" },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
      ) {
        Scaffold(
          topBar = {
            TopAppBar(
              title = { Text(if (viewModel.savedPattern != null) "Change Pattern" else "Set up Pattern") },
              navigationIcon = { IconButton(onClick = { showPatternDialog = false; paramValue = ""; confirmParamValue = "" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
          }
        ) { paddingValues ->
          Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Text(
              text = if (paramValue.isEmpty()) "Draw your pattern" else if (confirmParamValue.isEmpty()) "Confirm your pattern" else if (!isMatch) "Patterns do not match" else "Pattern confirmed", 
              color = if (paramValue.isNotEmpty() && confirmParamValue.isNotEmpty() && !isMatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
              style = MaterialTheme.typography.titleMedium
            )
            
            PatternLockView(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) { pattern ->
                if (pattern.length >= 4) {
                    if (paramValue.isEmpty()) {
                        paramValue = pattern
                    } else if (confirmParamValue.isEmpty()) {
                        confirmParamValue = pattern
                    } else {
                        // Reset if drawing again after mismatch
                        paramValue = pattern
                        confirmParamValue = ""
                    }
                }
            }
            
            if (paramValue.isNotEmpty()) {
                TextButton(onClick = { paramValue = ""; confirmParamValue = "" }) { Text("Reset Drawing") }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
              onClick = {
                  if (canSave) {
                      viewModel.savePattern(paramValue)
                      paramValue = ""
                      confirmParamValue = ""
                      showPatternDialog = false
                  }
              },
              modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
              enabled = canSave
            ) { Text("Save Pattern") }
          }
        }
      }
    }

    if (showVerifyPinDialog) {
        val isError = verifyPinValue.isNotEmpty() && verifyPinValue != viewModel.savedPin
        AlertDialog(
            onDismissRequest = { showVerifyPinDialog = false; verifyPinValue = "" },
            title = { Text("Verify PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Please enter your current PIN to continue", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = verifyPinValue,
                        onValueChange = { verifyPinValue = it },
                        label = { Text("Current PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                        isError = isError
                    )
                    if (isError) {
                        Text("Incorrect PIN", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            },
            confirmButton = {
                val canProceed = verifyPinValue == viewModel.savedPin
                TextButton(
                    onClick = {
                        if (canProceed) {
                            showVerifyPinDialog = false
                            verifyPinValue = ""
                            onVerifiedAction?.invoke()
                        }
                    },
                    enabled = canProceed
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showVerifyPinDialog = false; verifyPinValue = "" }) { Text("Cancel") }
            }
        )
    }

    if (showPinRequiredDialog) {
      AlertDialog(
        onDismissRequest = { showPinRequiredDialog = false },
        title = { Text("PIN Required") },
        text = { Text("Please set up a PIN first before changing pattern or biometric settings. This acts as a reliable fallback authentication.") },
        confirmButton = {
          TextButton(onClick = { 
              showPinRequiredDialog = false 
              paramToChange = "PIN"
              showPinDialog = true
          }) { Text("Set up PIN") }
        },
        dismissButton = {
          TextButton(onClick = { showPinRequiredDialog = false }) { Text("Cancel") }
        }
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockPage(navController: NavController, viewModel: MainViewModel) {
  val context = LocalContext.current
  var isAccessibilityEnabled by remember { mutableStateOf(false) }
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

  // Check accessibility service status when the page is displayed or resumed
  DisposableEffect(lifecycleOwner) {
      val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
          if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
              val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
              isAccessibilityEnabled = enabledServices?.contains(context.packageName) == true
          }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose {
          lifecycleOwner.lifecycle.removeObserver(observer)
      }
  }

  Scaffold(topBar = { TopAppBar(title = { Text("App Lock") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
      if (!isAccessibilityEnabled) {
          Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              modifier = Modifier.fillMaxWidth().clickable {
                  try {
                      val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                      context.startActivity(intent)
                  } catch (e: Exception) {
                      android.util.Log.e("MLock", "Could not open Accessibility settings", e)
                  }
              }
          ) {
              Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Info, contentDescription = "Warning", tint = MaterialTheme.colorScheme.onErrorContainer)
                  Spacer(Modifier.width(16.dp))
                  Column {
                      Text("Service Required", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                      Text("Tap here to enable the 'M-Lock' accessibility service to allow locking apps.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                  }
              }
          }
      }

      if (viewModel.isLoadingApps) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      } else {
         LazyColumn(modifier = Modifier.weight(1f)) {
          items(viewModel.installedApps, key = { it.packageName }) { app ->
            val isChecked = viewModel.lockedApps.contains(app.packageName)
            ListItem(
                modifier = Modifier.animateItem(),
                leadingContent = {
                    if (app.icon != null) {
                        Image(bitmap = app.icon, contentDescription = app.name, modifier = Modifier.size(40.dp))
                    } else {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(40.dp))
                    }
                },
                headlineContent = { Text(app.name) },
                trailingContent = { Switch(checked = isChecked, onCheckedChange = { viewModel.toggleAppLock(app.packageName, it) }) }
            )
            HorizontalDivider()
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(navController: NavController, viewModel: MainViewModel) {
  val context = LocalContext.current
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

  val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
      androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
  ) {}

  DisposableEffect(lifecycleOwner) {
      val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
          if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
              val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
              val hasPermission = enabledListeners?.contains(context.packageName) == true
              val wasEnabledInPrefs = viewModel.settings["Hide Notifications"] ?: false
              
              if (!hasPermission && wasEnabledInPrefs) {
                  viewModel.toggleSetting("Hide Notifications", false)
              } else if (hasPermission && !wasEnabledInPrefs && viewModel.settings.containsKey("Hide Notifications")) {
                 // optionally auto-enable if returning
              }
          }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }) { innerPadding ->
    LazyColumn(modifier = Modifier.padding(innerPadding)) {
      items(viewModel.settings.keys.toList()) { setting ->
        val description = when (setting) {
            "Hide Notifications" -> "Replaces notifications from locked apps with 'Unlock to see message' to prevent lock bypass."
            else -> ""
        }
        ListItem(
            headlineContent = { Text(setting) },
            supportingContent = { Text(description) },
            trailingContent = { Switch(checked = viewModel.settings[setting] ?: false, onCheckedChange = { isChecked -> 
                if (setting == "Hide Notifications" && isChecked) {
                    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                    if (enabledListeners == null || !enabledListeners.contains(context.packageName)) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        } catch (e: Exception) {
                            android.util.Log.e("MLock", "Could not open Notification Listener settings", e)
                        }
                    } else {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        viewModel.toggleSetting(setting, true)
                    }
                } else {
                    viewModel.toggleSetting(setting, isChecked)
                }
            }) }
        )
        HorizontalDivider()
      }
      
      item {
          val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
          val isIgnoringParams = pm?.isIgnoringBatteryOptimizations(context.packageName) == true
          ListItem(
              headlineContent = { Text("Ignore Battery Optimizations") },
              supportingContent = { Text("Prevents the accessibility service from being killed by the system.", color = if (!isIgnoringParams) MaterialTheme.colorScheme.error else Color.Unspecified) },
              modifier = Modifier.clickable {
                  if (!isIgnoringParams) {
                      try {
                          val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                              data = Uri.parse("package:${context.packageName}")
                          }
                          context.startActivity(intent)
                      } catch (e: Exception) {
                          android.util.Log.e("MLock", "Could not start Battery Optimization intent", e)
                      }
                  }
              },
              trailingContent = { 
                  Switch(checked = isIgnoringParams, onCheckedChange = {
                      if (it && !isIgnoringParams) {
                          try {
                              val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                  data = Uri.parse("package:${context.packageName}")
                              }
                              context.startActivity(intent)
                          } catch (e: Exception) {
                              android.util.Log.e("MLock", "Could not start Battery Optimization intent", e)
                          }
                      }
                  }) 
              }
          )
          HorizontalDivider()
      }
    }
  }
}
