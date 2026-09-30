import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { accountApi, customerApi } from '../api/api'
import { useAuth } from '../auth/AuthContext.jsx'

export default function CustomerDetail() {
  const { id } = useParams()
  const { hasRole } = useAuth()
  const isAdmin = hasRole('ROLE_ADMIN')
  const canSanction = hasRole(['ROLE_ADMIN', 'ROLE_OFFICER'])
  const [customer, setCustomer] = useState(null)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    customerApi.getById(id).then(setCustomer).catch((err) => setError(err.message))
  }, [id])

  useEffect(() => { load() }, [load])

  const close = async (accountId) => {
    if (!window.confirm('Close this account?')) return
    try {
      await accountApi.close(accountId)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  if (error) return <div className="banner">{error}</div>
  if (!customer) return <p className="muted">Loading…</p>

  return (
    <>
      <div className="spread">
        <div>
          <h1>{customer.name}</h1>
          <div className="muted">
            Customer {customer.customerId} · {customer.email} · {customer.city} · PAN {customer.panNumber || '—'}
          </div>
        </div>
        <Link className="btn ghost" to={`/customers/${id}/edit`}>Edit</Link>
      </div>

      {error && <div className="banner">{error}</div>}

      <div className="spread">
        <h2 style={{ fontSize: 15, margin: 0 }}>Accounts</h2>
        <Link className="btn" to={`/customers/${id}/accounts/new`}>Open account</Link>
      </div>
      {customer.accounts?.length ? (
        <table>
          <thead><tr><th>Account no</th><th>Type</th><th>Balance</th><th>Opened</th><th></th></tr></thead>
          <tbody>
            {customer.accounts.map((a) => (
              <tr key={a.accountId}>
                <td>{a.accountNumber}</td>
                <td>{a.accountType}</td>
                <td>{a.balance}</td>
                <td>{a.openedOn}</td>
                <td>{isAdmin && <button className="link" onClick={() => close(a.accountId)}>Close</button>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : <p className="muted">No accounts yet.</p>}

      <div className="spread">
        <h2 style={{ fontSize: 15, margin: 0 }}>Loans</h2>
        {canSanction && <Link className="btn" to={`/customers/${id}/loans/new`}>Sanction loan</Link>}
      </div>
      {customer.loans?.length ? (
        <table>
          <thead><tr><th>Loan id</th><th>Type</th><th>Principal</th><th>Rate</th><th>Tenure</th><th>EMI</th></tr></thead>
          <tbody>
            {customer.loans.map((l) => (
              <tr key={l.loanId}>
                <td>{l.loanId}</td>
                <td>{l.loanType}</td>
                <td>{l.principalAmount}</td>
                <td>{l.interestRate}%</td>
                <td>{l.tenureMonths} m</td>
                <td>{l.emi}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : <p className="muted">No loans yet.</p>}
    </>
  )
}
