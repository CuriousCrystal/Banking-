import http from './http'

export const authApi = {
  login: (payload) => http.post('/auth/login', payload).then((r) => r.data)
}

export const customerApi = {
  getAll: (params) => http.get('/customers', { params }).then((r) => r.data),
  getById: (id) => http.get(`/customers/${id}`).then((r) => r.data),
  create: (payload) => http.post('/customers', payload).then((r) => r.data),
  update: (id, payload) => http.put(`/customers/${id}`, payload).then((r) => r.data),
  remove: (id) => http.delete(`/customers/${id}`)
}

export const accountApi = {
  getAll: () => http.get('/accounts').then((r) => r.data),
  byCustomer: (customerId) =>
    http.get('/accounts', { params: { customerId } }).then((r) => r.data),
  open: (payload) => http.post('/accounts', payload).then((r) => r.data),
  close: (id) => http.delete(`/accounts/${id}`),
  deposit: (id, amount) => http.patch(`/accounts/${id}/deposit`, { amount }).then((r) => r.data),
  withdraw: (id, amount) => http.patch(`/accounts/${id}/withdraw`, { amount }).then((r) => r.data)
}

export const loanApi = {
  getAll: () => http.get('/loans').then((r) => r.data),
  sanction: (payload) => http.post('/loans', payload).then((r) => r.data),
  byCustomer: (customerId) => http.get(`/loans/customer/${customerId}`).then((r) => r.data)
}

export const userApi = {
  getAll: () => http.get('/users').then((r) => r.data),
  create: (payload) => http.post('/users', payload).then((r) => r.data),
  resetPassword: (id, password) => http.patch(`/users/${id}/password`, { password }),
  remove: (id) => http.delete(`/users/${id}`)
}

export const reportApi = {
  summary: () => http.get('/reports/summary').then((r) => r.data)
}
