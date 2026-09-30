import { Link } from 'react-router-dom'

export default function CustomerTable({ customers, onDelete, canDelete }) {
  if (!customers.length) return <p className="muted">No customers match this filter.</p>

  return (
    <table>
      <thead>
        <tr>
          <th>Id</th><th>Name</th><th>Email</th><th>City</th><th>Accounts</th><th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {customers.map((c) => (
          <tr key={c.customerId}>
            <td>{c.customerId}</td>
            <td>{c.name}</td>
            <td>{c.email}</td>
            <td>{c.city}</td>
            <td>{c.accountCount}</td>
            <td style={{ display: 'flex', gap: 10 }}>
              <Link to={`/customers/${c.customerId}`}>View</Link>
              <Link to={`/customers/${c.customerId}/edit`}>Edit</Link>
              {canDelete && (
                <button className="link" onClick={() => onDelete(c.customerId)}>Delete</button>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
