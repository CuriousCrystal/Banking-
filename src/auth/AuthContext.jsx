import { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => {
    try {
      const saved = JSON.parse(sessionStorage.getItem('auth'))
      // an expired token would make every API call fail - start logged out instead
      if (saved?.token && (!saved.expiresAt || saved.expiresAt > Date.now())) return saved
    } catch { /* corrupt entry - ignore */ }
    sessionStorage.removeItem('auth')
    return null
  })

  const login = (data) => {
    const session = { ...data, expiresAt: Date.now() + (data.expiresInMs || 3600000) }
    sessionStorage.setItem('auth', JSON.stringify(session))
    setAuth(session)
  }

  const logout = () => {
    sessionStorage.removeItem('auth')
    setAuth(null)
  }

  // accepts one role or a list: hasRole('ROLE_ADMIN') / hasRole(['ROLE_ADMIN', 'ROLE_OFFICER'])
  const hasRole = (role) => !!auth && [].concat(role).some((r) => auth.roles?.includes(r))

  return (
    <AuthContext.Provider value={{ auth, login, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
