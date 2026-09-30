import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000
})

// attach the token to every outgoing request
http.interceptors.request.use((config) => {
  const raw = sessionStorage.getItem('auth')
  if (raw) {
    const { token, expiresAt } = JSON.parse(raw)
    if (expiresAt && expiresAt <= Date.now() && !config.url.includes('/auth/login')) {
      // token already expired - go back to login instead of sending a doomed request
      sessionStorage.removeItem('auth')
      window.location.href = '/login'
      return Promise.reject(new axios.Cancel('Session expired'))
    }
    if (token) config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// turn every backend failure into one predictable object
http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401 && !error.config.url.includes('/auth/login')) {
        sessionStorage.removeItem('auth')
        window.location.href = '/login'
      }
      const fallback = status === 403
        ? 'Your role is not allowed to perform this action'
        : `Request failed (${status})`
      return Promise.reject({
        status,
        message: data?.message || fallback,
        fields: data?.errors && Object.keys(data.errors).length ? data.errors : null
      })
    }
    if (axios.isCancel(error)) {
      return Promise.reject({ message: 'Session expired - please sign in again' })
    }
    if (error.request) {
      return Promise.reject({ message: 'Server not reachable. Is Spring Boot running on 8080?' })
    }
    return Promise.reject({ message: error.message })
  }
)

export default http
