package com.hiregrid.app.presentation.auth

import com.hiregrid.app.core.datastore.SessionManager
import com.hiregrid.app.core.network.ApiService
import com.hiregrid.app.data.repository.AuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class AuthViewModelTest {

    @Test
    fun testRoleSelectionUpdatesRole() {
        val mockApiService = Mockito.mock(ApiService::class.java)
        val mockSession = Mockito.mock(SessionManager::class.java)
        val repo = AuthRepository(mockApiService, mockSession)
        val viewModel = AuthViewModel(repo)

        assertEquals("candidate", viewModel.selectedRole.value)

        viewModel.onRoleSelected("employer")
        assertEquals("employer", viewModel.selectedRole.value)

        viewModel.companyName.value = "Acme Corp"
        viewModel.onRoleSelected("candidate")
        assertEquals("candidate", viewModel.selectedRole.value)
        assertEquals("", viewModel.companyName.value)
    }

    @Test
    fun testInitialUiStateIsIdle() {
        val mockApiService = Mockito.mock(ApiService::class.java)
        val mockSession = Mockito.mock(SessionManager::class.java)
        val repo = AuthRepository(mockApiService, mockSession)
        val viewModel = AuthViewModel(repo)

        assertTrue(viewModel.uiState.value is AuthUiState.Idle)
    }
}
