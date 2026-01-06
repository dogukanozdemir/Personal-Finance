import { useState, useEffect } from 'react';
import { transactionsAPI } from '../utils/api';
import { Search, X, Calendar, DollarSign } from 'lucide-react';

interface Transaction {
  id: number;
  transactionDate: string;
  merchant: string;
  amount: number;
  balance: number;
  transactionId?: string;
  isSubscription: boolean;
  rawDescription?: string;
  importTimestamp: string;
}

interface TransactionFilters {
  startDate?: string;
  endDate?: string;
  minAmount?: number;
  maxAmount?: number;
  merchant?: string;
}

const Transactions = () => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState<TransactionFilters>({});
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [merchants, setMerchants] = useState<string[]>([]);
  const [showFilters, setShowFilters] = useState(false);
  const pageSize = 50;

  useEffect(() => {
    fetchTransactions();
    fetchMerchants();
  }, [filters, currentPage]);

  const fetchTransactions = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await transactionsAPI.getTransactions({
        ...filters,
        page: currentPage,
        size: pageSize,
      });
      setTransactions(response.data.content);
      setTotalPages(response.data.totalPages);
      setTotalElements(response.data.totalElements);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch transactions');
    } finally {
      setLoading(false);
    }
  };

  const fetchMerchants = async () => {
    try {
      const response = await transactionsAPI.getMerchants();
      setMerchants(response.data);
    } catch (err) {
      // Silently fail - merchants are optional
    }
  };

  const handleFilterChange = (key: keyof TransactionFilters, value: any) => {
    setFilters((prev) => ({ ...prev, [key]: value || undefined }));
    setCurrentPage(0);
  };

  const clearFilters = () => {
    setFilters({});
    setCurrentPage(0);
  };

  const applyQuickFilter = (type: 'last7days' | 'thismonth' | 'large') => {
    const today = new Date();
    const newFilters: TransactionFilters = {};

    if (type === 'last7days') {
      const sevenDaysAgo = new Date(today);
      sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
      newFilters.startDate = sevenDaysAgo.toISOString().split('T')[0];
      newFilters.endDate = today.toISOString().split('T')[0];
    } else if (type === 'thismonth') {
      const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
      newFilters.startDate = firstDay.toISOString().split('T')[0];
      newFilters.endDate = today.toISOString().split('T')[0];
    } else if (type === 'large') {
      // Large transactions: > 500 TL
      newFilters.minAmount = 500;
    }

    setFilters(newFilters);
    setCurrentPage(0);
  };

  const formatAmount = (amount: number) => {
    // Amounts are negative in the database, convert to positive for display
    return Math.abs(amount).toFixed(2);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const hasActiveFilters = Object.keys(filters).length > 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Transactions</h1>
          <p className="text-text-muted mt-1">
            {totalElements.toLocaleString()} transaction{totalElements !== 1 ? 's' : ''} found
          </p>
        </div>
        <button
          onClick={() => setShowFilters(!showFilters)}
          className="btn-secondary flex items-center space-x-2"
        >
          <Search size={18} />
          <span>Filters</span>
          {hasActiveFilters && (
            <span className="bg-primary text-white rounded-full px-2 py-0.5 text-xs">
              {Object.keys(filters).length}
            </span>
          )}
        </button>
      </div>

      {/* Quick Filters */}
      <div className="flex flex-wrap gap-2">
        <button
          onClick={() => applyQuickFilter('last7days')}
          className="px-4 py-2 bg-card border border-gray-700 rounded-lg hover:bg-card-hover transition-colors text-sm"
        >
          Last 7 days
        </button>
        <button
          onClick={() => applyQuickFilter('thismonth')}
          className="px-4 py-2 bg-card border border-gray-700 rounded-lg hover:bg-card-hover transition-colors text-sm"
        >
          This month
        </button>
        <button
          onClick={() => applyQuickFilter('large')}
          className="px-4 py-2 bg-card border border-gray-700 rounded-lg hover:bg-card-hover transition-colors text-sm"
        >
          Large transactions (&gt; 500 TL)
        </button>
        {hasActiveFilters && (
          <button
            onClick={clearFilters}
            className="px-4 py-2 bg-card border border-gray-700 rounded-lg hover:bg-card-hover transition-colors text-sm flex items-center space-x-1"
          >
            <X size={14} />
            <span>Clear all</span>
          </button>
        )}
      </div>

      {/* Filter Panel */}
      {showFilters && (
        <div className="card p-4 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Start Date */}
            <div>
              <label className="block text-sm text-text-muted mb-2">Start Date</label>
              <div className="relative">
                <Calendar size={18} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-text-muted" />
                <input
                  type="date"
                  value={filters.startDate || ''}
                  onChange={(e) => handleFilterChange('startDate', e.target.value)}
                  className="w-full pl-10 pr-4 py-2 bg-background border border-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                />
              </div>
            </div>

            {/* End Date */}
            <div>
              <label className="block text-sm text-text-muted mb-2">End Date</label>
              <div className="relative">
                <Calendar size={18} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-text-muted" />
                <input
                  type="date"
                  value={filters.endDate || ''}
                  onChange={(e) => handleFilterChange('endDate', e.target.value)}
                  className="w-full pl-10 pr-4 py-2 bg-background border border-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                />
              </div>
            </div>

            {/* Min Amount */}
            <div>
              <label className="block text-sm text-text-muted mb-2">Min Amount (TL)</label>
              <div className="relative">
                <DollarSign size={18} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-text-muted" />
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={filters.minAmount || ''}
                  onChange={(e) =>
                    handleFilterChange('minAmount', e.target.value ? parseFloat(e.target.value) : undefined)
                  }
                  placeholder="0.00"
                  className="w-full pl-10 pr-4 py-2 bg-background border border-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                />
              </div>
            </div>

            {/* Max Amount */}
            <div>
              <label className="block text-sm text-text-muted mb-2">Max Amount (TL)</label>
              <div className="relative">
                <DollarSign size={18} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-text-muted" />
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={filters.maxAmount || ''}
                  onChange={(e) =>
                    handleFilterChange('maxAmount', e.target.value ? parseFloat(e.target.value) : undefined)
                  }
                  placeholder="0.00"
                  className="w-full pl-10 pr-4 py-2 bg-background border border-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                />
              </div>
            </div>
          </div>

          {/* Merchant Search */}
          <div>
            <label className="block text-sm text-text-muted mb-2">Merchant</label>
            <div className="relative">
              <Search size={18} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-text-muted" />
              <input
                type="text"
                value={filters.merchant || ''}
                onChange={(e) => handleFilterChange('merchant', e.target.value)}
                placeholder="Search by merchant name..."
                className="w-full pl-10 pr-4 py-2 bg-background border border-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
          </div>
        </div>
      )}

      {/* Error State */}
      {error && (
        <div className="card p-4 bg-danger/10 border border-danger/20">
          <p className="text-danger">{error}</p>
        </div>
      )}

      {/* Loading State */}
      {loading && (
        <div className="flex items-center justify-center py-12">
          <div className="text-text-muted">Loading transactions...</div>
        </div>
      )}

      {/* Transactions Table */}
      {!loading && !error && (
        <>
          <div className="card overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-card-hover border-b border-gray-800">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                      Date
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                      Merchant
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-text-muted uppercase tracking-wider">
                      Amount
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-text-muted uppercase tracking-wider">
                      Balance
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                      Description
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-800">
                  {transactions.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="px-6 py-12 text-center text-text-muted">
                        No transactions found
                      </td>
                    </tr>
                  ) : (
                    transactions.map((transaction) => (
                      <tr key={transaction.id} className="hover:bg-card-hover transition-colors">
                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                          {formatDate(transaction.transactionDate)}
                        </td>
                        <td className="px-6 py-4 text-sm">
                          <div className="font-medium">{transaction.merchant}</div>
                          {transaction.isSubscription && (
                            <span className="text-xs text-primary">Subscription</span>
                          )}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-right font-medium text-danger">
                          {formatAmount(transaction.amount)} TL
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-text-muted">
                          {transaction.balance ? `${transaction.balance.toFixed(2)} TL` : '-'}
                        </td>
                        <td className="px-6 py-4 text-sm text-text-muted max-w-md truncate">
                          {transaction.rawDescription || '-'}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between">
              <div className="text-sm text-text-muted">
                Page {currentPage + 1} of {totalPages}
              </div>
              <div className="flex space-x-2">
                <button
                  onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                  disabled={currentPage === 0}
                  className="px-4 py-2 bg-card border border-gray-700 rounded-lg hover:bg-card-hover transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <button
                  onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={currentPage >= totalPages - 1}
                  className="px-4 py-2 bg-card border border-gray-700 rounded-lg hover:bg-card-hover transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default Transactions;

