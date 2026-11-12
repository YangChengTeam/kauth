package com.yangfeisg.kauth.feature.setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yangfeisg.kauth.core.data.AccountRepository
import com.yangfeisg.kauth.core.model.Account
import com.yangfeisg.kauth.core.model.OtpType
import com.yangfeisg.kauth.core.otp.Base32
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The "enter a setup key" form. */
@HiltViewModel
internal class SetupViewModel @Inject constructor(
    private val repository: AccountRepository,
) : ViewModel() {

    var name by mutableStateOf("")
        private set
    var key by mutableStateOf("")
        private set
    var type by mutableStateOf(OtpType.TOTP)
        private set
    var keyInvalid by mutableStateOf(false)
        private set
    private var saving by mutableStateOf(false)

    val canSave: Boolean
        get() = name.isNotBlank() && key.isNotBlank() && !saving

    fun onNameChange(value: String) {
        name = value
    }

    fun onKeyChange(value: String) {
        key = value
        keyInvalid = false
    }

    fun onTypeChange(value: OtpType) {
        type = value
    }

    fun save(onSaved: () -> Unit) {
        if (!canSave) return
        if (!Base32.isValid(key)) {
            keyInvalid = true
            return
        }
        saving = true
        viewModelScope.launch {
            repository.add(Account(name = name.trim(), secret = Base32.normalize(key), type = type))
            onSaved()
        }
    }
}
