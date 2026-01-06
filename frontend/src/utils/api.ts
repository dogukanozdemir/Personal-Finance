import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const dashboardAPI = {
  getDashboard: (period: string = 'THIS_MONTH', month?: number, year?: number) => {
    let url = `/dashboard?period=${period}`;
    if (month !== undefined) {
      url += `&month=${month}`;
    }
    if (year !== undefined) {
      url += `&year=${year}`;
    }
    return api.get(url);
  },
};

export const importAPI = {
  importTransactions: (files: File[]) => {
    const formData = new FormData();
    files.forEach(file => {
      formData.append('files', file);
    });
    return api.post('/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
};

export const settingsAPI = {
  deleteAllData: () => api.delete('/import/delete-all'),
};

export const transactionsAPI = {
  getTransactions: (params: {
    startDate?: string;
    endDate?: string;
    minAmount?: number;
    maxAmount?: number;
    merchant?: string;
    page?: number;
    size?: number;
  }) => {
    const queryParams = new URLSearchParams();
    if (params.startDate) queryParams.append('startDate', params.startDate);
    if (params.endDate) queryParams.append('endDate', params.endDate);
    if (params.minAmount !== undefined) queryParams.append('minAmount', params.minAmount.toString());
    if (params.maxAmount !== undefined) queryParams.append('maxAmount', params.maxAmount.toString());
    if (params.merchant) queryParams.append('merchant', params.merchant);
    if (params.page !== undefined) queryParams.append('page', params.page.toString());
    if (params.size !== undefined) queryParams.append('size', params.size.toString());
    return api.get(`/transactions?${queryParams.toString()}`);
  },
  getMerchants: () => api.get('/transactions/merchants'),
};

export default api;

