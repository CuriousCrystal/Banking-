import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { accountApi } from '../api/api'

export default function AccountOpen() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [form, setForm] = useState({ accountType: 'SAVINGS', balance: '' })
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setFieldErrors({})
    try {
      await accountApi.open({
        customerId: Number(id),
        accountType: form.accountType,
        balance: Number(form.balance)
      })
      navigate(`/customers/${id}`)
    } catch (err) {
      setError(err.message)
      if (err.fields) setFieldErrors(err.fields)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h1>Open an account</h1>
      <div className="muted">For customer {id}</div>
      {error && <div className="banner">{error}</div>}

      <form onSubmit={submit} style={{ width: 600, display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div className="field">
          <label htmlFor="accountType">Account type</label>
          <select id="accountType" value={form.accountType}
                  onChange={(e) => setForm({ ...form, accountType: e.target.value })}>
            <option value="SAVINGS">Savings — 4.0% interest</option>
            <option value="CURRENT">Current — overdraft allowed</option>
          </select>
        </div>

        <div className="field">
          <label htmlFor="balance">Initial deposit</label>
          <input id="balance" value={form.balance} className={fieldErrors.balance ? 'invalid' : ''}
                 onChange={(e) => setForm({ ...form, balance: e.target.value })} placeholder="25000" />
          {fieldErrors.balance && <span className="err">{fieldErrors.balance}</span>}
        </div>

        <div style={{ display: 'flex', gap: 12 }}>
          <button type="submit" disabled={busy}>{busy ? 'Opening…' : 'Open account'}</button>
          <button type="button" className="ghost" onClick={() => navigate(`/customers/${id}`)}>Cancel</button>
        </div>
      </form>
    </>
  )
}
