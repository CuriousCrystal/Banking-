import { useEffect, useState } from 'react'
import { userApi } from '../api/api'
import { useAuth } from '../auth/AuthContext.jsx'

const EMPTY = { username: '', password: '', role: 'USER' }

export default function Users() {
  const { auth } = useAuth()
  const [users, setUsers] = useState([])
  const [form, setForm] = useState(EMPTY)
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState(null)
  const [message, setMessage] = useState(null)
  const [busy, setBusy] = useState(false)

  const load = () => userApi.getAll().then(setUsers).catch((err) => setError(err.message))
  useEffect(() => { load() }, [])

  const change = (e) => setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))

  const create = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setMessage(null)
    setFieldErrors({})
    try {
      // the password goes to the backend in plain text (over HTTPS in production);
      // UserService hashes it with BCrypt before saving - the browser never hashes
      const created = await userApi.create({ ...form, username: form.username.trim() })
      setMessage(`User "${created.username}" created as ${created.role}. They can log in now.`)
      setForm(EMPTY)
      load()
    } catch (err) {
      setError(err.message)
      if (err.fields) setFieldErrors(err.fields)
    } finally {
      setBusy(false)
    }
  }

  const reset = async (u) => {
    const password = window.prompt(`New password for ${u.username} (6 to 72 characters):`)
    if (!password) return
    setError(null)
    setMessage(null)
    try {
      await userApi.resetPassword(u.userId, password)
      setMessage(`Password for "${u.username}" changed.`)
    } catch (err) {
      setError(err.message)
    }
  }

  const remove = async (u) => {
    if (!window.confirm(`Delete user ${u.username}?`)) return
    setError(null)
    setMessage(null)
    try {
      await userApi.remove(u.userId)
      setUsers((prev) => prev.filter((x) => x.userId !== u.userId))
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <div>
        <h1>Users</h1>
        <div className="muted">Who can log in to the app. Passwords are stored as BCrypt hashes.</div>
      </div>

      {error && <div className="banner">{error}</div>}
      {message && <div className="ok">{message}</div>}

      <form className="card" onSubmit={create}
            style={{ display: 'flex', gap: 16, alignItems: 'flex-start', flexWrap: 'wrap' }}>
        <div className="field" style={{ width: 220 }}>
          <label htmlFor="u-name">Username</label>
          <input id="u-name" name="username" value={form.username} onChange={change}
                 autoComplete="off" className={fieldErrors.username ? 'invalid' : ''} />
          {fieldErrors.username && <span className="err">{fieldErrors.username}</span>}
        </div>
        <div className="field" style={{ width: 220 }}>
          <label htmlFor="u-pass">Password</label>
          <input id="u-pass" name="password" type="password" value={form.password} onChange={change}
                 autoComplete="new-password" className={fieldErrors.password ? 'invalid' : ''} />
          {fieldErrors.password && <span className="err">{fieldErrors.password}</span>}
        </div>
        <div className="field" style={{ width: 180 }}>
          <label htmlFor="u-role">Role</label>
          <select id="u-role" name="role" value={form.role} onChange={change}>
            <option value="USER">USER (clerk)</option>
            <option value="OFFICER">OFFICER</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </div>
        <button type="submit" disabled={busy} style={{ marginTop: 26 }}>{busy ? 'Creating…' : 'Create user'}</button>
      </form>

      <table>
        <thead><tr><th>Id</th><th>Username</th><th>Role</th><th>Actions</th></tr></thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.userId}>
              <td>{u.userId}</td>
              <td>{u.username}</td>
              <td>{u.role}</td>
              <td style={{ display: 'flex', gap: 10 }}>
                <button className="link" onClick={() => reset(u)}>Reset password</button>
                {u.username !== auth.username && (
                  <button className="link" onClick={() => remove(u)}>Delete</button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  )
}
