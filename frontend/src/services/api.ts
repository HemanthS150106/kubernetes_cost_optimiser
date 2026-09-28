import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to inject JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle token expiry (401 errors)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authService = {
  login: async (credentials: any) => {
    const response = await api.post('/api/v1/auth/login', credentials);
    return response.data;
  },
  register: async (user: any) => {
    const response = await api.post('/api/v1/auth/register', user);
    return response.data;
  },
};

export const clusterService = {
  getOverview: async () => {
    const response = await api.get('/api/v1/cluster/overview');
    return response.data;
  },
  getNamespaces: async () => {
    const response = await api.get('/api/v1/cluster/namespaces');
    return response.data;
  },
};

export const recommendationService = {
  getRecommendations: async (status?: string, namespace?: string) => {
    const params: any = {};
    if (status) params.status = status;
    if (namespace && namespace !== 'all') params.namespace = namespace;
    const response = await api.get('/api/v1/recommendations', { params });
    return response.data;
  },
  updateStatus: async (id: number, status: string) => {
    const response = await api.put(`/api/v1/recommendations/${id}/status?status=${status}`);
    return response.data;
  },
  triggerScan: async () => {
    const response = await api.post('/api/v1/recommendations/scan');
    return response.data;
  },
  deleteRecommendation: async (id: number) => {
    const response = await api.delete(`/api/v1/recommendations/${id}`);
    return response.data;
  },
};

export const managementService = {
  scaleDeployment: async (namespace: string, name: string, replicas: number) => {
    const response = await api.post(`/api/v1/management/scale?namespace=${namespace}&name=${name}&replicas=${replicas}`);
    return response.data;
  },
  restartDeployment: async (namespace: string, name: string) => {
    const response = await api.post(`/api/v1/management/restart?namespace=${namespace}&name=${name}`);
    return response.data;
  },
  deletePod: async (namespace: string, name: string) => {
    const response = await api.delete(`/api/v1/management/pods?namespace=${namespace}&name=${name}`);
    return response.data;
  },
  cordonNode: async (name: string, cordon: boolean) => {
    const response = await api.post(`/api/v1/management/nodes/cordon?name=${name}&cordon=${cordon}`);
    return response.data;
  },
  drainNode: async (name: string) => {
    const response = await api.post(`/api/v1/management/nodes/drain?name=${name}`);
    return response.data;
  },
  applyRecommendation: async (id: number) => {
    const response = await api.post(`/api/v1/management/recommendations/${id}/apply`);
    return response.data;
  },
  rollbackRecommendation: async (auditLogId: number) => {
    const response = await api.post(`/api/v1/management/recommendations/rollback?auditLogId=${auditLogId}`);
    return response.data;
  },
  getAuditLogs: async () => {
    const response = await api.get('/api/v1/management/audit-logs');
    return response.data;
  },
};

export const temporalService = {
  getProfiles: async () => {
    const response = await api.get('/api/v1/temporal/profiles');
    return response.data;
  },
  getSummary: async () => {
    const response = await api.get('/api/v1/temporal/summary');
    return response.data;
  },
};
