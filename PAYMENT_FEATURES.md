# Payment and Refund Features - User Guide

## Overview
The frontend now includes complete payment and refund functionality with a modern, intuitive UI.

## New Features

### 1. **Enhanced Order Details Page** (`/orders/:id`)
- **Payment Status Display**: Shows payment information if the order has been paid
- **Pay Now Button**: Allows users to pay for CREATED orders
- **Cancel Order Button**: Cancel unpaid or paid orders
- **Visual Status Indicators**: Color-coded status badges for both orders and payments
- **Auto-refresh**: Automatically refreshes order status after payment (2s delay)
- **Refund Notification**: Shows when a payment has been refunded after order cancellation

**Payment Status Colors:**
- 🟢 SUCCESS (green) - Payment completed
- 🔵 REFUNDED (blue) - Payment refunded
- 🔴 FAILED (red) - Payment failed
- ⚫ PENDING (gray) - Payment in progress

**Order Status Colors:**
- 🟢 COMPLETED (green) - Order completed, inventory deducted
- 🔴 CANCELED (red) - Order canceled
- 🟠 CREATED (orange) - Order created, awaiting payment

### 2. **My Orders Page** (`/my-orders`)
- View all orders in one place
- Click any order to view details
- Sort by creation date (newest first)
- Quick status overview
- Shows item count and total amount
- Empty state with "Create First Order" button

### 3. **Enhanced Create Order Page** (`/orders/new`)
- Real-time subtotal calculation
- Total items and amount display
- Visual feedback for selected items (highlighted rows)
- Available inventory display
- Input validation (max = available units)
- Disabled state when no items selected
- Auto-redirect to order details after creation

### 4. **Improved Navigation**
- Modern navbar with icons
- "My Orders" link for authenticated users
- Better visual hierarchy
- Responsive layout

## User Flow

### Complete Order Flow
```
1. Browse Items (/items)
   ↓
2. Create Order (/orders/new)
   - Select items and quantities
   - Review total amount
   ↓
3. Order Details (/orders/:id)
   - Order status: CREATED
   - Click "Pay Now"
   ↓
4. Payment Processing
   - Payment submitted to payment-service
   - PaymentSucceeded event published to Kafka
   ↓
5. Order Completion (automatic via Kafka)
   - Order-service receives PaymentSucceeded event
   - Order status → COMPLETED
   - Inventory deducted
   - Page auto-refreshes to show new status
```

### Cancel Order Flow
```
1. View Order (/orders/:id)
   - Order status: CREATED or COMPLETED
   ↓
2. Click "Cancel Order"
   - Confirm cancellation
   ↓
3. Order Cancellation
   - Order status → CANCELED
   - OrderCancelled event published to Kafka
   ↓
4. Payment Refund (automatic via Kafka)
   - Payment-service receives OrderCancelled event
   - Payment status → REFUNDED
   - Page auto-refreshes to show refund status
```

## API Endpoints Used

### Order Service (`http://localhost:8083`)
- `GET /api/v1/orders` - List all orders for current user
- `GET /api/v1/orders/:id` - Get order details
- `POST /api/v1/orders` - Create new order
- `PUT /api/v1/orders/:id` - Update order (only CREATED orders)
- `POST /api/v1/orders/:id/cancel` - Cancel order

### Payment Service (`http://localhost:8084`)
- `POST /api/v1/payments` - Submit payment
- `GET /api/v1/payments/:id` - Get payment details
- `GET /api/v1/payments/by-order?orderId=:id` - Get payment by order ID
- `POST /api/v1/payments/:id/refund` - Manual refund (not exposed in UI)

## Testing the Features

### Prerequisites
1. Start all services: `docker-compose up -d`
2. Verify all services are healthy:
   ```bash
   docker-compose ps
   ```
3. Access frontend: `http://localhost` (or appropriate port)

### Test Scenario 1: Create and Pay for Order
```bash
# 1. Register/Login
# 2. Navigate to "Create Order"
# 3. Select items (e.g., Laptop x1, Mouse x2)
# 4. Click "Create Order"
# 5. On order details page, click "Pay Now"
# 6. Wait 2 seconds, order status should change to COMPLETED
# 7. Check "My Orders" to see all orders
```

