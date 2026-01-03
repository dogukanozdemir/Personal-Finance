import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const dashboardAPI = {
  getKPIs: (period: string = 'month') => 
    api.get(`/dashboard/kpis?period=${period}`),
};

export const transactionsAPI = {
  getAll: () => api.get('/transactions'),
  getRecent: (days: number = 30) => api.get(`/transactions/recent?days=${days}`),
  getByRange: (start: string, end: string) => 
    api.get(`/transactions/range?start=${start}&end=${end}`),
  update: (id: number, data: any) => api.put(`/transactions/${id}`, data),
};

export const importAPI = {
  uploadFile: (file: File, accountId?: number) => {
    const formData = new FormData();
    formData.append('file', file);
    if (accountId) {
      formData.append('accountId', accountId.toString());
    }
    return api.post('/import/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
  uploadFiles: async (
    files: File[], 
    onProgress?: (filename: string, status: 'pending' | 'processing' | 'success' | 'error') => void
  ) => {
    const formData = new FormData();
    files.forEach(file => {
      formData.append('files', file);
    });
    
    // Update status for each file
    if (onProgress) {
      files.forEach(file => onProgress(file.name, 'processing'));
    }
    
    try {
      const response = await api.post('/import/upload-multiple', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      // Mark all as success
      if (onProgress) {
        files.forEach(file => onProgress(file.name, 'success'));
      }
      
      return response;
    } catch (error) {
      // Mark all as error
      if (onProgress) {
        files.forEach(file => onProgress(file.name, 'error'));
      }
      throw error;
    }
  },
  getAccounts: () => api.get('/import/accounts'),
  createAccount: (data: any) => api.post('/import/accounts', data),
  deleteAllData: () => api.delete('/import/delete-all'),
};

export const insightsAPI = {
  getInsights: () => api.get('/insights'),
  generateInsights: () => api.post('/insights/generate'),
};

export const aiAPI = {
  chat: (question: string) => api.post('/ai/chat', { question }),
};

export const subscriptionAPI = {
  getPotential: () => api.get('/subscriptions/potential'),
  getActive: () => api.get('/subscriptions/active'),
  confirm: (merchant: string) => api.post('/subscriptions/confirm', { merchant }),
  unmark: (merchant: string) => api.post('/subscriptions/unmark', { merchant }),
};

export default api;

