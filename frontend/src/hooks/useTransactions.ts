import { useState, useEffect } from 'react';
import { transactionsAPI } from '../utils/api';

export const useTransactions = (days: number = 30) => {
  const [transactions, setTransactions] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    const fetchTransactions = async () => {
      try {
        setLoading(true);
        const response = await transactionsAPI.getRecent(days);
        setTransactions(response.data);
        setError(null);
      } catch (err: any) {
        setError(err.message || 'Failed to fetch transactions');
      } finally {
        setLoading(false);
      }
    };
    
    fetchTransactions();
  }, [days]);
  
  return { transactions, loading, error, refetch: () => {} };
};

