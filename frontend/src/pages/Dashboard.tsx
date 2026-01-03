import { useDashboard } from '../hooks/useDashboard';
import KPICard from '../components/dashboard/KPICard';
import SpendChart from '../components/dashboard/SpendChart';
import CategoryChart from '../components/dashboard/CategoryChart';
import SubscriptionsPanel from '../components/dashboard/SubscriptionsPanel';
import { Wallet, TrendingUp, Calendar, Tag } from 'lucide-react';

const Dashboard = () => {
  const { kpis, loading, error } = useDashboard('month');
  
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
  
  // Transform categories data for pie chart
  const categoryData = kpis?.categories ? 
    Object.entries(kpis.categories).map(([name, value]) => ({
      name,
      value: Math.abs(value as number)
    })) : [];
  
  // Mock chart data
  const spendData = [
    { date: '1', amount: 1200 },
    { date: '5', amount: 1800 },
    { date: '10', amount: 2200 },
    { date: '15', amount: 1500 },
    { date: '20', amount: 2800 },
    { date: '25', amount: 2400 },
    { date: '30', amount: 3100 },
  ];
  
  return (
    <div className="space-y-6">
      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <KPICard
          title="Total Spent This Period"
          value={`${kpis?.totalSpent?.toFixed(2) || '0.00'} TL`}
          change={kpis?.changePercent}
          icon={<Wallet size={32} />}
        />
        
        <KPICard
          title="Average per Day"
          value={`${kpis?.avgPerDay?.toFixed(2) || '0.00'} TL`}
          subtitle="Daily spending average"
          icon={<Calendar size={32} />}
        />
        
        <KPICard
          title="Projected Month-End"
          value={`${kpis?.projectedMonthEnd?.toFixed(2) || '0.00'} TL`}
          subtitle="Based on current rate"
          icon={<TrendingUp size={32} />}
        />
        
        <KPICard
          title="Top Category"
          value={kpis?.topCategory || 'N/A'}
          subtitle={`${kpis?.topCategoryAmount?.toFixed(2) || '0.00'} TL`}
          icon={<Tag size={32} />}
        />
      </div>
      
      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <SpendChart data={spendData} />
        <CategoryChart data={categoryData} />
      </div>
      
      {/* Insights Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <SubscriptionsPanel />
        
        <div className="card">
          <h3 className="text-xl font-semibold mb-4">What Changed?</h3>
          <div className="space-y-3">
            <div className="p-3 bg-background rounded-lg">
              <p className="font-medium">Delivery spending up 25%</p>
              <p className="text-sm text-text-muted">vs last month</p>
            </div>
            <div className="p-3 bg-background rounded-lg">
              <p className="font-medium">New merchant detected</p>
              <p className="text-sm text-text-muted">Sunam Gida</p>
            </div>
          </div>
        </div>
        
        <div className="card">
          <h3 className="text-xl font-semibold mb-4">Largest Spike</h3>
          <div className="p-3 bg-background rounded-lg">
            <p className="font-medium">Paycell/Hessapli MP</p>
            <p className="text-2xl font-bold my-2">2,242.22 TL</p>
            <p className="text-sm text-text-muted">Akaryakıt - Nov 28</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;

