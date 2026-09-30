import { Routes, Route, Navigate } from 'react-router-dom'
import ProtectedRoute from './auth/ProtectedRoute.jsx'
import Layout from './components/Layout.jsx'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Customers from './pages/Customers.jsx'
import CustomerForm from './pages/CustomerForm.jsx'
import CustomerDetail from './pages/CustomerDetail.jsx'
import AccountOpen from './pages/AccountOpen.jsx'
import LoanSanction from './pages/LoanSanction.jsx'
import Accounts from './pages/Accounts.jsx'
import Loans from './pages/Loans.jsx'
import Users from './pages/Users.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/customers" element={<Customers />} />
        <Route path="/accounts" element={<Accounts />} />
        <Route path="/users"
               element={<ProtectedRoute role="ROLE_ADMIN"><Users /></ProtectedRoute>} />
        <Route path="/loans"
               element={<ProtectedRoute role={['ROLE_ADMIN', 'ROLE_OFFICER']}><Loans /></ProtectedRoute>} />
        <Route path="/customers/new" element={<CustomerForm />} />
        <Route path="/customers/:id" element={<CustomerDetail />} />
        <Route path="/customers/:id/edit" element={<CustomerForm />} />
        <Route path="/customers/:id/accounts/new" element={<AccountOpen />} />
        <Route path="/customers/:id/loans/new"
               element={<ProtectedRoute role={['ROLE_ADMIN', 'ROLE_OFFICER']}><LoanSanction /></ProtectedRoute>} />
      </Route>

      <Route path="*" element={<p style={{ padding: 30 }}>404 - page not found</p>} />
    </Routes>
  )
}
