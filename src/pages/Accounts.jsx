import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { accountApi } from '../api/api'

const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })

export default function Accounts() {
  const [accounts, setAccounts] = useState([])
  const [type, setType] = useState('')
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [message, setMessage] = useState(null)

  // transaction form: { account, kind: 'deposit' | 'withdraw', amount }
  const [txn, setTxn] = useState(null)
  const [busy, setBusy] = useState(false)

  const load = () => {
    setLoading(true)
    accountApi.getAll()
      .then(setAccounts)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const shown = useMemo(() => accounts.filter((a) =>
    (!type || a.accountType === type) &&
    (!search || `${a.accountNumber} ${a.customerName}`.toLowerCase().includes(search.toLowerCase()))
  ), [accounts, type, search])

  const total = shown.reduce((sum, a) => sum + (a.balance || 0), 0)

  const submitTxn = async (e) => {
    e.preventDefault()
    const amount = Number(txn.amount)
    if (!amount || amount <= 0) {
      setError('Enter an amount greater than zero')
      return
    }
    setBusy(true)
    setError(null)
    setMessage(null)
    try {
      const updated = txn.kind === 'deposit'
        ? await accountApi.deposit(txn.account.accountId, amount)
        : await accountApi.withdraw(txn.account.accountId, amount)
      setAccounts((prev) => prev.map((a) => (a.accountId === updated.accountId ? updated : a)))
      setMessage(`${txn.kind === 'deposit' ? 'Deposited' : 'Withdrew'} ${money.format(amount)} - ` +
        `${updated.accountNumber} balance is now ${money.format(updated.balance)}`)
      setTxn(null)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div>
        <h1>Accounts</h1>
        <div className="muted">{shown.length} shown · total balance {money.format(total)}</div>
      </div>

      {error && <div className="banner">{error}</div>}
      {message && <div className="ok">{message}</div>}

      <div className="row">
        <div className="field" style={{ width: 300 }}>
          <label htmlFor="acc-search">Search</label>
          <input id="acc-search" value={search} onChange={(e) => setSearch(e.target.value)}
                 placeholder="account no or customer" />
        </div>
        <div className="field" style={{ width: 200 }}>
          <label htmlFor="acc-type">Type</label>
          <select id="acc-type" value={type} onChange={(e) => setType(e.target.value)}>
            <option value="">All types</option>
            <option value="SAVINGS">Savings</option>
            <option value="CURRENT">Current</option>
          </select>
        </div>
      </div>

      {txn && (
        <form className="card" onSubmit={submitTxn}
              style={{ display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div>
            <div className="muted">{txn.kind === 'deposit' ? 'Deposit to' : 'Withdraw from'}</div>
            <strong>{txn.account.accountNumber} · {txn.account.customerName}</strong>
          </div>
          <div className="field" style={{ width: 200 }}>
            <label htmlFor="amount">Amount</label>
            <input id="amount" autoFocus value={txn.amount} placeholder="5000"
                   onChange={(e) => setTxn({ ...txn, amount: e.target.value })} />
          </div>
          <button type="submit" disabled={busy}>{busy ? 'Saving…' : 'Confirm'}</button>
          <button type="button" className="ghost" onClick={() => setTxn(null)}>Cancel</button>
        </form>
      )}

      {loading ? <p className="muted">Loading…</p> : !shown.length ? <p className="muted">No accounts match.</p> : (
        <table>
          <thead>
            <tr><th>Account no</th><th>Type</th><th>Customer</th><th>Balance</th><th>Opened</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {shown.map((a) => (
              <tr key={a.accountId}>
                <td>{a.accountNumber}</td>
                <td>{a.accountType}</td>
                <td><Link to={`/customers/${a.customerId}`}>{a.customerName}</Link></td>
                <td>{money.format(a.balance)}</td>
                <td>{a.openedOn}</td>
                <td style={{ display: 'flex', gap: 10 }}>
                  <button className="link" onClick={() => setTxn({ account: a, kind: 'deposit', amount: '' })}>Deposit</button>
                  <button className="link" onClick={() => setTxn({ account: a, kind: 'withdraw', amount: '' })}>Withdraw</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
