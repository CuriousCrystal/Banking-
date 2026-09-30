import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { loanApi } from '../api/api'

export default function LoanSanction() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    loanType: 'HOME', principalAmount: '', interestRate: '8.4', tenureMonths: '240'
  })
  const [fieldErrors, setFieldErrors] = useState({})
  const [error, setError] = useState(null)
  const [result, setResult] = useState(null)
  const [busy, setBusy] = useState(false)

  const change = (e) => setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setFieldErrors({})
    setResult(null)
    try {
      const loan = await loanApi.sanction({
        customerId: Number(id),
        loanType: form.loanType,
        principalAmount: Number(form.principalAmount),
        interestRate: Number(form.interestRate),
        tenureMonths: Number(form.tenureMonths)
      })
      setResult(loan)
    } catch (err) {
      setError(err.message)
      if (err.fields) setFieldErrors(err.fields)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <h1>Sanction a loan</h1>
      <div className="muted">For customer {id}</div>

      <div style={{ display: 'flex', gap: 22, alignItems: 'flex-start' }}>
        <form onSubmit={submit} style={{ width: 460, display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="field">
            <label htmlFor="loanType">Loan type</label>
            <select id="loanType" name="loanType" value={form.loanType} onChange={change}>
              <option>HOME</option><option>CAR</option><option>PERSONAL</option>
            </select>
          </div>

          {[['principalAmount', 'Principal amount', '500000'],
            ['interestRate', 'Interest rate (%)', '8.4'],
            ['tenureMonths', 'Tenure (months)', '240']].map(([n, label, ph]) => (
            <div className="field" key={n}>
              <label htmlFor={n}>{label}</label>
              <input id={n} name={n} value={form[n]} onChange={change} placeholder={ph}
                     className={fieldErrors[n] ? 'invalid' : ''} />
              {fieldErrors[n] && <span className="err">{fieldErrors[n]}</span>}
            </div>
          ))}

          <div style={{ display: 'flex', gap: 12 }}>
            <button type="submit" disabled={busy}>{busy ? 'Checking…' : 'Sanction'}</button>
            <button type="button" className="ghost" onClick={() => navigate(`/customers/${id}`)}>Cancel</button>
          </div>
        </form>

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 14 }}>
          {result && (
            <div className="card">
              <div className="muted">Monthly EMI</div>
              <div style={{ fontSize: 34, fontWeight: 700, margin: '6px 0' }}>{result.emi}</div>
              <div className="muted">Loan {result.loanId} · {result.loanType} · sanctioned {result.sanctionedOn}</div>
              <p><button className="ghost" onClick={() => navigate(`/customers/${id}`)}>Back to customer</button></p>
            </div>
          )}

          {error && <div className="banner">{error}</div>}

          <div className="card">
            <div className="muted" style={{ marginBottom: 6 }}>Rules the server enforces</div>
            <ul style={{ margin: 0, paddingLeft: 18, fontSize: 13, lineHeight: 1.7 }}>
              <li>Maximum 3 active loans per customer</li>
              <li>Customer must hold 10% of the principal across accounts</li>
              <li>Minimum principal 10,000 · tenure 6 to 360 months</li>
            </ul>
          </div>
        </div>
      </div>
    </>
  )
}
