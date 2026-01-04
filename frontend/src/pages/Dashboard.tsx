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
      
      {/* KPI Section - 4 Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
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
        
        {/* KPI 4 - Projected Month-End (Always visible) */}
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
        isMonthly={period === 'YTD'}
        overallAvgPerDay={kpis?.overallAvgPerDay ? Number(kpis.overallAvgPerDay) : undefined}
        period={period}
      />
    </div>
  );
};

export default Dashboard;
