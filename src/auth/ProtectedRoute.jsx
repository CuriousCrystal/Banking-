import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext.jsx'

export default function ProtectedRoute({ children, role }) {
  const { auth, hasRole } = useAuth()

  if (!auth) return <Navigate to="/login" replace />
  if (role && !hasRole(role)) return <p style={{ padding: 30 }}>Your role cannot open this page.</p>

  return children
}
