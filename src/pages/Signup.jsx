import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../api/api'
import { useAuth } from '../auth/AuthContext.jsx'

// "Acme Savings Bank" -> "acme-savings-bank"
const slugify = (name) =>
  name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 40)

export default function Signup() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ bankName: '', bankCode: '', username: '', password: '' })
  const [codeEdited, setCodeEdited] = useState(false)
  const [error, setError] = useState(null)      // { message, fields }
  const [busy, setBusy] = useState(false)

  const change = (e) => {
    const { name, value } = e.target
    if (name === 'bankCode') setCodeEdited(true)
    setForm((prev) => {
      const next = { ...prev, [name]: value }
      // suggest a bank code from the name until the user types one of their own
      if (name === 'bankName' && !codeEdited) next.bankCode = slugify(value)
      return next
    })
  }

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const data = await authApi.signup({
        ...form, bankName: form.bankName.trim(), username: form.username.trim()
      })
      try { localStorage.setItem('bankCode', form.bankCode) } catch { /* private mode - not important */ }
      login(data)
      navigate('/dashboard')
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  const fieldError = (name) => error?.fields?.[name] && <div className="err">{error.fields[name]}</div>

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={submit}>
        <div>
          <div className="brand" style={{ fontSize: 22 }}>Register your bank</div>
          <div className="muted">Free plan · up to 25 customers and 3 users · upgrade any time</div>
        </div>

        {error && !error.fields && <div className="banner">{error.message}</div>}

        <div className="field">
          <label htmlFor="bankName">Bank name</label>
          <input id="bankName" name="bankName" value={form.bankName} onChange={change}
                 className={error?.fields?.bankName ? 'invalid' : ''} />
          {fieldError('bankName')}
        </div>

        <div className="field">
          <label htmlFor="bankCode">Bank code (your team signs in with this)</label>
          <input id="bankCode" name="bankCode" value={form.bankCode} onChange={change}
                 className={error?.fields?.bankCode ? 'invalid' : ''} autoComplete="off" />
          {fieldError('bankCode')}
        </div>

        <div className="field">
          <label htmlFor="username">Admin username</label>
          <input id="username" name="username" value={form.username} onChange={change}
                 className={error?.fields?.username ? 'invalid' : ''} autoComplete="username" />
          {fieldError('username')}
        </div>

        <div className="field">
          <label htmlFor="password">Admin password (8+ characters)</label>
          <input id="password" name="password" type="password" value={form.password} onChange={change}
                 className={error?.fields?.password ? 'invalid' : ''} autoComplete="new-password" />
          {fieldError('password')}
        </div>

        <button type="submit" disabled={busy}>{busy ? 'Creating…' : 'Create bank'}</button>

        <div className="muted">Already registered? <Link to="/login">Sign in</Link></div>
      </form>
    </div>
  )
}
