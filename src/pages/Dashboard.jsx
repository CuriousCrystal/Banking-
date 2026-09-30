import { useEffect, useState } from 'react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import { reportApi } from '../api/api'

export default function Dashboard() {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    reportApi.summary().then(setData).catch((err) => setError(err.message))
  }, [])

  if (error) return <div className="banner">{error}</div>
  if (!data) return <p className="muted">Loading dashboard…</p>

  const money = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR',
    maximumFractionDigits: 0 })

  return (
    <>
      <div>
        <h1>Dashboard</h1>
        <div className="muted">Live figures from the database</div>
      </div>

      <div className="tiles">
        <div className="tile"><div className="label">Customers</div><div className="value">{data.customers}</div></div>
        <div className="tile"><div className="label">Accounts</div><div className="value">{data.accounts}</div></div>
        <div className="tile"><div className="label">Active loans</div><div className="value">{data.activeLoans}</div></div>
        <div className="tile"><div className="label">Total deposits</div>
          <div className="value" style={{ fontSize: 24 }}>{money.format(data.totalDeposits)}</div></div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 16 }}>
        <div className="card">
          <div className="muted" style={{ marginBottom: 10 }}>Loans by type</div>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={data.loansByType}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e4e2db" />
              <XAxis dataKey="label" fontSize={12} />
              <YAxis fontSize={12} allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="value" fill="#33604f" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="card">
          <div className="muted" style={{ marginBottom: 10 }}>Deposits by city</div>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={data.depositsByCity}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e4e2db" />
              <XAxis dataKey="label" fontSize={12} />
              <YAxis fontSize={12} />
              <Tooltip />
              <Bar dataKey="value" fill="#a6462a" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </>
  )
}
