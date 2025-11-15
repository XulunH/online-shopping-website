import { BrowserRouter, Routes, Route, Link, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import Register from './pages/Register'
import Profile from './pages/Profile'
import Items from './pages/Items'
import Order from './pages/Order'
import CreateOrder from './pages/CreateOrder'
import MyOrders from './pages/MyOrders'
import { getToken, clearToken } from './auth'

function Protected({ children }: { children: JSX.Element }) {
  const token = getToken()
  if (!token) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  const token = getToken()
  return (
    <BrowserRouter>
      <nav style={{ display: 'flex', gap: 12, padding: 12, borderBottom: '1px solid #ddd', background: '#f8f9fa', alignItems: 'center' }}>
        <Link to="/" style={{ textDecoration: 'none', color: '#007bff', fontWeight: 'bold' }}>🏠 Home</Link>
        <Link to="/items" style={{ textDecoration: 'none', color: '#007bff' }}>🛍️ Items</Link>
        {token && <Link to="/my-orders" style={{ textDecoration: 'none', color: '#007bff' }}>📦 My Orders</Link>}
        {token && <Link to="/orders/new" style={{ textDecoration: 'none', color: '#28a745' }}>➕ Create Order</Link>}
        <div style={{ marginLeft: 'auto', display: 'flex', gap: 12, alignItems: 'center' }}>
          {!token && <Link to="/login" style={{ textDecoration: 'none', color: '#007bff' }}>Login</Link>}
          {!token && <Link to="/register" style={{ textDecoration: 'none', color: '#007bff' }}>Register</Link>}
          {token && <Link to="/profile" style={{ textDecoration: 'none', color: '#007bff' }}>👤 Profile</Link>}
          {token && (
            <button onClick={() => { clearToken(); location.href = '/login' }} style={{ background: '#dc3545', color: 'white', border: 'none', padding: '6px 12px', borderRadius: 4, cursor: 'pointer' }}>
              Logout
            </button>
          )}
        </div>
      </nav>
      <div style={{ padding: 16 }}>
        <Routes>
          <Route path="/" element={
            <div style={{ textAlign: 'center', padding: 40 }}>
              <h1>🛒 Online Shopping Platform</h1>
              <p style={{ color: '#666', marginBottom: 24 }}>Microservices Architecture Demo</p>
              <div style={{ display: 'flex', justifyContent: 'center', gap: 16 }}>
                <Link to="/items" style={{ background: '#007bff', color: 'white', padding: '12px 24px', borderRadius: 4, textDecoration: 'none' }}>
                  Browse Items
                </Link>
                {!token && (
                  <Link to="/register" style={{ background: '#28a745', color: 'white', padding: '12px 24px', borderRadius: 4, textDecoration: 'none' }}>
                    Get Started
                  </Link>
                )}
                {token && (
                  <Link to="/my-orders" style={{ background: '#28a745', color: 'white', padding: '12px 24px', borderRadius: 4, textDecoration: 'none' }}>
                    View My Orders
                  </Link>
                )}
              </div>
            </div>
          } />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/items" element={<Items />} />
          <Route
            path="/my-orders"
            element={
              <Protected>
                <MyOrders />
              </Protected>
            }
          />
          <Route
            path="/orders/new"
            element={
              <Protected>
                <CreateOrder />
              </Protected>
            }
          />
          <Route
            path="/orders/:id"
            element={
              <Protected>
                <Order />
              </Protected>
            }
          />
          <Route
            path="/profile"
            element={
              <Protected>
                <Profile />
              </Protected>
            }
          />
        </Routes>
      </div>
    </BrowserRouter>
  )
}


