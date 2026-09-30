import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../api/api'
import { useAuth } from '../auth/AuthContext.jsx'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const change = (e) => setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    const username = form.username.trim()
    if (!username || !form.password) {
      setError(!username && !form.password ? 'Username and password are required'
        : !username ? 'Username is required' : 'Password is required')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const data = await authApi.login({ username, password: form.password })
      login(data)
      navigate('/dashboard')
    } catch (err) {
      setError(err.fields ? Object.values(err.fields).join(', ') : err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={submit}>
        <div>
          <div className="brand" style={{ fontSize: 22 }}>AMC Bank</div>
          <div className="muted">Sign in to continue</div>
        </div>

        {error && <div className="banner">{error}</div>}

        <div className="field">
          <label htmlFor="username">Username</label>
          <input id="username" name="username" value={form.username} onChange={change} autoComplete="username" />
        </div>

        <div className="field">
          <label htmlFor="password">Password</label>
          <input id="password" name="password" type="password" value={form.password}
                 onChange={change} autoComplete="current-password" />
        </div>

        <button type="submit" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
             </form>
    </div>
  )
}
