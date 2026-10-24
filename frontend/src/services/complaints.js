import api from './api';

export const complaintsService = {
  // Citizen: submit complaint
  create: (data) => api.post('/complaints', data),

  // Citizen: get my complaints
  getMy: (params) => api.get('/complaints/my', { params }),

  // Officer/Admin: get all complaints
  getAll: (params) => api.get('/complaints', { params }),

  // Get single complaint
  getById: (id) => api.get(`/complaints/${id}`),

  // Officer/Admin: update complaint status
  updateStatus: (id, data) => api.patch(`/complaints/${id}/status`, data),

  // Officer/Admin: assign to officer
  assign: (id, data) => api.post(`/complaints/${id}/assign`, data),

  // Get status history
  getHistory: (id) => api.get(`/complaints/${id}/history`),

  // Upload image
  uploadImage: (id, file, imageType) => {
    const form = new FormData();
    form.append('file', file);
    if (imageType) form.append('imageType', imageType);
    return api.post(`/complaints/${id}/images`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  // Get complaint images list
  getImages: (id) => api.get(`/complaints/${id}/images`),
};

export const notificationsService = {
  getAll: (params) => api.get('/notifications', { params }),
  markRead: (id) => api.patch(`/notifications/${id}/read`),
  markAllRead: () => api.patch('/notifications/read-all'),
  getUnreadCount: () => api.get('/notifications/unread-count'),
};

export const authService = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  me: () => api.get('/auth/me'),
};

export const adminService = {
  // List users, optionally filtered by role: ROLE_OFFICER, ROLE_CITIZEN, ROLE_ADMIN
  listUsers: (params) => api.get('/admin/users', { params }),
  // Get single user
  getUser: (id) => api.get(`/admin/users/${id}`),
  // Deactivate user
  deactivateUser: (id) => api.patch(`/admin/users/${id}/deactivate`),
  // Activate user
  activateUser: (id) => api.patch(`/admin/users/${id}/activate`),
};
