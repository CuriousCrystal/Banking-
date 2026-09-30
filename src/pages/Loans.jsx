import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { loanApi } from '../api/api'

const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })

export default function Loans() {
  const [loans, setLoans] = useState([])
  const [type, setType] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    loanApi.getAll()
      .then(setLoans)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  const shown = useMemo(() => loans.filter((l) => !type || l.loanType === type), [loans, type])
  const principal = shown.reduce((sum, l) => sum + (l.principalAmount || 0), 0)
  const emi = shown.reduce((sum, l) => sum + (l.emi || 0), 0)

  return (
    <>
      <div>
        <h1>Loans</h1>
        <div className="muted">{shown.length} shown</div>
      </div>

      {error && <div className="banner">{error}</div>}

      <div className="tiles">
        <div className="tile"><div className="label">Loans</div><div className="value">{shown.length}</div></div>
        <div className="tile"><div className="label">Total principal</div>
          <div className="value" style={{ fontSize: 24 }}>{money.format(principal)}</div></div>
        <div className="tile"><div className="label">Monthly EMI collection</div>
          <div className="value" style={{ fontSize: 24 }}>{money.format(emi)}</div></div>
      </div>

      <div className="row">
        <div className="field" style={{ width: 200 }}>
          <label htmlFor="loan-type">Loan type</label>
          <select id="loan-type" value={type} onChange={(e) => setType(e.target.value)}>
            <option value="">All types</option>
            <option>HOME</option><option>CAR</option><option>PERSONAL</option>
          </select>
        </div>
      </div>

      {loading ? <p className="muted">Loading…</p> : !shown.length ? <p className="muted">No loans match.</p> : (
        <table>
          <thead>
            <tr><th>Loan id</th><th>Customer</th><th>Type</th><th>Principal</th><th>Rate</th>
              <th>Tenure</th><th>EMI</th><th>Sanctioned</th></tr>
          </thead>
          <tbody>
            {shown.map((l) => (
              <tr key={l.loanId}>
                <td>{l.loanId}</td>
                <td><Link to={`/customers/${l.customerId}`}>{l.customerName}</Link></td>
                <td>{l.loanType}</td>
                <td>{money.format(l.principalAmount)}</td>
                <td>{l.interestRate}%</td>
                <td>{l.tenureMonths} m</td>
                <td>{money.format(l.emi)}</td>
                <td>{l.sanctionedOn}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
