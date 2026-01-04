import { useState, useEffect } from 'react';
import { dashboardAPI } from '../utils/api';

export const useDashboard = (period: string = 'THIS_MONTH', month?: number, year?: number) => {
  const [kpis, setKpis] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        setLoading(true);
        const response = await dashboardAPI.getDashboard(period, month, year);
        setKpis(response.data);
        setError(null);
      } catch (err: any) {
        setError(err.message || 'Failed to fetch dashboard data');
      } finally {
        setLoading(false);
      }
    };
    
    fetchDashboard();
  }, [period, month, year]);
  
  return { kpis, loading, error };
};

