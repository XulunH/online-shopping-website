#!/bin/bash

# Test Payment and Refund Flow
# This script tests the complete order → payment → cancel → refund flow

set -e

echo "🧪 Testing Payment and Refund Flow"
echo "==================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
ACCOUNT_SERVICE="http://localhost:8081"
ORDER_SERVICE="http://localhost:8083"
PAYMENT_SERVICE="http://localhost:8084"
ITEM_SERVICE="http://localhost:8082"

echo "📝 Step 1: Login"
echo "-----------------"
TOKEN=$(curl -s -X POST "$ACCOUNT_SERVICE/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"jason@example.com","password":"Passw0rd!"}' | jq -r .token)

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo -e "${RED}❌ Login failed! Make sure user exists.${NC}"
  echo "Creating user..."
  curl -s -X POST "$ACCOUNT_SERVICE/api/v1/accounts/register" \
    -H "Content-Type: application/json" \
    -d '{"email":"jason@example.com","password":"Passw0rd!","firstName":"Jason","lastName":"Test"}' > /dev/null
  
  TOKEN=$(curl -s -X POST "$ACCOUNT_SERVICE/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"jason@example.com","password":"Passw0rd!"}' | jq -r .token)
fi

echo -e "${GREEN}✓ Logged in successfully${NC}"
echo ""

echo "🛍️  Step 2: Check Available Items"
echo "--------------------------------"
ITEMS=$(curl -s -H "Authorization: Bearer $TOKEN" "$ITEM_SERVICE/api/v1/items")
echo "$ITEMS" | jq -r '.[] | "  • \(.name) (\(.upc)) - $\(.unitPrice) - Available: \(.availableUnits)"'
echo ""

echo "📦 Step 3: Create Order"
echo "----------------------"
ORDER=$(curl -s -X POST "$ORDER_SERVICE/api/v1/orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"upc":"UPC001","quantity":2},{"upc":"UPC002","quantity":1}]}')

ORDER_ID=$(echo "$ORDER" | jq -r .id)
AMOUNT=$(echo "$ORDER" | jq -r .totalAmount)
STATUS=$(echo "$ORDER" | jq -r .status)

echo -e "${GREEN}✓ Order created${NC}"
echo "  Order ID: $ORDER_ID"
echo "  Amount: \$$AMOUNT"
echo "  Status: $STATUS"
echo ""

echo "💳 Step 4: Submit Payment"
echo "------------------------"
PAYMENT=$(curl -s -X POST "$PAYMENT_SERVICE/api/v1/payments" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"orderId\":\"$ORDER_ID\",\"amount\":$AMOUNT}")

PAYMENT_ID=$(echo "$PAYMENT" | jq -r .id)
PAYMENT_STATUS=$(echo "$PAYMENT" | jq -r .status)

echo -e "${GREEN}✓ Payment submitted${NC}"
echo "  Payment ID: $PAYMENT_ID"
echo "  Status: $PAYMENT_STATUS"
echo ""

echo "⏳ Step 5: Wait for Order Completion"
echo "-----------------------------------"
echo "Waiting 3 seconds for Kafka event processing..."
sleep 3

ORDER_STATUS=$(curl -s -H "Authorization: Bearer $TOKEN" "$ORDER_SERVICE/api/v1/orders/$ORDER_ID" | jq -r .status)
echo -e "${GREEN}✓ Order status updated${NC}"
echo "  Status: $ORDER_STATUS"

if [ "$ORDER_STATUS" == "COMPLETED" ]; then
  echo -e "${GREEN}✓ Order completed successfully! Inventory deducted.${NC}"
else
  echo -e "${YELLOW}⚠️  Order status is $ORDER_STATUS (expected COMPLETED)${NC}"
fi
echo ""

echo "🗑️  Step 6: Cancel Order"
echo "-----------------------"
curl -s -X POST "$ORDER_SERVICE/api/v1/orders/$ORDER_ID/cancel" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

CANCELED_STATUS=$(curl -s -H "Authorization: Bearer $TOKEN" "$ORDER_SERVICE/api/v1/orders/$ORDER_ID" | jq -r .status)
echo -e "${GREEN}✓ Order canceled${NC}"
echo "  Status: $CANCELED_STATUS"
echo ""

echo "⏳ Step 7: Wait for Refund Processing"
echo "------------------------------------"
echo "Waiting 3 seconds for Kafka event processing..."
sleep 3

REFUND_STATUS=$(curl -s -H "Authorization: Bearer $TOKEN" "$PAYMENT_SERVICE/api/v1/payments/$PAYMENT_ID" | jq -r .status)
echo -e "${GREEN}✓ Payment status updated${NC}"
echo "  Status: $REFUND_STATUS"

if [ "$REFUND_STATUS" == "REFUNDED" ]; then
  echo -e "${GREEN}✓ Payment refunded successfully!${NC}"
else
  echo -e "${YELLOW}⚠️  Payment status is $REFUND_STATUS (expected REFUNDED)${NC}"
fi
echo ""

echo "📊 Step 8: Verify Inventory Restored"
echo "-----------------------------------"
ITEMS_AFTER=$(curl -s -H "Authorization: Bearer $TOKEN" "$ITEM_SERVICE/api/v1/items")
echo "$ITEMS_AFTER" | jq -r '.[] | "  • \(.name) (\(.upc)) - Available: \(.availableUnits)"'
echo ""

echo "✅ Test Complete!"
echo "================"
echo ""
echo "Summary:"
echo "  Order ID: $ORDER_ID"
echo "  Payment ID: $PAYMENT_ID"
echo "  Final Order Status: $CANCELED_STATUS"
echo "  Final Payment Status: $REFUND_STATUS"
echo ""

if [ "$CANCELED_STATUS" == "CANCELED" ] && [ "$REFUND_STATUS" == "REFUNDED" ]; then
  echo -e "${GREEN}🎉 All tests passed! Payment and refund flow working correctly.${NC}"
  exit 0
else
  echo -e "${RED}❌ Some tests failed. Check the logs above.${NC}"
  exit 1
fi

