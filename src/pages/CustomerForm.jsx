import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { customerApi } from '../api/api'

const EMPTY = { name: '', email: '', city: '', panNumber: '' }

export default function CustomerForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const editing = Boolean(id)

  const [form, setForm] = useState(EMPTY)
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!editing) return
    customerApi.getById(id)
      .then((c) => setForm({ name: c.name, email: c.email, city: c.city, panNumber: c.panNumber || '' }))
      .catch((err) => setError(err.message))
  }, [id, editing])

  const change = (e) => setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setFieldErrors({})
    try {
      if (editing) await customerApi.update(id, form)
      else await customerApi.create(form)
      navigate('/customers')
    } catch (err) {
      setError(err.message)
      if (err.fields) setFieldErrors(err.fields)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h1>{editing ? 'Edit customer' : 'Add customer'}</h1>
      {error && <div className="banner">{error}</div>}

      <form onSubmit={submit} style={{ width: 620, display: 'flex', flexDirection: 'column', gap: 16 }}>
        {['name', 'email', 'panNumber'].map((f) => (
          <div className="field" key={f}>
            <label htmlFor={f}>{f === 'panNumber' ? 'PAN number' : f}</label>
            <input id={f} name={f} value={form[f]} onChange={change}
                   className={fieldErrors[f] ? 'invalid' : ''} />
            {fieldErrors[f] && <span className="err">{fieldErrors[f]}</span>}
          </div>
        ))}

        <div className="field">
          <label htmlFor="city">City</label>
          <select id="city" name="city" value={form.city} onChange={change}>
            <option value="">Select a city</option>
            <option>Bangalore</option>
            <option>Mumbai</option>
            <option>Chennai</option>
          </select>
          {fieldErrors.city && <span className="err">{fieldErrors.city}</span>}
        </div>

        <div style={{ display: 'flex', gap: 12 }}>
          <button type="submit" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
          <button type="button" className="ghost" onClick={() => navigate('/customers')}>Cancel</button>
        </div>
      </form>
    </>
  )
}
