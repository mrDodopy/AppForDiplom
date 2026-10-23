package com.roamly.app.entity.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roamly.app.data.source.UserDataSource
import com.roamly.app.entity.Role
import com.roamly.app.entity.User
import com.roamly.app.factory.RetrofitFactory
import com.roamly.app.websocket.SockJSManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userDataSource: UserDataSource,
    private val application: Application
) : ViewModel() {

    val user = userDataSource.currentUser

    private val _isServerConnected = MutableStateFlow(false)
    val isServerConnected = _isServerConnected.asStateFlow()

    init {
        // Убрана проверка подключения, так как endpoint /ping не реализован на сервере
        // Если нужен, добавьте на бэкенде @GET("ping") и верните "pong"

        // Слушаем изменения пользователя для WebSocket подключения
        viewModelScope.launch {
            user.collect { currentUser ->
                // Если пользователь залогинен - подключаем WebSocket
                if (currentUser.id != null && currentUser.role != Role.UnRegistered) {
                    val userId = currentUser.id.toString()
                    Log.d("UserViewModel", "Пользователь залогинен, запускаем WebSocket для userId: $userId")

                    // Проверяем, не подключен ли уже WebSocket
                    val sockJSManager = SockJSManager.getInstance()
                    if (!sockJSManager.isConnected()) {
                        launchWebSocketConnection(userId)
                    }
                } else {
                    // Если пользователь вышел - отключаем WebSocket
                    SockJSManager.getInstance().disconnect()
                }
            }
        }
    }

    fun loginUser(login: String, password: String, onResult: (User?) -> Unit) {
        viewModelScope.launch {
            try {
                val loginData = User(login = login, password = password)
                val response = withContext(Dispatchers.IO) {
                    apiService.loginUser(loginData)
                }

                if (response != null) {
                    val loggedInUser = response.copy(password = "")
                    userDataSource.saveUserState(loggedInUser)

                    // 🔥 WebSocket подключение запускается автоматически в init через collect
                    onResult(loggedInUser)
                    Log.d("UserViewModel", "Авторизация успешна: ${getAllData()}")
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Ошибка авторизации: ${e.message}")
                onResult(null)
            }
        }
    }

    private fun launchWebSocketConnection(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Даем время на инициализацию других компонентов
            kotlinx.coroutines.delay(500)

            val sockJSManager = SockJSManager.getInstance()

            Log.d("UserViewModel", "Starting SockJS WebSocket connection for user $userId")

            // Проверяем, не пытаемся ли уже подключиться
            if (!sockJSManager.isConnected()) {
                sockJSManager.connectWithUser(userId)
            } else {
                Log.d("UserViewModel", "SockJS уже подключен для пользователя $userId")
            }
        }
    }

    fun registerUser(name: String, login: String, password: String, onResult: (User?) -> Unit) {
        viewModelScope.launch {
            try {
                val newUser = User(name = name, login = login, password = password)
                val newId = withContext(Dispatchers.IO) {
                    apiService.createUser(newUser)
                }

                val registeredUser = newUser.copy(id = newId, role = Role.Registered, password = "")
                userDataSource.saveUserState(registeredUser)

                // 🔥 WebSocket подключение запускается автоматически в init через collect
                onResult(registeredUser)
                Log.d("UserViewModel", "Регистрация успешна: ${getAllData()}")

            } catch (e: Exception) {
                Log.e("UserViewModel", "Ошибка регистрации: ${e.message}")
                onResult(null)
            }
        }
    }

    fun logout() {
        // Сначала отключаем WebSocket
        SockJSManager.getInstance().disconnect()
        // Затем очищаем данные пользователя
        userDataSource.clearUserState()
        Log.d("UserViewModel", "Пользователь вышел из системы")
    }

    fun updateRole(newRole: Role) {
        userDataSource.updateRole(newRole)
    }

    fun getId(): Long? = userDataSource.currentUserId

    fun isLoggedIn(): Boolean = user.value.role != Role.UnRegistered
    fun isAdmin(): Boolean = user.value.role == Role.AdminOfApp || user.value.role == Role.AdminOfInstitution
    fun getAllData(): String = user.value.let {
        "Name: ${it.name}, Role: ${it.role}, Login: ${it.login}, Password: [HIDDEN], Id: ${it.id}"
    }

    fun updateUserProfile(
        newName: String?,
        newLogin: String?,
        newPassword: String?,
        onSuccess: () -> Unit,
        onError: (String?) -> Unit
    ) {
        val currentUser = user.value
        val userId = currentUser.id ?: return onError("Пользователь не авторизован")

        viewModelScope.launch {
            try {
                // Сначала обновляем основные данные, если есть изменения
                val hasProfileChanges = newName != null || newLogin != null
                if (hasProfileChanges) {
                    val updateData = User(
                        id = userId,
                        name = newName ?: currentUser.name,
                        login = newLogin ?: currentUser.login,
                        role = currentUser.role
                    )
                    withContext(Dispatchers.IO) {
                        apiService.updateUser(updateData)
                    }
                }

                // Затем обновляем пароль, если указан
                if (newPassword != null) {
                    val passwordUpdate = User(id = userId, password = newPassword)
                    withContext(Dispatchers.IO) {
                        apiService.updateUserPassword(passwordUpdate)
                    }
                }

                // Обновляем локальное состояние
                val updatedUser = currentUser.copy(
                    name = newName ?: currentUser.name,
                    login = newLogin ?: currentUser.login
                )
                userDataSource.saveUserState(updatedUser)

                onSuccess()
                Log.d("UserViewModel", "Профиль обновлен: ${getAllData()}")

            } catch (e: Exception) {
                Log.e("UserViewModel", "Ошибка обновления профиля: ${e.message}")
                onError(e.message)
            }
        }
    }

    private val apiService by lazy {
        RetrofitFactory.create()
    }
}