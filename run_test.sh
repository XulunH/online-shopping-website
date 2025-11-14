#!/usr/bin/env bash
set -euo pipefail

services=(account-service item-service order-service payment-service)

for svc in "${services[@]}"; do
  echo "============================================================"
  echo "==> Running tests in $svc"
  echo "============================================================"
  (cd "$svc" && chmod +x mvnw && ./mvnw -q clean verify)
done

echo ""
echo "All tests finished."
echo "JaCoCo coverage reports (HTML):"
for svc in "${services[@]}"; do
  echo " - $svc/target/site/jacoco/index.html"
done

echo ""
echo "Surefire test reports:"
for svc in "${services[@]}"; do
  echo " - $svc/target/surefire-reports/"
done