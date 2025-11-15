import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { getToken } from '../auth'

type OrderItem = {
  itemId: string
  upc: string
  name: string
  unitPrice: number
  quantity: number
}

type OrderResponse = {
  id: string
  accountEmail: string
  status: string
  totalAmount: number
  items: OrderItem[]
  createdAt: string
  updatedAt: string
}

export default function MyOrders() {
  const navigate = useNavigate()
  const [orders, setOrders] = useState<OrderResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let ignore = false
    async function load() {
      const token = getToken()
      if (!token) {
        navigate('/login')
        return
      }

      try {
        setLoading(true)
        setError(null)
        const data = await api<OrderResponse[]>('/api/v1/orders')
        if (!ignore) {
          // Sort by created date, newest first
          data.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          setOrders(data)
        }
      } catch (e: any) {
        if (!ignore) setError(e.message || 'Failed to load orders')
      } finally {
        if (!ignore) setLoading(false)
      }
    }
    load()
    return () => { ignore = true }
  }, [navigate])

  if (loading) return <div>Loading your orders...</div>
  if (error) return <div style={{ color: 'red' }}>{error}</div>

  const statusColor = (status: string) => 
    status === 'COMPLETED' ? 'green' :
    status === 'CANCELED' ? 'red' :
    status === 'CREATED' ? 'orange' : 'black'

  return (
    <div>
      <h2>My Orders</h2>
      {orders.length === 0 ? (
        <div style={{ padding: 20, textAlign: 'center', color: '#666' }}>
          <p>You haven't placed any orders yet.</p>
          <button 
            onClick={() => navigate('/orders/new')} 
            style={{ marginTop: 12, background: '#4CAF50', color: 'white', border: 'none', padding: '10px 20px', borderRadius: 4, cursor: 'pointer' }}
          >
            Create Your First Order
          </button>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {orders.map(order => {
            const totalQty = order.items.reduce((s, it) => s + it.quantity, 0)
            return (
              <div 
                key={order.id} 
                style={{ 
                  border: '1px solid #ddd', 
                  borderRadius: 8, 
                  padding: 16, 
                  cursor: 'pointer',
                  transition: 'box-shadow 0.2s',
                  background: '#fafafa'
                }}
                onClick={() => navigate(`/orders/${order.id}`)}
                onMouseEnter={(e) => e.currentTarget.style.boxShadow = '0 4px 8px rgba(0,0,0,0.1)'}
                onMouseLeave={(e) => e.currentTarget.style.boxShadow = 'none'}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                  <div>
                    <div style={{ fontSize: 14, color: '#666' }}>
                      Order #{order.id.substring(0, 8)}...
                    </div>
                    <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>
                      {new Date(order.createdAt).toLocaleString()}
                    </div>
                  </div>
                  <div style={{ 
                    background: statusColor(order.status), 
                    color: 'white', 
                    padding: '4px 12px', 
                    borderRadius: 12, 
                    fontSize: 12, 
                    fontWeight: 'bold' 
                  }}>
                    {order.status}
                  </div>
                </div>

                <div style={{ marginBottom: 8 }}>
                  <strong>{totalQty}</strong> item{totalQty !== 1 ? 's' : ''} · <strong>${Number(order.totalAmount).toFixed(2)}</strong>
                </div>

                <div style={{ fontSize: 14, color: '#666' }}>
                  {order.items.slice(0, 2).map((item, idx) => (
                    <div key={idx}>• {item.name} x {item.quantity}</div>
                  ))}
                  {order.items.length > 2 && (
                    <div>... and {order.items.length - 2} more</div>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

