import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'

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

type PaymentResponse = {
  id: string
  orderId: string
  status: string
  amount: number
  accountEmail: string
  createdAt: string
  updatedAt: string
}

export default function Order() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [order, setOrder] = useState<OrderResponse | null>(null)
  const [payment, setPayment] = useState<PaymentResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [edit, setEdit] = useState(false)
  const [quantities, setQuantities] = useState<Record<string, number>>({})
  const [saving, setSaving] = useState(false)
  const [paying, setPaying] = useState(false)
  const [canceling, setCanceling] = useState(false)
  const [showPaymentDialog, setShowPaymentDialog] = useState(false)
  const [paymentAmount, setPaymentAmount] = useState<number>(0)

  useEffect(() => {
    let ignore = false
    async function load() {
      if (!id) return
      setLoading(true)
      setError(null)
      try {
        const data = await api<OrderResponse>(`/api/v1/orders/${id}`)
        if (ignore) return
        setOrder(data)
        const q: Record<string, number> = {}
        data.items.forEach(it => { q[it.upc] = it.quantity })
        setQuantities(q)
        
        // Try to load payment info
        try {
          const paymentData = await api<PaymentResponse>(`/api/v1/payments/by-order?orderId=${id}`)
          if (!ignore) setPayment(paymentData)
        } catch {
          // Payment doesn't exist yet, that's ok
          if (!ignore) setPayment(null)
        }
      } catch (e: any) {
        if (!ignore) setError(e.message || 'Failed to load order')
      } finally {
        if (!ignore) setLoading(false)
      }
    }
    load()
    return () => { ignore = true }
  }, [id])

  function setQty(upc: string, qty: number) {
    setQuantities(prev => ({ ...prev, [upc]: Math.max(0, Math.floor(qty) || 0) }))
  }

  async function saveUpdate() {
    if (!id || !order) return
    const itemsReq = Object.entries(quantities)
      .filter(([, q]) => q > 0)
      .map(([upc, quantity]) => ({ upc, quantity }))
    if (itemsReq.length === 0) {
      setError('Order must have at least one item')
      return
    }
    try {
      setSaving(true)
      setError(null)
      const updated = await api<OrderResponse>(`/api/v1/orders/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ items: itemsReq })
      })
      setOrder(updated)
      setEdit(false)
    } catch (e: any) {
      setError(e.message || 'Failed to update order')
    } finally {
      setSaving(false)
    }
  }

  function openPaymentDialog() {
    if (!order) return
    setPaymentAmount(order.totalAmount)
    setShowPaymentDialog(true)
    setError(null)
  }

  async function submitPayment() {
    if (!id || !order) return
    if (paymentAmount <= 0) {
      setError('Payment amount must be greater than 0')
      return
    }
    try {
      setPaying(true)
      setError(null)
      const paymentData = await api<PaymentResponse>('/api/v1/payments', {
        method: 'POST',
        body: JSON.stringify({
          orderId: id,
          amount: paymentAmount
        })
      })
      setPayment(paymentData)
      setShowPaymentDialog(false)
      // Reload order to get updated status
      setTimeout(async () => {
        try {
          const refreshed = await api<OrderResponse>(`/api/v1/orders/${id}`)
          setOrder(refreshed)
        } catch {}
      }, 2000)
    } catch (e: any) {
      setError(e.message || 'Payment failed')
    } finally {
      setPaying(false)
    }
  }

  async function cancelOrder() {
    if (!id || !order) return
    if (!confirm('Are you sure you want to cancel this order?')) return
    try {
      setCanceling(true)
      setError(null)
      const updated = await api<OrderResponse>(`/api/v1/orders/${id}/cancel`, {
        method: 'POST'
      })
      setOrder(updated)
      // Reload payment to check if refunded
      if (payment) {
        setTimeout(async () => {
          try {
            const refreshed = await api<PaymentResponse>(`/api/v1/payments/${payment.id}`)
            setPayment(refreshed)
          } catch {}
        }, 2000)
      }
    } catch (e: any) {
      setError(e.message || 'Failed to cancel order')
    } finally {
      setCanceling(false)
    }
  }

  if (!id) return <div>Missing order id</div>
  if (loading) return <div>Loading...</div>
  if (error) return <div style={{ color: 'red' }}>{error}</div>
  if (!order) return <div>Not found</div>

  const totalQty = order.items.reduce((s, it) => s + it.quantity, 0)

  const statusColor = 
    order.status === 'COMPLETED' ? 'green' :
    order.status === 'CANCELED' ? 'red' :
    order.status === 'CREATED' ? 'orange' : 'black'

  const paymentStatusColor = 
    payment?.status === 'SUCCESS' ? 'green' :
    payment?.status === 'REFUNDED' ? 'blue' :
    payment?.status === 'FAILED' ? 'red' : 'gray'

  return (
    <div>
      <h2>Order Details</h2>
      <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, marginBottom: 16 }}>
        <div><strong>Order ID:</strong> {order.id}</div>
        <div><strong>Status:</strong> <span style={{ color: statusColor, fontWeight: 'bold' }}>{order.status}</span></div>
        <div><strong>Account:</strong> {order.accountEmail}</div>
        <div><strong>Total items:</strong> {totalQty}</div>
        <div><strong>Total Amount:</strong> ${Number(order.totalAmount).toFixed(2)}</div>
        <div><strong>Created:</strong> {new Date(order.createdAt).toLocaleString()}</div>
      </div>

      {payment && (
        <div style={{ background: '#e8f4f8', padding: 12, borderRadius: 4, marginBottom: 16 }}>
          <h3 style={{ marginTop: 0 }}>💳 Payment Information</h3>
          <div><strong>Payment ID:</strong> {payment.id}</div>
          <div><strong>Status:</strong> <span style={{ color: paymentStatusColor, fontWeight: 'bold' }}>{payment.status}</span></div>
          <div><strong>Amount:</strong> ${Number(payment.amount).toFixed(2)}</div>
          <div><strong>Paid at:</strong> {new Date(payment.createdAt).toLocaleString()}</div>
          {payment.status === 'REFUNDED' && (
            <div style={{ color: 'blue', marginTop: 8 }}>✓ This payment has been refunded</div>
          )}
        </div>
      )}

      <h3 style={{ marginTop: 16 }}>Items</h3>
      <table style={{ borderCollapse: 'collapse', width: '100%' }}>
        <thead>
          <tr>
            <th style={{ borderBottom: '1px solid #ddd', textAlign: 'left', padding: 6 }}>Name</th>
            <th style={{ borderBottom: '1px solid #ddd', textAlign: 'left', padding: 6 }}>UPC</th>
            <th style={{ borderBottom: '1px solid #ddd', textAlign: 'left', padding: 6 }}>Unit Price</th>
            <th style={{ borderBottom: '1px solid #ddd', textAlign: 'left', padding: 6 }}>Quantity</th>
          </tr>
        </thead>
        <tbody>
          {order.items.map(it => (
            <tr key={it.upc}>
              <td style={{ borderBottom: '1px solid #eee', padding: 6 }}>{it.name}</td>
              <td style={{ borderBottom: '1px solid #eee', padding: 6 }}>{it.upc}</td>
              <td style={{ borderBottom: '1px solid #eee', padding: 6 }}>${Number(it.unitPrice).toFixed(2)}</td>
              <td style={{ borderBottom: '1px solid #eee', padding: 6 }}>
                {edit ? (
                  <input
                    type="number"
                    min={0}
                    value={quantities[it.upc] ?? it.quantity}
                    onChange={e => setQty(it.upc, Number(e.target.value))}
                    style={{ width: 80 }}
                  />
                ) : (
                  it.quantity
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div style={{ marginTop: 16, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        {!edit && order.status === 'CREATED' && (
          <button onClick={() => setEdit(true)} style={{ background: '#4CAF50', color: 'white', border: 'none', padding: '8px 16px', borderRadius: 4, cursor: 'pointer' }}>
            📝 Update Order
          </button>
        )}
        {edit && (
          <>
            <button onClick={saveUpdate} disabled={saving} style={{ background: '#2196F3', color: 'white', border: 'none', padding: '8px 16px', borderRadius: 4, cursor: 'pointer' }}>
              {saving ? 'Saving...' : '💾 Save Update'}
            </button>
            <button onClick={() => setEdit(false)} style={{ background: '#9E9E9E', color: 'white', border: 'none', padding: '8px 16px', borderRadius: 4, cursor: 'pointer' }}>
              ✕ Cancel Edit
            </button>
          </>
        )}
        
        {!payment && order.status === 'CREATED' && !edit && (
          <button onClick={openPaymentDialog} disabled={paying} style={{ background: '#FF9800', color: 'white', border: 'none', padding: '8px 16px', borderRadius: 4, cursor: 'pointer', fontWeight: 'bold' }}>
            💳 Pay Now
          </button>
        )}

        {order.status !== 'CANCELED' && order.status !== 'COMPLETED' && !edit && (
          <button onClick={cancelOrder} disabled={canceling} style={{ background: '#f44336', color: 'white', border: 'none', padding: '8px 16px', borderRadius: 4, cursor: 'pointer' }}>
            {canceling ? 'Canceling...' : '🗑️ Cancel Order'}
          </button>
        )}

        <button onClick={() => navigate('/items')} style={{ background: '#607D8B', color: 'white', border: 'none', padding: '8px 16px', borderRadius: 4, cursor: 'pointer' }}>
          ← Back to Items
        </button>
      </div>

      {order.status === 'COMPLETED' && (
        <div style={{ marginTop: 16, padding: 12, background: '#d4edda', color: '#155724', borderRadius: 4 }}>
          ✓ This order has been completed and inventory has been deducted.
        </div>
      )}

      {order.status === 'CANCELED' && (
        <div style={{ marginTop: 16, padding: 12, background: '#f8d7da', color: '#721c24', borderRadius: 4 }}>
          ✗ This order has been canceled. {payment?.status === 'REFUNDED' ? 'Payment has been refunded.' : ''}
        </div>
      )}

      {/* Payment Dialog */}
      {showPaymentDialog && (
        <div style={{ 
          position: 'fixed', 
          top: 0, 
          left: 0, 
          right: 0, 
          bottom: 0, 
          background: 'rgba(0,0,0,0.5)', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{ 
            background: 'white', 
            padding: 24, 
            borderRadius: 8, 
            maxWidth: 400, 
            width: '90%',
            boxShadow: '0 4px 16px rgba(0,0,0,0.2)'
          }}>
            <h3 style={{ marginTop: 0 }}>💳 Submit Payment</h3>
            
            <div style={{ marginBottom: 16 }}>
              <div style={{ marginBottom: 8, fontSize: 14, color: '#666' }}>
                <strong>Order ID:</strong> {order.id.substring(0, 13)}...
              </div>
              <div style={{ marginBottom: 8, fontSize: 14, color: '#666' }}>
                <strong>Order Total:</strong> ${Number(order.totalAmount).toFixed(2)}
              </div>
            </div>

            <div style={{ marginBottom: 16 }}>
              <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
                Payment Amount ($)
              </label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                value={paymentAmount}
                onChange={(e) => setPaymentAmount(Number(e.target.value))}
                style={{ 
                  width: '100%', 
                  padding: 12, 
                  fontSize: 18,
                  border: '2px solid #007bff', 
                  borderRadius: 4,
                  boxSizing: 'border-box'
                }}
                autoFocus
              />
              <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>
                Enter the amount you want to pay
              </div>
            </div>

            {error && (
              <div style={{ padding: 12, background: '#f8d7da', color: '#721c24', borderRadius: 4, marginBottom: 16 }}>
                {error}
              </div>
            )}

            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
              <button 
                onClick={() => { setShowPaymentDialog(false); setError(null) }} 
                disabled={paying}
                style={{ 
                  background: '#6c757d', 
                  color: 'white', 
                  border: 'none', 
                  padding: '10px 20px', 
                  borderRadius: 4, 
                  cursor: 'pointer' 
                }}
              >
                Cancel
              </button>
              <button 
                onClick={submitPayment} 
                disabled={paying || paymentAmount <= 0}
                style={{ 
                  background: paymentAmount > 0 ? '#28a745' : '#ccc', 
                  color: 'white', 
                  border: 'none', 
                  padding: '10px 20px', 
                  borderRadius: 4, 
                  cursor: paymentAmount > 0 ? 'pointer' : 'not-allowed',
                  fontWeight: 'bold'
                }}
              >
                {paying ? '⏳ Processing...' : '✓ Submit Payment'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}



