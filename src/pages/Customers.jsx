import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { customerApi } from '../api/api'
import { useAuth } from '../auth/AuthContext.jsx'
import CustomerTable from '../components/CustomerTable.jsx'

export default function Customers() {
  const { hasRole } = useAuth()
  const [customers, setCustomers] = useState([])
  const [search, setSearch] = useState('')
  const [city, setCity] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(async (params) => {
    setLoading(true)
    setError(null)
    try {
      setCustomers(await customerApi.getAll(params))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load({}) }, [load])

  const apply = (e) => {
    e.preventDefault()
    load({ search: search || undefined, city: city || undefined })
  }

  const remove = async (id) => {
    if (!window.confirm('Delete this customer?')) return
    try {
      await customerApi.remove(id)
      setCustomers((prev) => prev.filter((c) => c.customerId !== id))
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <div className="spread">
        <div>
          <h1>Customers</h1>
          <div className="muted">{customers.length} shown</div>
        </div>
        <Link className="btn" to="/customers/new">Add customer</Link>
      </div>

      {error && <div className="banner">{error}</div>}

      <form className="row" onSubmit={apply}>
        <div className="field" style={{ width: 300 }}>
          <label htmlFor="search">Search</label>
          <input id="search" value={search} onChange={(e) => setSearch(e.target.value)}
                 placeholder="name or email" />
        </div>
        <div className="field" style={{ width: 200 }}>
          <label htmlFor="city">City</label>
          <select id="city" value={city} onChange={(e) => setCity(e.target.value)}>
            <option value="">All cities</option>
            <option>Bangalore</option>
            <option>Mumbai</option>
            <option>Chennai</option>
          </select>
        </div>
        <button className="ghost" type="submit">Apply</button>
      </form>

      {loading
        ? <p className="muted">Loading…</p>
        : <CustomerTable customers={customers} onDelete={remove} canDelete={hasRole('ROLE_ADMIN')} />}
    </>
  )
}
