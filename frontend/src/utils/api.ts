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

export default api;