### Test Scenario 2: Cancel Paid Order (with Refund)
```bash
# 1. Create and pay for an order (follow scenario 1)
# 2. Click "Cancel Order"
# 3. Confirm cancellation
# 4. Order status → CANCELED
# 5. Wait 2 seconds, payment status should show REFUNDED
```

### Test Scenario 3: Cancel Unpaid Order
```bash
# 1. Create an order but DON'T pay
# 2. Click "Cancel Order"
# 3. Confirm cancellation
# 4. Order status → CANCELED
# 5. No payment to refund
```

### Test Scenario 4: View Order History
```bash
# 1. Create multiple orders with different statuses
# 2. Navigate to "My Orders"
# 3. See all orders sorted by date
# 4. Click any order to view details
```

## Technical Implementation

### State Management
- React hooks (`useState`, `useEffect`) for local state
- Automatic refresh after async operations
- Optimistic UI updates with error handling

### Error Handling
- User-friendly error messages
- Network error handling
- Invalid state prevention (e.g., can't pay for canceled order)

### Event-Driven Architecture
- **Payment → Order**: `PaymentSucceeded` event completes order
- **Order → Payment**: `OrderCancelled` event triggers refund
- Kafka topics: `payment.events`, `order.events`

### Security
- JWT authentication for all authenticated routes
- Authorization header automatically added to API calls
- Protected routes redirect to login

## UI/UX Features

### Visual Feedback
- Loading states for all async operations
- Disabled buttons during processing
- Color-coded status indicators
- Hover effects on interactive elements

### Responsive Design
- Flexible layouts
- Readable on all screen sizes
- Touch-friendly button sizes

### Accessibility
- Clear labels and status indicators
- Confirmation dialogs for destructive actions
- Keyboard navigation support

## Troubleshooting

### Payment Not Completing Order
1. Check order-service logs for Kafka errors
2. Verify `payment.events` topic exists: 
   ```bash
   docker exec -it online-shopping-website-kafka-1 \
     /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
   ```
3. Check consumer configuration in `order-service/src/main/resources/application.properties`

### Refund Not Processing
1. Check payment-service logs for Kafka errors
2. Verify `order.events` topic exists
3. Check consumer configuration in `payment-service/src/main/resources/application.properties`

### Frontend Not Updating
1. Hard refresh browser (Cmd+Shift+R or Ctrl+Shift+R)
2. Clear browser cache
3. Rebuild frontend: `cd frontend && npm run build`

## File Changes Summary

### New Files
- `frontend/src/pages/MyOrders.tsx` - Orders list page

### Modified Files
- `frontend/src/pages/Order.tsx` - Added payment and cancellation features
- `frontend/src/pages/CreateOrder.tsx` - Enhanced UI with totals and validation
- `frontend/src/App.tsx` - Added routes and improved navigation

### Backend Files (Reference)
- `order-service/src/main/resources/application.properties` - Kafka consumer config
- `payment-service/src/main/resources/application.properties` - Kafka consumer config
- `order-service/src/main/java/com/xulunh/orderservice/events/PaymentEventsListener.java` - Listens for PaymentSucceeded
- `payment-service/src/main/java/com/xulunh/paymentservice/events/OrderEventsListener.java` - Listens for OrderCancelled

## Next Steps (Optional Enhancements)

1. **Payment History Page**: Dedicated page showing all payments
2. **Refund Requests**: Manual refund button for admin users
3. **Real-time Updates**: WebSocket for live status updates
4. **Payment Methods**: Support multiple payment methods (credit card, PayPal, etc.)
5. **Order Search**: Filter and search orders by date, status, amount
6. **Receipt Generation**: Download PDF receipts for completed orders
7. **Email Notifications**: Send emails on payment success/refund
8. **Analytics Dashboard**: Show order and payment statistics

---

**Documentation Last Updated**: November 15, 2025
**Version**: 1.0.0

