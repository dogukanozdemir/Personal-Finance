import { useState } from 'react';
import { useDashboard } from '../hooks/useDashboard';
import KPICard from '../components/dashboard/KPICard';
import SpendChart from '../components/dashboard/SpendChart';
import TimeSelector from '../components/dashboard/TimeSelector';
import { Wallet, Calendar, TrendingUp } from 'lucide-react';

const Dashboard = () => {
  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth() + 1; // 1-indexed
  
  const [period, setPeriod] = useState<string>('THIS_MONTH');
  const [month, setMonth] = useState<number | undefined>(undefined);
  const [year, setYear] = useState<number | undefined>(undefined);
  
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

  // Determine if projection should be shown (for THIS_MONTH only, and only if projectedMonthEnd is not null)
  const shouldShowProjection = () => {
    return period === 'THIS_MONTH' && kpis?.projectedMonthEnd != null;
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
    } else {
      setPeriod(newPeriod);
      if (newPeriod === 'THIS_MONTH' || newPeriod === 'YTD') {
        setMonth(undefined);
        setYear(undefined);
      }
    }
  };

  return (
    <div className="space-y-6">
      {/* Global Time Selector */}
      <TimeSelector period={period === 'MONTH' ? `MONTH:${year || currentYear}:${month || currentMonth}` : period} onPeriodChange={handlePeriodChange} />
      
      {/* KPI Section - 3 Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
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
        
        {/* KPI 3 - Projection (Conditional) */}
        {shouldShowProjection() && kpis?.projectedMonthEnd != null && (
          <KPICard
            title="Projected Month-End"
            value={`${(Number(kpis.projectedMonthEnd) || 0).toFixed(2)} TL`}
            subtitle="Based on historical patterns and current pace"
            icon={<TrendingUp size={32} />}
          />
        )}
      </div>
      
      {/* Main Chart Section */}
      <SpendChart 
        dataPoints={kpis?.dataPoints}
        isMonthly={period === 'YTD'}
      />
    </div>
  );
};

export default Dashboard;
