import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { getToken } from '../auth'
import { useNavigate } from 'react-router-dom'

type ItemDto = {
  id: string
  upc: string
  name: string
  unitPrice: number
  availableUnits: number
}

export default function CreateOrder() {
  const [items, setItems] = useState<ItemDto[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [quantities, setQuantities] = useState<Record<string, number>>({})
  const [creating, setCreating] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    let ignore = false
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const data = await api<ItemDto[]>('/api/v1/items')
        if (!ignore) setItems(data)
      } catch (err: any) {
        if (!ignore) setError(err.message || 'Failed to load items')
      } finally {
        if (!ignore) setLoading(false)
      }
    }
    load()
    return () => { ignore = true }
  }, [])

  function setQty(upc: string, qty: number) {
    setQuantities(prev => ({ ...prev, [upc]: Math.max(0, Math.floor(qty) || 0) }))
  }

  async function finishCreateOrder() {
    const token = getToken()
    if (!token) {
      navigate('/login')
      return
    }
    const itemsReq = Object.entries(quantities)
      .filter(([, q]) => q > 0)
      .map(([upc, quantity]) => ({ upc, quantity }))
    if (itemsReq.length === 0) {
      setError('Please choose at least one item quantity > 0')
      return
    }
    try {
      setCreating(true)
      setError(null)
      const order = await api<{ id: string }>('/api/v1/orders', {
        method: 'POST',
        body: JSON.stringify({ items: itemsReq })
      })
      navigate(`/orders/${order.id}`)
    } catch (e: any) {
      setError(e.message || 'Failed to create order')
    } finally {
      setCreating(false)
    }
  }

  if (loading) return <div>Loading items...</div>
  if (error) return <div style={{ color: 'red' }}>{error}</div>

  // Calculate totals
  const totalItems = Object.values(quantities).reduce((sum, qty) => sum + qty, 0)
  const totalAmount = items.reduce((sum, item) => {
    const qty = quantities[item.upc] || 0
    return sum + (item.unitPrice * qty)
  }, 0)

  return (
    <div>
      <h2>Create New Order</h2>
      <p style={{ color: '#666' }}>Select items and quantities to add to your order</p>
      
      <table style={{ borderCollapse: 'collapse', width: '100%', marginTop: 16 }}>
        <thead>
          <tr>
            <th style={{ borderBottom: '2px solid #007bff', textAlign: 'left', padding: 8, background: '#f8f9fa' }}>Item</th>
            <th style={{ borderBottom: '2px solid #007bff', textAlign: 'left', padding: 8, background: '#f8f9fa' }}>UPC</th>
            <th style={{ borderBottom: '2px solid #007bff', textAlign: 'right', padding: 8, background: '#f8f9fa' }}>Price</th>
            <th style={{ borderBottom: '2px solid #007bff', textAlign: 'center', padding: 8, background: '#f8f9fa' }}>Available</th>
            <th style={{ borderBottom: '2px solid #007bff', textAlign: 'center', padding: 8, background: '#f8f9fa' }}>Quantity</th>
            <th style={{ borderBottom: '2px solid #007bff', textAlign: 'right', padding: 8, background: '#f8f9fa' }}>Subtotal</th>
          </tr>
        </thead>
        <tbody>
          {items.map(it => {
            const qty = quantities[it.upc] ?? 0
            const subtotal = it.unitPrice * qty
            return (
              <tr key={it.id} style={{ background: qty > 0 ? '#e7f3ff' : 'white' }}>
                <td style={{ borderBottom: '1px solid #eee', padding: 8 }}>{it.name}</td>
                <td style={{ borderBottom: '1px solid #eee', padding: 8, fontSize: 12, color: '#666' }}>{it.upc}</td>
                <td style={{ borderBottom: '1px solid #eee', padding: 8, textAlign: 'right' }}>${Number(it.unitPrice).toFixed(2)}</td>
                <td style={{ borderBottom: '1px solid #eee', padding: 8, textAlign: 'center', color: it.availableUnits > 0 ? 'green' : 'red' }}>
                  {it.availableUnits}
                </td>
                <td style={{ borderBottom: '1px solid #eee', padding: 8, textAlign: 'center' }}>
                  <input
                    type="number"
                    min={0}
                    max={it.availableUnits}
                    value={qty}
                    onChange={e => setQty(it.upc, Number(e.target.value))}
                    style={{ width: 80, padding: 4, textAlign: 'center', border: '1px solid #ddd', borderRadius: 4 }}
                  />
                </td>
                <td style={{ borderBottom: '1px solid #eee', padding: 8, textAlign: 'right', fontWeight: qty > 0 ? 'bold' : 'normal' }}>
                  ${subtotal.toFixed(2)}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>

      <div style={{ marginTop: 20, padding: 16, background: '#f8f9fa', borderRadius: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ fontSize: 14, color: '#666' }}>Total Items: <strong>{totalItems}</strong></div>
          <div style={{ fontSize: 20, fontWeight: 'bold', color: '#007bff', marginTop: 4 }}>
            Total Amount: ${totalAmount.toFixed(2)}
          </div>
        </div>
        <button 
          onClick={finishCreateOrder} 
          disabled={creating || totalItems === 0}
          style={{ 
            background: totalItems > 0 ? '#28a745' : '#ccc', 
            color: 'white', 
            border: 'none', 
            padding: '12px 24px', 
            borderRadius: 4, 
            cursor: totalItems > 0 ? 'pointer' : 'not-allowed',
            fontSize: 16,
            fontWeight: 'bold'
          }}
        >
          {creating ? '⏳ Creating...' : totalItems > 0 ? '✓ Create Order' : 'Select Items'}
        </button>
      </div>
      
      {totalItems === 0 && (
        <div style={{ marginTop: 12, padding: 12, background: '#fff3cd', color: '#856404', borderRadius: 4, textAlign: 'center' }}>
          ℹ️ Please select at least one item to create an order
        </div>
      )}
    </div>
  )
}



