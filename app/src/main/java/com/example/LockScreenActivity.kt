package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class LockScreenActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        
        enableEdgeToEdge()
        
        val targetPackage = intent.getStringExtra("package_name") ?: ""
        val fromNotification = intent.getBooleanExtra("from_notification", false)
        val originalIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("original_intent", android.app.PendingIntent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<android.app.PendingIntent>("original_intent")
        }
        
        var appNameStr = targetPackage
        var appIconDrawable: android.graphics.drawable.Drawable? = null
        try {
            val pm = packageManager
            val info = pm.getApplicationInfo(targetPackage, 0)
            appNameStr = pm.getApplicationLabel(info).toString()
            appIconDrawable = pm.getApplicationIcon(info)
        } catch (e: Exception) {}
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
            }
        })
        
        setContent {
            MyApplicationTheme {
                LockScreen(targetPackage = targetPackage, appNameData = appNameStr, appIconData = appIconDrawable, onUnlock = {
                    val prefs = getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("current_unlocked_app", targetPackage)
                         .putLong("leave_time_$targetPackage", System.currentTimeMillis())
                         .putLong("just_unlocked_time_$targetPackage", System.currentTimeMillis())
                         .apply()
                         
                    if (fromNotification) {
                        try {
                            originalIntent?.send() ?: packageManager.getLaunchIntentForPackage(targetPackage)?.let { startActivity(it) }
                        } catch (e: Exception) {
                            packageManager.getLaunchIntentForPackage(targetPackage)?.let { startActivity(it) }
                        }
                    }
                    finish()
                })
            }
        }
    }
}

@Composable
fun LockScreen(targetPackage: String, appNameData: String, appIconData: android.graphics.drawable.Drawable?, onUnlock: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
    
    val savedPin = prefs.getString("saved_pin", null)
    val savedPassword = prefs.getString("saved_password", null)
    val savedPattern = prefs.getString("saved_pattern", null)
    val isBiometricEnabled = prefs.getBoolean("biometric_enabled", false)
    
    var appName by remember { mutableStateOf(appNameData) }
    var appIcon by remember { mutableStateOf<android.graphics.drawable.Drawable?>(appIconData) }
    
    var enteredText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showPatternView by remember { mutableStateOf(savedPattern != null) }

    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled) {
            delay(50) // Wait briefly for activity to be visible
            val executor = ContextCompat.getMainExecutor(context)
            var currentContext = context
            var fragmentActivity: FragmentActivity? = null
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is FragmentActivity) {
                    fragmentActivity = currentContext
                    break
                }
                currentContext = currentContext.baseContext
            }
            if (fragmentActivity != null) {
                try {
                    val biometricPrompt = BiometricPrompt(fragmentActivity, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                onUnlock()
                            }
                        })
                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock App")
                        .setSubtitle("Use biometric to unlock")
                        .setNegativeButtonText("Use PIN/Password")
                        .build()
                    biometricPrompt.authenticate(promptInfo)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (appIcon != null) {
                AsyncImage(model = appIcon, contentDescription = appName, modifier = Modifier.size(80.dp))
            } else {
                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Unlock $appName", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Please authenticate to access this app", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (showPatternView) {
                PatternLockView(modifier = Modifier.fillMaxWidth().height(300.dp)) { pattern ->
                    if (pattern == savedPattern) {
                        onUnlock()
                    } else if (pattern.isNotEmpty()) {
                        showError = true
                    }
                }
                
                if (showError) {
                    Text(
                        "Incorrect Pattern", 
                        color = MaterialTheme.colorScheme.error, 
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (savedPin != null || savedPassword != null) {
                    TextButton(onClick = { showPatternView = false; showError = false }) {
                        Text("Use PIN/Password")
                    }
                }
            } else {
                OutlinedTextField(
                    value = enteredText,
                    onValueChange = { 
                        enteredText = it
                        showError = false
                    },
                    label = { Text("Enter PIN/Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (savedPin != null) KeyboardType.NumberPassword else KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    isError = showError,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (showError) {
                    Text(
                        "Incorrect PIN/Password", 
                        color = MaterialTheme.colorScheme.error, 
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if ((savedPin != null && enteredText == savedPin) || 
                            (savedPassword != null && enteredText == savedPassword)) {
                            onUnlock()
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock")
                }

                if (savedPattern != null) {
                    TextButton(onClick = { showPatternView = true; showError = false }) {
                        Text("Use Pattern instead")
                    }
                }
            }
        }
    }
}
