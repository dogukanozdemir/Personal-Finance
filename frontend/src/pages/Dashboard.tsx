import { useState, useEffect } from 'react';
import { useDashboard } from '../hooks/useDashboard';
import KPICard from '../components/dashboard/KPICard';
import SpendChart from '../components/dashboard/SpendChart';
import TimeSelector from '../components/dashboard/TimeSelector';
import { Wallet, Calendar, TrendingUp, X } from 'lucide-react';
import { transactionsAPI } from '../utils/api';

const Dashboard = () => {
  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth() + 1; // 1-indexed
  
  const [period, setPeriod] = useState<string>('THIS_MONTH');
  const [month, setMonth] = useState<number | undefined>(undefined);
  const [year, setYear] = useState<number | undefined>(undefined);
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [selectedDateTransactions, setSelectedDateTransactions] = useState<any[]>([]);
  const [loadingTransactions, setLoadingTransactions] = useState(false);
  
  const { kpis, loading, error } = useDashboard(period, month, year);
  
  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-text-muted">Loading...</div>
      </div>
    );
  }
  
  if (error) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-danger">Error: {error}</div>
      </div>
    );
  }

  // Format period subtitle
  const getPeriodSubtitle = () => {
    if (period === 'THIS_MONTH') {
      return 'This Month';
    } else if (period === 'MONTH') {
      if (month && year) {
        const date = new Date(year, month - 1);
        return date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
      }
      return 'Selected Month';
    } else if (period === 'YTD') {
      return year ? `Year to Date ${year}` : 'Year to Date';
    } else if (period === 'YEAR') {
      return year ? `Year ${year}` : 'Selected Year';
    }
    return 'Selected period';
  };

  // Calculate active days from dataPoints (for daily views)
  const activeDays = (period === 'THIS_MONTH' || period === 'MONTH') && kpis?.dataPoints
    ? Object.keys(kpis.dataPoints).length
    : 0;
  const avgPerDaySubtitle = activeDays > 0 
    ? `Based on ${activeDays} active ${activeDays === 1 ? 'day' : 'days'}`
    : 'Based on active days';

  // Get projection value and subtitle
  const getProjectionValue = () => {
    if (period === 'THIS_MONTH' && kpis?.projectedMonthEnd != null) {
      return `${(Number(kpis.projectedMonthEnd) || 0).toFixed(2)} TL`;
    }
    return 'N/A';
  };

  const getProjectionSubtitle = () => {
    if (period === 'THIS_MONTH' && kpis?.projectedMonthEndComparedPercent != null) {
      const percent = Number(kpis.projectedMonthEndComparedPercent);
      const isPositive = percent > 0;
      const colorClass = isPositive ? 'text-danger' : 'text-success';
      const direction = isPositive ? 'more' : 'less';
      return (
        <span className={colorClass}>
          {percent > 0 ? '+' : ''}{percent.toFixed(1)}% {direction} than average
        </span>
      );
    }
    return 'Not available for selected period';
  };

  // Handle period change from TimeSelector
  const handlePeriodChange = (newPeriod: string) => {
    if (newPeriod.startsWith('MONTH:')) {
      // Parse MONTH:year:month format
      const parts = newPeriod.split(':');
      if (parts.length === 3) {
        setPeriod('MONTH');
        setYear(parseInt(parts[1]));
        setMonth(parseInt(parts[2])); // Already 1-indexed from TimeSelector
      }
    } else if (newPeriod.startsWith('YEAR:')) {
      // Parse YEAR:year format
      const parts = newPeriod.split(':');
      if (parts.length === 2) {
        setPeriod('YEAR');
        setYear(parseInt(parts[1]));
        setMonth(undefined);
      }
    } else {
      setPeriod(newPeriod);
      if (newPeriod === 'THIS_MONTH' || newPeriod === 'YTD') {
        setMonth(undefined);
        setYear(undefined);
      }
    }
    // Clear selected date when period changes
    setSelectedDate(null);
    setSelectedDateTransactions([]);
  };

  // Handle day click from chart
  const handleDayClick = async (date: string) => {
    // Only allow day selection for daily views (not YTD or YEAR)
    if (period === 'YTD' || period === 'YEAR') return;
    
    setSelectedDate(date);
    setLoadingTransactions(true);
    
    try {
      const response = await transactionsAPI.getTransactions({
        startDate: date,
        endDate: date,
        size: 100, // Get all transactions for the day
      });
      setSelectedDateTransactions(response.data.content || []);
    } catch (err) {
      console.error('Failed to fetch transactions:', err);
      setSelectedDateTransactions([]);
    } finally {
      setLoadingTransactions(false);
    }
  };

  // Format amount (convert negative to positive for display)
  const formatAmount = (amount: number) => {
    return Math.abs(amount).toFixed(2);
  };

  // Format date for display
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  return (
    <div className="space-y-6">
      {/* Global Time Selector */}
      <TimeSelector 
        period={
          period === 'MONTH' 
            ? `MONTH:${year || currentYear}:${month || currentMonth}` 
            : period === 'YEAR'
            ? `YEAR:${year || currentYear}`
            : period
        } 
        onPeriodChange={handlePeriodChange} 
      />
      
      {/* KPI Section - 4 or 5 Cards depending on period */}
      <div className="grid grid-cols-1 md:grid-cols-4 lg:grid-cols-5 gap-6">
        {/* KPI 1 - Total Spent */}
        <KPICard
          title="Total Spent"
          value={`${(Number(kpis?.totalSpent) || 0).toFixed(2)} TL`}
          subtitle={getPeriodSubtitle()}
          icon={<Wallet size={32} />}
        />
        
        {/* KPI 2 - Average Per Day */}
        <KPICard
          title="Average Per Day"
          value={`${(Number(kpis?.avgPerDay) || 0).toFixed(2)} TL`}
          subtitle={avgPerDaySubtitle}
          icon={<Calendar size={32} />}
        />

        {/* KPI 3 - Overall Average Per Day */}
        <KPICard
          title="Overall Average Per Day"
          value={`${(Number(kpis?.overallAvgPerDay) || 0).toFixed(2)} TL`}
          subtitle="Across all transactions"
          icon={<Calendar size={32} />}
        />
        
        {/* KPI 4 - Average Monthly Spend (shown for YTD and YEAR) */}
        {(period === 'YTD' || period === 'YEAR') && kpis?.avgMonthlySpend != null && (
          <KPICard
            title="Average Monthly Spend"
            value={`${(Number(kpis.avgMonthlySpend) || 0).toFixed(2)} TL`}
            subtitle={period === 'YTD' ? 'Year to Date' : `Year ${year}`}
            icon={<TrendingUp size={32} />}
          />
        )}
        
        {/* KPI 5 - Projected Month-End (Always visible) */}
        <KPICard
          title="Projected Month-End"
          value={getProjectionValue()}
          subtitle={getProjectionSubtitle()}
          icon={<TrendingUp size={32} />}
        />
      </div>
      
      {/* Main Chart Section */}
      <SpendChart 
        dataPoints={kpis?.dataPoints}
        isMonthly={period === 'YTD' || period === 'YEAR'}
        overallAvgPerDay={kpis?.overallAvgPerDay ? Number(kpis.overallAvgPerDay) : undefined}
        avgMonthlySpend={kpis?.avgMonthlySpend ? Number(kpis.avgMonthlySpend) : undefined}
        period={period}
        onDayClick={handleDayClick}
        selectedDate={selectedDate}
      />

      {/* Selected Day Transactions */}
      {selectedDate && (
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-xl font-semibold">Transactions for {formatDate(selectedDate)}</h3>
              <p className="text-text-muted text-sm mt-1">
                {selectedDateTransactions.length} transaction{selectedDateTransactions.length !== 1 ? 's' : ''}
              </p>
            </div>
            <button
              onClick={() => {
                setSelectedDate(null);
                setSelectedDateTransactions([]);
              }}
              className="p-2 hover:bg-card-hover rounded-lg transition-colors"
              title="Close"
            >
              <X size={20} className="text-text-muted" />
            </button>
          </div>

          {loadingTransactions ? (
            <div className="flex items-center justify-center py-12">
              <div className="text-text-muted">Loading transactions...</div>
            </div>
          ) : selectedDateTransactions.length === 0 ? (
            <div className="flex items-center justify-center py-12 text-text-muted">
              No transactions found for this day
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-card-hover border-b border-gray-800">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                      Merchant
                    </th>
                    <th className="px-6 py-3 text-right text-xs font-medium text-text-muted uppercase tracking-wider">
                      Amount
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-text-muted uppercase tracking-wider">
                      Description
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-800">
                  {selectedDateTransactions.map((transaction) => (
                    <tr key={transaction.id} className="hover:bg-card-hover transition-colors">
                      <td className="px-6 py-4 text-sm">
                        <div className="font-medium">{transaction.merchant}</div>
                        {transaction.isSubscription && (
                          <span className="text-xs text-primary">Subscription</span>
                        )}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-right font-medium text-danger">
                        {formatAmount(transaction.amount)} TL
                      </td>
                      <td className="px-6 py-4 text-sm text-text-muted max-w-md truncate">
                        {transaction.rawDescription || '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default Dashboard;
