import { useState, useEffect } from 'react';
import { dashboardAPI } from '../utils/api';

export const useDashboard = (period: string = 'month') => {
  const [kpis, setKpis] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    const fetchKPIs = async () => {
      try {
        setLoading(true);
        const response = await dashboardAPI.getKPIs(period);
        setKpis(response.data);
        setError(null);
      } catch (err: any) {
        setError(err.message || 'Failed to fetch dashboard data');
      } finally {
        setLoading(false);
      }
    };
    
    fetchKPIs();
  }, [period]);
  
  return { kpis, loading, error };
};

