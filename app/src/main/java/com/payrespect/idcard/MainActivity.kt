package com.payrespect.idcard

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.payrespect.idcard.ui.theme.IdcardTheme
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch



val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d("Main Activity", "Hello world!")
        setContent {
            IdcardTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) { innerPadding ->
                    var displaying by remember { mutableStateOf("") }
                    ApduService.status.observe(this) {
                        displaying += "$it\n"
                    }

                    var cardNumInput by remember { mutableStateOf("") }

                    LaunchedEffect(Unit) {
                        cardNumInput = dataStore.data.first()[stringPreferencesKey("K")] ?: "0000000000000"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .pointerInput(Unit) {
                                detectTapGestures(onLongPress = {
                                    displaying = ""
                                })
                            }
                    ) {
                        OutlinedTextField(
                            value = cardNumInput,
                            onValueChange = { value ->
                                cardNumInput = value
                                if(value.length == 13) {
                                    lifecycleScope.launch {
                                        dataStore.edit {
                                            if (value != it[stringPreferencesKey("K")]) {
                                                Log.d("Main Activity", "Updating cardNum to $value")
                                                it[stringPreferencesKey("K")] = value
                                                ApduService.changeCardNum(value)
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(2.dp, 20.dp)
                        )

                        Text(
                            text = displaying,
                            fontSize = 14.sp,
                            lineHeight = 1.2.em
                        )
                    }
                }
            }
        }
    }
}

