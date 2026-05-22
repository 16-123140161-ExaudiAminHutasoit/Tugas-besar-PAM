package com.example.mapenumkm.presentation.screens.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapenumkm.domain.model.Transaction
import com.example.mapenumkm.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

data class ReportUiState(
    val totalSales: Double = 0.0,
    val totalTransactions: Int = 0,
    val totalProductsSold: Int = 0,
    val averageTransactionValue: Double = 0.0,
    val transactions: List<Transaction> = emptyList(),
    val selectedFilter: ReportFilter = ReportFilter.DAILY,
    val isLoading: Boolean = true
)

enum class ReportFilter {
    DAILY, WEEKLY, MONTHLY
}

class ReportViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(ReportFilter.DAILY)

    val uiState: StateFlow<ReportUiState> = combine(
        transactionRepository.getAllTransactions(),
        _filter
    ) { transactions, filter ->
        val now = Clock.System.now()
        val systemTZ = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(systemTZ).date

        val filteredTransactions = when (filter) {
            ReportFilter.DAILY -> transactions.filter {
                it.createdAt.toLocalDateTime(systemTZ).date == today
            }
            ReportFilter.WEEKLY -> {
                val startOfWeek = now.minus(7, DateTimeUnit.DAY, systemTZ)
                transactions.filter { it.createdAt >= startOfWeek }
            }
            ReportFilter.MONTHLY -> {
                val startOfMonth = now.toLocalDateTime(systemTZ).let {
                    it.date.minus(it.dayOfMonth - 1, DateTimeUnit.DAY)
                }
                transactions.filter {
                    it.createdAt.toLocalDateTime(systemTZ).date >= startOfMonth
                }
            }
        }

        val totalSales = filteredTransactions.sumOf { it.total }
        val totalProductsSold = filteredTransactions.sumOf { t -> t.items.sumOf { it.quantity } }

        ReportUiState(
            totalSales = totalSales,
            totalTransactions = filteredTransactions.size,
            totalProductsSold = totalProductsSold,
            averageTransactionValue = if (filteredTransactions.isNotEmpty()) totalSales / filteredTransactions.size else 0.0,
            transactions = filteredTransactions,
            selectedFilter = filter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportUiState()
    )

    fun onFilterSelected(filter: ReportFilter) {
        _filter.value = filter
    }
}
