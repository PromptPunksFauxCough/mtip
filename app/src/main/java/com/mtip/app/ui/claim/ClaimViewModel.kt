package com.mtip.app.ui.claim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mtip.app.MTipApp
import com.mtip.app.domain.QrCodec
import com.mtip.app.domain.WalletRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Step { SCAN, ADDRESS, SWEEPING, DONE, ERROR }

data class ClaimState(
    val step: Step = Step.SCAN,
    val address: String = "",
    val txHash: String = "",
    val error: String = "",
    val seed: String = "",
    val restoreHeight: Long = 0
)

class ClaimViewModel(
    private val repo: WalletRepository
) : ViewModel() {

    private val _s = MutableStateFlow(ClaimState())
    val state = _s.asStateFlow()

    fun setAddress(a: String) {
        _s.update { it.copy(address = a) }
    }

    fun onScanned(raw: String) {
        val p = try {
            QrCodec.decode(raw)
        } catch (e: Exception) {
            _s.update {
                it.copy(step = Step.ERROR,
                    error = "QR parse: ${e.message}")
            }
            return
        }

        if (p == null) {
            _s.update {
                it.copy(step = Step.ERROR,
                    error = "Invalid ɱTip QR code")
            }
            return
        }

        _s.update {
            it.copy(
                step = Step.ADDRESS,
                seed = p.seed,
                restoreHeight = p.restoreHeight
            )
        }
    }

    fun sweep() {
        val s = _s.value
        if (s.address.length < 95) {
            _s.update {
                it.copy(step = Step.ERROR,
                    error = "Enter a valid destination address")
            }
            return
        }

        _s.update { it.copy(step = Step.SWEEPING) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tx = repo.sweepFromQr(
                    s.seed, s.restoreHeight,
                    s.address, MTipApp.instance.network.id
                )
                _s.update {
                    it.copy(step = Step.DONE, txHash = tx)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _s.update {
                    it.copy(step = Step.ERROR,
                        error = "${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }
    }

    fun reset() {
        _s.value = ClaimState()
    }

    class Factory(
        private val r: WalletRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            c: Class<T>
        ): T = ClaimViewModel(r) as T
    }
}