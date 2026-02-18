package com.mtip.app.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mtip.app.data.db.GiftWallet
import com.mtip.app.domain.QrCodec
import com.mtip.app.domain.WalletRepository
import com.mtip.app.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListState(
    val wallets: List<GiftWallet> = emptyList(),
    val expandedId: Long? = null,
    val busy: Boolean = false,
    val checking: Set<Long> = emptySet(),
    val error: String? = null
)

class WalletListViewModel(private val repo: WalletRepository) : ViewModel() {
    private val _s = MutableStateFlow(ListState())
    val state = _s.asStateFlow()

    init { viewModelScope.launch { repo.wallets.collect { w -> _s.update { it.copy(wallets = w) } } } }

    fun create() { viewModelScope.launch {
        _s.update { it.copy(busy = true, error = null) }
        runCatching { repo.create() }
            .onFailure { e -> _s.update { it.copy(error = e.message) } }
        _s.update { it.copy(busy = false) }
    }}

    fun toggle(id: Long) { _s.update { it.copy(expandedId = if (it.expandedId == id) null else id) } }

    fun check(w: GiftWallet) {
        if (_s.value.checking.isNotEmpty()) return
        viewModelScope.launch {
            _s.update { it.copy(checking = it.checking + w.id, error = null) }
            runCatching { repo.checkBalance(w) }
                .onSuccess { bal -> _s.update { it.copy(error = "Balance: ${Formatters.xmr(bal)}") } }
                .onFailure { e -> _s.update { it.copy(error = e.message) } }
            _s.update { it.copy(checking = it.checking - w.id) }
        }
    }

    fun delete(w: GiftWallet) { viewModelScope.launch { repo.delete(w) } }

    fun qrPayload(w: GiftWallet): String =
        QrCodec.encode(w.primaryAddress, w.seed, w.restoreHeight)

    fun clearError() { _s.update { it.copy(error = null) } }

    class Factory(private val r: WalletRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T = WalletListViewModel(r) as T
    }
}
